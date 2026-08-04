package com.query_service.WalletQueryService.service;

import jakarta.annotation.PostConstruct;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationListner {
    private final RedissonClient redissonClient;
    private final QueryServiceImplementation queryService;

    public CacheInvalidationListner(RedissonClient redissonClient, QueryServiceImplementation queryService) {
        this.redissonClient = redissonClient;
        this.queryService = queryService;
    }


    //this runs first and listens for cache-invalidation
    //when different instance publish cache-invalidation it invalidate L1 cache of this instance
    @PostConstruct
    void subscribe() {
        RTopic topic = redissonClient.getTopic("cache-invalidation");
        topic.addListener(String.class, (channel, aggregateId) -> {
            queryService.evictLocalCache(aggregateId);
        });
    }
}
