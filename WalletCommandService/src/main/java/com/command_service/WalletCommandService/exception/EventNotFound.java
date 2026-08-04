package com.command_service.WalletCommandService.exception;

public class EventNotFound extends RuntimeException {
    public EventNotFound(String message) {
        super(message);
    }
}
