package dev.iseal.sealUtils.systems.database;

import dev.iseal.sealUtils.SealUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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
 */
public class JDBCHandler {

    // Note: Table name quoting (e.g., with double quotes) might be necessary for names with
    // special characters or reserved keywords, depending on the database.
    // This implementation does not quote table names in SQL statements,
    // assuming they are sanitized and do not contain special characters.
    // FIXME: Consider quoting table names in SQL statements to prevent SQL injection and handle special characters.

    private final Logger LOGGER;

    private String jdbcUrl;
    private String username;
    private String password;
    private HikariDataSource dataSource;
    private final boolean strictMode;
    private final int maxPoolSize;

    /**
     * Constructor for JDBCHandler. Protected to enforce use of {@link JDBCHandlerBuilder}.
     * @param strictMode Whether to enable strict mode for error handling
     */
    protected JDBCHandler(boolean strictMode, int maxPoolSize) {
        this.LOGGER = SealUtils.getLogger();
        this.strictMode = strictMode;
        this.maxPoolSize = maxPoolSize;
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
     * Establishes a connection to the database.
     *
     * @return true if connection is successful, false otherwise
     */
    public boolean connect() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                return true;
            }
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            if (username != null) config.setUsername(username);
            if (password != null) config.setPassword(password);
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(0);
            config.setLeakDetectionThreshold(30000); // 30 seconds leak detection threshold
            dataSource = new HikariDataSource(config);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize connection pool", e);
            if (strictMode) throw new RuntimeException(e);
            return false;
        }
    }

    /**
     * Closes the connection pool.
     */
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Gets a connection from the pool.
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource is not initialized or closed");
        }
        return dataSource.getConnection();
    }

    /**
     * Checks if the database connection is currently established and valid.
     *
     * @return true if connected to the database, false otherwise
     */
    public boolean isConnected() {
        try {
            return dataSource != null && !dataSource.isClosed();
        } catch (Exception e) {
            if (strictMode)
                throw new RuntimeException("Error checking connection status: " + e.getMessage(), e);
            else
                LOGGER.log(Level.SEVERE, "Error checking connection status", e);
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
        String sql = "CREATE DATABASE " + databaseName;
        try (Connection conn = getConnection(); Statement statement = conn.createStatement()) {
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
        String sql = "DROP DATABASE " + databaseName;
        try (Connection conn = getConnection(); Statement statement = conn.createStatement()) {
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

        try (Connection conn = getConnection(); Statement statement = conn.createStatement()) {
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
     * Creates a new table in the database.
     *
     * @param conn The connection to use (must be managed by the caller)
     * @param tableName The name of the table to create
     * @param columns A map of column names and their SQL type definitions
     * @return true if creation is successful, false otherwise
     */
    public boolean createTable(Connection conn, String tableName, Map<String, String> columns) {
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

        try (Statement statement = conn.createStatement()) {
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
        if (dataSource == null) {
            LOGGER.log(Level.WARNING, "Cannot check table existence, dataSource is null.");
            if (strictMode) {
                throw new IllegalStateException("DataSource is not initialized.");
            }
            return false;
        }
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String effectiveTableName = tableName;

            // Adjust table name case based on database metadata
            // For databases like Oracle that store unquoted identifiers in uppercase
            if (metaData.storesUpperCaseIdentifiers()) {
                effectiveTableName = tableName.toUpperCase();
            } else if (metaData.storesLowerCaseIdentifiers()) {
                // For databases that store unquoted identifiers in lowercase
                effectiveTableName = tableName.toLowerCase();
            }
            // If the database stores mixed case identifiers and is case-sensitive,
            // the original tableName should be used as is.

            try (ResultSet rs = metaData.getTables(null, null, effectiveTableName, new String[]{"TABLE"})) {
                return rs.next(); // If rs.next() is true, a table with that name exists.
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
     * This method first checks if the table exists using {@link #tableExists(String)}.
     * If the table does not exist (or if {@link #tableExists(String)} returns false,
     * for example, due to an error in non-strict mode after logging it), this
     * operation is considered successful as no drop action is needed or possible based on the check.
     * If the table exists, it attempts to drop it.
     *
     * @param tableName The name of the table to drop
     * @return true if the table was dropped, or if it was determined that the table does not exist
     *         or its existence could not be confirmed (in which case no drop is attempted).
     *         Returns false if an error occurred during the actual drop attempt (and not in strictMode).
     *         Throws RuntimeException in strictMode if any underlying operation fails (including the check or the drop).
     */
    public boolean dropTable(String tableName) {
        if (dataSource == null) {
            LOGGER.log(Level.WARNING, "Cannot drop table '" + tableName + "', dataSource is null.");
            if (strictMode) {
                throw new IllegalStateException("DataSource is not initialized.");
            }
            return false;
        }

        // tableExists will handle its own strictMode exceptions and logging.
        // If strictMode is on and tableExists fails, it throws, and this method call propagates that.
        // If strictMode is off and tableExists fails (e.g., SQLException), it logs and returns false.
        if (!tableExists(tableName)) {
            // This means either:
            // 1. The table genuinely does not exist.
            // 2. tableExists encountered an error in non-strict mode (and logged it), returning false.
            // In either scenario, we don't proceed with a DROP SQL command.
            LOGGER.info("Table '" + tableName + "' does not exist or its existence could not be confirmed; no drop attempt will be made.");
            return true; // The desired state (table not present) is effectively met or action is appropriately skipped.
        }

        // If we reach here, tableExists(tableName) returned true, meaning the table was found.
        String sql = "DROP TABLE " + tableName;

        try (Connection conn = getConnection(); Statement statement = conn.createStatement()) {
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
        StringBuilder columnsBuilder = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> paramValues = new ArrayList<>();

        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                columnsBuilder.append(", ");
                placeholders.append(", ");
            }
            // Quote column names to handle reserved keywords or special characters
            columnsBuilder.append("\"").append(entry.getKey()).append("\"");
            placeholders.append("?");
            paramValues.add(entry.getValue());
            first = false;
        }

        // It's generally safer to quote table names as well,
        // but the original sanitization is kept here.
        // Consider replacing with `String safeTableName = "\"" + tableName.replace("\"", "\"\"") + "\"";`
        // if table names can also be keywords or contain special characters.
        String sanitizedTableName = tableName.replaceAll("\\s+", "_").replaceAll("\"", ""); // Basic sanitization

        String sql = "INSERT INTO " + sanitizedTableName + " (" + columnsBuilder.toString() + ") VALUES (" + placeholders.toString() + ")";

        try (Connection conn = getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
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
     * Overloaded: Inserts a record into a table using a provided connection (for transaction support).
     * @param conn The connection to use (must be managed by the caller)
     * @param tableName The name of the table
     * @param values A map of column names and their values
     * @return true if insertion is successful, false otherwise
     */
    public boolean insertRecord(Connection conn, String tableName, Map<String, Object> values) {
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
        String sql = "INSERT INTO " + sanitizedTableName + " (" + columnsBuilder + ") VALUES (" + placeholders + ")";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
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
        String columnStr = "*";
        if (columns != null && columns.length > 0) {
            // Quote column names if they are specified
            StringBuilder quotedColumns = new StringBuilder();
            for (int i = 0; i < columns.length; i++) {
                quotedColumns.append("\"").append(columns[i]).append("\"");
                if (i < columns.length - 1) {
                    quotedColumns.append(", ");
                }
            }
            columnStr = quotedColumns.toString();
        }

        // Consider quoting table name: String safeTableName = "\"" + tableName.replace("\"", "\"\"") + "\"";
        String sql = "SELECT " + columnStr + " FROM " + tableName;


        if (condition != null && !condition.isEmpty()) {
            // Note: Column names within the 'condition' string are not automatically quoted here.
            // The caller must ensure 'condition' is safe and correctly formatted.
            sql += " WHERE " + condition;
        }

        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i); // Already correct from DB
                        Object value = resultSet.getObject(i);
                        row.put(columnName, value);
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
     * Overloaded: Queries records from a table using a provided connection (for transaction support).
     * @param conn The connection to use (must be managed by the caller)
     * @param tableName The name of the table
     * @param columns The columns to retrieve, or null for all columns
     * @param condition The WHERE condition for the query
     * @param params Parameters for the WHERE condition
     * @return A list of maps representing the query results
     */
    public List<Map<String, Object>> queryRecords(Connection conn, String tableName, String[] columns, String condition, Object... params) {
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
        List<Map<String, Object>> results = new ArrayList<>();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = resultSet.getObject(i);
                        row.put(columnName, value);
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
            // No conditions, return all records
            return queryRecords(tableName, null, null);
        }

        StringBuilder conditionBuilder = new StringBuilder();
        List<Object> params = new ArrayList<>();

        boolean first = true;
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            if (!first) {
                conditionBuilder.append(" AND ");
            }
            // Quote column names in conditions
            conditionBuilder.append("\"").append(entry.getKey()).append("\"").append(" = ?");
            params.add(entry.getValue());
            first = false;
        }

        return queryRecords(tableName, null, conditionBuilder.toString(), params.toArray());
    }

    /**
     * Overloaded: Queries records from a table using a provided connection and a map for conditions (for transaction support).
     * @param conn The connection to use (must be managed by the caller)
     * @param tableName The name of the table
     * @param conditions A map of column names and their values for WHERE conditions
     * @return A list of maps representing the query results
     */
    public List<Map<String, Object>> queryRecords(Connection conn, String tableName, Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            // No conditions, return all records
            return queryRecords(conn, tableName, null, null);
        }

        StringBuilder conditionBuilder = new StringBuilder();
        List<Object> params = new ArrayList<>();

        boolean first = true;
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            if (!first) {
                conditionBuilder.append(" AND ");
            }
            // Quote column names in conditions
            conditionBuilder.append("\"").append(entry.getKey()).append("\"").append(" = ?");
            params.add(entry.getValue());
            first = false;
        }

        return queryRecords(conn, tableName, null, conditionBuilder.toString(), params.toArray());
    }

    /**
     * Checks if the required database driver is available based on the JDBC URL.
     *
     * @throws SQLException if the appropriate driver can't be found
     */
    private void checkDriverAvailability() throws SQLException { //NOPMD - testing
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
        StringBuilder setClause = new StringBuilder();
        List<Object> paramValues = new ArrayList<>();

        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                setClause.append(", ");
            }
            // Quote column names in SET clause
            setClause.append("\"").append(entry.getKey()).append("\"").append(" = ?");
            paramValues.add(entry.getValue());
            first = false;
        }

        // Consider quoting table name: String safeTableName = "\"" + tableName.replace("\"", "\"\"") + "\"";
        String sql = "UPDATE " + tableName + " SET " + setClause;


        if (condition != null && !condition.isEmpty()) {
            // Note: Column names within the 'condition' string are not automatically quoted here.
            // The caller must ensure 'condition' is safe and correctly formatted.
            sql += " WHERE " + condition;
        }

        try (Connection conn = getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
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
     * Overloaded: Updates records in a table using a provided connection (for transaction support).
     * @param conn The connection to use (must be managed by the caller)
     * @param tableName The name of the table
     * @param values A map of column names and their new values
     * @param condition The WHERE condition for the update
     * @param conditionParams Parameters for the WHERE condition
     * @return true if update is successful, false otherwise
     */
    public boolean updateRecords(Connection conn, String tableName, Map<String, Object> values, String condition, Object... conditionParams) {
        StringBuilder setClause = new StringBuilder();
        List<Object> paramValues = new ArrayList<>();
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                setClause.append(", ");
            }
            setClause.append("\"").append(entry.getKey()).append("\" = ?");
            paramValues.add(entry.getValue());
            first = false;
        }
        String sql = "UPDATE " + tableName + " SET " + setClause;
        if (condition != null && !condition.isEmpty()) {
            sql += " WHERE " + condition;
        }
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
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
        String sql = "DELETE FROM " + tableName;

        if (condition != null && !condition.isEmpty()) {
            // Note: Column names within the 'condition' string are not automatically quoted here.
            // The caller must ensure 'condition' is safe and correctly formatted.
            sql += " WHERE " + condition;
        }

        try (Connection conn = getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
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
     * Overloaded: Deletes records from a table using a provided connection (for transaction support).
     * @param conn The connection to use (must be managed by the caller)
     * @param tableName The name of the table
     * @param condition The WHERE condition for deletion
     * @param params Parameters for the WHERE condition
     * @return true if deletion is successful, false otherwise
     */
    public boolean deleteRecords(Connection conn, String tableName, String condition, Object... params) {
        String sql = "DELETE FROM " + tableName;
        if (condition != null && !condition.isEmpty()) {
            sql += " WHERE " + condition;
        }
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
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
     * Begins a transaction.
     *
     * @return true if the transaction was successfully started, false otherwise
     */
    public boolean beginTransaction() {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
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
     * Commits a transaction.
     *
     * @return true if the transaction was successfully committed, false otherwise
     */
    public boolean commitTransaction() {
        try (Connection conn = getConnection()) {
            conn.commit();
            conn.setAutoCommit(true);
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
     * Rolls back a transaction.
     *
     * @return true if the transaction was successfully rolled back, false otherwise
     */
    public boolean rollbackTransaction() {
        try (Connection conn = getConnection()) {
            conn.rollback();
            conn.setAutoCommit(true);
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
