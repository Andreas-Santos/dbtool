package com.example.dbtool.autocomplete;

import com.example.dbtool.model.ForeignKey.ColumnPair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlJoinExtractorTest {

    private final SqlJoinExtractor extractor = new SqlJoinExtractor();

    @Test
    void shouldExtractMultiColumnRelationshipFromFromTable() {
        String sql = """
                SELECT *
                FROM VEN_PEDIDOVENDA PED
                INNER JOIN VEN_EXPEDICAO OE ON
                       PED.ORG_TAB_IN_CODIGO = OE.ORG_TAB_IN_CODIGO
                 AND PED.ORG_PAD_IN_CODIGO = OE.ORG_PAD_IN_CODIGO
                 AND PED.ORG_IN_CODIGO = OE.ORG_IN_CODIGO
                """;

        List<ExtractedJoinRelationship> found = extractor.extractAll(sql);

        assertEquals(1, found.size());
        ExtractedJoinRelationship rel = found.get(0);
        assertEquals("VEN_PEDIDOVENDA", rel.sourceTable());
        assertEquals("VEN_EXPEDICAO", rel.joinedTable());
        assertEquals(List.of(
                new ColumnPair("ORG_TAB_IN_CODIGO", "ORG_TAB_IN_CODIGO"),
                new ColumnPair("ORG_PAD_IN_CODIGO", "ORG_PAD_IN_CODIGO"),
                new ColumnPair("ORG_IN_CODIGO", "ORG_IN_CODIGO")), rel.columns());
    }

    @Test
    void shouldExtractRelationshipAgainstAnEarlierJoinNotTheFromTable() {
        String sql = """
                SELECT *
                FROM VEN_PEDIDOVENDA PED
                INNER JOIN VEN_EXPEDICAO OE ON
                       PED.ORG_TAB_IN_CODIGO = OE.ORG_TAB_IN_CODIGO
                 AND PED.ORG_PAD_IN_CODIGO = OE.ORG_PAD_IN_CODIGO
                 AND PED.ORG_IN_CODIGO = OE.ORG_IN_CODIGO
                 AND PED.ORG_TAU_ST_CODIGO = OE.ORG_TAU_ST_CODIGO
                 AND PED.SER_ST_CODIGO = OE.SER_ST_CODIGO
                 AND PED.PED_IN_CODIGO = OE.PED_IN_CODIGO
                INNER JOIN EST_PRODUTOS PRO ON
                      PRO.PRO_TAB_IN_CODIGO = OE.PRO_TAB_IN_CODIGO
                 AND PRO.PRO_PAD_IN_CODIGO = OE.PRO_PAD_IN_CODIGO
                 AND PRO.PRO_IN_CODIGO = OE.PRO_IN_CODIGO
                """;

        List<ExtractedJoinRelationship> found = extractor.extractAll(sql);

        assertEquals(2, found.size());

        ExtractedJoinRelationship second = found.get(1);
        assertEquals("VEN_EXPEDICAO", second.sourceTable());
        assertEquals("EST_PRODUTOS", second.joinedTable());
        assertEquals(List.of(
                new ColumnPair("PRO_TAB_IN_CODIGO", "PRO_TAB_IN_CODIGO"),
                new ColumnPair("PRO_PAD_IN_CODIGO", "PRO_PAD_IN_CODIGO"),
                new ColumnPair("PRO_IN_CODIGO", "PRO_IN_CODIGO")), second.columns());
    }

    @Test
    void shouldReturnEmptyWhenNoJoinPresent() {
        List<ExtractedJoinRelationship> found = extractor.extractAll("SELECT * FROM VEN_PEDIDOVENDA PED");

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldStopConditionsAtWhereClause() {
        String sql = """
                SELECT * FROM VEN_PEDIDOVENDA PED
                JOIN VEN_EXPEDICAO OE ON PED.PED_IN_CODIGO = OE.PED_IN_CODIGO
                WHERE OE.OUTRA_COISA = PED.ALGO
                """;

        List<ExtractedJoinRelationship> found = extractor.extractAll(sql);

        assertEquals(1, found.size());
        assertEquals(List.of(new ColumnPair("PED_IN_CODIGO", "PED_IN_CODIGO")), found.get(0).columns());
    }
}
