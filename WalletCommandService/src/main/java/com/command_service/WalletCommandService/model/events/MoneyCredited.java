package com.command_service.WalletCommandService.model.events;

public record MoneyCredited(java.math.BigDecimal amount, String reason) implements Event {
}
