package com.example.dbtool.join;

import com.example.dbtool.model.ForeignKey;

import java.util.List;
import java.util.stream.Collectors;

public class AmbiguousRelationshipException extends RuntimeException {

    private final List<ForeignKey> candidates;

    public AmbiguousRelationshipException(String sourceTable, String targetTable, List<ForeignKey> candidates) {
        super("Multiple foreign keys found between '" + sourceTable + "' and '" + targetTable + "': "
                + candidates.stream().map(ForeignKey::constraintName).collect(Collectors.joining(", "))
                + ". Specify which one to use.");
        this.candidates = candidates;
    }

    public List<ForeignKey> candidates() {
        return candidates;
    }
}
