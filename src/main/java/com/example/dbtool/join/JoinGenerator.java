package com.example.dbtool.join;

import com.example.dbtool.model.ForeignKey;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds JOIN clauses from foreign key metadata. Contains no database access —
 * it only formats SQL from the relationships it is given.
 */
public class JoinGenerator {

    public String generate(String table, String alias, String sourceAlias, List<ForeignKey> relationships) {
        return generate(table, alias, sourceAlias, resolveSingle(sourceAlias, table, relationships));
    }

    public String generate(String table, String alias, String sourceAlias, ForeignKey fk) {
        String conditions = String.join("\n AND ", buildConditions(table, alias, sourceAlias, fk));
        return "JOIN " + table + " " + alias + "\n  ON " + conditions;
    }

    /**
     * Same relationship resolution as {@link #generate}, but returns just the condition
     * list (e.g. "PED.COL = OE.COL") without the "JOIN table alias" prefix — used to
     * complete an ON clause the user is already typing.
     */
    public List<String> generateConditionList(String table, String alias, String sourceAlias,
                                               List<ForeignKey> relationships) {
        ForeignKey fk = resolveSingle(sourceAlias, table, relationships);
        return buildConditions(table, alias, sourceAlias, fk);
    }

    private ForeignKey resolveSingle(String sourceAlias, String table, List<ForeignKey> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            throw new NoRelationshipException(sourceAlias, table);
        }
        if (relationships.size() > 1) {
            throw new AmbiguousRelationshipException(sourceAlias, table, relationships);
        }
        return relationships.get(0);
    }

    private List<String> buildConditions(String table, String alias, String sourceAlias, ForeignKey fk) {
        List<ForeignKey.ColumnPair> pairs = resolveColumnPairs(table, fk);
        return pairs.stream()
                .map(pair -> sourceAlias + "." + pair.columnName() + " = " + alias + "." + pair.referencedColumnName())
                .collect(Collectors.toList());
    }

    /**
     * A ForeignKey's tableName/referencedTableName record which side owns the constraint.
     * The generated SQL always reads as sourceAlias.column = alias.column, so when the
     * target table is the constraint owner the pairs must be flipped.
     */
    private List<ForeignKey.ColumnPair> resolveColumnPairs(String table, ForeignKey fk) {
        if (fk.tableName().equalsIgnoreCase(table)) {
            return fk.columns().stream()
                    .map(pair -> new ForeignKey.ColumnPair(pair.referencedColumnName(), pair.columnName()))
                    .toList();
        }
        if (fk.referencedTableName().equalsIgnoreCase(table)) {
            return fk.columns();
        }
        throw new IllegalArgumentException(
                "Foreign key '" + fk.constraintName() + "' does not relate to table '" + table + "'");
    }
}
