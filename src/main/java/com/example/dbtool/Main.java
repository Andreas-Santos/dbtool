package com.example.dbtool;

import com.example.dbtool.database.MetadataService;
import com.example.dbtool.database.MetadataServiceFactory;
import com.example.dbtool.join.JoinGenerator;
import com.example.dbtool.model.ForeignKey;

import java.util.List;

/**
 * Validates the core JOIN-discovery flow from the command line, before any UI exists.
 * Usage: Main [mainTable] [relatedTable] [mainAlias] [relatedAlias]
 */
public class Main {

    public static void main(String[] args) {
        String mainTable = args.length > 0 ? args[0] : "VEN_PEDIDOVENDA";
        String relatedTable = args.length > 1 ? args[1] : "GLO_ACAO";
        String mainAlias = args.length > 2 ? args[2] : "PED";
        String relatedAlias = args.length > 3 ? args[3] : "ACAO";

        MetadataService metadataService = new MetadataServiceFactory().create();
        JoinGenerator joinGenerator = new JoinGenerator();

        List<ForeignKey> relationships = metadataService.findRelationship(mainTable, relatedTable);
        String sql = joinGenerator.generate(relatedTable, relatedAlias, mainAlias, relationships);

        System.out.println(sql);
    }
}
