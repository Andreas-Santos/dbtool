package com.example.dbtool.autocomplete;

import com.example.dbtool.database.MetadataService;
import com.example.dbtool.model.Column;
import com.example.dbtool.model.ForeignKey;
import com.example.dbtool.model.ForeignKey.ColumnPair;
import com.example.dbtool.model.Table;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JoinCompletionServiceTest {

    @Test
    void shouldOmitLeadingAliasFromFirstCondition() {
        MetadataService metadataService = stubService(new ForeignKey(
                "FK", "VEN_EXPEDICAO", "VEN_PEDIDOVENDA",
                List.of(
                        new ColumnPair("ORG_TAB_IN_CODIGO", "ORG_TAB_IN_CODIGO"),
                        new ColumnPair("ORG_IN_CODIGO", "ORG_IN_CODIGO"))));
        JoinCompletionService service = new JoinCompletionService(metadataService);

        SqlJoinContext context = new SqlJoinContext("VEN_PEDIDOVENDA", "PED", "VEN_EXPEDICAO", "OE");

        assertEquals("ORG_TAB_IN_CODIGO = OE.ORG_TAB_IN_CODIGO\n AND PED.ORG_IN_CODIGO = OE.ORG_IN_CODIGO",
                service.complete(context));
    }

    private MetadataService stubService(ForeignKey fk) {
        return new MetadataService() {
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
                return List.of(fk);
            }

            @Override
            public List<ForeignKey> findRelationship(String tableA, String tableB) {
                return List.of(fk);
            }
        };
    }
}
