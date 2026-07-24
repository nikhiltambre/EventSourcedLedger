package com.wallet_service.WalletService.controller;

import com.wallet_service.WalletService.service.EventStore;
import org.redisson.api.RedissonClient;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class CacheDebugController {
    private final RedissonClient redissonClient;
    private final EventStore eventStore;

    public CacheDebugController(RedissonClient redissonClient, EventStore eventStore) {
        this.redissonClient = redissonClient;
        this.eventStore = eventStore;
    }

    @GetMapping("/cache")
    public Map<String, Object> showCacheContents() {
        Map<String, Object> result = new HashMap<>();
        result.put("l1_caffine", eventStore.getL1CacheContents());
        Map<String, Object> l2Contents = new HashMap<>();
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern("balance:*");
        for (String key : keys) {
            Object value = redissonClient.getBucket(key).get();
            l2Contents.put(key, value);
        }
        result.put("l2_redis", l2Contents);
        return result;
    }
}
