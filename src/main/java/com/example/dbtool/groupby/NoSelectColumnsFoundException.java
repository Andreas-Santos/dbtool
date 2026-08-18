package com.example.dbtool.groupby;

public class NoSelectColumnsFoundException extends RuntimeException {

    public NoSelectColumnsFoundException(String message) {
        super(message);
    }
}
