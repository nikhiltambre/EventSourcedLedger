package com.query_service.WalletQueryService.model.entries;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_history",
        indexes = @Index(name = "idx_account_timestamp", columnList = "account_id, timestamp DESC"))
public class TransactionHistory {

    @Id
    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    @Column(name = "account_id", length = 64, nullable = false)
    private String accountId;

    @Column(name = "type", length = 20, nullable = false)
    private String type;

    @Column(name = "amount", precision = 18, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 18, scale = 4, nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public TransactionHistory() {}

    public TransactionHistory(String transactionId, String accountId, String type,
                              BigDecimal amount, BigDecimal balanceAfter, LocalDateTime timestamp) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp;
    }

    // getters and setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}