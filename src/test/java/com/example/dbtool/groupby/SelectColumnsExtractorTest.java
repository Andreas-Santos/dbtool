package com.example.dbtool.groupby;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SelectColumnsExtractorTest {

    private final SelectColumnsExtractor extractor = new SelectColumnsExtractor();

    @Test
    void shouldSplitSimpleColumnList() {
        String text = "SELECT PED.ID, PED.STATUS, OE.DATA_ENVIO FROM VEN_PEDIDOVENDA PED";

        List<String> columns = extractor.extract(text);

        assertEquals(List.of("PED.ID", "PED.STATUS", "OE.DATA_ENVIO"), columns);
    }

    @Test
    void shouldIgnoreDistinctKeyword() {
        String text = "SELECT DISTINCT PED.ID FROM VEN_PEDIDOVENDA PED";

        assertEquals(List.of("PED.ID"), extractor.extract(text));
    }

    @Test
    void shouldNotSplitOnCommaInsideFunctionCall() {
        String text = "SELECT NVL(PED.DESCONTO, 0) DESCONTO, PED.STATUS FROM VEN_PEDIDOVENDA PED";

        List<String> columns = extractor.extract(text);

        assertEquals(List.of("NVL(PED.DESCONTO, 0) DESCONTO", "PED.STATUS"), columns);
    }

    @Test
    void shouldNotSplitOnCommaInsideStringLiteral() {
        String text = "SELECT TO_CHAR(PED.DATA, 'YYYY, MM') MES FROM VEN_PEDIDOVENDA PED";

        assertEquals(List.of("TO_CHAR(PED.DATA, 'YYYY, MM') MES"), extractor.extract(text));
    }

    @Test
    void shouldStopAtTheFromOfTheOuterQueryNotASubquery() {
        String text = "SELECT PED.ID, (SELECT MAX(X.DATA) FROM OUTRA X) ULTIMA_DATA FROM VEN_PEDIDOVENDA PED";

        List<String> columns = extractor.extract(text);

        assertEquals(List.of("PED.ID", "(SELECT MAX(X.DATA) FROM OUTRA X) ULTIMA_DATA"), columns);
    }

    @Test
    void shouldIgnoreEarlierStatementsInTheSameEditor() {
        String text = """
                SELECT X.ID FROM OUTRA_TABELA X;
                SELECT PED.ID, PED.STATUS FROM VEN_PEDIDOVENDA PED""";

        assertEquals(List.of("PED.ID", "PED.STATUS"), extractor.extract(text));
    }

    @Test
    void shouldExtractColumnsWhenManyJoinsAndAGroupByFollow() {
        String text = """
                SELECT
                    PED.PED_IN_CODIGO,
                    OE.EXP_IN_CODIGO,
                    OE.EXP_DT_EMISSAO,
                    COUNT(OE.PRO_IN_CODIGO)
                FROM
                    VEN_PEDIDOVENDA PED
                INNER JOIN VEN_EQUIPE EQP ON
                    PED.EQU_TAB_IN_CODIGO = EQP.EQU_TAB_IN_CODIGO
                 AND PED.EQU_PAD_IN_CODIGO = EQP.EQU_PAD_IN_CODIGO
                INNER JOIN VEN_EXPEDICAO OE ON
                    PED.ORG_TAB_IN_CODIGO = OE.ORG_TAB_IN_CODIGO
                 AND PED.PED_IN_CODIGO = OE.PED_IN_CODIGO
                GROUP BY""";

        List<String> columns = extractor.extract(text);

        assertEquals(List.of(
                "PED.PED_IN_CODIGO", "OE.EXP_IN_CODIGO", "OE.EXP_DT_EMISSAO", "COUNT(OE.PRO_IN_CODIGO)"), columns);
    }

    @Test
    void shouldThrowWhenSelectIsMissing() {
        assertThrows(NoSelectColumnsFoundException.class,
                () -> extractor.extract("FROM VEN_PEDIDOVENDA PED"));
    }

    @Test
    void shouldThrowWhenFromIsMissing() {
        assertThrows(NoSelectColumnsFoundException.class,
                () -> extractor.extract("SELECT PED.ID"));
    }
}
