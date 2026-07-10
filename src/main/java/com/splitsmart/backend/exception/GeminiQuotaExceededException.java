package com.splitsmart.backend.exception;

public class GeminiQuotaExceededException extends RuntimeException {
    public GeminiQuotaExceededException(String message) {
        super(message);
    }
}
