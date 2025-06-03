package dev.iseal.sealUtils.systems.database;

import java.io.File;

/**
 * Builder for creating file-based database connections with JDBCHandler.
 * Simplifies database creation by handling JDBC URL construction.
 */
public class JDBCHandlerBuilder {

    private String dbType;
    private String filePath;
    private String username;
    private String password;
    private boolean createIfNotExists;
    private boolean strictMode;

    public JDBCHandlerBuilder() {
        this.dbType = "h2"; // Default to H2
        this.username = "";
        this.password = "";
        this.createIfNotExists = true;
        this.strictMode = false;
    }

    /**
     *
     * Sets the database type.
     * @param dbType The database type (sqlite, h2, hsqldb, mysql, postgresql)
     * @return This builder
     */
    public JDBCHandlerBuilder withDatabaseType(String dbType) {
        this.dbType = dbType.toLowerCase();
        return this;
    }

    /**
     * Sets the file path for the database.
     * @param filePath Path to the database file
     * @return This builder
     */
    public JDBCHandlerBuilder withFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }


    /**
     * Sets credentials for database access if needed.
     * @param username Database username
     * @param password Database password
     * @return This builder
     */
    public JDBCHandlerBuilder withCredentials(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    /**
     * Controls whether the database should be created if it doesn't exist.
     * @param create Whether to create the database
     * @return This builder
     */
    public JDBCHandlerBuilder createIfNotExists(boolean create) {
        this.createIfNotExists = create;
        return this;
    }

    /**
     * Sets strict mode for database operations.
     * @param strict Whether to enable strict mode
     * @return This builder
     */
    public JDBCHandlerBuilder strictMode(boolean strict) {
        this.strictMode = strict;
        return this;
    }

    /**
     * Builds the JDBC URL based on configuration.
     * @return Properly formatted JDBC URL
     */
    private String buildJdbcUrl() {
        if (createIfNotExists) {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
        }

        return switch (dbType) {
            case "sqlite" -> "jdbc:sqlite:" + filePath;
            case "h2" -> "jdbc:h2:file:" + filePath;
            case "hsqldb" -> "jdbc:hsqldb:file:" + filePath;
            case "mysql" -> "jdbc:mysql://" + filePath;
            case "postgresql" -> "jdbc:postgresql://" + filePath;
            default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
        };
    }

    /**
     * Builds and initializes a JDBCHandler.
     * @return An initialized JDBCHandler
     */
    public JDBCHandler build() {
        JDBCHandler handler = new JDBCHandler(strictMode);
        handler.init(buildJdbcUrl(), username, password);
        return handler;
    }


    /**
     * Builds, initializes, and connects a JDBCHandler.
     * @return A connected JDBCHandler
     * @throws RuntimeException if connection fails
     */
    public JDBCHandler buildAndConnect() {
        JDBCHandler handler = build();
        if (!handler.connect()) {
            throw new RuntimeException("Failed to connect to database: " + filePath);
        }
        return handler;
    }


    // Static factory methods for supported databases
    /**
    * Creates a builder for SQLite databases.
    * @param filePath Path to SQLite database file
    * @return Configured builder
    */
    public static JDBCHandlerBuilder forSqlite(String filePath) {
        return new JDBCHandlerBuilder()
                .withDatabaseType("sqlite")
                .withFilePath(filePath);
    }

    /**
     * Creates a builder for H2 databases.
     * @param filePath Path to H2 database file
     * @return Configured builder
     */
    public static JDBCHandlerBuilder forH2(String filePath) {
        return new JDBCHandlerBuilder()
                .withDatabaseType("h2")
                .withFilePath(filePath);

    }

    /**
     * Creates a builder for HSQLDB databases.
     * @param filePath Path to HSQLDB database file
     * @return Configured builder
     */
    public static JDBCHandlerBuilder forHSQLDB(String filePath) {
        return new JDBCHandlerBuilder()
                .withDatabaseType("hsqldb")
                .withFilePath(filePath);
    }

    /**
     * Creates a builder for MySQL databases.
     * @param hostPortDatabase Connection string in format "host:port/database"
     * @return Configured builder
     */
    public static JDBCHandlerBuilder forMySQL(String hostPortDatabase) {
        return new JDBCHandlerBuilder()
                .withDatabaseType("mysql")
                .withFilePath(hostPortDatabase);
    }

    /**
     * Creates a builder for PostgreSQL databases.
     * @param hostPortDatabase Connection string in format "host:port/database"
     * @return Configured builder
     */
    public static JDBCHandlerBuilder forPostgreSQL(String hostPortDatabase) {
        return new JDBCHandlerBuilder()
                .withDatabaseType("postgresql")
                .withFilePath(hostPortDatabase);
    }
}