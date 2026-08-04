package com.query_service.WalletQueryService.repository;

import com.query_service.WalletQueryService.model.entries.WalletBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletBalanceRepository extends JpaRepository<WalletBalance, String> {
    Optional<WalletBalance> findByAccountId(String accountId);
}
