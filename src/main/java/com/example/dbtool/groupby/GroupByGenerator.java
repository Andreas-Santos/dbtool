package com.example.dbtool.groupby;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the column list for a GROUP BY clause from a query's SELECT columns:
 * aggregate expressions (SUM, COUNT, AVG, ...) don't belong in GROUP BY and are
 * dropped entirely; every other expression keeps its table-qualified reference
 * (e.g. {@code PED.STATUS}) but loses its column alias, since GROUP BY must match
 * the underlying expression exactly, not the name it's exposed as in the SELECT.
 */
public class GroupByGenerator {

    private static final Pattern AGGREGATE_CALL_PATTERN = Pattern.compile(
            "(?i)^(SUM|COUNT|AVG|MIN|MAX|STDDEV|STDDEV_POP|STDDEV_SAMP|"
                    + "VARIANCE|VAR_POP|VAR_SAMP|LISTAGG|MEDIAN|CORR|COVAR_POP|COVAR_SAMP)\\s*\\(");
    private static final Pattern AS_ALIAS_PATTERN = Pattern.compile("(?i)\\bAS\\s+[A-Za-z0-9_$#]+$");
    private static final Pattern IMPLICIT_ALIAS_PATTERN = Pattern.compile("[\\s)]([A-Za-z_][A-Za-z0-9_$#]*)$");

    public String generate(List<String> selectColumns) {
        return "GROUP BY " + generateColumnList(selectColumns);
    }

    /**
     * Same column resolution as {@link #generate}, but without the "GROUP BY" prefix —
     * used to complete a GROUP BY clause the user already started typing.
     */
    public String generateColumnList(List<String> selectColumns) {
        List<String> groupByColumns = buildGroupByColumns(selectColumns);
        if (groupByColumns.isEmpty()) {
            throw new NoSelectColumnsFoundException(
                    "Nenhuma coluna agrupável encontrada — todas as colunas do SELECT são agregações.");
        }
        return String.join(",\n         ", groupByColumns);
    }

    private List<String> buildGroupByColumns(List<String> selectColumns) {
        List<String> result = new ArrayList<>();
        for (String column : selectColumns) {
            String expression = column.trim();
            if (!AGGREGATE_CALL_PATTERN.matcher(expression).find()) {
                result.add(stripAlias(expression));
            }
        }
        return result;
    }

    /**
     * Strips a trailing "AS alias" or implicit "expr alias" from a non-aggregate
     * expression, leaving everything else (including a table-qualified prefix like
     * "PED.") untouched.
     */
    private String stripAlias(String expression) {
        Matcher asAlias = AS_ALIAS_PATTERN.matcher(expression);
        if (asAlias.find()) {
            return expression.substring(0, asAlias.start()).trim();
        }

        Matcher implicitAlias = IMPLICIT_ALIAS_PATTERN.matcher(expression);
        if (implicitAlias.find()) {
            return expression.substring(0, implicitAlias.start()).trim();
        }

        return expression;
    }
}
