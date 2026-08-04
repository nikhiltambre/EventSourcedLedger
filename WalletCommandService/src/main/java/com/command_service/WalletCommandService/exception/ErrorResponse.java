package com.command_service.WalletCommandService.exception;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ErrorResponse {
    private int StatusCode;
    private String message;

    public ErrorResponse(int value, String message) {
        this.StatusCode = value;
        this.message = message;
    }
}
