package com.command_service.WalletCommandService.service;

import jakarta.annotation.PostConstruct;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationListner {
    private final RedissonClient redissonClient;
    private final CommandServiceImplementation commandService;
    public CacheInvalidationListner(RedissonClient redissonClient, CommandServiceImplementation commandService) {
        this.redissonClient = redissonClient;
        this.commandService = commandService;
    }


    //this runs first and listens for cache-invalidation
    //when different instance publish cache-invalidation it invalidate L1 cache of this instance
    @PostConstruct
    void subscribe() {
        RTopic topic = redissonClient.getTopic("cache-invalidation");
        topic.addListener(String.class, (channel, aggregateId) -> {
            commandService.evictLocalCache(aggregateId);
        });
    }
}
