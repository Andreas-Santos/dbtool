package com.example.dbtool.database;

import com.example.dbtool.model.Column;
import com.example.dbtool.model.ForeignKey;
import com.example.dbtool.model.ForeignKey.ColumnPair;
import com.example.dbtool.model.Table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads table/column/constraint metadata from Oracle's USER_* data dictionary views.
 * USER_* is used instead of ALL_* or DBA_* on purpose: those views join against privilege
 * tables to filter rows the current user can see, which is a well-known source of slow
 * dictionary queries. Since the connection always logs in as the schema owner (see
 * MetadataServiceFactory), USER_* returns exactly the same rows without that overhead.
 * Results are cached per table name for the lifetime of this instance — a fresh instance
 * is created per DB connection (see MetadataServiceFactory/Main), so the cache can't go
 * stale within a session but never survives a reconnect.
 */
public class OracleMetadataService implements MetadataService {

    private static final int QUERY_TIMEOUT_SECONDS = 10;

    private final DatabaseConnection connection;
    private final String owner;

    private final Map<String, List<Column>> columnsCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> primaryKeyCache = new ConcurrentHashMap<>();
    private final Map<String, List<ForeignKey>> foreignKeyCache = new ConcurrentHashMap<>();

    public OracleMetadataService(DatabaseConnection connection, String owner) {
        this.connection = connection;
        this.owner = owner.toUpperCase();
    }

