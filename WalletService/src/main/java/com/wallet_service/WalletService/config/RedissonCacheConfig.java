package com.wallet_service.WalletService.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@EnableCaching
public class RedissonCacheConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() throws IOException {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379")
                .setTimeout(2000)
                .setRetryAttempts(3);
        return Redisson.create(config);
    }

}