package com.query_service.WalletQueryService.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.query_service.WalletQueryService.dto.KafkaCdcObject;
import com.query_service.WalletQueryService.model.entries.TransactionHistory;
import com.query_service.WalletQueryService.model.entries.WalletBalance;
import com.query_service.WalletQueryService.repository.TransactionHistoryRepository;
import com.query_service.WalletQueryService.repository.WalletBalanceRepository;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class ReadModelProjector {

    private static final Logger log = LoggerFactory.getLogger(ReadModelProjector.class);
    private static final String L2_KEY_PREFIX = "balance:";

    private final WalletBalanceRepository walletBalanceRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;
    private final QueryServiceImplementation queryService;

    public ReadModelProjector(WalletBalanceRepository walletBalanceRepository, TransactionHistoryRepository transactionHistoryRepository, ObjectMapper objectMapper, RedissonClient redissonClient, QueryServiceImplementation queryService) {
        this.walletBalanceRepository = walletBalanceRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.objectMapper = objectMapper;
        this.redissonClient = redissonClient;
        this.queryService = queryService;
    }

    private BigDecimal parseAmount(String payloadJson) throws JsonProcessingException {
        JsonNode eventNode = objectMapper.readTree(payloadJson);
        return eventNode.has("amount") ? eventNode.get("amount").decimalValue() : BigDecimal.ZERO;
    }

    private WalletBalance upsertBalance(String accountId, Integer version, String evenType, BigDecimal amount) {
        Optional<WalletBalance> existing = walletBalanceRepository.findById(accountId);
        WalletBalance balance = existing.isPresent()
                ? applyEventToExistingBalance(existing.get(), accountId, version, evenType, amount)
                : createInitialBalance(accountId, version, evenType, amount);
        walletBalanceRepository.save(balance);
        return balance;
    }


    private void applyBalanceEventSpecific(WalletBalance balance, String accountId, String eventType, BigDecimal amount) {
        if ("MoneyCredited".equals(eventType)) {
            balance.setBalance(balance.getBalance().add(amount));
        } else if ("MoneyDebited".equals(eventType)) {
            BigDecimal current = balance.getBalance();
            if (current.compareTo(amount) >= 0) {
                balance.setBalance(current.subtract(amount));
            } else {
                log.warn("Insufficient balance for debit: accountId={}, current={}, amount={}", accountId, current, amount);
                return;
            }
        } else {
            // AccountOpened or unknown type
            log.debug("Unhandled eventType={} for accountId={}, ignoring balance update", eventType, accountId);
        }
    }

    private WalletBalance applyEventToExistingBalance(WalletBalance balance, String accountId, Integer version, String eventType, BigDecimal amount) {

        if (version <= balance.getLastEventVersion()) {
            log.info("Skipping duplicate/old event: accountId={}, version={}, lastProcessed={}", accountId, version, balance.getLastEventVersion());
            return balance;
        }

        applyBalanceEventSpecific(balance, accountId, eventType, amount);
        balance.setLastEventVersion(version);
        return balance;
    }

    private WalletBalance createInitialBalance(String accountId, Integer version, String eventType, BigDecimal amount) {
        BigDecimal initialBalance = "MoneyCredited".equals(eventType) ? amount : BigDecimal.ZERO;
        return new WalletBalance(accountId, initialBalance, version);
    }

    @Transactional
    public void project(KafkaCdcObject.After after) {
        String accountId = after.getAccountId();
        Integer version = after.getVersion();
        String eventType = after.getEventType();

        try {
            BigDecimal amount = parseAmount(after.getPayload());
            WalletBalance balance = upsertBalance(accountId, version, eventType, amount);


            saveTransactionHistory(after, accountId, eventType, amount, balance.getBalance());

            log.info("Read model projected: accountId={}, eventType={}, newBalance={}, version={}", accountId, eventType, balance.getBalance(), version);

            invalidateCache(accountId);
        } catch (
                Exception e) {
            log.error("Failed to project event: accountId={}, version={}, error={}", accountId, version, e.getMessage(), e);
            throw new RuntimeException("Projection failed for accountId=" + accountId + " version=" + version, e);
        }
    }

    public void saveTransactionHistory(KafkaCdcObject.After after, String accountId, String eventType, BigDecimal amount, BigDecimal balanceAfter) {
        LocalDateTime timestamp = after.getCreatedAt() != null ? LocalDateTime.ofInstant(Instant.ofEpochMilli(after.getCreatedAt() / 1000), ZoneId.systemDefault()) : LocalDateTime.now();

        TransactionHistory transactionHistory = new TransactionHistory(after.getTraceId(), accountId, eventType, amount, balanceAfter, timestamp);
        transactionHistoryRepository.save(transactionHistory);
    }

    public void invalidateCache(String accountId) {
        queryService.evictLocalCache(accountId);
        redissonClient.getBucket(L2_KEY_PREFIX + accountId).delete();
        redissonClient.getTopic("cache-invalidation").publish(accountId);

    }
}
