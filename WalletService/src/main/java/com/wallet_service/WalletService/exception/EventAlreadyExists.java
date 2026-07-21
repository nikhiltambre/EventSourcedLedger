package com.wallet_service.WalletService.exception;

public class EventAlreadyExists extends RuntimeException {
    public EventAlreadyExists(String message) {
        super(message);
    }
}
