package com.wallet_service.WalletService.model.events;

public record MoneyDebited(java.math.BigDecimal amount, String reason) implements Event {
}
