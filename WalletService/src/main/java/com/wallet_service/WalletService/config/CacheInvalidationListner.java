package com.wallet_service.WalletService.config;

import com.wallet_service.WalletService.service.WalletServiceImplementation;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationListner {
    private final RedissonClient redissonClient;
    private final WalletServiceImplementation walletService;

    public CacheInvalidationListner(RedissonClient redissonClient, WalletServiceImplementation walletService) {
        this.redissonClient = redissonClient;
        this.walletService = walletService;
    }


    //this runs first and listens for cache-invalidation
    //when different instance publish cache-invalidation it invalidate L1 cache of this instance
    @PostConstruct
    void subscribe() {
        RTopic topic = redissonClient.getTopic("cache-invalidation");
        topic.addListener(String.class, (channel, aggregateId) -> {
            walletService.evictLocalCache(aggregateId);
        });
    }
}
