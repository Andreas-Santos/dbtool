package com.example.dbtool.join;

public class NoRelationshipException extends RuntimeException {

    public NoRelationshipException(String sourceTable, String targetTable) {
        super("No foreign key relationship found between '" + sourceTable + "' and '" + targetTable + "'");
    }
}
