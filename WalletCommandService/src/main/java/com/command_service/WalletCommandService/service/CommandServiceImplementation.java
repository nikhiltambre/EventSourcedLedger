package com.command_service.WalletCommandService.service;

import static com.command_service.WalletCommandService.util.BalanceCalculator.calculateBalance;

import com.command_service.WalletCommandService.dto.IdempotencyResponse;
import com.command_service.WalletCommandService.exception.EventAlreadyExists;
import com.command_service.WalletCommandService.exception.EventNotFound;
import com.command_service.WalletCommandService.model.entries.AccountSnapshots;
import com.command_service.WalletCommandService.model.entries.IdempotencyKeys;
import com.command_service.WalletCommandService.model.entries.LedgerEvents;
import com.command_service.WalletCommandService.repository.EventRepository;
import com.command_service.WalletCommandService.repository.IdempotencyRepository;
import com.command_service.WalletCommandService.repository.SnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommandServiceImplementation implements EventStore {
    private static final Logger log = LoggerFactory.getLogger(CommandServiceImplementation.class);
    private final EventRepository eventRepository;
    private final SnapshotRepository snapshotRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final Cache<String, BigDecimal> l1Cache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(1000).build();
    private final String L2_KEY_PREFIX = "balance:";
    private final String IDEMPOTENCY_KEY_PREFIX = "idem:";

    public CommandServiceImplementation(EventRepository eventRepository, SnapshotRepository snapshotRepository, IdempotencyRepository idempotencyRepository, RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.snapshotRepository = snapshotRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    private Optional<IdempotencyResponse<LedgerEvents>> precheckIdempotencyKeys(String idempotencyKey) {
        //redis check
        RBucket<LedgerEvents> rBucket = redissonClient.getBucket(IDEMPOTENCY_KEY_PREFIX + idempotencyKey);
        LedgerEvents redisResponse = rBucket.get();
        if (redisResponse != null) {
            return Optional.of(new IdempotencyResponse<>(redisResponse, 201));
        }
        //db check and storing to redis
        Optional<IdempotencyKeys> dbRecord = idempotencyRepository.findById(idempotencyKey);
        if (dbRecord.isPresent()) {
            try {
                IdempotencyKeys keystore = dbRecord.get();
                LedgerEvents cachedEvent = objectMapper.readValue(keystore.getResponsePayload(), LedgerEvents.class);
                rBucket.set(cachedEvent, 24, TimeUnit.HOURS);
                return Optional.of(new IdempotencyResponse<>(cachedEvent, keystore.getStatusCode()));
            } catch (
                    Exception e) {
                log.error("Failed to deserialize idempotency response payload", e);
            }
        }
        return Optional.empty();
    }

    @Override
    public IdempotencyResponse<LedgerEvents> appendEvent(LedgerEvents event, String idempotencyKey) {
        boolean hasIdempotencyKey = idempotencyKey != null && !idempotencyKey.isBlank();
        if (hasIdempotencyKey) {
            Optional<IdempotencyResponse<LedgerEvents>> existingResponse = precheckIdempotencyKeys(idempotencyKey);
            if (existingResponse.isPresent()) {
                return existingResponse.get();
            }
        }
        Integer nextVersion = event.getVersion() + 1;
        // if next version already exists then throw Exception
        if (eventRepository.existsByAggregateIdAndVersion(event.getAggregateId(), nextVersion)) {
            throw new EventAlreadyExists("Version " + nextVersion + " already exists for aggregate " + event.getAggregateId());
        }
        LedgerEvents newEvent = new LedgerEvents(null, event.getAggregateId(), nextVersion, event.getEventType(), event.getPayload(), null, event.getTraceId());

        LedgerEvents savedEvent = savingEvent(nextVersion, event, newEvent);
        //cacheInvalidation
        cacheInvalidation(event);
        if (hasIdempotencyKey) {
            keyStoreUnknownKey(idempotencyKey, savedEvent);
        }
        return new IdempotencyResponse<>(savedEvent, 201);
    }

    private LedgerEvents savingEvent(Integer nextVersion, LedgerEvents event, LedgerEvents newEvent) {
        LedgerEvents savedEvent;
        if (nextVersion % 50 == 0) {
            BigDecimal previousBalance = getBalance(event.getAggregateId());
            BigDecimal eventImpact = calculateBalance(List.of(newEvent));
            BigDecimal currentBalance = previousBalance.add(eventImpact);
            AccountSnapshots entity = new AccountSnapshots(event.getAggregateId(), nextVersion, currentBalance);
            savedEvent = eventRepository.save(newEvent);
            snapshotRepository.save(entity);
        } else {
            savedEvent = eventRepository.save(newEvent);
        }
        return savedEvent;

    }

    private void cacheInvalidation(LedgerEvents event) {
        // invalidatate this replica's L1 CACHE
        evictLocalCache(event.getAggregateId());
        // invalidate shared redis shared cache (L2 CACHE)
        redissonClient.getBucket(L2_KEY_PREFIX + event.getAggregateId()).delete();
        // publish to all subscribers (multiple replicas) to invalidate their L1
        // CACHE
        redissonClient.getTopic("cache-invalidation").publish(event.getAggregateId());

    }

    //storing idempotencyKey
    private void keyStoreUnknownKey(String idempotencyKey, LedgerEvents savedEvent) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(savedEvent);
            IdempotencyKeys keys = new IdempotencyKeys(idempotencyKey, jsonPayload, 201, LocalDateTime.now().plusHours(24));
            idempotencyRepository.save(keys);
            RBucket rBucket = redissonClient.getBucket(IDEMPOTENCY_KEY_PREFIX + idempotencyKey);
            rBucket.set(savedEvent, 24, TimeUnit.HOURS);
        } catch (
                Exception e) {
            log.error("Failed to store idempotency key record", e);
        }

    }

    @Override
    public List<LedgerEvents> getEvents(String aggregateId) {
        List<LedgerEvents> events = eventRepository.findByAggregateIdOrderByVersionAsc(aggregateId);
        if (events.isEmpty()) {
            throw new EventNotFound("No events found for aggregate " + aggregateId);
        }
        return events;
    }

    @Override
    public List<LedgerEvents> getEventsAfterVersion(String aggregateId, Integer afterVersion) {
        return eventRepository.findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(aggregateId, afterVersion);
    }

    @Override
    public BigDecimal getBalance(String aggregateId) {
        // checking L1 (Caffeine)
        BigDecimal cached = l1Cache.getIfPresent(aggregateId);
        if (cached != null) {
            return cached;
        }

        // checking L2 (Redis)
        RBucket<BigDecimal> rBucket = redissonClient.getBucket(L2_KEY_PREFIX + aggregateId);
        BigDecimal redisResult = rBucket.get();
        if (redisResult != null) {
            // add redis result into L1
            l1Cache.put(aggregateId, redisResult);
            return redisResult;
        }
        Optional<AccountSnapshots> snapshot = snapshotRepository.findFirstByAggregateIdOrderByVersionDesc(aggregateId);
        BigDecimal result;
        // no snapshot exists (full replay of events for aggregateId)
        if (snapshot.isEmpty()) {
            result = calculateBalance(getEvents(aggregateId));
        }
        // snapshot exists (snapshotBalance + balanceOfVersionsAfterSnapshots)
        else {
            List<LedgerEvents> eventsAfterVersion = getEventsAfterVersion(aggregateId, snapshot.get().getVersion());
            BigDecimal snapshotBalance = snapshot.get().getBalance();
            BigDecimal balanceAfterVersion = calculateBalance(eventsAfterVersion);
            result = snapshotBalance.add(balanceAfterVersion);
        }

        l1Cache.put(aggregateId, result);
        rBucket.set(result, 24, TimeUnit.HOURS);
        return result;
    }

    public void evictLocalCache(String aggregateId) {
        l1Cache.invalidate(aggregateId);
    }

    @Override
    public Map<String, BigDecimal> getL1CacheContents() {
        return l1Cache.asMap();
    }
}
