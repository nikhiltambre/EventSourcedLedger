package com.wallet_service.WalletService.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wallet_service.WalletService.dto.DataObject;
import com.wallet_service.WalletService.exception.EventAlreadyExists;
import com.wallet_service.WalletService.exception.EventNotFound;
import com.wallet_service.WalletService.model.entries.AccountSnapshots;
import com.wallet_service.WalletService.model.entries.LedgerEvents;
import com.wallet_service.WalletService.repository.SnapshotRepository;
import com.wallet_service.WalletService.repository.WalletRepository;
import com.wallet_service.WalletService.util.BalanceCalculator;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class WalletServiceImplementation implements EventStore {

    private final WalletRepository walletRepository;
    private final SnapshotRepository snapshotRepository;
    private final RedissonClient redissonClient;
    private final Cache<String, DataObject> l1Cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
    private final String L2_KEY_PREFIX = "balance:";

    public WalletServiceImplementation(WalletRepository walletRepository, SnapshotRepository snapshotRepository, RedissonClient redissonClient) {
        this.walletRepository = walletRepository;
        this.snapshotRepository = snapshotRepository;
        this.redissonClient = redissonClient;
    }

    @Override
    public LedgerEvents appendEvent(LedgerEvents event) {
        Integer nextVersion = event.getVersion() + 1;
        //if next version already exists then throw Exception
        if (walletRepository.existsByAggregateIdAndVersion(event.getAggregateId(), nextVersion)) {
            throw new EventAlreadyExists("Version " + nextVersion + " already exists for aggregate " + event.getAggregateId());
        }
        LedgerEvents newEvent = new LedgerEvents(
                null,
                event.getAggregateId(),
                nextVersion,
                event.getEventType(),
                event.getPayload(),
                null,
                event.getTraceId()
        );

        LedgerEvents savedEvent;
        if (nextVersion % 50 == 0) {
            BigDecimal previousBalance = getBalance(event.getAggregateId()).getBalance();
            BigDecimal eventImpact = BalanceCalculator.calculateBalance(List.of(newEvent));
            BigDecimal currentBalance = previousBalance.add(eventImpact);
            AccountSnapshots entity = new AccountSnapshots(event.getAggregateId(), nextVersion, currentBalance);
            savedEvent = walletRepository.save(newEvent);
            snapshotRepository.save(entity);
        } else {
            savedEvent = walletRepository.save(newEvent);
        }
        //invalidatate this replica's L1 CACHE
        evictLocalCache(event.getAggregateId());
        //invalidate shared redis shared cache (L2 CACHE)
        redissonClient.getBucket(L2_KEY_PREFIX + event.getAggregateId()).delete();
        //publish to all subscribers (multiple replicas) to invalidate their L1 CACHE
        redissonClient.getTopic("cache-invalidation").publish(event.getAggregateId());
        return savedEvent;

    }

    @Override
    public List<LedgerEvents> getEvents(String aggregateId) {
        List<LedgerEvents> events = walletRepository.findByAggregateIdOrderByVersionAsc(aggregateId);
        if (events.isEmpty()) {
            throw new EventNotFound("No events found for aggregate " + aggregateId);
        }
        return events;
    }

    @Override
    public List<LedgerEvents> getEventsAfterVersion(String aggregateId, Integer afterVersion) {
        return walletRepository.findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(aggregateId, afterVersion);

    }

    @Override
    public Integer getCurrentVersion(String aggregateId) {
        return walletRepository.findFirstByAggregateIdOrderByVersionDesc(aggregateId).map(LedgerEvents::getVersion).orElse(0);
    }

    @Override
    public DataObject getBalance(String aggregateId) {
        //checking L1 (Caffeine)
        DataObject cached = l1Cache.getIfPresent(aggregateId);
        if (cached != null) {
            return cached;
        }

        //checking L2 (Redis)
        RBucket<DataObject> rBucket = redissonClient.getBucket(L2_KEY_PREFIX + aggregateId);
        DataObject redisResult = rBucket.get();
        if (redisResult != null) {
            //add redis result into L1
            l1Cache.put(aggregateId, redisResult);
            return redisResult;
        }

        //nothing in L1 and L2 -> check DB
        Optional<AccountSnapshots> snapshot = snapshotRepository.findFirstByAggregateIdOrderByVersionDesc(aggregateId);
        DataObject result;
        //no snapshot exists (full replay of events for aggregateId)
        if (snapshot.isEmpty()) {
            result = new DataObject(aggregateId, BalanceCalculator.calculateBalance(getEvents(aggregateId)));
        }
        //snapshot exists (snapshotBalance + balanceOfVersionsAfterSnapshots)
        else {
            List<LedgerEvents> eventsAfterVersion = getEventsAfterVersion(aggregateId, snapshot.get().getVersion());
            BigDecimal snapshotBalance = snapshot.get().getBalance();
            BigDecimal balanceAfterVersion = BalanceCalculator.calculateBalance(eventsAfterVersion);
            result = new DataObject(aggregateId, snapshotBalance.add(balanceAfterVersion));
        }
        l1Cache.put(aggregateId, result);
        rBucket.set(result, 24, TimeUnit.HOURS);
        return result;

    }

    public void evictLocalCache(String aggregateId) {
        l1Cache.invalidate(aggregateId);
    }

    @Override
    public Map<String, DataObject> getL1CacheContents() {
        return l1Cache.asMap();
    }
}
