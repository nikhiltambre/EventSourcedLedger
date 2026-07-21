package com.wallet_service.WalletService.repository;

import com.wallet_service.WalletService.model.entries.AccountSnapshots;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SnapshotRepository extends JpaRepository<AccountSnapshots, String> {
    Optional<AccountSnapshots> findFirstByAggregateIdOrderByVersionDesc(String aggregateId);
}
