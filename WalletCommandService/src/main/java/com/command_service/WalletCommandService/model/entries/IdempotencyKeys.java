package com.command_service.WalletCommandService.model.entries;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeys {

    @Id
    @Column(name = "client_key", length = 128, nullable = false)
    private String clientKey;

    @Column(name = "response_payload", nullable = false, columnDefinition = "json")
    @ColumnTransformer(write = "?::json")
    private String responsePayload;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public IdempotencyKeys() {
    }

    public IdempotencyKeys(String clientKey, String responsePayload, Integer statusCode, LocalDateTime expiresAt) {
        this.clientKey = clientKey;
        this.responsePayload = responsePayload;
        this.statusCode = statusCode;
        this.expiresAt = expiresAt;
    }

    public String getClientKey() {
        return clientKey;
    }
    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }
    public String getResponsePayload() {
        return responsePayload;
    }
    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }
    public Integer getStatusCode() {
        return statusCode;
    }
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}