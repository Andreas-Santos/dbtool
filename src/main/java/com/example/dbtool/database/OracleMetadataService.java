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

/**
 * Reads table/column/constraint metadata from Oracle's ALL_* data dictionary views,
 * scoped to a single schema (owner).
 */
public class OracleMetadataService implements MetadataService {

    private final DatabaseConnection connection;
    private final String owner;

    public OracleMetadataService(DatabaseConnection connection, String owner) {
        this.connection = connection;
        this.owner = owner.toUpperCase();
    }

    @Override
    public List<Table> getTables() {
        String sql = "SELECT table_name FROM all_tables WHERE owner = ? ORDER BY table_name";
        List<Table> tables = new ArrayList<>();
        try (PreparedStatement stmt = connection.open().prepareStatement(sql)) {
            stmt.setString(1, owner);
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
        String sql = """
                SELECT column_name, data_type, nullable
                FROM all_tab_columns
                WHERE owner = ? AND table_name = ?
                ORDER BY column_id
                """;
        List<Column> columns = new ArrayList<>();
        try (PreparedStatement stmt = connection.open().prepareStatement(sql)) {
            stmt.setString(1, owner);
            stmt.setString(2, tableName.toUpperCase());
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
        return columns;
    }

    @Override
    public List<String> getPrimaryKeyColumns(String tableName) {
        String sql = """
                SELECT cc.column_name
                FROM all_constraints c
                JOIN all_cons_columns cc
                  ON cc.constraint_name = c.constraint_name
                 AND cc.owner = c.owner
                WHERE c.constraint_type = 'P'
                  AND c.owner = ?
                  AND c.table_name = ?
                ORDER BY cc.position
                """;
        List<String> pkColumns = new ArrayList<>();
        try (PreparedStatement stmt = connection.open().prepareStatement(sql)) {
            stmt.setString(1, owner);
            stmt.setString(2, tableName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pkColumns.add(rs.getString("column_name"));
                }
            }
        } catch (SQLException e) {
            throw new MetadataAccessException("Failed to read primary key for table " + tableName, e);
        }
        return pkColumns;
    }

    @Override
    public List<ForeignKey> getForeignKeys(String tableName) {
        String sql = """
                SELECT fk.constraint_name  AS constraint_name,
                       fk.table_name       AS table_name,
                       pk.table_name       AS referenced_table_name,
                       fkcol.column_name   AS column_name,
                       pkcol.column_name   AS referenced_column_name,
                       fkcol.position      AS position
                FROM all_constraints fk
                JOIN all_constraints pk
                  ON fk.r_constraint_name = pk.constraint_name
                 AND fk.r_owner = pk.owner
                JOIN all_cons_columns fkcol
                  ON fkcol.constraint_name = fk.constraint_name
                 AND fkcol.owner = fk.owner
                JOIN all_cons_columns pkcol
                  ON pkcol.constraint_name = pk.constraint_name
                 AND pkcol.owner = pk.owner
                 AND pkcol.position = fkcol.position
                WHERE fk.constraint_type = 'R'
                  AND fk.owner = ?
                  AND fk.table_name = ?
                ORDER BY fk.constraint_name, fkcol.position
                """;

        Map<String, String> tableNameByConstraint = new LinkedHashMap<>();
        Map<String, String> referencedTableByConstraint = new LinkedHashMap<>();
        Map<String, List<ColumnPair>> columnsByConstraint = new LinkedHashMap<>();

        try (PreparedStatement stmt = connection.open().prepareStatement(sql)) {
            stmt.setString(1, owner);
            stmt.setString(2, tableName.toUpperCase());
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
            throw new MetadataAccessException("Failed to read foreign keys for table " + tableName, e);
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

    @Override
    public List<ForeignKey> findRelationship(String tableA, String tableB) {
        String upperB = tableB.toUpperCase();
        String upperA = tableA.toUpperCase();

        List<ForeignKey> relationships = new ArrayList<>();
        getForeignKeys(tableA).stream()
                .filter(fk -> fk.referencedTableName().equalsIgnoreCase(upperB))
                .forEach(relationships::add);
        getForeignKeys(tableB).stream()
                .filter(fk -> fk.referencedTableName().equalsIgnoreCase(upperA))
                .forEach(relationships::add);
        return relationships;
    }
}
