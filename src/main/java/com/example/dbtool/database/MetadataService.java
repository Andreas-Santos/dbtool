package com.example.dbtool.database;

import com.example.dbtool.model.Column;
import com.example.dbtool.model.ForeignKey;
import com.example.dbtool.model.Table;

import java.util.List;

public interface MetadataService {

    List<Table> getTables();

    Table getTable(String tableName);

    List<Column> getColumns(String tableName);

    List<String> getPrimaryKeyColumns(String tableName);

    List<ForeignKey> getForeignKeys(String tableName);

    /**
     * Foreign keys connecting the two tables, in either direction (tableA -> tableB
     * or tableB -> tableA). May return more than one when several FKs exist between
     * the same pair of tables.
     */
    List<ForeignKey> findRelationship(String tableA, String tableB);
}
