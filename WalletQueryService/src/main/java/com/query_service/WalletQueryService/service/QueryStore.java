package com.query_service.WalletQueryService.service;

import com.query_service.WalletQueryService.model.entries.TransactionHistory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface QueryStore {
    //    Integer getCurrentVersion(String aggregateId);
    List<TransactionHistory> getTransactionHistory(String accountId,int targetPage,int recordsPerPage);
    BigDecimal getBalance(String accountId);
    Map<String, BigDecimal> getL1CacheContents();
}