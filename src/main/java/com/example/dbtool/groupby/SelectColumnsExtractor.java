package com.example.dbtool.groupby;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the raw SELECT column list of the statement currently being written —
 * one entry per top-level comma-separated expression, between SELECT [DISTINCT]
 * and the FROM that belongs to the same query (not one inside a subquery in the
 * column list itself). Splitting/searching only at parenthesis depth zero and
 * outside string literals keeps scalar subqueries and function calls like
 * {@code NVL(a, 'x, y')} from being mistaken for column boundaries or the outer FROM.
 */
public class SelectColumnsExtractor {

    private static final Pattern SELECT_PATTERN = Pattern.compile("(?i)\\bSELECT\\b(\\s+DISTINCT\\b)?");
    private static final Pattern FROM_KEYWORD_PATTERN = Pattern.compile("(?i)\\bFROM\\b");

    public List<String> extract(String textBeforeCursor) {
        String statement = currentStatement(textBeforeCursor);

        Matcher selectMatcher = SELECT_PATTERN.matcher(statement);
        if (!selectMatcher.find()) {
            throw new NoSelectColumnsFoundException("SELECT não encontrado no texto capturado.");
        }
        int columnsStart = selectMatcher.end();

        int columnsEnd = topLevelIndexOf(statement, FROM_KEYWORD_PATTERN, columnsStart);
        if (columnsEnd < 0) {
            throw new NoSelectColumnsFoundException("FROM não encontrado no texto capturado.");
        }

        List<String> columns = splitTopLevel(statement.substring(columnsStart, columnsEnd), ',');
        if (columns.isEmpty()) {
            throw new NoSelectColumnsFoundException("Nenhuma coluna encontrada entre SELECT e FROM.");
        }
        return columns;
    }

    /**
     * The editor may hold several ';'-separated statements; only the one the cursor
     * is currently in should feed the SELECT/FROM search.
     */
    private String currentStatement(String textBeforeCursor) {
        int lastSemicolon = textBeforeCursor.lastIndexOf(';');
        return lastSemicolon >= 0 ? textBeforeCursor.substring(lastSemicolon + 1) : textBeforeCursor;
    }

    private int topLevelIndexOf(String text, Pattern pattern, int from) {
        // Transparent bounds let \b see the character just before each region we test,
        // so a keyword match isn't wrongly accepted mid-identifier (e.g. "XFROM").
        Matcher matcher = pattern.matcher(text).useTransparentBounds(true);
        int depth = 0;
        boolean inQuotes = false;
        int i = from;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (inQuotes) {
                inQuotes = c != '\'';
                i++;
                continue;
            }
            switch (c) {
                case '\'' -> inQuotes = true;
                case '(' -> depth++;
                case ')' -> depth--;
                default -> {
                    if (depth == 0 && matcher.region(i, text.length()).lookingAt()) {
                        return i;
                    }
                }
            }
            i++;
        }
        return -1;
    }

    private List<String> splitTopLevel(String text, char delimiter) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean inQuotes = false;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                inQuotes = c != '\'';
                continue;
            }
            if (c == '\'') {
                inQuotes = true;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == delimiter && depth == 0) {
                parts.add(text.substring(start, i).trim());
                start = i + 1;
            }
        }
        parts.add(text.substring(start).trim());
        return parts.stream().filter(part -> !part.isEmpty()).toList();
    }
}
