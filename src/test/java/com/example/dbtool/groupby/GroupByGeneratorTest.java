package com.example.dbtool.groupby;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GroupByGeneratorTest {

    private final GroupByGenerator generator = new GroupByGenerator();

    @Test
    void shouldKeepPlainColumnsUnchanged() {
        String groupBy = generator.generate(List.of("PED.ID", "PED.STATUS"));

        assertEquals("GROUP BY PED.ID,\n         PED.STATUS", groupBy);
    }

    @Test
    void shouldDropAggregateFunctionsEntirely() {
        String groupBy = generator.generate(List.of(
                "PED.STATUS", "SUM(PED.VALOR) TOTAL", "COUNT(DISTINCT PED.ID) QTD_PEDIDOS"));

        assertEquals("GROUP BY PED.STATUS", groupBy);
    }

    @Test
    void shouldStripExplicitAsAliasButKeepTableQualifiedExpression() {
        String groupBy = generator.generate(List.of("PED.VALOR AS VALOR_TOTAL"));

        assertEquals("GROUP BY PED.VALOR", groupBy);
    }

    @Test
    void shouldStripImplicitAliasButKeepTableQualifiedExpression() {
        String groupBy = generator.generate(List.of("PED.VALOR VALOR_TOTAL"));

        assertEquals("GROUP BY PED.VALOR", groupBy);
    }

    @Test
    void shouldStripAliasFromFunctionCallButKeepItsArguments() {
        String groupBy = generator.generate(List.of("NVL(PED.DESCONTO, 0) DESCONTO"));

        assertEquals("GROUP BY NVL(PED.DESCONTO, 0)", groupBy);
    }

    @Test
    void shouldKeepMultiTableExpressionWithoutMistakingItsLastColumnForAnAlias() {
        String groupBy = generator.generate(List.of("PED.QTD * PED.PRECO"));

        assertEquals("GROUP BY PED.QTD * PED.PRECO", groupBy);
    }

    @Test
    void shouldStripImplicitAliasEvenWhenExpressionContainsAnInnerAsKeyword() {
        String groupBy = generator.generate(List.of("CAST(PED.VALOR AS NUMBER) VALOR_NUM"));

        assertEquals("GROUP BY CAST(PED.VALOR AS NUMBER)", groupBy);
    }

    @Test
    void shouldThrowWhenEveryColumnIsAnAggregate() {
        assertThrows(NoSelectColumnsFoundException.class,
                () -> generator.generate(List.of("SUM(PED.VALOR) TOTAL", "COUNT(*) QTD")));
    }

    @Test
    void generateColumnListShouldOmitTheGroupByPrefix() {
        String columnList = generator.generateColumnList(List.of("PED.ID", "PED.STATUS", "COUNT(*) QTD"));

        assertEquals("PED.ID,\n         PED.STATUS", columnList);
    }
}
