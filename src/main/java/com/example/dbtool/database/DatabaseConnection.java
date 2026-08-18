package com.example.dbtool.database;

import com.example.dbtool.config.DbConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Owns creation of JDBC connections to Oracle. Nothing else in the app opens a
 * java.sql.Connection directly. Reuses a single connection across calls — opening a
 * new one costs a network round trip, which matters when this runs on every hotkey
 * press instead of once per CLI invocation.
 */
public class DatabaseConnection {

    private final DbConfig config;
    private Connection cached;

    public DatabaseConnection(DbConfig config) {
        this.config = config;
    }

    public synchronized Connection open() throws SQLException {
        if (cached != null && isValid(cached)) {
            return cached;
        }
        cached = DriverManager.getConnection(config.jdbcUrl(), config.username(), config.password());
        return cached;
    }

    private boolean isValid(Connection connection) {
        try {
            return !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
