package com.query_service.WalletQueryService.repository;

import com.query_service.WalletQueryService.model.entries.TransactionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, String> {

    // Spring Data auto-generates:
    // SELECT * FROM transaction_history WHERE account_id = ?
    // ORDER BY <from Pageable> LIMIT <size> OFFSET <page * size>
    Page<TransactionHistory> findByAccountId(String accountId, Pageable pageable);

}
