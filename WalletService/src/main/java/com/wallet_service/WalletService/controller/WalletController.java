package com.wallet_service.WalletService.controller;


import com.wallet_service.WalletService.model.entries.LedgerEvents;
import com.wallet_service.WalletService.model.events.Event;
import com.wallet_service.WalletService.service.EventStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event")
public class WalletController {
    private final EventStore eventService;

    public WalletController(EventStore eventService) {
        this.eventService = eventService;
    }


    @PostMapping("/addEvent")
    public ResponseEntity<LedgerEvents> addNewEvent(@RequestBody LedgerEvents event) {
        LedgerEvents savedEvent = eventService.appendEvent(event);
        return new ResponseEntity<>(savedEvent, HttpStatus.CREATED);
    }

    @GetMapping("/findById/{aggregateId}")
    public ResponseEntity<List<LedgerEvents>> findEventById(@PathVariable String aggregateId) {
        List<LedgerEvents> eventList = eventService.getEvents(aggregateId);
        return ResponseEntity.ok(eventList);
    }

    @GetMapping("/findEventAfterVersion/{aggregateId}/{version}")
    public ResponseEntity<List<LedgerEvents>> findEventsAfterVersion(
            @PathVariable String aggregateId,
            @PathVariable Integer version) {
        List<LedgerEvents> eventList = eventService.getEventsAfterVersion(aggregateId, version);
        return ResponseEntity.ok(eventList);
    }

    @GetMapping("/currentVersion/{aggregateId}")
    public ResponseEntity<Integer> getCurrentVersion(@PathVariable String aggregateId) {
        return ResponseEntity.ok(eventService.getCurrentVersion(aggregateId));
    }


}
