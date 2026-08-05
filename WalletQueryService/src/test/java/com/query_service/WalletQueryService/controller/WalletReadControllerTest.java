package com.query_service.WalletQueryService.controller;

import com.query_service.WalletQueryService.model.entries.TransactionHistory;
import com.query_service.WalletQueryService.service.QueryStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(WalletReadController.class)
public class WalletReadControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    QueryStore queryService;

    @Test
    @DisplayName("getCurrentBalance -> should return balance in the account")
    void getCurrentBalance_shouldReturnBalanceFromAccount() throws Exception {
        String accountId = "ACC300";
        BigDecimal expectedBalance = new BigDecimal("50.00");
        when(queryService.getBalance(accountId))
                .thenReturn(expectedBalance);
        mockMvc.perform(
                        get("/api/accounts/getBalance/{accountId}", accountId)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("50.00"));
    }
    //no transaction history (204)
    @Test
    @DisplayName("getPaginatedTransactionHistory -> should return 204 NO_CONTENT when account has no transaction history")
    void getPaginatedTransactionHistory_shouldReturn204_whenHistoryIsEmpty() throws Exception {
        when(queryService.getTransactionHistory(eq("ACC300"), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        mockMvc.perform(
                        get("/api/accounts/getTransactionHistory/ACC300")
                )
                .andExpect(status().isNoContent());
    }

    //has transaction history
    @Test
    @DisplayName("getPaginatedTransactionsHistory -> should return transaction history ")
    void getPaginatedTransactionHistory_shouldReturnTransactionHistory() throws Exception {
        String accountId = "ACC300";
        int page = 1;
        int size = 1;
        TransactionHistory history1 = new TransactionHistory(
                "trace-001",
                accountId,
                "MoneyCredited",
                new BigDecimal("100.00"),
                new BigDecimal("250.00"),
                LocalDateTime.now()

        );
        when(queryService.getTransactionHistory(eq(accountId), anyInt(),anyInt()))
                .thenReturn(List.of(history1));
        mockMvc.perform(
                        get("/api/accounts/getTransactionHistory/{accountId}",accountId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transactionId").value("trace-001"))
                .andExpect(jsonPath("$[0].accountId").value("ACC300"))
                .andExpect(jsonPath("$[0].type").value("MoneyCredited"))
                .andExpect(jsonPath("$[0].amount").value(100.00))
                .andExpect(jsonPath("$[0].balanceAfter").value(250.00));
    }


}
