package com.command_service.WalletCommandService.model.events;

public record MoneyDebited(java.math.BigDecimal amount, String reason) implements Event {
}
