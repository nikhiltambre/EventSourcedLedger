package com.wallet_service.WalletService.repository;

import com.wallet_service.WalletService.model.entries.LedgerEvents;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<LedgerEvents, Long> {
    List<LedgerEvents> findByAggregateIdOrderByVersionAsc(String aggregateId);
    List<LedgerEvents> findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(String aggregateId, Integer version);
    Optional<LedgerEvents> findFirstByAggregateIdOrderByVersionDesc(String aggregateId);
    boolean existsByAggregateIdAndVersion(String aggregateId, Integer version);

}