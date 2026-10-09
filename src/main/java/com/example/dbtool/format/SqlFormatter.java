package com.example.dbtool.format;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reformats a single SQL statement into this project's house style:
 *
 * <pre>
 * SELECT
 *     COL_A,
 *     COL_B
 * FROM
 *     OWNER.TABLE_A A
 * INNER JOIN OWNER.TABLE_B B ON
 *         A.ID     = B.A_ID
 *     AND A.FILIAL = B.FILIAL
 * WHERE
 *         A.STATUS = 1
 *     AND B.ATIVO  = 'S'
 * GROUP BY
 *     A.ID,
 *     A.FILIAL
 * </pre>
 *
 * Clauses are located by scanning for keywords at parenthesis depth zero and outside
 * string literals — the same technique {@code SelectColumnsExtractor} uses — so a
 * subquery inside a column or condition is never mistaken for a clause boundary of the
 * outer statement. A subquery's own text is otherwise left untouched (only its
 * surrounding whitespace is collapsed): this formatter only reformats the outermost
 * statement, not anything nested inside it.
 *
 * <p>Within each JOIN's ON block and each WHERE/HAVING block, every top-level "=" is
 * aligned to the same column by padding the shorter left-hand sides — purely cosmetic,
 * scoped independently to each block (a later JOIN's conditions never affect an earlier
 * one's alignment).
 *
 * <p>Every table reference in FROM and JOIN is qualified with the schema owner
 * configured for this connection (see {@link com.example.dbtool.config.DbConfig#owner()}),
 * unless it already names a schema (contains a dot) or is a subquery (starts with "(")
 * — either way it is left untouched. An owner left blank in the configuration disables
 * this qualification entirely, leaving every table reference as typed.
 */
public class SqlFormatter {

    private enum ClauseType { SELECT, FROM, JOIN, WHERE, HAVING, GROUP_BY, ORDER_BY }

    private record Boundary(ClauseType type, int start, int end) {
    }

    private record ConditionPart(String operator, String text) {
    }

    private static final Pattern JOIN_PATTERN = Pattern.compile(
            "(?i)\\b(?:(?:INNER|CROSS|LEFT(?:\\s+OUTER)?|RIGHT(?:\\s+OUTER)?|FULL(?:\\s+OUTER)?)\\s+)?JOIN\\b");
    private static final Pattern GROUP_BY_PATTERN = Pattern.compile("(?i)\\bGROUP\\s+BY\\b");
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile("(?i)\\bORDER\\s+BY\\b");
    private static final Pattern SELECT_PATTERN = Pattern.compile("(?i)\\bSELECT\\b");
    private static final Pattern FROM_PATTERN = Pattern.compile("(?i)\\bFROM\\b");
    private static final Pattern WHERE_PATTERN = Pattern.compile("(?i)\\bWHERE\\b");
    private static final Pattern HAVING_PATTERN = Pattern.compile("(?i)\\bHAVING\\b");
    private static final Pattern ON_PATTERN = Pattern.compile("(?i)\\bON\\b");
    private static final Pattern AND_OR_PATTERN = Pattern.compile("(?i)\\b(AND|OR)\\b");
    private static final Pattern DISTINCT_PREFIX_PATTERN = Pattern.compile("(?i)\\bDISTINCT\\b\\s*");
    private static final Pattern QUALIFIED_NAME_PATTERN = Pattern.compile(
            "^(\"[^\"]+\"|[A-Za-z_][\\w$#]*)(\\.(\"[^\"]+\"|[A-Za-z_][\\w$#]*))?");

    private static final List<Pattern> CLAUSE_PATTERNS = List.of(
            JOIN_PATTERN, GROUP_BY_PATTERN, ORDER_BY_PATTERN, SELECT_PATTERN, FROM_PATTERN, WHERE_PATTERN, HAVING_PATTERN);
    private static final List<ClauseType> CLAUSE_TYPES = List.of(
            ClauseType.JOIN, ClauseType.GROUP_BY, ClauseType.ORDER_BY, ClauseType.SELECT, ClauseType.FROM,
            ClauseType.WHERE, ClauseType.HAVING);

    /** "" when no owner is configured, so table references are left unqualified. */
    private final String ownerPrefix;

    /**
     * @param owner the schema owner to qualify every FROM/JOIN table with, or blank/null
     *              to leave table references unqualified.
     */
    public SqlFormatter(String owner) {
        this.ownerPrefix = (owner == null || owner.isBlank()) ? "" : owner.strip() + ".";
    }

    public String format(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new UnformattableQueryException("Nenhum texto para formatar.");
        }
        String statement = stripTrailingSemicolon(sql.strip());

        List<Boundary> boundaries = scanTopLevel(statement, CLAUSE_PATTERNS).stream()
                .map(m -> new Boundary(CLAUSE_TYPES.get(m[2]), m[0], m[1]))
                .toList();

        boolean hasSelect = boundaries.stream().anyMatch(b -> b.type() == ClauseType.SELECT);
        boolean hasFrom = boundaries.stream().anyMatch(b -> b.type() == ClauseType.FROM);
        if (!hasSelect || !hasFrom) {
            throw new UnformattableQueryException("SELECT/FROM não encontrados no texto capturado.");
        }

        List<String> clauses = new ArrayList<>();
        for (int i = 0; i < boundaries.size(); i++) {
            Boundary boundary = boundaries.get(i);
            int bodyEnd = (i + 1 < boundaries.size()) ? boundaries.get(i + 1).start() : statement.length();
            String header = statement.substring(boundary.start(), boundary.end());
            String body = statement.substring(boundary.end(), bodyEnd);
            clauses.add(formatClause(boundary.type(), header, body));
        }
        return String.join("\n", clauses);
    }

    private String stripTrailingSemicolon(String statement) {
        return statement.endsWith(";") ? statement.substring(0, statement.length() - 1).stripTrailing() : statement;
    }

    private String formatClause(ClauseType type, String header, String body) {
        return switch (type) {
            case SELECT -> formatSelect(body);
            case FROM -> formatCommaBlock("FROM", body);
            case JOIN -> formatJoin(header, body);
            case WHERE -> formatPredicateList("WHERE", body);
            case HAVING -> formatPredicateList("HAVING", body);
            case GROUP_BY -> formatHangingCommaList("GROUP BY", body);
            case ORDER_BY -> formatHangingCommaList("ORDER BY", body);
        };
    }

    private String formatSelect(String body) {
        Matcher distinctMatcher = DISTINCT_PREFIX_PATTERN.matcher(body.strip());
        boolean distinct = distinctMatcher.lookingAt();
        String columnsText = distinct ? body.strip().substring(distinctMatcher.end()) : body;

        List<String> columns = splitTopLevelCommas(columnsText);
        if (columns.isEmpty()) {
            throw new UnformattableQueryException("Nenhuma coluna encontrada no SELECT.");
        }
        String header = distinct ? "SELECT DISTINCT" : "SELECT";
        return header + "\n    " + String.join(",\n    ", columns);
    }

    /**
     * SELECT/FROM style: the keyword stands alone on its own line, every item gets its
     * own 4-space-indented line, and only the item list is comma-separated. Each item is
     * a table reference, so it is qualified with the project's schema owner.
     */
    private String formatCommaBlock(String keyword, String body) {
        List<String> items = splitTopLevelCommas(body);
        if (items.isEmpty()) {
            throw new UnformattableQueryException("Nenhum item encontrado em " + keyword + ".");
        }
        List<String> qualified = items.stream().map(this::qualifyTableReference).toList();
        return keyword + "\n    " + String.join(",\n    ", qualified);
    }

    /**
     * Prefixes {@code tableReference} with the configured schema owner, unless no owner
     * is configured, it is a subquery (starts with "("), or it already names a schema
     * (an identifier followed by a dot) — any of those is left untouched so the owner is
     * never applied twice or to something that isn't a bare table name.
     */
    private String qualifyTableReference(String tableReference) {
        if (ownerPrefix.isEmpty() || tableReference.isEmpty() || tableReference.charAt(0) == '(') {
            return tableReference;
        }
        Matcher matcher = QUALIFIED_NAME_PATTERN.matcher(tableReference);
        if (!matcher.lookingAt() || matcher.group(2) != null) {
            return tableReference;
        }
        return ownerPrefix + tableReference;
    }

    /**
     * GROUP BY/ORDER BY style: the keyword stands alone on its own line with a trailing
     * space, and every item — including the first — gets the same 4-space indent, just
     * like SELECT/FROM.
     */
    private String formatHangingCommaList(String keyword, String body) {
        List<String> items = splitTopLevelCommas(body);
        if (items.isEmpty()) {
            throw new UnformattableQueryException("Nenhuma coluna encontrada em " + keyword + ".");
        }
        return keyword + " \n    " + String.join(",\n    ", items);
    }

    /**
     * WHERE/HAVING style: a hanging hint where the first predicate sits one indent level
     * deeper (8 spaces) than the AND/OR-prefixed predicates that follow (4 spaces plus
     * the 4-character operator), so every predicate's own text lines up at column 8 —
     * the same convention used for a JOIN's ON conditions.
     */
    private String formatPredicateList(String keyword, String body) {
        List<ConditionPart> conditions = splitConditions(body);
        if (conditions.isEmpty()) {
            throw new UnformattableQueryException("Nenhuma condição encontrada em " + keyword + ".");
        }
        return keyword + "\n" + renderConditions(conditions);
    }

    private String formatJoin(String header, String body) {
        String joinHeader = normalizeWhitespace(header).toUpperCase();

        int[] onRange = findTopLevelOn(body);
        if (onRange == null) {
            return joinHeader + " " + qualifyTableReference(normalizeWhitespace(body));
        }

        String tableAlias = qualifyTableReference(normalizeWhitespace(body.substring(0, onRange[0])));
        List<ConditionPart> conditions = splitConditions(body.substring(onRange[1]));
        if (conditions.isEmpty()) {
            return joinHeader + " " + tableAlias + " ON";
        }
        return joinHeader + " " + tableAlias + " ON\n" + renderConditions(conditions);
    }

    private String renderConditions(List<ConditionPart> conditions) {
        List<ConditionPart> aligned = alignEquals(conditions);
        StringBuilder sb = new StringBuilder("        ").append(aligned.get(0).text());
        for (int i = 1; i < aligned.size(); i++) {
            ConditionPart part = aligned.get(i);
            String operator = part.operator() != null ? part.operator() : "AND";
            sb.append("\n    ").append(operator).append(' ').append(part.text());
        }
        return sb.toString();
    }

    /**
     * Pads every condition's left-hand side up to the widest one in the block so their
     * "=" signs land in the same column — purely cosmetic, so a condition that isn't a
     * plain top-level equality (IS NULL, LIKE, IN (...), a comparison using &lt;, &gt;,
     * &lt;=, &gt;=, &lt;&gt; or !=) is left untouched and also excluded from the width
     * calculation, since padding it to match would misalign everything else instead.
     */
    private List<ConditionPart> alignEquals(List<ConditionPart> conditions) {
        int maxLhsLength = 0;
        for (ConditionPart part : conditions) {
            int equalsIndex = findTopLevelEquals(part.text());
            if (equalsIndex >= 0) {
                maxLhsLength = Math.max(maxLhsLength, part.text().substring(0, equalsIndex).stripTrailing().length());
            }
        }
        if (maxLhsLength == 0) {
            return conditions;
        }

        List<ConditionPart> aligned = new ArrayList<>(conditions.size());
        for (ConditionPart part : conditions) {
            int equalsIndex = findTopLevelEquals(part.text());
            if (equalsIndex < 0) {
                aligned.add(part);
                continue;
            }
            String lhs = part.text().substring(0, equalsIndex).stripTrailing();
            String rhs = part.text().substring(equalsIndex + 1).stripLeading();
            String padding = " ".repeat(maxLhsLength - lhs.length());
            aligned.add(new ConditionPart(part.operator(), lhs + padding + " = " + rhs));
        }
        return aligned;
    }

    /**
     * Finds a top-level, plain-equality "=" — one that isn't part of &lt;=, &gt;=, &lt;&gt;
     * or != — outside string literals and parentheses, so an "=" belonging to a nested
     * subquery's own condition is never mistaken for this condition's comparison.
     */
    private int findTopLevelEquals(String text) {
        int depth = 0;
        boolean inQuotes = false;
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
            } else if (c == '=' && depth == 0) {
                char previous = i > 0 ? text.charAt(i - 1) : ' ';
                if (previous != '<' && previous != '>' && previous != '!') {
                    return i;
                }
            }
        }
        return -1;
    }

    private int[] findTopLevelOn(String text) {
        List<int[]> matches = scanTopLevel(text, List.of(ON_PATTERN));
        return matches.isEmpty() ? null : matches.get(0);
    }

    /**
     * Splits on top-level AND/OR, keeping track of which operator preceded each piece
     * (null for the first) so the caller can render "AND "/"OR " with the operator the
     * query actually used, rather than assuming AND throughout.
     */
    private List<ConditionPart> splitConditions(String text) {
        List<ConditionPart> parts = new ArrayList<>();
        List<int[]> operatorMatches = scanTopLevel(text, List.of(AND_OR_PATTERN));

        int start = 0;
        String pendingOperator = null;
        for (int[] match : operatorMatches) {
            String piece = normalizeWhitespace(text.substring(start, match[0]));
            if (!piece.isEmpty()) {
                parts.add(new ConditionPart(pendingOperator, piece));
                pendingOperator = text.substring(match[0], match[1]).trim().toUpperCase();
            }
            start = match[1];
        }
        String lastPiece = normalizeWhitespace(text.substring(start));
        if (!lastPiece.isEmpty()) {
            parts.add(new ConditionPart(pendingOperator, lastPiece));
        }
        return parts;
    }

    private List<String> splitTopLevelCommas(String text) {
        return splitTopLevel(text, ',').stream()
                .map(this::normalizeWhitespace)
                .filter(part -> !part.isEmpty())
                .toList();
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
                parts.add(text.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(text.substring(start));
        return parts;
    }

    /**
     * Scans {@code text} left to right at parenthesis depth zero and outside string
     * literals, trying each pattern (in order) at every such position. Returns every
     * match found as {@code {start, end, patternIndex}}, in scan order, and resumes
     * scanning right after each match so a keyword is never matched twice.
     */
    private List<int[]> scanTopLevel(String text, List<Pattern> patterns) {
        List<int[]> matches = new ArrayList<>();
        int depth = 0;
        boolean inQuotes = false;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (inQuotes) {
                inQuotes = c != '\'';
                i++;
                continue;
            }
            if (c == '\'') {
                inQuotes = true;
                i++;
                continue;
            }
            if (c == '(') {
                depth++;
                i++;
                continue;
            }
            if (c == ')') {
                depth--;
                i++;
                continue;
            }
            if (depth == 0) {
                int matched = tryMatch(text, i, patterns, matches);
                if (matched >= 0) {
                    i = matched;
                    continue;
                }
            }
            i++;
        }
        return matches;
    }

    private int tryMatch(String text, int at, List<Pattern> patterns, List<int[]> matches) {
        for (int p = 0; p < patterns.size(); p++) {
            Matcher matcher = patterns.get(p).matcher(text).region(at, text.length());
            matcher.useTransparentBounds(true);
            if (matcher.lookingAt()) {
                matches.add(new int[] {at, matcher.end(), p});
                return matcher.end();
            }
        }
        return -1;
    }

    /**
     * Collapses any run of whitespace (including newlines carried over from the
     * original multi-line input) into a single space, without touching whitespace
     * inside a string literal.
     */
    private String normalizeWhitespace(String text) {
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        boolean pendingSpace = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                sb.append(c);
                if (c == '\'') {
                    inQuotes = false;
                }
                continue;
            }
            if (c == '\'') {
                inQuotes = true;
                if (pendingSpace && !sb.isEmpty()) {
                    sb.append(' ');
                }
                pendingSpace = false;
                sb.append(c);
                continue;
            }
            if (Character.isWhitespace(c)) {
                pendingSpace = true;
                continue;
            }
            if (pendingSpace && !sb.isEmpty()) {
                sb.append(' ');
            }
            pendingSpace = false;
            sb.append(c);
        }
        return sb.toString();
    }
}
