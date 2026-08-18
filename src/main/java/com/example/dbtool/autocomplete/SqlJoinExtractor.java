package com.example.dbtool.autocomplete;

import com.example.dbtool.model.ForeignKey.ColumnPair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reverse-engineers table relationships from SQL that's already fully written: for each
 * JOIN ... ON clause, reads its actual conditions (alias.col = alias.col) to find which
 * previously-declared table it relates to and which columns pair up. Used to harvest
 * relationships into config/manual-relationships.conf from queries the user already
 * got working, rather than requiring them to type the file by hand.
 */
public class SqlJoinExtractor {

    private static final Pattern FROM_PATTERN =
            Pattern.compile("(?i)\\bFROM\\s+([A-Za-z0-9_$#]+)\\s+([A-Za-z0-9_$#]+)");
    private static final Pattern JOIN_PATTERN =
            Pattern.compile("(?i)\\bJOIN\\s+([A-Za-z0-9_$#]+)\\s+([A-Za-z0-9_$#]+)\\s+ON\\b");
    private static final Pattern BOUNDARY_PATTERN =
            Pattern.compile("(?i)\\b(WHERE|GROUP\\s+BY|ORDER\\s+BY|HAVING)\\b");
    private static final Pattern CONDITION_PATTERN =
            Pattern.compile("([A-Za-z0-9_$#]+)\\.([A-Za-z0-9_$#]+)\\s*=\\s*([A-Za-z0-9_$#]+)\\.([A-Za-z0-9_$#]+)");

    public List<ExtractedJoinRelationship> extractAll(String sqlText) {
        List<ExtractedJoinRelationship> results = new ArrayList<>();
        for (String statement : sqlText.split(";")) {
            results.addAll(extractFromStatement(statement));
        }
        return results;
    }

    private List<ExtractedJoinRelationship> extractFromStatement(String statement) {
        List<ExtractedJoinRelationship> results = new ArrayList<>();

        Map<String, String> declaredAliases = new LinkedHashMap<>();
        Matcher fromMatcher = FROM_PATTERN.matcher(statement);
        if (fromMatcher.find()) {
            declaredAliases.put(fromMatcher.group(2).toUpperCase(), fromMatcher.group(1));
        }

        List<JoinOccurrence> joins = new ArrayList<>();
        Matcher joinMatcher = JOIN_PATTERN.matcher(statement);
        while (joinMatcher.find()) {
            joins.add(new JoinOccurrence(joinMatcher.group(1), joinMatcher.group(2),
                    joinMatcher.start(), joinMatcher.end()));
        }

        for (int i = 0; i < joins.size(); i++) {
            JoinOccurrence join = joins.get(i);
            declaredAliases.putIfAbsent(join.alias().toUpperCase(), join.table());

            int blockEnd = (i + 1 < joins.size()) ? joins.get(i + 1).joinStart() : findBoundary(statement, join.onEnd());
            String conditionsBlock = statement.substring(join.onEnd(), blockEnd);

            ExtractedJoinRelationship relationship = extractRelationship(join, conditionsBlock, declaredAliases);
            if (relationship != null) {
                results.add(relationship);
            }
        }
        return results;
    }

    private ExtractedJoinRelationship extractRelationship(JoinOccurrence join, String conditionsBlock,
                                                            Map<String, String> declaredAliases) {
        String sourceTable = null;
        String sourceAlias = null;
        List<ColumnPair> pairs = new ArrayList<>();

        Matcher condMatcher = CONDITION_PATTERN.matcher(conditionsBlock);
        while (condMatcher.find()) {
            String leftAlias = condMatcher.group(1);
            String leftCol = condMatcher.group(2);
            String rightAlias = condMatcher.group(3);
            String rightCol = condMatcher.group(4);

            String otherAlias;
            String otherCol;
            String selfCol;
            if (leftAlias.equalsIgnoreCase(join.alias())) {
                otherAlias = rightAlias;
                otherCol = rightCol;
                selfCol = leftCol;
            } else if (rightAlias.equalsIgnoreCase(join.alias())) {
                otherAlias = leftAlias;
                otherCol = leftCol;
                selfCol = rightCol;
            } else {
                continue;
            }

            if (sourceAlias == null) {
                String otherTable = declaredAliases.get(otherAlias.toUpperCase());
                if (otherTable == null) {
                    continue;
                }
                sourceAlias = otherAlias;
                sourceTable = otherTable;
            }
            if (!otherAlias.equalsIgnoreCase(sourceAlias)) {
                continue;
            }
            pairs.add(new ColumnPair(otherCol, selfCol));
        }

        return (sourceTable != null && !pairs.isEmpty())
                ? new ExtractedJoinRelationship(sourceTable, join.table(), pairs)
                : null;
    }

    private int findBoundary(String statement, int from) {
        Matcher boundaryMatcher = BOUNDARY_PATTERN.matcher(statement);
        return boundaryMatcher.find(from) ? boundaryMatcher.start() : statement.length();
    }

    private record JoinOccurrence(String table, String alias, int joinStart, int onEnd) {
    }
}
