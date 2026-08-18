package com.example.dbtool.database;

import com.example.dbtool.config.ConfigLoader;
import com.example.dbtool.config.DbConfig;

import java.nio.file.Path;

/**
 * Builds the MetadataService used across entry points (CLI, JavaFX UI, hotkey listener),
 * wired to prefer config/manual-relationships.conf over whatever Oracle reports. Keeps
 * one ManualRelationshipRepository per factory instance so callers that share a factory
 * (e.g. the JOIN-completion and manual-sync hotkeys) see each other's additions without
 * needing to reload the file.
 */
public class MetadataServiceFactory {

    private static final Path MANUAL_RELATIONSHIPS_FILE = Path.of("config", "manual-relationships.conf");

    private final ManualRelationshipRepository manualRelationships =
            new ManualRelationshipRepository(MANUAL_RELATIONSHIPS_FILE);

    public MetadataService create() {
        DbConfig config = new ConfigLoader().load();
        DatabaseConnection connection = new DatabaseConnection(config);
        MetadataService oracle = new OracleMetadataService(connection, config.username());
        return new OverridableMetadataService(oracle, manualRelationships);
    }

    public ManualRelationshipRepository manualRelationships() {
        return manualRelationships;
    }
}
