package com.wallet_service.WalletService.dto;

import java.math.BigDecimal;

public class DataObject {
    private String aggregateId;
    private BigDecimal balance;

    public DataObject(String aggregateId, BigDecimal balance) {
        this.aggregateId = aggregateId;
        this.balance = balance;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
