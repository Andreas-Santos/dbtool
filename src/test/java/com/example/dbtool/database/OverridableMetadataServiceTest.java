package com.example.dbtool.database;

import com.example.dbtool.model.Column;
import com.example.dbtool.model.ForeignKey;
import com.example.dbtool.model.ForeignKey.ColumnPair;
import com.example.dbtool.model.Table;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverridableMetadataServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPreferManualRelationshipOverDelegate() throws IOException {
        ForeignKey dbForeignKey = new ForeignKey("FK_DB", "A", "B", List.of(new ColumnPair("X", "X")));
        MetadataService delegate = new StubMetadataService(List.of(dbForeignKey));

        Path file = tempDir.resolve("manual-relationships.conf");
        Files.write(file, List.of("A -> B: MANUAL_COL=MANUAL_COL"));
        ManualRelationshipRepository manual = new ManualRelationshipRepository(file);

        OverridableMetadataService service = new OverridableMetadataService(delegate, manual);
        List<ForeignKey> result = service.findRelationship("A", "B");

        assertEquals(1, result.size());
        assertEquals("MANUAL_COL", result.get(0).columns().get(0).columnName());
    }

    @Test
    void shouldFallBackToDelegateWhenNoManualRelationshipExists() {
        ForeignKey dbForeignKey = new ForeignKey("FK_DB", "A", "B", List.of(new ColumnPair("X", "X")));
        MetadataService delegate = new StubMetadataService(List.of(dbForeignKey));
        ManualRelationshipRepository manual = new ManualRelationshipRepository(tempDir.resolve("missing.conf"));

        OverridableMetadataService service = new OverridableMetadataService(delegate, manual);
        List<ForeignKey> result = service.findRelationship("A", "B");

        assertEquals(1, result.size());
        assertEquals("FK_DB", result.get(0).constraintName());
    }

    private static class StubMetadataService implements MetadataService {

        private final List<ForeignKey> foreignKeys;

        StubMetadataService(List<ForeignKey> foreignKeys) {
            this.foreignKeys = foreignKeys;
        }

        @Override
        public List<Table> getTables() {
            return List.of();
        }

        @Override
        public Table getTable(String tableName) {
            return new Table(tableName, List.of());
        }

        @Override
        public List<Column> getColumns(String tableName) {
            return List.of();
        }

        @Override
        public List<String> getPrimaryKeyColumns(String tableName) {
            return List.of();
        }

        @Override
        public List<ForeignKey> getForeignKeys(String tableName) {
            return foreignKeys;
        }

        @Override
        public List<ForeignKey> findRelationship(String tableA, String tableB) {
            return foreignKeys;
        }
    }
}
