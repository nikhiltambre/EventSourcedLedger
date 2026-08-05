package com.command_service.WalletCommandService.dto;

public class IdempotencyResponse<T>{
    private final T data;
    private final int statusCode;

    public IdempotencyResponse(T data, int statusCode) {
        this.data = data;
        this.statusCode = statusCode;
    }

    public T getData() {
        return data;
    }
    public int getStatusCode() {
        return statusCode;
    }
}
