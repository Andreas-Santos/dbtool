package com.example.dbtool.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlFormatterTest {

    private final SqlFormatter formatter = new SqlFormatter();

    @Test
    void shouldFormatSelectJoinAndGroupByFromMinifiedInput() {
        String messy = "SELECT PED.PED_IN_CODIGO AS PEDIDO, PED.FIL_IN_CODIGO AS FILIAL, "
                + "OEX.EXP_IN_CODIGO AS OE, NOTAFISCAL.NOT_IN_NUMERO AS NOTA, "
                + "COUNT(OEX.PRO_IN_CODIGO) AS TOTAL_PRODUTOS "
                + "FROM VEN_EXPEDICAO OEX "
                + "inner join VEN_PEDIDOVENDA PED on OEX.ORG_TAB_IN_CODIGO = PED.ORG_TAB_IN_CODIGO "
                + "and OEX.ORG_PAD_IN_CODIGO = PED.ORG_PAD_IN_CODIGO "
                + "and OEX.ORG_IN_CODIGO = PED.ORG_IN_CODIGO "
                + "and OEX.ORG_TAU_ST_CODIGO = PED.ORG_TAU_ST_CODIGO "
                + "and OEX.SER_ST_CODIGO = PED.SER_ST_CODIGO "
                + "and OEX.PED_IN_CODIGO = PED.PED_IN_CODIGO "
                + "inner join VEN_ITEMPEDI_VEN_ITEMNOT ITN on OEX.ORG_TAB_IN_CODIGO = ITN.NF_ORG_TAB_IN_CODIGO "
                + "and OEX.ORG_PAD_IN_CODIGO = ITN.NF_ORG_PAD_IN_CODIGO "
                + "and OEX.ORG_IN_CODIGO = ITN.NF_ORG_IN_CODIGO "
                + "and OEX.ORG_TAU_ST_CODIGO = ITN.NF_ORG_TAU_ST_CODIGO "
                + "and OEX.SEQ_TAB_IN_CODIGO = ITN.EXP_SEQ_TAB_IN_CODIGO "
                + "and OEX.SEQ_IN_CODIGO = ITN.EXP_SEQ_IN_CODIGO "
                + "and OEX.EXP_IN_SEQUENCIA = ITN.EXP_IN_SEQUENCIA "
                + "inner join VEN_NOTAFISCAL NOTAFISCAL on ITN.NF_ORG_TAB_IN_CODIGO = NOTAFISCAL.ORG_TAB_IN_CODIGO "
                + "and ITN.NF_ORG_PAD_IN_CODIGO = NOTAFISCAL.ORG_PAD_IN_CODIGO "
                + "and ITN.NF_ORG_IN_CODIGO = NOTAFISCAL.ORG_IN_CODIGO "
                + "and ITN.NF_ORG_TAU_ST_CODIGO = NOTAFISCAL.ORG_TAU_ST_CODIGO "
                + "and ITN.NF_SEQ_TAB_IN_CODIGO = NOTAFISCAL.SEQ_TAB_IN_CODIGO "
                + "and ITN.NF_SEQ_IN_CODIGO = NOTAFISCAL.SEQ_IN_CODIGO "
                + "and ITN.NF_NOT_IN_CODIGO = NOTAFISCAL.NOT_IN_CODIGO "
                + "group by PED.PED_IN_CODIGO, PED.FIL_IN_CODIGO, OEX.EXP_IN_CODIGO, NOTAFISCAL.NOT_IN_NUMERO";

        String expected = """
                SELECT
                    PED.PED_IN_CODIGO AS PEDIDO,
                    PED.FIL_IN_CODIGO AS FILIAL,
                    OEX.EXP_IN_CODIGO AS OE,
                    NOTAFISCAL.NOT_IN_NUMERO AS NOTA,
                    COUNT(OEX.PRO_IN_CODIGO) AS TOTAL_PRODUTOS
                FROM
                    MEGA.VEN_EXPEDICAO OEX
                INNER JOIN MEGA.VEN_PEDIDOVENDA PED ON
                        OEX.ORG_TAB_IN_CODIGO = PED.ORG_TAB_IN_CODIGO
                    AND OEX.ORG_PAD_IN_CODIGO = PED.ORG_PAD_IN_CODIGO
                    AND OEX.ORG_IN_CODIGO     = PED.ORG_IN_CODIGO
                    AND OEX.ORG_TAU_ST_CODIGO = PED.ORG_TAU_ST_CODIGO
                    AND OEX.SER_ST_CODIGO     = PED.SER_ST_CODIGO
                    AND OEX.PED_IN_CODIGO     = PED.PED_IN_CODIGO
                INNER JOIN MEGA.VEN_ITEMPEDI_VEN_ITEMNOT ITN ON
                        OEX.ORG_TAB_IN_CODIGO = ITN.NF_ORG_TAB_IN_CODIGO
                    AND OEX.ORG_PAD_IN_CODIGO = ITN.NF_ORG_PAD_IN_CODIGO
                    AND OEX.ORG_IN_CODIGO     = ITN.NF_ORG_IN_CODIGO
                    AND OEX.ORG_TAU_ST_CODIGO = ITN.NF_ORG_TAU_ST_CODIGO
                    AND OEX.SEQ_TAB_IN_CODIGO = ITN.EXP_SEQ_TAB_IN_CODIGO
                    AND OEX.SEQ_IN_CODIGO     = ITN.EXP_SEQ_IN_CODIGO
                    AND OEX.EXP_IN_SEQUENCIA  = ITN.EXP_IN_SEQUENCIA
                INNER JOIN MEGA.VEN_NOTAFISCAL NOTAFISCAL ON
                        ITN.NF_ORG_TAB_IN_CODIGO = NOTAFISCAL.ORG_TAB_IN_CODIGO
                    AND ITN.NF_ORG_PAD_IN_CODIGO = NOTAFISCAL.ORG_PAD_IN_CODIGO
                    AND ITN.NF_ORG_IN_CODIGO     = NOTAFISCAL.ORG_IN_CODIGO
                    AND ITN.NF_ORG_TAU_ST_CODIGO = NOTAFISCAL.ORG_TAU_ST_CODIGO
                    AND ITN.NF_SEQ_TAB_IN_CODIGO = NOTAFISCAL.SEQ_TAB_IN_CODIGO
                    AND ITN.NF_SEQ_IN_CODIGO     = NOTAFISCAL.SEQ_IN_CODIGO
                    AND ITN.NF_NOT_IN_CODIGO     = NOTAFISCAL.NOT_IN_CODIGO
                GROUP BY\s
                    PED.PED_IN_CODIGO,
                    PED.FIL_IN_CODIGO,
                    OEX.EXP_IN_CODIGO,
                    NOTAFISCAL.NOT_IN_NUMERO""";

        assertEquals(expected, formatter.format(messy));
    }

    @Test
    void shouldPreserveIdentifierCaseAndOnlyNormalizeItsOwnKeywords() {
        assertEquals("SELECT\n    a.id\nFROM\n    MEGA.t a", formatter.format("select a.id from t a"));
    }

    @Test
    void shouldBeIdempotentOnAlreadyFormattedInput() {
        String formattedOnce = formatter.format("SELECT A.ID FROM T A INNER JOIN T2 B ON A.ID = B.ID");
        assertEquals(formattedOnce, formatter.format(formattedOnce));
    }

    @Test
    void shouldFormatWhereClauseWithHangingAndOr() {
        String sql = "SELECT A.ID FROM T A WHERE A.STATUS = 1 AND A.ATIVO = 'S' OR A.FORCE = 'Y'";

        String expected = """
                SELECT
                    A.ID
                FROM
                    MEGA.T A
                WHERE
                        A.STATUS = 1
                    AND A.ATIVO  = 'S'
                    OR A.FORCE  = 'Y'""";

        assertEquals(expected, formatter.format(sql));
    }

    @Test
    void shouldAlignEqualsSignsIndependentlyPerJoinBlock() {
        String sql = "SELECT A.ID FROM T A "
                + "INNER JOIN T2 B ON A.ID = B.A_ID AND A.FILIAL = B.FILIAL "
                + "INNER JOIN T3 C ON A.VERYLONGCOLUMN = C.X AND A.ID = C.A_ID";

        String expected = """
                SELECT
                    A.ID
                FROM
                    MEGA.T A
                INNER JOIN MEGA.T2 B ON
                        A.ID     = B.A_ID
                    AND A.FILIAL = B.FILIAL
                INNER JOIN MEGA.T3 C ON
                        A.VERYLONGCOLUMN = C.X
                    AND A.ID             = C.A_ID""";

        assertEquals(expected, formatter.format(sql));
    }

    @Test
    void shouldNotAlignComparisonOperatorsThatContainEquals() {
        String sql = "SELECT A.ID FROM T A WHERE A.ID = 1 AND A.QTD >= 2 AND A.QTD2 <> 3";

        String expected = """
                SELECT
                    A.ID
                FROM
                    MEGA.T A
                WHERE
                        A.ID = 1
                    AND A.QTD >= 2
                    AND A.QTD2 <> 3""";

        assertEquals(expected, formatter.format(sql));
    }

    @Test
    void shouldExcludeNonEqualityConditionsFromAlignmentWidth() {
        String sql = "SELECT A.ID FROM T A WHERE A.VERYLONGCOLUMNNAME IS NOT NULL AND A.ID = 1";

        String expected = """
                SELECT
                    A.ID
                FROM
                    MEGA.T A
                WHERE
                        A.VERYLONGCOLUMNNAME IS NOT NULL
                    AND A.ID = 1""";

        assertEquals(expected, formatter.format(sql));
    }

    @Test
    void shouldSwallowGroupByIntoThePrecedingJoinConditionWhenGluedWithNoSeparator() {
        // Regression guard documenting why GroupByController always prefixes a newline
        // before a freshly-generated "GROUP BY ..." clause: \bGROUP\s+BY\b requires a
        // word boundary, so gluing it directly onto the previous token (as happens if the
        // completion is pasted with no separator) hides the clause from the scanner
        // entirely — it gets absorbed into the JOIN's last condition instead of becoming
        // its own GROUP BY clause.
        String glued = "SELECT A.ID FROM T A INNER JOIN T2 B ON A.ID = B.IDGROUP BY A.ID, A.NAME";

        String formatted = formatter.format(glued);

        assertEquals("""
                SELECT
                    A.ID
                FROM
                    MEGA.T A
                INNER JOIN MEGA.T2 B ON
                        A.ID = B.IDGROUP BY A.ID, A.NAME""", formatted);
    }

    @Test
    void shouldFormatGroupByCorrectlyWhenSeparatedFromThePrecedingJoinByANewline() {
        String withSeparator = "SELECT A.ID FROM T A INNER JOIN T2 B ON A.ID = B.ID\nGROUP BY A.ID, A.NAME";

        String expected = """
                SELECT
                    A.ID
                FROM
                    MEGA.T A
                INNER JOIN MEGA.T2 B ON
                        A.ID = B.ID
                GROUP BY\s
                    A.ID,
                    A.NAME""";

        assertEquals(expected, formatter.format(withSeparator));
    }

    @Test
    void shouldKeepSingleQuotedCommasAndWhitespaceIntact() {
        String sql = "SELECT NVL(A.X, 'a, b') AS X FROM T A";

        String expected = """
                SELECT
                    NVL(A.X, 'a, b') AS X
                FROM
                    MEGA.T A""";

        assertEquals(expected, formatter.format(sql));
    }

    @Test
    void shouldStripTrailingSemicolon() {
        assertEquals("SELECT\n    A.ID\nFROM\n    MEGA.T A", formatter.format("SELECT A.ID FROM T A;"));
    }

    @Test
    void shouldFormatSelectDistinct() {
        assertEquals("SELECT DISTINCT\n    A.ID\nFROM\n    MEGA.T A",
                formatter.format("SELECT DISTINCT A.ID FROM T A"));
    }

    @Test
    void shouldNotDoubleQualifyATableThatAlreadyNamesASchema() {
        assertEquals("SELECT\n    A.ID\nFROM\n    OTHER.T A",
                formatter.format("SELECT A.ID FROM OTHER.T A"));
    }

    @Test
    void shouldNotQualifyASubqueryInFrom() {
        assertEquals("SELECT\n    X.ID\nFROM\n    (SELECT A.ID FROM T A) X",
                formatter.format("SELECT X.ID FROM (SELECT A.ID FROM T A) X"));
    }

    @Test
    void shouldQualifyEveryTableInAnOldStyleCommaJoin() {
        assertEquals("SELECT\n    A.ID\nFROM\n    MEGA.T A,\n    MEGA.T2 B",
                formatter.format("SELECT A.ID FROM T A, T2 B"));
    }

    @Test
    void shouldNotQualifyAJoinThatHasNoOnClause() {
        assertEquals("SELECT\n    A.ID\nFROM\n    MEGA.T A\nCROSS JOIN MEGA.T2 B",
                formatter.format("SELECT A.ID FROM T A CROSS JOIN T2 B"));
    }

    @Test
    void shouldThrowWhenFromIsMissing() {
        assertThrows(UnformattableQueryException.class, () -> formatter.format("SELECT A.ID"));
    }

    @Test
    void shouldThrowOnBlankInput() {
        assertThrows(UnformattableQueryException.class, () -> formatter.format("   "));
    }
}
