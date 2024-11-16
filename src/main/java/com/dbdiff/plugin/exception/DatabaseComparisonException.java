package com.dbdiff.plugin.exception;

public class DatabaseComparisonException extends RuntimeException {
    public DatabaseComparisonException(String message, Throwable cause) {
        super(message, cause);
    }
} 