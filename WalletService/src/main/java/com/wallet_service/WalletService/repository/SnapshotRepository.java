package com.wallet_service.WalletService.repository;

import com.wallet_service.WalletService.model.entries.AccountSnapshots;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotRepository extends JpaRepository<AccountSnapshots,Long> {
}
