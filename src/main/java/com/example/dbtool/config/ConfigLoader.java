package com.example.dbtool.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        DbConfig config = tryLoad();
        if (config != null) {
            return config;
        }

        throw new IllegalStateException(
                "No Oracle connection settings found. Set DB_HOST, DB_PORT, DB_SERVICE, "
                        + "DB_USERNAME and DB_PASSWORD as environment variables, or copy "
                        + "config/db.properties.example to config/db.properties and fill it in.");
    }

    /**
     * Same lookup as {@link #load()} but returns null instead of throwing, so callers
     * like the settings window can tell "not configured yet" from a real error.
     */
    public DbConfig tryLoad() {
        DbConfig fromEnv = loadFromEnv();
        if (fromEnv != null) {
            return fromEnv;
        }
        return loadFromFile(DEFAULT_CONFIG_FILE);
    }

    /**
     * Persists to config/db.properties, overwriting whatever was there. Environment
     * variables (if set) still win on the next load — this only affects the file.
     */
    public void save(DbConfig config) {
        Properties props = new Properties();
        props.setProperty("db.host", config.host());
        props.setProperty("db.port", config.port());
        props.setProperty("db.service", config.service());
        props.setProperty("db.username", config.username());
        props.setProperty("db.password", config.password());

        try {
            Path parent = DEFAULT_CONFIG_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(DEFAULT_CONFIG_FILE)) {
                props.store(out, "DB Tool connection settings");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write " + DEFAULT_CONFIG_FILE, e);
        }
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
