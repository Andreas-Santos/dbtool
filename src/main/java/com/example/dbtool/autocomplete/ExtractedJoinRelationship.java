package com.example.dbtool.autocomplete;

import com.example.dbtool.model.ForeignKey.ColumnPair;

import java.util.List;

/**
 * A relationship reverse-engineered from an already-written JOIN ... ON clause:
 * {@code columns} pairs each sourceTable column with its counterpart on joinedTable.
 */
public record ExtractedJoinRelationship(String sourceTable, String joinedTable, List<ColumnPair> columns) {
}
