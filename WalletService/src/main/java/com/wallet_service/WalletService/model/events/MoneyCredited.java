package com.wallet_service.WalletService.model.events;

public record MoneyCredited(java.math.BigDecimal amount, String reason) implements Event {
}
