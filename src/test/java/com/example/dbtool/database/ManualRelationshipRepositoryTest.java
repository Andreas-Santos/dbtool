package com.example.dbtool.database;

import com.example.dbtool.model.ForeignKey;
import com.example.dbtool.model.ForeignKey.ColumnPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualRelationshipRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnEmptyWhenFileDoesNotExist() {
        ManualRelationshipRepository repo = new ManualRelationshipRepository(tempDir.resolve("missing.conf"));

        assertTrue(repo.findRelationship("A", "B").isEmpty());
    }

    @Test
    void shouldParseSingleColumnRelationship() throws IOException {
        Path file = writeFile("VEN_PEDIDOVENDA -> VEN_EXPEDICAO: PED_IN_CODIGO=PED_IN_CODIGO");
        ManualRelationshipRepository repo = new ManualRelationshipRepository(file);

        List<ForeignKey> found = repo.findRelationship("VEN_PEDIDOVENDA", "VEN_EXPEDICAO");

        assertEquals(1, found.size());
        assertEquals("VEN_PEDIDOVENDA", found.get(0).tableName());
        assertEquals("VEN_EXPEDICAO", found.get(0).referencedTableName());
        assertEquals(List.of(new ForeignKey.ColumnPair("PED_IN_CODIGO", "PED_IN_CODIGO")), found.get(0).columns());
    }

    @Test
    void shouldParseMultiColumnRelationship() throws IOException {
        Path file = writeFile(
                "VEN_PEDIDOVENDA -> GLO_ACAO: ORG_TAB_IN_CODIGO=ORG_TAB_IN_CODIGO, ORG_PAD_IN_CODIGO=ORG_PAD_IN_CODIGO, ORG_IN_CODIGO=ORG_IN_CODIGO");
        ManualRelationshipRepository repo = new ManualRelationshipRepository(file);

        List<ForeignKey> found = repo.findRelationship("VEN_PEDIDOVENDA", "GLO_ACAO");

        assertEquals(1, found.size());
        assertEquals(3, found.get(0).columns().size());
    }

    @Test
    void shouldFindRelationshipRegardlessOfLookupDirection() throws IOException {
        Path file = writeFile("VEN_PEDIDOVENDA -> VEN_EXPEDICAO: PED_IN_CODIGO=PED_IN_CODIGO");
        ManualRelationshipRepository repo = new ManualRelationshipRepository(file);

        assertEquals(1, repo.findRelationship("VEN_EXPEDICAO", "VEN_PEDIDOVENDA").size());
    }

    @Test
    void shouldIgnoreBlankLinesAndComments() throws IOException {
        Path file = writeFile(
                "# comentario",
                "",
                "VEN_PEDIDOVENDA -> VEN_EXPEDICAO: PED_IN_CODIGO=PED_IN_CODIGO");
        ManualRelationshipRepository repo = new ManualRelationshipRepository(file);

        assertEquals(1, repo.findRelationship("VEN_PEDIDOVENDA", "VEN_EXPEDICAO").size());
    }

    @Test
    void shouldThrowOnMalformedLine() throws IOException {
        Path file = writeFile("VEN_PEDIDOVENDA VEN_EXPEDICAO PED_IN_CODIGO");

        assertThrows(IllegalArgumentException.class, () -> new ManualRelationshipRepository(file));
    }

    @Test
    void shouldAddAndPersistWhenMissing() {
        Path file = tempDir.resolve("manual-relationships.conf");
        ManualRelationshipRepository repo = new ManualRelationshipRepository(file);

        boolean added = repo.addIfMissing("VEN_PEDIDOVENDA", "VEN_EXPEDICAO",
                List.of(new ColumnPair("PED_IN_CODIGO", "PED_IN_CODIGO")));

        assertTrue(added);
        assertEquals(1, repo.findRelationship("VEN_PEDIDOVENDA", "VEN_EXPEDICAO").size());
        assertTrue(Files.exists(file));

        ManualRelationshipRepository reloaded = new ManualRelationshipRepository(file);
        assertEquals(1, reloaded.findRelationship("VEN_PEDIDOVENDA", "VEN_EXPEDICAO").size());
    }

    @Test
    void shouldNotAddWhenRelationshipAlreadyExists() throws IOException {
        Path file = writeFile("VEN_PEDIDOVENDA -> VEN_EXPEDICAO: PED_IN_CODIGO=PED_IN_CODIGO");
        ManualRelationshipRepository repo = new ManualRelationshipRepository(file);

        boolean added = repo.addIfMissing("VEN_EXPEDICAO", "VEN_PEDIDOVENDA",
                List.of(new ColumnPair("OUTRA_COL", "OUTRA_COL")));

        assertFalse(added);
        assertEquals(1, repo.findRelationship("VEN_PEDIDOVENDA", "VEN_EXPEDICAO").size());
    }

    private Path writeFile(String... lines) throws IOException {
        Path file = tempDir.resolve("manual-relationships.conf");
        Files.write(file, List.of(lines));
        return file;
    }
}
