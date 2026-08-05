package com.command_service.WalletCommandService.repository;

import com.command_service.WalletCommandService.model.entries.IdempotencyKeys;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyKeys,String> {
}
