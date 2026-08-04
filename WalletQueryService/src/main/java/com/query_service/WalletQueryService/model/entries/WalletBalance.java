package com.query_service.WalletQueryService.model.entries;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_balances")
public class WalletBalance {

    @Id
    @Column(name = "account_id", length = 64)
    private String accountId;

    @Column(name = "balance", precision = 18, scale = 4, nullable = false)
    private BigDecimal balance;

    @Column(name = "last_event_version", nullable = false)
    private Integer lastEventVersion;

    @Column(name = "last_updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime lastUpdatedAt;

    public WalletBalance() {}

    public WalletBalance(String accountId, BigDecimal balance, Integer lastEventVersion) {
        this.accountId = accountId;
        this.balance = balance;
        this.lastEventVersion = lastEventVersion;
    }

    // getters and setters
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public Integer getLastEventVersion() { return lastEventVersion; }
    public void setLastEventVersion(Integer lastEventVersion) { this.lastEventVersion = lastEventVersion; }

    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
}
