package com.example.dbtool.database;

import com.example.dbtool.model.Column;
import com.example.dbtool.model.ForeignKey;
import com.example.dbtool.model.Table;

import java.util.List;

/**
 * Wraps a real MetadataService and prefers manually-declared relationships over
 * whatever the database reports, since many tables have no real FK constraint.
 */
public class OverridableMetadataService implements MetadataService {

    private final MetadataService delegate;
    private final ManualRelationshipRepository manualRelationships;

    public OverridableMetadataService(MetadataService delegate, ManualRelationshipRepository manualRelationships) {
        this.delegate = delegate;
        this.manualRelationships = manualRelationships;
    }

    @Override
    public List<Table> getTables() {
        return delegate.getTables();
    }

    @Override
    public Table getTable(String tableName) {
        return delegate.getTable(tableName);
    }

    @Override
    public List<Column> getColumns(String tableName) {
        return delegate.getColumns(tableName);
    }

    @Override
    public List<String> getPrimaryKeyColumns(String tableName) {
        return delegate.getPrimaryKeyColumns(tableName);
    }

    @Override
    public List<ForeignKey> getForeignKeys(String tableName) {
        return delegate.getForeignKeys(tableName);
    }

    @Override
    public List<ForeignKey> findRelationship(String tableA, String tableB) {
        List<ForeignKey> manual = manualRelationships.findRelationship(tableA, tableB);
        if (!manual.isEmpty()) {
            return manual;
        }
        return delegate.findRelationship(tableA, tableB);
    }
}
