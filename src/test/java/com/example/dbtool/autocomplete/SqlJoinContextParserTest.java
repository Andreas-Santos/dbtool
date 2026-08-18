package com.example.dbtool.autocomplete;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlJoinContextParserTest {

    private final SqlJoinContextParser parser = new SqlJoinContextParser();

    @Test
    void shouldParseWhenSourceAliasIsTheFromTable() {
        String text = """
                SELECT
                    *
                FROM
                    VEN_PEDIDOVENDA PED
                INNER JOIN VEN_EXPEDICAO OE ON
                    PED.""";

        SqlJoinContext context = parser.parse(text);

        assertEquals("VEN_PEDIDOVENDA", context.sourceTable());
        assertEquals("PED", context.sourceAlias());
        assertEquals("VEN_EXPEDICAO", context.joinedTable());
        assertEquals("OE", context.joinedAlias());
    }

    @Test
    void shouldResolveAgainstAnEarlierJoinWhenItsAliasIsTyped() {
        String text = """
                SELECT * FROM VEN_PEDIDOVENDA PED
                INNER JOIN VEN_EQUIPE EQP ON
                    PED.EQU_TAB_IN_CODIGO = EQP.EQU_TAB_IN_CODIGO
                 AND PED.EQU_PAD_IN_CODIGO = EQP.EQU_PAD_IN_CODIGO
                 AND PED.EQU_IN_CODIGO = EQP.EQU_IN_CODIGO
                INNER JOIN GLO_ACAO ACAO ON
                    EQP.""";

        SqlJoinContext context = parser.parse(text);

        assertEquals("VEN_EQUIPE", context.sourceTable());
        assertEquals("EQP", context.sourceAlias());
        assertEquals("GLO_ACAO", context.joinedTable());
        assertEquals("ACAO", context.joinedAlias());
    }

    @Test
    void shouldThrowWhenTypedAliasWasNeverDeclared() {
        String text = """
                SELECT * FROM VEN_PEDIDOVENDA PED
                INNER JOIN GLO_ACAO ACAO ON
                    XXX.""";

        assertThrows(NoJoinContextFoundException.class, () -> parser.parse(text));
    }

    @Test
    void shouldThrowWhenTypedAliasIsTheJoinsOwnAlias() {
        String text = """
                SELECT * FROM VEN_PEDIDOVENDA PED
                INNER JOIN EST_PRODUTOS PRO ON
                    PRO.""";

        assertThrows(NoJoinContextFoundException.class, () -> parser.parse(text));
    }

    @Test
    void shouldThrowWhenNothingTypedAfterOn() {
        String text = "SELECT * FROM VEN_PEDIDOVENDA PED JOIN VEN_EXPEDICAO OE ON";

        assertThrows(NoJoinContextFoundException.class, () -> parser.parse(text));
    }

    @Test
    void shouldUseTheLastJoinWhenMultipleExist() {
        String text = """
                SELECT * FROM VEN_PEDIDOVENDA PED
                JOIN VEN_EXPEDICAO OE ON PED.PED_IN_CODIGO = OE.PED_IN_CODIGO
                JOIN GLO_ACAO ACAO ON
                    PED.""";

        SqlJoinContext context = parser.parse(text);

        assertEquals("GLO_ACAO", context.joinedTable());
        assertEquals("ACAO", context.joinedAlias());
    }

    @Test
    void shouldIgnoreEarlierStatementsInTheSameEditor() {
        String text = """
                SELECT * FROM OUTRA_TABELA X JOIN OUTRA_COISA Y ON X.ID = Y.ID;
                SELECT * FROM VEN_PEDIDOVENDA PED JOIN VEN_EXPEDICAO OE ON
                    PED.""";

        SqlJoinContext context = parser.parse(text);

        assertEquals("VEN_PEDIDOVENDA", context.sourceTable());
        assertEquals("VEN_EXPEDICAO", context.joinedTable());
    }

    @Test
    void shouldThrowWhenFromIsMissing() {
        assertThrows(NoJoinContextFoundException.class,
                () -> parser.parse("SELECT * JOIN VEN_EXPEDICAO OE ON PED."));
    }

    @Test
    void shouldThrowWhenJoinIsMissing() {
        assertThrows(NoJoinContextFoundException.class,
                () -> parser.parse("SELECT * FROM VEN_PEDIDOVENDA PED WHERE 1=1"));
    }
}
