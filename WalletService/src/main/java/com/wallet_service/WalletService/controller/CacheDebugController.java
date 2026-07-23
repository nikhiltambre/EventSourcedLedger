package com.wallet_service.WalletService.controller;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/debug")
public class CacheDebugController {
    private final CacheManager cacheManager;

    public CacheDebugController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @GetMapping("/cache")
    public Map<Object, Object> showCacheContents() {
        CaffeineCache springCache = (CaffeineCache) cacheManager.getCache("account-balance-cache");
        if (springCache == null) {
            return Map.of("status", "Cache not initialized");
        }
        return springCache.getNativeCache().asMap();
    }
}
