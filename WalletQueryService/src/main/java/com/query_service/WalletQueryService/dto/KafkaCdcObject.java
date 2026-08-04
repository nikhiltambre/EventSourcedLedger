package com.query_service.WalletQueryService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaCdcObject {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload{
       private After before;
       private After after;
       private String op;

        public After getBefore() {
            return before;
        }
        public void setBefore(After before) {
            this.before = before;
        }
        public After getAfter() {
            return after;
        }
        public void setAfter(After after) {
            this.after = after;
        }
        public String getOp() {
            return op;
        }
        public void setOp(String op) {
            this.op = op;
        }
    }

    @JsonIgnoreProperties
    public static class After{
        private Long id;

        @JsonProperty("aggregate_id")
        private String accountId;

        private Integer version;

        @JsonProperty("event_type")
        private String eventType;

        private String payload;

        @JsonProperty("created_at")
        private Long createdAt;

        @JsonProperty("trace_id")
        private String traceId;

        public Long getId() {
            return id;
        }
        public void setId(Long id) {
            this.id = id;
        }
        public String getAccountId() {
            return accountId;
        }
        public void setAccountId(String accountId) {
            this.accountId = accountId;
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
        public String getPayload() {
            return payload;
        }
        public void setPayload(String payload) {
            this.payload = payload;
        }
        public Long getCreatedAt() {
            return createdAt;
        }
        public void setCreatedAt(Long createdAt) {
            this.createdAt = createdAt;
        }
        public String getTraceId() {
            return traceId;
        }
        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }
    }

    private Payload payload;

    public Payload getPayload() {
        return payload;
    }
    public void setPayload(Payload payload) {
        this.payload = payload;
    }
}
