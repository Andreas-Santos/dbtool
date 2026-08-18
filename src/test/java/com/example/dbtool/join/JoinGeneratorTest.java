package com.example.dbtool.join;

import com.example.dbtool.model.ForeignKey;
import com.example.dbtool.model.ForeignKey.ColumnPair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoinGeneratorTest {

    private final JoinGenerator generator = new JoinGenerator();

    @Test
    void shouldGenerateJoinUsingSingleColumn() {
        ForeignKey fk = new ForeignKey(
                "FK_EXPEDICAO_PEDIDO",
                "VEN_EXPEDICAO",
                "VEN_PEDIDOVENDA",
                List.of(new ColumnPair("PED_IN_CODIGO", "PED_IN_CODIGO"))
        );

        String sql = generator.generate("VEN_EXPEDICAO", "OE", "PED", List.of(fk));

        assertEquals("JOIN VEN_EXPEDICAO OE\n  ON PED.PED_IN_CODIGO = OE.PED_IN_CODIGO", sql);
    }

    @Test
    void shouldGenerateJoinUsingMultipleColumns() {
        ForeignKey fk = new ForeignKey(
                "FK_EXPEDICAO_PEDIDO",
                "VEN_EXPEDICAO",
                "VEN_PEDIDOVENDA",
                List.of(
                        new ColumnPair("ORG_TAB_IN_CODIGO", "ORG_TAB_IN_CODIGO"),
                        new ColumnPair("ORG_PAD_IN_CODIGO", "ORG_PAD_IN_CODIGO"),
                        new ColumnPair("ORG_IN_CODIGO", "ORG_IN_CODIGO")
                )
        );

        String sql = generator.generate("VEN_EXPEDICAO", "OE", "PED", List.of(fk));

        assertEquals("""
                JOIN VEN_EXPEDICAO OE
                  ON PED.ORG_TAB_IN_CODIGO = OE.ORG_TAB_IN_CODIGO
                 AND PED.ORG_PAD_IN_CODIGO = OE.ORG_PAD_IN_CODIGO
                 AND PED.ORG_IN_CODIGO = OE.ORG_IN_CODIGO""", sql);
    }

    @Test
    void shouldThrowWhenNoRelationshipExists() {
        assertThrows(NoRelationshipException.class,
                () -> generator.generate("VEN_EXPEDICAO", "OE", "PED", List.<ForeignKey>of()));
    }

    @Test
    void shouldThrowWhenTableIsUnrelatedToForeignKey() {
        ForeignKey fk = new ForeignKey(
                "FK_OUTRA",
                "OUTRA_TABELA",
                "AINDA_OUTRA",
                List.of(new ColumnPair("COD", "COD"))
        );

        assertThrows(IllegalArgumentException.class,
                () -> generator.generate("VEN_EXPEDICAO", "OE", "PED", fk));
    }

    @Test
    void shouldGenerateJoinWithDifferentAliases() {
        ForeignKey fk = new ForeignKey(
                "FK_EXPEDICAO_PEDIDO",
                "VEN_EXPEDICAO",
                "VEN_PEDIDOVENDA",
                List.of(new ColumnPair("PED_IN_CODIGO", "PED_IN_CODIGO"))
        );

        String sql = generator.generate("VEN_EXPEDICAO", "EXP", "PV", List.of(fk));

        assertEquals("JOIN VEN_EXPEDICAO EXP\n  ON PV.PED_IN_CODIGO = EXP.PED_IN_CODIGO", sql);
    }

    @Test
    void shouldGenerateJoinWhenForeignKeyIsDeclaredInReverseDirection() {
        // Here the source table (PED) owns the constraint and references the target table (OE).
        ForeignKey fk = new ForeignKey(
                "FK_PEDIDO_EXPEDICAO",
                "VEN_PEDIDOVENDA",
                "VEN_EXPEDICAO",
                List.of(new ColumnPair("EXP_IN_CODIGO", "EXP_IN_CODIGO"))
        );

        String sql = generator.generate("VEN_EXPEDICAO", "OE", "PED", List.of(fk));

        assertEquals("JOIN VEN_EXPEDICAO OE\n  ON PED.EXP_IN_CODIGO = OE.EXP_IN_CODIGO", sql);
    }

    @Test
    void shouldThrowAmbiguousExceptionWhenMultipleForeignKeysExist() {
        ForeignKey fk1 = new ForeignKey(
                "FK_EXPEDICAO_PEDIDO_1",
                "VEN_EXPEDICAO",
                "VEN_PEDIDOVENDA",
                List.of(new ColumnPair("PED_IN_CODIGO", "PED_IN_CODIGO"))
        );
        ForeignKey fk2 = new ForeignKey(
                "FK_EXPEDICAO_PEDIDO_2",
                "VEN_EXPEDICAO",
                "VEN_PEDIDOVENDA",
                List.of(new ColumnPair("PED_IN_CODIGO_ALT", "PED_IN_CODIGO_ALT"))
        );

        AmbiguousRelationshipException ex = assertThrows(AmbiguousRelationshipException.class,
                () -> generator.generate("VEN_EXPEDICAO", "OE", "PED", List.of(fk1, fk2)));

        assertEquals(2, ex.candidates().size());
        assertTrue(ex.getMessage().contains("FK_EXPEDICAO_PEDIDO_1"));
        assertTrue(ex.getMessage().contains("FK_EXPEDICAO_PEDIDO_2"));
    }
}
