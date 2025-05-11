package dev.iseal.sealUtils.systems.database;

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

  /**
   * Constructor for JDBCHandler. Protected to enforce use of {@link JDBCHandlerBuilder}.
   * @param log Logger to use for logging operations
   */
  protected JDBCHandler(Logger log) {
    this.LOGGER = log;
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
      LOGGER.log(Level.SEVERE, "Failed to connect to database", e);
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
      sql.append(column.getKey()).append(" ").append(column.getValue());
      first = false;
    }

    sql.append(")");

    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(sql.toString());
      LOGGER.info("Table created successfully: " + tableName);
      return true;
    } catch (SQLException e) {
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
    StringBuilder columns = new StringBuilder();
    StringBuilder placeholders = new StringBuilder();
    List<Object> paramValues = new ArrayList<>();

    boolean first = true;
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      if (!first) {
        columns.append(", ");
        placeholders.append(", ");
      }
      columns.append(entry.getKey());
      placeholders.append("?");
      paramValues.add(entry.getValue());
      first = false;
    }
    tableName = tableName.replaceAll("\\s+", " ");
    tableName = tableName.replaceAll("\"", "");

    String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < paramValues.size(); i++) {
        statement.setObject(i + 1, paramValues.get(i));
      }

      int rowsAffected = statement.executeUpdate();
      return rowsAffected > 0;
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to insert record into table: " + tableName, e);
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
      columnStr = String.join(", ", columns);
    }

    String sql = "SELECT " + columnStr + " FROM " + tableName;

    if (condition != null && !condition.isEmpty()) {
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
            String columnName = metaData.getColumnName(i);
            Object value = resultSet.getObject(i);
            row.put(columnName, value);
          }

          results.add(row);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to query records from table: " + tableName, e);
    }

    return results;
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
      setClause.append(entry.getKey()).append(" = ?");
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
      sql += " WHERE " + condition;
    }

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) {
        statement.setObject(i + 1, params[i]);
      }

      int rowsAffected = statement.executeUpdate();
      return rowsAffected > 0;
    } catch (SQLException e) {
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
      LOGGER.log(Level.SEVERE, "Failed to rollback transaction", e);
      return false;
    }
  }
}