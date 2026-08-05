package com.command_service.WalletCommandService.controller;


import com.command_service.WalletCommandService.dto.IdempotencyResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.command_service.WalletCommandService.model.entries.LedgerEvents;
import com.command_service.WalletCommandService.service.EventStore;

@RestController
@RequestMapping("/api/event")
public class WalletWriteController {
    private final EventStore eventService;

    public WalletWriteController(EventStore eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/addEvent")
    public ResponseEntity<LedgerEvents> addNewEvent(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, @RequestBody LedgerEvents event) {
        IdempotencyResponse<LedgerEvents> savedEvent = eventService.appendEvent(event, idempotencyKey);
        return new ResponseEntity<>(savedEvent.getData(), HttpStatus.CREATED);
    }

}
