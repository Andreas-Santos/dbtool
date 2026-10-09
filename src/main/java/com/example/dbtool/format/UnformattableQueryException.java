package com.example.dbtool.format;

public class UnformattableQueryException extends RuntimeException {

    public UnformattableQueryException(String message) {
        super(message);
    }
}
