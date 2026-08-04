package com.command_service.WalletCommandService.util;

import com.command_service.WalletCommandService.model.entries.LedgerEvents;
import com.command_service.WalletCommandService.model.events.Event;
import com.command_service.WalletCommandService.model.events.MoneyCredited;
import com.command_service.WalletCommandService.model.events.MoneyDebited;

import java.math.BigDecimal;
import java.util.List;

public class BalanceCalculator {
    public static BigDecimal calculateBalance(List<LedgerEvents> events) {
        BigDecimal balance = BigDecimal.ZERO;
        if (events == null || events.isEmpty()) {
            return BigDecimal.ZERO;
        }

        for (LedgerEvents eventEntry : events) {
            if (eventEntry == null || eventEntry.getPayload() == null) {
                continue;
            }
            Event payload = eventEntry.getPayload();
            BigDecimal impact = switch (payload) {
                case MoneyCredited credited ->
                        credited.amount() != null ? credited.amount() : BigDecimal.ZERO;
                case MoneyDebited debited ->
                        debited.amount() != null ? debited.amount().negate() : BigDecimal.ZERO;
                default ->
                        BigDecimal.ZERO;
            };
            balance = balance.add(impact);
        }
        return balance;

    }
}
