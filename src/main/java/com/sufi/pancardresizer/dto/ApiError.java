package com.sufi.pancardresizer.dto;

import java.time.Instant;

public class ApiError {
    private String message;
    private String code;
    private Instant timestamp;

    public ApiError(String message, String code) {
        this.message = message;
        this.code = code;
        this.timestamp = Instant.now();
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
