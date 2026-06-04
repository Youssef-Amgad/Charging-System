package com.ocs.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages a single JDBC connection to NeonDB (PostgreSQL).
 * Replace the constants below with your actual NeonDB credentials.
 */
public class DatabaseConnection {

    // ── NeonDB connection details ───────────────────────────────────────────
    private static final String HOST     = System.getenv().getOrDefault("DB_HOST",
            "ep-xxxx-xxxx.us-east-2.aws.neon.tech");
    private static final String DB_NAME  = System.getenv().getOrDefault("DB_NAME",  "neondb");
    private static final String USER     = System.getenv().getOrDefault("DB_USER",  "neondb_owner");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASS",  "your_password");

    private static final String URL =
            "jdbc:postgresql://" + HOST + "/" + DB_NAME + "?sslmode=require";

    // ── Singleton ───────────────────────────────────────────────────────────
    private static Connection instance;

    private DatabaseConnection() {}

    /**
     * Returns (and lazily creates) the shared JDBC Connection.
     * Thread-safe via synchronized.
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (instance == null || instance.isClosed()) {
            System.out.println("[DB] Connecting to NeonDB...");
            instance = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[DB] Connected successfully.");
        }
        return instance;
    }

    /** Gracefully closes the shared connection. */
    public static synchronized void close() {
        try {
            if (instance != null && !instance.isClosed()) {
                instance.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error closing connection: " + e.getMessage());
        }
    }
}
