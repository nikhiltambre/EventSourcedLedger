package com.command_service.WalletCommandService.exception;

public class EventAlreadyExists extends RuntimeException {
    public EventAlreadyExists(String message) {
        super(message);
    }
}
