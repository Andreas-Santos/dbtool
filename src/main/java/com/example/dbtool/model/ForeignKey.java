package com.example.dbtool.model;

import java.util.List;

/**
 * Represents a (possibly multi-column) foreign key relationship between two tables.
 * {@code tableName} is the table that owns the constraint and {@code referencedTableName}
 * is the table it points to; {@code columns} pairs each local column with its counterpart
 * on the referenced table, in constraint order.
 */
public record ForeignKey(
        String constraintName,
        String tableName,
        String referencedTableName,
        List<ColumnPair> columns
) {

    public record ColumnPair(String columnName, String referencedColumnName) {
    }
}
