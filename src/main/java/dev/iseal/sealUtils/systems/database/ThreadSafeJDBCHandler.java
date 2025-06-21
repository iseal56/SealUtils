package dev.iseal.sealUtils.systems.database;

import dev.iseal.sealUtils.SealUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A JDBC handler class that abstracts database operations.
 * Provides functionality for database management, table operations, and CRUD operations.
 * This version is thread-safe, using a ThreadLocal to manage database connections.
 * Note: This thread-safety comes at the cost of potentially higher memory usage and a slight overhead, but it ensures that each thread has its own connection instance.
 */
public class ThreadSafeJDBCHandler {

    // Note: Table name quoting (e.g., with double quotes) might be necessary for names with
    // special characters or reserved keywords, depending on the database.
    // This implementation does not quote table names in SQL statements,
    // assuming they are sanitized and do not contain special characters.
    // FIXME: Consider quoting table names in SQL statements to prevent SQL injection and handle special characters.

    private final Logger LOGGER;

    private String jdbcUrl;
    private String username;
    private String password;
    private final ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();
    private boolean strictMode;

    /**
     * Constructor for ThreadSafeJDBCHandler. Protected to enforce use of {@link JDBCHandlerBuilder}.
     * @param strictMode Whether to enable strict mode for error handling
     */
    protected ThreadSafeJDBCHandler(boolean strictMode) {
        this.LOGGER = SealUtils.getLogger();
        this.strictMode = strictMode;
    }

    /**
     * Initializes the JDBC handler with database credentials.
     *
     * @param jdbcUrl The JDBC URL for connecting to the database
     * @param username The username for database authentication
     * @param password The password for database authentication
     */
    public void init(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * Establishes a connection to the database for the current thread.
     *
     * @return true if connection is successful, false otherwise
     */
    public boolean connect() {
        try {
            Connection conn = connectionThreadLocal.get();
            if (conn != null && !conn.isClosed()) {
                return true;
            }

            checkDriverAvailability();

            conn = DriverManager.getConnection(jdbcUrl, username, password);
            conn.setAutoCommit(true);
            connectionThreadLocal.set(conn);
            return true;
        } catch (SQLException e) {
            if (strictMode)
                throw new RuntimeException("Failed to connect to database: " + e.getMessage(), e);
            else
                LOGGER.log(Level.WARNING, "Failed to connect to database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the database connection is currently established and valid for the current thread.
     *
     * @return true if connected to the database, false otherwise
     */
    public boolean isConnected() {
        try {
            Connection connection = connectionThreadLocal.get();
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            if (strictMode)
                throw new RuntimeException("Error checking connection status: " + e.getMessage(), e);
            else
                LOGGER.log(Level.SEVERE, "Error checking connection status", e);
            return false;
        }
    }

    /**
     * Closes the database connection for the current thread.
     *
     * @return true if disconnection is successful, false otherwise
     */
    public boolean disconnect() {
        try {
            Connection connection = connectionThreadLocal.get();
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            connectionThreadLocal.remove();
            return true;
        } catch (SQLException e) {
            if (strictMode)
                throw new RuntimeException("Failed to close database connection: " + e.getMessage(), e);
            else
                LOGGER.log(Level.SEVERE, "Failed to close database connection", e);
            return false;
        }
    }

    /**
     * Creates a new database.
     *
     * @param databaseName The name of the database to create
     * @return true if creation is successful, false otherwise
     */
    public boolean createDatabase(String databaseName) {
        Connection connection = connectionThreadLocal.get();
        if (connection == null) {
            LOGGER.log(Level.WARNING, "Cannot create database, connection is not established for this thread.");
            if (strictMode) throw new IllegalStateException("Connection is not established for this thread.");
            return false;
        }
        String sql = "CREATE DATABASE " + databaseName;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            LOGGER.info("Database created successfully: " + databaseName);
            return true;
        } catch (SQLException e) {
            if (strictMode)
                throw new RuntimeException("Failed to create database: " + databaseName + ": " + e.getMessage(), e);
            else
                LOGGER.log(Level.SEVERE, "Failed to create database: " + databaseName, e);
            return false;
        }
    }

    /**
     * Deletes a database.
     *
     * @param databaseName The name of the database to delete
     * @return true if deletion is successful, false otherwise
     */
    public boolean deleteDatabase(String databaseName) {
        Connection connection = connectionThreadLocal.get();
        if (connection == null) {
            LOGGER.log(Level.WARNING, "Cannot delete database, connection is not established for this thread.");
            if (strictMode) throw new IllegalStateException("Connection is not established for this thread.");
            return false;
        }
        String sql = "DROP DATABASE " + databaseName;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            LOGGER.info("Database deleted successfully: " + databaseName);
            return true;
        } catch (SQLException e) {
            if (strictMode)
                throw new RuntimeException("Failed to delete database: " + databaseName + ": " + e.getMessage(), e);
            else
                LOGGER.log(Level.SEVERE, "Failed to delete database: " + databaseName, e);
            return false;
        }
    }

    /**
     * Creates a new table in the database.
     *
     * @param tableName The name of the table to create
     * @param columns A map of column names and their SQL type definitions
     * @return true if creation is successful, false otherwise
     */
    public boolean createTable(String tableName, Map<String, String> columns) {
        Connection connection = connectionThreadLocal.get();
        if (connection == null) {
            LOGGER.log(Level.WARNING, "Cannot create table, connection is not established for this thread.");
            if (strictMode) throw new IllegalStateException("Connection is not established for this thread.");
            return false;
        }
        StringBuilder sql = new StringBuilder("CREATE TABLE " + tableName + " (");

        boolean first = true;
        for (Map.Entry<String, String> column : columns.entrySet()) {
            if (!first) {
                sql.append(", ");
            }
            sql.append("\"").append(column.getKey()).append("\"").append(" ").append(column.getValue()); // Quote column name
            first = false;
        }

        sql.append(")");

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql.toString());
            LOGGER.info("Table created successfully: " + tableName);
            return true;
        } catch (SQLException e) {
            if (strictMode)
                throw new RuntimeException("Failed to create table: " + tableName + ": " + e.getMessage(), e);
            else
                LOGGER.log(Level.SEVERE, "Failed to create table: " + tableName, e);
            return false;
        }
    }

