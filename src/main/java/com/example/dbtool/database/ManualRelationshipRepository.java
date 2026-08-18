package com.example.dbtool.database;

import com.example.dbtool.model.ForeignKey;
import com.example.dbtool.model.ForeignKey.ColumnPair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Relationships declared by hand for tables that have no real FK constraint in Oracle.
 * File format, one relationship per line:
 *
 *   TABLE_A -> TABLE_B: COL_A1=COL_B1, COL_A2=COL_B2
 *
 * Lines starting with # and blank lines are ignored. Missing file means no overrides.
 */
public class ManualRelationshipRepository {

    private final Path file;
    private final Map<String, List<ForeignKey>> relationshipsByPair;

    public ManualRelationshipRepository(Path file) {
        this.file = file;
        this.relationshipsByPair = Files.isRegularFile(file) ? parse(file) : new HashMap<>();
    }

    public List<ForeignKey> findRelationship(String tableA, String tableB) {
        return relationshipsByPair.getOrDefault(pairKey(tableA, tableB), List.of());
    }

    /**
     * Adds a relationship discovered by reading already-written SQL, unless one already
     * covers this table pair (in either direction). Updates both the in-memory lookup
     * and the backing file, so later lookups in the same run see it immediately.
     */
    public synchronized boolean addIfMissing(String tableA, String tableB, List<ColumnPair> columns) {
        if (!findRelationship(tableA, tableB).isEmpty()) {
            return false;
        }
        String upperA = tableA.toUpperCase();
        String upperB = tableB.toUpperCase();
        String constraintName = "MANUAL:" + upperA + ":" + upperB + ":auto";
        ForeignKey fk = new ForeignKey(constraintName, upperA, upperB, columns);

        relationshipsByPair.computeIfAbsent(pairKey(upperA, upperB), k -> new ArrayList<>()).add(fk);
        appendLine(upperA, upperB, columns);
        return true;
    }

    private void appendLine(String tableA, String tableB, List<ColumnPair> columns) {
        String columnsText = columns.stream()
                .map(pair -> pair.columnName() + "=" + pair.referencedColumnName())
                .collect(Collectors.joining(", "));
        String line = tableA + " -> " + tableB + ": " + columnsText + System.lineSeparator();
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to append to " + file, e);
        }
    }

    private static String pairKey(String a, String b) {
        String upperA = a.toUpperCase();
        String upperB = b.toUpperCase();
        return upperA.compareTo(upperB) <= 0 ? upperA + "|" + upperB : upperB + "|" + upperA;
    }

    private Map<String, List<ForeignKey>> parse(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + file, e);
        }

        Map<String, List<ForeignKey>> result = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            ForeignKey fk = parseLine(line, i + 1, file);
            result.computeIfAbsent(pairKey(fk.tableName(), fk.referencedTableName()), k -> new ArrayList<>()).add(fk);
        }
        return result;
    }

    private ForeignKey parseLine(String line, int lineNumber, Path file) {
        int colonIndex = line.indexOf(':');
        if (colonIndex < 0) {
            throw new IllegalArgumentException(
                    "Invalid line " + lineNumber + " in " + file + " (missing ':'): " + line);
        }

        String[] tables = line.substring(0, colonIndex).trim().split("->");
        if (tables.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid line " + lineNumber + " in " + file + " (expected 'TABLE_A -> TABLE_B'): " + line);
        }
        String tableA = tables[0].trim().toUpperCase();
        String tableB = tables[1].trim().toUpperCase();

        List<ColumnPair> pairs = new ArrayList<>();
        for (String pairText : line.substring(colonIndex + 1).split(",")) {
            String[] cols = pairText.trim().split("=");
            if (cols.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid column pair on line " + lineNumber + " in " + file + ": " + pairText);
            }
            pairs.add(new ColumnPair(cols[0].trim().toUpperCase(), cols[1].trim().toUpperCase()));
        }
        if (pairs.isEmpty()) {
            throw new IllegalArgumentException(
                    "Line " + lineNumber + " in " + file + " has no column pairs: " + line);
        }

        String constraintName = "MANUAL:" + tableA + ":" + tableB + ":" + lineNumber;
        return new ForeignKey(constraintName, tableA, tableB, pairs);
    }
}
