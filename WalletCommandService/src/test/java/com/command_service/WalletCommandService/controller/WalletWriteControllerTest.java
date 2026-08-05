package com.command_service.WalletCommandService.controller;

import com.command_service.WalletCommandService.dto.IdempotencyResponse;
import com.command_service.WalletCommandService.model.entries.LedgerEvents;
import com.command_service.WalletCommandService.model.events.MoneyCredited;
import com.command_service.WalletCommandService.service.EventStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletWriteController.class)
public class WalletWriteControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EventStore eventService;

    @Test
    @DisplayName("addNewEvent -> should reaturn 201 CREATED on successfully saving event")
    void addNewEvent_shouldReturn201_onSuccessfullingSavingEvent() throws Exception {
        String idempotencyKey = "idem-key-123";

        LedgerEvents savedEvent = new LedgerEvents();
        savedEvent.setId(1L);
        savedEvent.setAggregateId("ACC300");
        savedEvent.setVersion(1);
        savedEvent.setEventType("MoneyCredited");
        savedEvent.setPayload(new MoneyCredited(new BigDecimal("250.00"), "Initial deposit"));
        savedEvent.setTraceId("trace-001");
        when(eventService.appendEvent(any(LedgerEvents.class), eq(idempotencyKey)))
                .thenReturn(new IdempotencyResponse<>(savedEvent, 201));
        String jsonRequestBody = """
                {
                    "aggregateId": "ACC300",
                    "version": 0,
                    "eventType": "MoneyCredited",
                    "payload": {
                        "type": "MoneyCredited",
                        "amount": 250.00,
                        "reason": "Initial deposit"
                    },
                    "traceId": "trace-001"
                }
                """;
        mockMvc.perform(
                        post("/api/event/addEvent")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.aggregateId").value("ACC300"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.eventType").value("MoneyCredited"));

    }

}
