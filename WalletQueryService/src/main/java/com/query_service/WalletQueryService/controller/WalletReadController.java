package com.query_service.WalletQueryService.controller;


import com.query_service.WalletQueryService.model.entries.TransactionHistory;
import com.query_service.WalletQueryService.service.QueryStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class WalletReadController {
    private final QueryStore eventService;

    public WalletReadController(QueryStore eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/getBalance/{accountId}")
    public ResponseEntity<BigDecimal> getCurrentBalance(@PathVariable String accountId) {
        return ResponseEntity.ok(eventService.getBalance(accountId));
    }

    @GetMapping("/getTransactionHistory/{accountId}")
    public ResponseEntity<List<TransactionHistory>> getPaginatedTransactionHistory(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<TransactionHistory> history = eventService.getTransactionHistory(accountId, page, size);
        if (history.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(history);
    }
}
