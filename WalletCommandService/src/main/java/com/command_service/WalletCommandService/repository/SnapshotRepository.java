package com.command_service.WalletCommandService.repository;

import com.command_service.WalletCommandService.model.entries.AccountSnapshots;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SnapshotRepository extends JpaRepository<AccountSnapshots, String> {
    Optional<AccountSnapshots> findFirstByAggregateIdOrderByVersionDesc(String aggregateId);
}