    @Override
    public List<Table> getTables() {
        String sql = "SELECT table_name FROM user_tables ORDER BY table_name";
        List<Table> tables = new ArrayList<>();
        try (PreparedStatement stmt = prepare(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(getTable(rs.getString("table_name")));
                }
            }
        } catch (SQLException e) {
            throw new MetadataAccessException("Failed to list tables for owner " + owner, e);
        }
        return tables;
    }

    @Override
    public Table getTable(String tableName) {
        return new Table(tableName.toUpperCase(), getColumns(tableName));
    }

    @Override
    public List<Column> getColumns(String tableName) {
        String key = tableName.toUpperCase();
        List<Column> cached = columnsCache.get(key);
        if (cached != null) {
            return cached;
        }

        String sql = """
                SELECT column_name, data_type, nullable
                FROM user_tab_columns
                WHERE table_name = ?
                ORDER BY column_id
                """;
        List<Column> columns = new ArrayList<>();
        try (PreparedStatement stmt = prepare(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    columns.add(new Column(
                            rs.getString("column_name"),
                            rs.getString("data_type"),
                            "Y".equals(rs.getString("nullable"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new MetadataAccessException("Failed to read columns for table " + tableName, e);
        }
        columnsCache.put(key, columns);
        return columns;
    }

    @Override
    public List<String> getPrimaryKeyColumns(String tableName) {
        String key = tableName.toUpperCase();
        List<String> cached = primaryKeyCache.get(key);
        if (cached != null) {
            return cached;
        }

        String sql = """
                SELECT cc.column_name
                FROM user_constraints c
                JOIN user_cons_columns cc
                  ON cc.constraint_name = c.constraint_name
                WHERE c.constraint_type = 'P'
                  AND c.table_name = ?
                ORDER BY cc.position
                """;
        List<String> pkColumns = new ArrayList<>();
        try (PreparedStatement stmt = prepare(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pkColumns.add(rs.getString("column_name"));
                }
            }
        } catch (SQLException e) {
            throw new MetadataAccessException("Failed to read primary key for table " + tableName, e);
        }
        primaryKeyCache.put(key, pkColumns);
        return pkColumns;
    }

    @Override
    public List<ForeignKey> getForeignKeys(String tableName) {
        String key = tableName.toUpperCase();
        List<ForeignKey> cached = foreignKeyCache.get(key);
        if (cached != null) {
            return cached;
        }

        List<ForeignKey> foreignKeys = queryForeignKeys("fk.table_name = ?", key);
        foreignKeyCache.put(key, foreignKeys);
        return foreignKeys;
    }

    /**
     * Resolves both tables' foreign keys in a single round trip instead of calling
     * {@link #getForeignKeys} twice (once per table) and filtering the results in Java —
     * that used to mean two full dictionary joins per JOIN completion, most of whose rows
     * were thrown away immediately after.
     */
    @Override
    public List<ForeignKey> findRelationship(String tableA, String tableB) {
        String upperA = tableA.toUpperCase();
        String upperB = tableB.toUpperCase();

        if (foreignKeyCache.containsKey(upperA) || foreignKeyCache.containsKey(upperB)) {
            return findRelationshipFromCache(upperA, upperB);
        }

        List<ForeignKey> relationships = queryForeignKeys(
                "((fk.table_name = ? AND pk.table_name = ?) OR (fk.table_name = ? AND pk.table_name = ?))",
                upperA, upperB, upperB, upperA);
        return relationships;
    }

    private List<ForeignKey> findRelationshipFromCache(String upperA, String upperB) {
        List<ForeignKey> relationships = new ArrayList<>();
        getForeignKeys(upperA).stream()
                .filter(fk -> fk.referencedTableName().equalsIgnoreCase(upperB))
                .forEach(relationships::add);
        getForeignKeys(upperB).stream()
                .filter(fk -> fk.referencedTableName().equalsIgnoreCase(upperA))
                .forEach(relationships::add);
        return relationships;
    }

    private List<ForeignKey> queryForeignKeys(String whereClause, String... params) {
        String sql = """
                SELECT fk.constraint_name  AS constraint_name,
                       fk.table_name       AS table_name,
                       pk.table_name       AS referenced_table_name,
                       fkcol.column_name   AS column_name,
                       pkcol.column_name   AS referenced_column_name,
                       fkcol.position      AS position
                FROM user_constraints fk
                JOIN user_constraints pk
                  ON fk.r_constraint_name = pk.constraint_name
                JOIN user_cons_columns fkcol
                  ON fkcol.constraint_name = fk.constraint_name
                JOIN user_cons_columns pkcol
                  ON pkcol.constraint_name = pk.constraint_name
                 AND pkcol.position = fkcol.position
                WHERE fk.constraint_type = 'R'
                  AND """ + whereClause + """

                ORDER BY fk.constraint_name, fkcol.position
                """;

        Map<String, String> tableNameByConstraint = new LinkedHashMap<>();
        Map<String, String> referencedTableByConstraint = new LinkedHashMap<>();
        Map<String, List<ColumnPair>> columnsByConstraint = new LinkedHashMap<>();

        try (PreparedStatement stmt = prepare(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setString(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String constraintName = rs.getString("constraint_name");
                    tableNameByConstraint.putIfAbsent(constraintName, rs.getString("table_name"));
                    referencedTableByConstraint.putIfAbsent(constraintName, rs.getString("referenced_table_name"));
                    columnsByConstraint
                            .computeIfAbsent(constraintName, k -> new ArrayList<>())
                            .add(new ColumnPair(rs.getString("column_name"), rs.getString("referenced_column_name")));
                }
            }
        } catch (SQLException e) {
            throw new MetadataAccessException("Failed to read foreign keys (owner " + owner + ")", e);
        }

        List<ForeignKey> foreignKeys = new ArrayList<>();
        for (String constraintName : columnsByConstraint.keySet()) {
            foreignKeys.add(new ForeignKey(
                    constraintName,
                    tableNameByConstraint.get(constraintName),
                    referencedTableByConstraint.get(constraintName),
                    columnsByConstraint.get(constraintName)
            ));
        }
        return foreignKeys;
    }

    private PreparedStatement prepare(String sql) throws SQLException {
        PreparedStatement stmt = connection.open().prepareStatement(sql);
        stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
        return stmt;
    }
}
