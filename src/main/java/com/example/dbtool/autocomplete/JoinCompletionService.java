package com.example.dbtool.autocomplete;

import com.example.dbtool.database.MetadataService;
import com.example.dbtool.join.JoinGenerator;
import com.example.dbtool.model.ForeignKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the relationship for a parsed SqlJoinContext and formats just the text
 * that should be inserted at the cursor to complete the ON clause.
 */
public class JoinCompletionService {

    private final MetadataService metadataService;
    private final JoinGenerator joinGenerator = new JoinGenerator();

    public JoinCompletionService(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    public String complete(SqlJoinContext context) {
        List<ForeignKey> relationships =
                metadataService.findRelationship(context.sourceTable(), context.joinedTable());
        List<String> conditions = joinGenerator.generateConditionList(
                context.joinedTable(), context.joinedAlias(), context.sourceAlias(), relationships);
        conditions = stripLeadingAlias(conditions, context.sourceAlias());

        return String.join("\n AND ", conditions);
    }

    private List<String> stripLeadingAlias(List<String> conditions, String sourceAlias) {
        String prefix = sourceAlias + ".";
        String first = conditions.get(0);
        if (!first.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return conditions;
        }
        List<String> adjusted = new ArrayList<>(conditions);
        adjusted.set(0, first.substring(prefix.length()));
        return adjusted;
    }
}