    /**
     * Tests if a table exists in the database using DatabaseMetaData.
     *
     * @param tableName The name of the table to check
     * @return true if the table exists, false otherwise
     */
    public boolean tableExists(String tableName) {
        Connection connection = connectionThreadLocal.get();
        if (connection == null) {
            LOGGER.log(Level.WARNING, "Cannot check table existence, connection is not established for this thread.");
            if (strictMode) {
                throw new IllegalStateException("Connection is not established for this thread.");
            }
            return false;
        }
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            String effectiveTableName = tableName;

            if (metaData.storesUpperCaseIdentifiers()) {
                effectiveTableName = tableName.toUpperCase();
            } else if (metaData.storesLowerCaseIdentifiers()) {
                effectiveTableName = tableName.toLowerCase();
            }

            try (ResultSet rs = metaData.getTables(null, null, effectiveTableName, new String[]{"TABLE"})) {
                return rs.next();
            }
        } catch (SQLException e) {
            if (strictMode) {
                throw new RuntimeException("Error checking if table exists: " + tableName + ": " + e.getMessage(), e);
            } else {
                LOGGER.log(Level.SEVERE, "Error checking if table exists: " + tableName, e);
            }
            return false;
        }
    }

    /**
     * Drops a table from the database.
     *
     * @param tableName The name of the table to drop
     * @return true if the table was dropped or did not exist, false on error.
     */
    public boolean dropTable(String tableName) {
        Connection connection = connectionThreadLocal.get();
        if (connection == null) {
            LOGGER.log(Level.WARNING, "Cannot drop table '" + tableName + "', connection is not established for this thread.");
            if (strictMode) {
                throw new IllegalStateException("Connection is not established for this thread.");
            }
            return false;
        }

        if (!tableExists(tableName)) {
            LOGGER.info("Table '" + tableName + "' does not exist or its existence could not be confirmed; no drop attempt will be made.");
            return true;
        }

        String sql = "DROP TABLE " + tableName;

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            LOGGER.info("Table '" + tableName + "' dropped successfully.");
            return true;
        } catch (SQLException e) {
            if (strictMode) {
                throw new RuntimeException("Failed to drop table '" + tableName + "': " + e.getMessage(), e);
            } else {
                LOGGER.log(Level.SEVERE, "Failed to drop table '" + tableName + "'. SQL: " + sql, e);
            }
            return false;
        }
    }

    /**
     * Inserts a record into a table.
     *
     * @param tableName The name of the table
     * @param values A map of column names and their values
     * @return true if insertion is successful, false otherwise
     */
    public boolean insertRecord(String tableName, Map<String, Object> values) {
        Connection connection = connectionThreadLocal.get();
        if (connection == null) {
            LOGGER.log(Level.WARNING, "Cannot insert record, connection is not established for this thread.");
            if (strictMode) throw new IllegalStateException("Connection is not established for this thread.");
            return false;
        }
        StringBuilder columnsBuilder = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> paramValues = new ArrayList<>();

        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                columnsBuilder.append(", ");
                placeholders.append(", ");
            }
            columnsBuilder.append("\"").append(entry.getKey()).append("\"");
            placeholders.append("?");
            paramValues.add(entry.getValue());
            first = false;
        }

        String sanitizedTableName = tableName.replaceAll("\\s+", "_").replaceAll("\"", "");
        String sql = "INSERT INTO " + sanitizedTableName + " (" + columnsBuilder.toString() + ") VALUES (" + placeholders.toString() + ")";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < paramValues.size(); i++) {
                statement.setObject(i + 1, paramValues.get(i));
            }

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            if (strictMode)
                throw new RuntimeException("Failed to insert record into table: " + tableName + ": " + e.getMessage(), e);
            else
                LOGGER.log(Level.SEVERE, "Failed to insert record into table: " + tableName + " with SQL: " + sql, e);
            return false;
        }
    }

    /**
     * Queries records from a table.
     *
     * @param tableName The name of the table
     * @param columns The columns to retrieve, or null for all columns
     * @param condition The WHERE condition for the query
     * @param params Parameters for the WHERE condition
     * @return A list of maps representing the query results
     */
    public List<Map<String, Object>> queryRecords(String tableName, String[] columns, String condition, Object... params) {
        Connection connection = connectionThreadLocal.get();
        List<Map<String, Object>> results = new ArrayList<>();
        if (connection == null) {
            LOGGER.log(Level.WARNING, "Cannot query records, connection is not established for this thread.");
            if (strictMode) throw new IllegalStateException("Connection is not established for this thread.");
            return results;
        }

        String columnStr = "*";
        if (columns != null && columns.length > 0) {
            StringBuilder quotedColumns = new StringBuilder();
            for (int i = 0; i < columns.length; i++) {
                quotedColumns.append("\"").append(columns[i]).append("\"");
                if (i < columns.length - 1) {
                    quotedColumns.append(", ");
                }
            }
            columnStr = quotedColumns.toString();
        }

        String sql = "SELECT " + columnStr + " FROM " + tableName;

        if (condition != null && !condition.isEmpty()) {
            sql += " WHERE " + condition;
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnName(i), resultSet.getObject(i));
                    }
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            if (strictMode)
                throw new RuntimeException("Failed to query records from table: " + tableName + ": " + e.getMessage(), e);
            else
                LOGGER.log(Level.SEVERE, "Failed to query records from table: " + tableName, e);
        }

        return results;
    }

    /**
     * Queries records from a table using a map for conditions.
     *
     * @param tableName The name of the table
     * @param conditions A map of column names and their values for WHERE conditions
     * @return A list of maps representing the query results
     */
    public List<Map<String, Object>> queryRecords(String tableName, Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return queryRecords(tableName, null, null);
        }

        StringBuilder conditionBuilder = new StringBuilder();
        List<Object> params = new ArrayList<>();

        boolean first = true;
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            if (!first) {
                conditionBuilder.append(" AND ");
            }
            conditionBuilder.append("\"").append(entry.getKey()).append("\"").append(" = ?");
            params.add(entry.getValue());
            first = false;
        }

        return queryRecords(tableName, null, conditionBuilder.toString(), params.toArray());
    }

    /**
     * Checks if the required database driver is available based on the JDBC URL.
     *
     * @throws SQLException if the appropriate driver can't be found
     */
    private void checkDriverAvailability() throws SQLException {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            throw new SQLException("JDBC URL is not set");
        }

        String driverClass;
        String dependencyInfo;

        if (jdbcUrl.startsWith("jdbc:sqlite:")) {
            driverClass = "org.sqlite.JDBC";
            dependencyInfo = "org.xerial:sqlite-jdbc";
        } else if (jdbcUrl.startsWith("jdbc:h2:")) {
            driverClass = "org.h2.Driver";
            dependencyInfo = "com.h2database:h2";
        } else if (jdbcUrl.startsWith("jdbc:hsqldb:")) {
            driverClass = "org.hsqldb.jdbc.JDBCDriver";
            dependencyInfo = "org.hsqldb:hsqldb";
        } else if (jdbcUrl.startsWith("jdbc:mysql:")) {
            driverClass = "com.mysql.cj.jdbc.Driver";
            dependencyInfo = "mysql:mysql-connector-java";
        } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            driverClass = "org.postgresql.Driver";
            dependencyInfo = "org.postgresql:postgresql";
        } else if (jdbcUrl.startsWith("jdbc:oracle:thin:@")) {
            driverClass = "oracle.jdbc.OracleDriver";
            dependencyInfo = "com.oracle.database.jdbc:ojdbc8";
        } else {
            throw new SQLException("Unsupported database type in URL: " + jdbcUrl);
        }

        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Database driver not found: " + driverClass +
                    ". Please add the following dependency to your project: " + dependencyInfo);
        }
    }

    /**
     * Updates records in a table.
     *
     * @param tableName The name of the table
     * @param values A map of column names and their new values
     * @param condition The WHERE condition for the update
     * @param conditionParams Parameters for the WHERE condition
     * @return true if update is successful, false otherwise
     */
    public boolean updateRecords(String tableName, Map<String, Object> values, String condition, Object... conditionParams) {
        Connection connection = connectionThreadLocal.get();
        if (connection == null) {
            LOGGER.log(Level.WARNING, "Cannot update records, connection is not established for this thread.");
            if (strictMode) throw new IllegalStateException("Connection is not established for this thread.");
            return false;
        }
        StringBuilder setClause = new StringBuilder();
        List<Object> paramValues = new ArrayList<>();

        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                setClause.append(", ");
            }
            setClause.append("\"").append(entry.getKey()).append("\"").append(" = ?");
            paramValues.add(entry.getValue());
            first = false;
        }

        String sql = "UPDATE " + tableName + " SET " + setClause;

        if (condition != null && !condition.isEmpty()) {
            sql += " WHERE " + condition;
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int paramIndex = 1;
            for (Object value : paramValues) {
                statement.setObject(paramIndex++, value);
            }
            for (Object param : conditionParams) {
                statement.setObject(paramIndex++, param);
            }

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            if (strictMode)
                throw new RuntimeException("Failed to update records in table: " + tableName + ": " + e.getMessage(), e);
            else
                LOGGER.log(Level.SEVERE, "Failed to update records in table: " + tableName, e);
            return false;
        }
    }

    /**
     * Deletes records from a table.
     *
     * @param tableName The name of the table
     * @param condition The WHERE condition for deletion
     * @param params Parameters for the WHERE condition
     * @return true if deletion is successful, false otherwise
     */
    public boolean deleteRecords(String tableName, String condition, Object... params) {
        Connection connection = connectionThreadLocal.get();
        if (connection == null) {
            LOGGER.log(Level.WARNING, "Cannot delete records, connection is not established for this thread.");
            if (strictMode) throw new IllegalStateException("Connection is not established for this thread.");
            return false;
        }
        String sql = "DELETE FROM " + tableName;

        if (condition != null && !condition.isEmpty()) {
            sql += " WHERE " + condition;
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            if (strictMode)
                throw new RuntimeException("Failed to delete records from table: " + tableName + ": " + e.getMessage(), e);
            else
                LOGGER.log(Level.SEVERE, "Failed to delete records from table: " + tableName, e);
            return false;
        }
    }

    /**
     * Begins a transaction for the current thread.
     *
     * @return true if the transaction was successfully started, false otherwise
     */
    public boolean beginTransaction() {
        Connection connection = connectionThreadLocal.get();
        if (connection == null) {
            LOGGER.log(Level.WARNING, "Cannot begin transaction, connection is not established for this thread.");
            if (strictMode) throw new IllegalStateException("Connection is not established for this thread.");
            return false;
        }
        try {
            connection.setAutoCommit(false);
            return true;
        } catch (SQLException e) {
            if (strictMode)
                throw new RuntimeException("Failed to begin transaction: " + e.getMessage(), e);
            else
                LOGGER.log(Level.SEVERE, "Failed to begin transaction", e);
            return false;
        }
    }

    /**
     * Commits a transaction for the current thread.
     *
     * @return true if the transaction was successfully committed, false otherwise
     */
    public boolean commitTransaction() {
        Connection connection = connectionThreadLocal.get();
        if (connection == null) {
            LOGGER.log(Level.WARNING, "Cannot commit transaction, connection is not established for this thread.");
            if (strictMode) throw new IllegalStateException("Connection is not established for this thread.");
            return false;
        }
        try {
            connection.commit();
            connection.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            if (strictMode)
                throw new RuntimeException("Failed to commit transaction: " + e.getMessage(), e);
            else
                LOGGER.log(Level.SEVERE, "Failed to commit transaction", e);
            return false;
        }
    }

    /**
     * Rolls back a transaction for the current thread.
     *
     * @return true if the transaction was successfully rolled back, false otherwise
     */
    public boolean rollbackTransaction() {
        Connection connection = connectionThreadLocal.get();
        if (connection == null) {
            LOGGER.log(Level.WARNING, "Cannot rollback transaction, connection is not established for this thread.");
            if (strictMode) throw new IllegalStateException("Connection is not established for this thread.");
            return false;
        }
        try {
            connection.rollback();
            connection.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            if (strictMode)
                throw new RuntimeException("Failed to rollback transaction: " + e.getMessage(), e);
            else
                LOGGER.log(Level.SEVERE, "Failed to rollback transaction", e);
            return false;
        }
    }

    public String getDatabaseType() {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            return "unknown";
        }
        if (jdbcUrl.startsWith("jdbc:sqlite:")) {
            return "sqlite";
        } else if (jdbcUrl.startsWith("jdbc:h2:")) {
            return "h2";
        } else if (jdbcUrl.startsWith("jdbc:hsqldb:")) {
            return "hsqldb";
        } else if (jdbcUrl.startsWith("jdbc:mysql:")) {
            return "mysql";
        } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            return "postgresql";
        } else if (jdbcUrl.startsWith("jdbc:oracle:thin:@")) {
            return "oracle";
        }
        return "unknown";
    }
}