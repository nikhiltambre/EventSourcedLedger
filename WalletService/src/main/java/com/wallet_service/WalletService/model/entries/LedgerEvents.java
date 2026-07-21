package com.wallet_service.WalletService.model.entries;

import com.wallet_service.WalletService.model.events.Event;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ledger_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_aggregate_version",
                        columnNames = {"aggregate_id", "version"}
                )
        }
)
@Data
public class LedgerEvents {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", length = 64, nullable = false)
    private String aggregateId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private Event payload;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime createdAt;

    @Column(name = "trace_id", length = 64)
    private String traceId;


    public LedgerEvents(){

    }

    public LedgerEvents(Object o, String aggregateId, Integer nextVersion, String eventType, Event payload, Object o1, String traceId) {
        this.aggregateId = aggregateId;
        this.version= nextVersion;
        this.eventType= eventType;
        this.payload=payload;
        this.traceId=traceId;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    //getter setter
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getAggregateId() {
        return aggregateId;
    }
    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }
    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }
    public String getEventType() {
        return eventType;
    }
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    public Event getPayload() {
        return payload;
    }
    public void setPayload(Event payload) {
        this.payload = payload;
    }
    public String getTraceId() {
        return traceId;
    }
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
