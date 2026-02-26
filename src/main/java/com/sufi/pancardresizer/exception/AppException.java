package com.sufi.pancardresizer.exception;

public class AppException extends RuntimeException {
    private final String code;

    public AppException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
