package com.command_service.WalletCommandService.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    public ResponseEntity<LedgerEvents> addNewEvent(@RequestBody LedgerEvents event) {
        LedgerEvents savedEvent = eventService.appendEvent(event);
        return new ResponseEntity<>(savedEvent, HttpStatus.CREATED);
    }

}
