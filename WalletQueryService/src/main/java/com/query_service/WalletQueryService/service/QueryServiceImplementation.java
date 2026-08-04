package com.query_service.WalletQueryService.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.query_service.WalletQueryService.model.entries.TransactionHistory;
import com.query_service.WalletQueryService.model.entries.WalletBalance;
import com.query_service.WalletQueryService.repository.TransactionHistoryRepository;
import com.query_service.WalletQueryService.repository.WalletBalanceRepository;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class QueryServiceImplementation implements QueryStore {
    private final WalletBalanceRepository walletBalanceRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final RedissonClient redissonClient;
    private final Cache<String, BigDecimal> l1Cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
    private final String L2_KEY_PREFIX = "balance:";

    public QueryServiceImplementation(WalletBalanceRepository walletBalanceRepository, TransactionHistoryRepository transactionHistoryRepository, RedissonClient redissonClient) {
        this.walletBalanceRepository = walletBalanceRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.redissonClient = redissonClient;
    }

    @Override
    public List<TransactionHistory> getTransactionHistory(String accountId, int targetPage, int recordsPerPage) {
        PageRequest pageRequest = PageRequest.of(
                targetPage,
                recordsPerPage,
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        Page<TransactionHistory> page = transactionHistoryRepository
                .findByAccountId(accountId, pageRequest);

        return page.getContent();
    }

    @Override
    public BigDecimal getBalance(String accountId) {
        //checking L1 (Caffeine)
        BigDecimal cached = l1Cache.getIfPresent(accountId);
        if (cached != null) {
            return cached;
        }

        //checking L2 (Redis)
        RBucket<BigDecimal> rBucket = redissonClient.getBucket(L2_KEY_PREFIX + accountId);
        BigDecimal redisResult = rBucket.get();
        if (redisResult != null) {
            //add redis result into L1
            l1Cache.put(accountId, redisResult);
            return redisResult;
        }

        //nothing in L1 and L2 -> check DB
        Optional<WalletBalance> balanceResult = walletBalanceRepository.findByAccountId(accountId);
        BigDecimal result;
        //no balance entry
        if (balanceResult.isEmpty()) {
            result = BigDecimal.ZERO;
        }
        //balance entry exists
        else {
            result = balanceResult.get().getBalance();
        }
        //update balance in L1 and L2
        l1Cache.put(accountId, result);
        rBucket.set(result, 24, TimeUnit.HOURS);
        return result;

    }

    public void evictLocalCache(String accountId) {
        l1Cache.invalidate(accountId);
    }

    @Override
    public Map<String, BigDecimal> getL1CacheContents() {
        return l1Cache.asMap();
    }
}
