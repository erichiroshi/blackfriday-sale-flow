package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.web;

import java.time.Instant;

public record ErrorResponseDto(String error, String message, Instant timestamp) {

    public static ErrorResponseDto of(String error, String message) {
        return new ErrorResponseDto(error, message, Instant.now());
    }
}
