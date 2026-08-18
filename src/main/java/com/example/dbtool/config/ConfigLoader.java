package com.example.dbtool.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads Oracle connection settings from environment variables (DB_HOST, DB_PORT,
 * DB_SERVICE, DB_USERNAME, DB_PASSWORD), falling back to config/db.properties.
 * Credentials never live in source code — see config/db.properties.example.
 */
public class ConfigLoader {

    private static final Path DEFAULT_CONFIG_FILE = Path.of("config", "db.properties");

    public DbConfig load() {
        DbConfig fromEnv = loadFromEnv();
        if (fromEnv != null) {
            return fromEnv;
        }

        DbConfig fromFile = loadFromFile(DEFAULT_CONFIG_FILE);
        if (fromFile != null) {
            return fromFile;
        }

        throw new IllegalStateException(
                "No Oracle connection settings found. Set DB_HOST, DB_PORT, DB_SERVICE, "
                        + "DB_USERNAME and DB_PASSWORD as environment variables, or copy "
                        + "config/db.properties.example to config/db.properties and fill it in.");
    }

    private DbConfig loadFromEnv() {
        String host = System.getenv("DB_HOST");
        String port = System.getenv("DB_PORT");
        String service = System.getenv("DB_SERVICE");
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");

        if (host == null || port == null || service == null || username == null || password == null) {
            return null;
        }
        return new DbConfig(host, port, service, username, password);
    }

    private DbConfig loadFromFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
        return new DbConfig(
                props.getProperty("db.host"),
                props.getProperty("db.port"),
                props.getProperty("db.service"),
                props.getProperty("db.username"),
                props.getProperty("db.password")
        );
    }
}
