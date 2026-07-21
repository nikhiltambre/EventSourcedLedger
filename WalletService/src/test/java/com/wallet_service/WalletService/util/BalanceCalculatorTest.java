package com.wallet_service.WalletService.util;

import com.wallet_service.WalletService.model.entries.LedgerEvents;
import com.wallet_service.WalletService.model.events.MoneyCredited;
import com.wallet_service.WalletService.model.events.MoneyDebited;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceCalculatorTest {

    @Test
    @DisplayName("calculateBalance -> correctly calculates credits and debits")
    void calculateBalance_shouldReturnCorrectBalance_whenEventsExist() {
        // ARRANGE
        LedgerEvents event1 = new LedgerEvents();
        event1.setPayload(new MoneyCredited(new BigDecimal("100.50"), "Salary"));

        LedgerEvents event2 = new LedgerEvents();
        event2.setPayload(new MoneyDebited(new BigDecimal("30.20"), "Shopping"));

        LedgerEvents event3 = new LedgerEvents();
        event3.setPayload(new MoneyCredited(new BigDecimal("20.00"), "Refund"));

        // ACT
        BigDecimal result = BalanceCalculator.calculateBalance(List.of(event1, event2, event3));

        // ASSERT
        assertThat(result).isEqualByComparingTo("90.30");
    }

    @Test
    @DisplayName("calculateBalance -> returns ZERO when list is empty")
    void calculateBalance_shouldReturnZero_whenListIsEmpty() {
        BigDecimal result = BalanceCalculator.calculateBalance(Collections.emptyList());
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("calculateBalance -> returns ZERO when list is null")
    void calculateBalance_shouldReturnZero_whenListIsNull() {
        BigDecimal result = BalanceCalculator.calculateBalance(null);
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("calculateBalance -> safely skips null entries or null payloads")
    void calculateBalance_shouldIgnoreNullPayloads() {
        LedgerEvents nullPayloadEvent = new LedgerEvents();

        BigDecimal result = BalanceCalculator.calculateBalance(Collections.singletonList(nullPayloadEvent));
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }
}