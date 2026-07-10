package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centralized JDBC connection factory for the Neon-hosted PostgreSQL database.
 *
 * Original connection string supplied for the project:
 * postgresql://neondb_owner:npg_p2Qsw5fhgFNo@ep-twilight-river-a2lb7my7-pooler.eu-central-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require
 *
 * It is translated below into the JDBC URL format PostgreSQL's driver expects
 * (jdbc:postgresql://host/db?params) with the user/password passed separately.
 */
public class DBConnection {

    private static final String HOST = "ep-twilight-river-a2lb7my7-pooler.eu-central-1.aws.neon.tech";
    private static final String DATABASE = "neondb";
    private static final String JDBC_URL =
            "jdbc:postgresql://" + HOST + "/" + DATABASE + "?sslmode=require&channel_binding=require";

    private static final String USER = "neondb_owner";
    private static final String PASSWORD = "npg_p2Qsw5fhgFNo";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC Driver not found on classpath.", e);
        }
    }

    private DBConnection() {
        // utility class - no instances
    }

    /**
     * Opens a brand-new JDBC connection. Callers are responsible for closing it
     * (use try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
    }
}
