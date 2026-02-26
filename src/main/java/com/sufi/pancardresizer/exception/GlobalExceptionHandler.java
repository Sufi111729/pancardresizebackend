package com.sufi.pancardresizer.exception;

import com.sufi.pancardresizer.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiError> handleApp(AppException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(new ApiError(ex.getMessage(), ex.getCode()), status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        return new ResponseEntity<>(new ApiError("Unexpected error", "unexpected_error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
