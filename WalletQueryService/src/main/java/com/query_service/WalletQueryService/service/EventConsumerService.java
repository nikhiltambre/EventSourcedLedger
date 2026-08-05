package com.query_service.WalletQueryService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.query_service.WalletQueryService.dto.KafkaCdcObject;
import org.apache.kafka.clients.consumer.internals.Acknowledgements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.kafka.support.Acknowledgment;
@Service
public class EventConsumerService {
    private static final Logger log = LoggerFactory.getLogger(EventConsumerService.class);
    private final ReadModelProjector readModelProjector;
    private final ObjectMapper objectMapper;

    public EventConsumerService(ReadModelProjector readModelProjector, ObjectMapper objectMapper) {
        this.readModelProjector = readModelProjector;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "wallet.ledger_write_db.ledger_events", groupId = "query-service-cdc")
    public void consume(String message, Acknowledgment ack) {
        try {
            KafkaCdcObject cdc = objectMapper.readValue(message, KafkaCdcObject.class);
            if (cdc.getPayload() == null) {
                log.warn("Received cdc event with null payload, skipping");
                ack.acknowledge();
                return;
            }
            String op = cdc.getPayload().getOp();
            if ("c".equals(op) || "r".equals(op)) {
                KafkaCdcObject.After after = cdc.getPayload().getAfter();
                if (after != null) {
                    log.info("CDC event received: op={}, accountId={}, version={}, eventType={}",
                            op, after.getAccountId(), after.getVersion(), after.getEventType());
                    readModelProjector.project(after);
                    ack.acknowledge();
                }
            } else {
                log.debug("Ignoring cdc op:{}", op);
                ack.acknowledge();
            }
        } catch (Exception e) {
            log.error("Failed to process CDC event: {}", e.getMessage(), e);
            //send to dead letter queue(for later)
        }
    }
}
