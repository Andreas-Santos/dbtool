package com.example.dbtool.format;

/**
 * Splits the editor's full text — captured as two separate halves meeting at the
 * cursor, since that's all keyboard automation can read — into the three pieces needed
 * to reformat just the statement the cursor is in: everything before it (kept byte for
 * byte, including a previous statement's trailing ';'), the statement itself (bounded
 * by the nearest ';' on each side, or the document's edges when the buffer holds only
 * one statement), and everything after it (kept byte for byte, including its own
 * leading ';').
 */
public record StatementBounds(String prefix, String statementText, String suffix) {

    public static StatementBounds locate(String textBeforeCursor, String textAfterCursor) {
        int lastSemicolonBefore = textBeforeCursor.lastIndexOf(';');
        String prefix = lastSemicolonBefore >= 0 ? textBeforeCursor.substring(0, lastSemicolonBefore + 1) : "";
        String statementBefore = lastSemicolonBefore >= 0
                ? textBeforeCursor.substring(lastSemicolonBefore + 1)
                : textBeforeCursor;

        int firstSemicolonAfter = textAfterCursor.indexOf(';');
        String statementAfter = firstSemicolonAfter >= 0
                ? textAfterCursor.substring(0, firstSemicolonAfter)
                : textAfterCursor;
        String suffix = firstSemicolonAfter >= 0 ? textAfterCursor.substring(firstSemicolonAfter) : "";

        return new StatementBounds(prefix, statementBefore + statementAfter, suffix);
    }

    /**
     * Rebuilds the full document with {@code formattedStatement} in place of the
     * original statement text, keeping {@link #prefix()} and {@link #suffix()} exactly
     * as captured.
     */
    public String rebuildDocument(String formattedStatement) {
        String separator = prefix.isEmpty() ? "" : "\n";
        return prefix + separator + formattedStatement + suffix;
    }
}
