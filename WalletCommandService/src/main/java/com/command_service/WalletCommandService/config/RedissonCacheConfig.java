package com.command_service.WalletCommandService.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@EnableCaching
public class RedissonCacheConfig {
    @Value("${redis.host:localhost}")
    private String redisHost;
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() throws IOException {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://"+redisHost+":localhost:6379")
                .setTimeout(2000)
                .setRetryAttempts(3);
        return Redisson.create(config);
    }

}