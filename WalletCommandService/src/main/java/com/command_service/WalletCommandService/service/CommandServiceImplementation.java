package com.command_service.WalletCommandService.service;

import static com.command_service.WalletCommandService.util.BalanceCalculator.calculateBalance;

import com.command_service.WalletCommandService.exception.EventAlreadyExists;
import com.command_service.WalletCommandService.exception.EventNotFound;
import com.command_service.WalletCommandService.model.entries.AccountSnapshots;
import com.command_service.WalletCommandService.model.entries.LedgerEvents;
import com.command_service.WalletCommandService.repository.EventRepository;
import com.command_service.WalletCommandService.repository.SnapshotRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommandServiceImplementation implements EventStore {

  private final EventRepository eventRepository;
  private final SnapshotRepository snapshotRepository;
  private final RedissonClient redissonClient;
  private final Cache<String, BigDecimal> l1Cache =
      Caffeine.newBuilder()
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .maximumSize(1000)
          .build();
  private final String L2_KEY_PREFIX = "balance:";

  public CommandServiceImplementation(EventRepository eventRepository,
                                      SnapshotRepository snapshotRepository,
                                      RedissonClient redissonClient) {
    this.eventRepository = eventRepository;
    this.snapshotRepository = snapshotRepository;
    this.redissonClient = redissonClient;
  }

  @Override
  public LedgerEvents appendEvent(LedgerEvents event) {
    Integer nextVersion = event.getVersion() + 1;
    // if next version already exists then throw Exception
    if (eventRepository.existsByAggregateIdAndVersion(event.getAggregateId(),
                                                      nextVersion)) {
      throw new EventAlreadyExists("Version " + nextVersion +
                                   " already exists for aggregate " +
                                   event.getAggregateId());
    }
    LedgerEvents newEvent = new LedgerEvents(
        null, event.getAggregateId(), nextVersion, event.getEventType(),
        event.getPayload(), null, event.getTraceId());

    LedgerEvents savedEvent;
    if (nextVersion % 50 == 0) {
      BigDecimal previousBalance = getBalance(event.getAggregateId());
      BigDecimal eventImpact = calculateBalance(List.of(newEvent));
      BigDecimal currentBalance = previousBalance.add(eventImpact);
      AccountSnapshots entity = new AccountSnapshots(
          event.getAggregateId(), nextVersion, currentBalance);
      savedEvent = eventRepository.save(newEvent);
      snapshotRepository.save(entity);
    } else {
      savedEvent = eventRepository.save(newEvent);
    }
    // invalidatate this replica's L1 CACHE
    evictLocalCache(event.getAggregateId());
    // invalidate shared redis shared cache (L2 CACHE)
    redissonClient.getBucket(L2_KEY_PREFIX + event.getAggregateId()).delete();
    // publish to all subscribers (multiple replicas) to invalidate their L1
    // CACHE
    redissonClient.getTopic("cache-invalidation")
        .publish(event.getAggregateId());
    return savedEvent;
  }

  @Override
  public List<LedgerEvents> getEvents(String aggregateId) {
    List<LedgerEvents> events =
        eventRepository.findByAggregateIdOrderByVersionAsc(aggregateId);
    if (events.isEmpty()) {
      throw new EventNotFound("No events found for aggregate " + aggregateId);
    }
    return events;
  }

  @Override
  public List<LedgerEvents> getEventsAfterVersion(String aggregateId,
                                                  Integer afterVersion) {
    return eventRepository
        .findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(aggregateId,
                                                                 afterVersion);
  }

  @Override
  public BigDecimal getBalance(String aggregateId) {
    // checking L1 (Caffeine)
    BigDecimal cached = l1Cache.getIfPresent(aggregateId);
    if (cached != null) {
      return cached;
    }

    // checking L2 (Redis)
    RBucket<BigDecimal> rBucket =
        redissonClient.getBucket(L2_KEY_PREFIX + aggregateId);
    BigDecimal redisResult = rBucket.get();
    if (redisResult != null) {
      // add redis result into L1
      l1Cache.put(aggregateId, redisResult);
      return redisResult;
    }
    Optional<AccountSnapshots> snapshot =
        snapshotRepository.findFirstByAggregateIdOrderByVersionDesc(
            aggregateId);
    BigDecimal result;
    // no snapshot exists (full replay of events for aggregateId)
    if (snapshot.isEmpty()) {
      result = calculateBalance(getEvents(aggregateId));
    }
    // snapshot exists (snapshotBalance + balanceOfVersionsAfterSnapshots)
    else {
      List<LedgerEvents> eventsAfterVersion =
          getEventsAfterVersion(aggregateId, snapshot.get().getVersion());
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
