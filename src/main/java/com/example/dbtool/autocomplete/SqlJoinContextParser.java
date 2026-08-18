package com.example.dbtool.autocomplete;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts, from the SQL text preceding the cursor, the table currently being joined
 * (the last JOIN ... ON of the CURRENT statement — text after the last ';') and which
 * previously-declared table/alias it should join against. That source table is always
 * whatever alias was typed right after ON (e.g. "EQP." picks the table declared for
 * EQP, even if it's an earlier JOIN rather than the FROM table) — there is no implicit
 * default, since guessing the FROM table is wrong whenever the real relationship is
 * with an earlier JOIN instead.
 */
public class SqlJoinContextParser {

    private static final Pattern FROM_PATTERN =
            Pattern.compile("(?i)\\bFROM\\s+([A-Za-z0-9_$#]+)\\s+([A-Za-z0-9_$#]+)");
    private static final Pattern JOIN_ON_PATTERN =
            Pattern.compile("(?i)\\bJOIN\\s+([A-Za-z0-9_$#]+)\\s+([A-Za-z0-9_$#]+)\\s+ON\\b");
    private static final Pattern TYPED_ALIAS_PATTERN =
            Pattern.compile("^([A-Za-z0-9_$#]+)\\.$");

    public SqlJoinContext parse(String textBeforeCursor) {
        String statement = currentStatement(textBeforeCursor);

        Matcher fromMatcher = FROM_PATTERN.matcher(statement);
        if (!fromMatcher.find()) {
            throw new NoJoinContextFoundException("FROM não encontrado no texto capturado.");
        }
        String fromTable = fromMatcher.group(1);
        String fromAlias = fromMatcher.group(2);

        Map<String, AliasedTable> declaredAliases = new LinkedHashMap<>();
        declaredAliases.put(fromAlias.toUpperCase(), new AliasedTable(fromTable, fromAlias));

        Matcher joinMatcher = JOIN_ON_PATTERN.matcher(statement);
        String joinedTable = null;
        String joinedAlias = null;
        int matchEnd = -1;
        boolean foundAnyJoin = false;
        while (joinMatcher.find()) {
            if (foundAnyJoin) {
                // the previous match is now a fully-declared JOIN, available as a source table
                declaredAliases.put(joinedAlias.toUpperCase(), new AliasedTable(joinedTable, joinedAlias));
            }
            joinedTable = joinMatcher.group(1);
            joinedAlias = joinMatcher.group(2);
            matchEnd = joinMatcher.end();
            foundAnyJoin = true;
        }
        if (!foundAnyJoin) {
            throw new NoJoinContextFoundException("Nenhum JOIN ... ON encontrado no texto capturado.");
        }

        String trailing = statement.substring(matchEnd).strip();

        Matcher typedAliasMatcher = TYPED_ALIAS_PATTERN.matcher(trailing);
        if (!typedAliasMatcher.matches()) {
            throw new NoJoinContextFoundException(
                    "Digite o alias da tabela de origem logo após o ON (ex: " + fromAlias + ".) antes de usar o atalho.");
        }

        String typedAlias = typedAliasMatcher.group(1);
        if (typedAlias.equalsIgnoreCase(joinedAlias)) {
            throw new NoJoinContextFoundException(
                    "'" + typedAlias + "' é o alias da própria tabela sendo unida (" + joinedTable
                            + "); digite o alias de uma tabela já declarada antes deste JOIN.");
        }

        AliasedTable source = declaredAliases.get(typedAlias.toUpperCase());
        if (source == null) {
            throw new NoJoinContextFoundException(
                    "Alias '" + typedAlias + "' não foi declarado antes deste JOIN.");
        }

        return new SqlJoinContext(source.table(), source.alias(), joinedTable, joinedAlias);
    }

    /**
     * The editor may hold several ';'-separated statements; only the one the cursor
     * is currently in should feed the FROM/JOIN search.
     */
    private String currentStatement(String textBeforeCursor) {
        int lastSemicolon = textBeforeCursor.lastIndexOf(';');
        return lastSemicolon >= 0 ? textBeforeCursor.substring(lastSemicolon + 1) : textBeforeCursor;
    }

    private record AliasedTable(String table, String alias) {
    }
}
