package com.command_service.WalletCommandService.repository;

import com.command_service.WalletCommandService.model.entries.LedgerEvents;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<LedgerEvents, Long> {
    List<LedgerEvents> findByAggregateIdOrderByVersionAsc(String aggregateId);
    List<LedgerEvents> findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(String aggregateId, Integer version);
    boolean existsByAggregateIdAndVersion(String aggregateId, Integer version);

}