package com.example.dbtool.autocomplete;

/**
 * The table being joined FROM and the table currently being typed in a JOIN, extracted
 * from the SQL text that precedes the user's cursor. {@code sourceTable}/{@code sourceAlias}
 * is whichever previously-declared table the alias typed after ON belongs to — not
 * necessarily the FROM table, since the user may be joining against an earlier JOIN
 * instead.
 */
public record SqlJoinContext(
        String sourceTable,
        String sourceAlias,
        String joinedTable,
        String joinedAlias
) {
}
