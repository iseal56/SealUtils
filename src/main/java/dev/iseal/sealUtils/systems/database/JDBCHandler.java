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
 */
public class JDBCHandler {
  private final Logger LOGGER;

  private String jdbcUrl;
  private String username;
  private String password;
  private Connection connection;
  private boolean strictMode;

  /**
   * Constructor for JDBCHandler. Protected to enforce use of {@link JDBCHandlerBuilder}.
   * @param strictMode Whether to enable strict mode for error handling
   */
  protected JDBCHandler(boolean strictMode) {
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
   * Establishes a connection to the database.
   *
   * @return true if connection is successful, false otherwise
   */
  public boolean connect() {
    try {
      // check if the connection is already established
      if (connection != null && !connection.isClosed()) {
        return true;
      }

      // check driver availability before connecting
      checkDriverAvailability();

      // initiatiate the connection
      connection = DriverManager.getConnection(jdbcUrl, username, password);
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
   * Checks if the database connection is currently established and valid.
   *
   * @return true if connected to the database, false otherwise
   */
  public boolean isConnected() {
    try {
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
   * Closes the database connection.
   *
   * @return true if disconnection is successful, false otherwise
   */
  public boolean disconnect() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
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
    // Consider quoting table name: String safeTableName = "\"" + tableName.replace("\"", "\"\"") + "\"";
    String sql = "DELETE FROM " + tableName;

    if (condition != null && !condition.isEmpty()) {
      // Note: Column names within the 'condition' string are not automatically quoted here.
      // The caller must ensure 'condition' is safe and correctly formatted.
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
   * Begins a transaction.
   *
   * @return true if the transaction was successfully started, false otherwise
   */
  public boolean beginTransaction() {
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
   * Commits a transaction.
   *
   * @return true if the transaction was successfully committed, false otherwise
   */
  public boolean commitTransaction() {
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
   * Rolls back a transaction.
   *
   * @return true if the transaction was successfully rolled back, false otherwise
   */
  public boolean rollbackTransaction() {
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
}