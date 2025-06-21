package dev.iseal.sealUtils.systems.jdbc;

import dev.iseal.sealUtils.systems.database.JDBCHandler;
import dev.iseal.sealUtils.systems.database.JDBCHandlerBuilder;
import dev.iseal.sealUtils.utils.ExceptionHandler;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class JDBCHandlerTest {

    private static final Logger LOGGER = Logger.getLogger(JDBCHandlerTest.class.getName());
    private static String dbPath = System.getProperty("user.dir") + "/tests/jdbc/test";
    private static Path dbDirPath = Paths.get(System.getProperty("user.dir") + "/tests/jdbc");
    private static Dotenv dotenv;

    @AfterAll
    static void tearDown() {
        // delete old db file and directory
        if (Files.exists(dbDirPath)) {
            LOGGER.info("Deleting old db dir and recreating it: " + dbDirPath);
            try {
                Files.walkFileTree(dbDirPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                ExceptionHandler.getInstance().dealWithException(e, Level.SEVERE, "FAILED_TO_DELETE_DB_DIR");
                fail("Failed to delete old db dir: " + e.getMessage());
            }
        }
    }

    @BeforeEach
    void setup() {

        try {
            dotenv = Dotenv.configure().load();
        } catch (DotenvException ex) {
            LOGGER.warning("Dotenv not found or failed to load. Skipping ADB test.");
            dotenv = null; // Set to null if dotenv is not available
        }

        // delete old db file and directory
        if (Files.exists(dbDirPath)) {
            LOGGER.info("Deleting old db dir and recreating it: " + dbDirPath);
            try {
                Files.walkFileTree(dbDirPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                ExceptionHandler.getInstance().dealWithException(e, Level.SEVERE, "FAILED_TO_DELETE_DB_DIR");
                fail("Failed to delete old db dir: " + e.getMessage());
            }
        }

        // Create directory
        try {
            Files.createDirectories(dbDirPath);
        } catch (IOException e) {
            ExceptionHandler.getInstance().dealWithException(e, Level.SEVERE, "FAILED_TO_CREATE_DB_DIR");
            fail("Failed to create db dir: " + e.getMessage());
        }

        LOGGER.info("Creating new db file: " + dbPath);
    }

    @Test
    void testSqlite() {
        // build a handler
        LOGGER.info("Starting SQLite test with dbPath: " + dbPath);
        JDBCHandlerBuilder builder = JDBCHandlerBuilder.forSqlite(dbPath)
                .withCredentials("admin", "password");
        JDBCHandler handler = builder.build();
        assertThat(handler)
                .isNotNull()
                .isExactlyInstanceOf(JDBCHandler.class);
        testTables(handler);
    }

    @Test
    void testH2() {
        // build a handler
        LOGGER.info("Starting H2 test with dbPath: " + dbPath);
        JDBCHandlerBuilder builder = JDBCHandlerBuilder.forH2(dbPath)
                .withCredentials("admin", "password");
        JDBCHandler handler = builder.build();
        assertThat(handler)
                .isNotNull()
                .isExactlyInstanceOf(JDBCHandler.class);
        testTables(handler);
    }

    @Test
    void testHSQLDB() {
        // build a handler
        LOGGER.info("Starting HSQLDB test with dbPath: " + dbPath);
        JDBCHandlerBuilder builder = JDBCHandlerBuilder.forHSQLDB(dbPath)
                .withCredentials("admin", "password");
        JDBCHandler handler = builder.build();
        assertThat(handler)
                .isNotNull()
                .isExactlyInstanceOf(JDBCHandler.class);
        testTables(handler);
    }

    @Test
    void testADB() {
        if (dotenv == null) {
            LOGGER.info("Skipping ADB test because dotenv is not available.");
            return; // Skip the test if dotenv is not available
        }

        LOGGER.info("Starting ADB test with dbPath: " + dbPath);

        // Load environment variables
        String adbUrl = dotenv.get("ADB_URL");
        String adbUser = dotenv.get("ADB_USER");
        String adbPassword = dotenv.get("ADB_PASSWORD");
        // Check if the environment variables are set
        assertNotNull(adbUrl);
        assertNotNull(adbUser);
        assertNotNull(adbPassword);
        assertFalse(adbUrl.isEmpty());
        assertFalse(adbUser.isEmpty());
        assertFalse(adbPassword.isEmpty());
        // build a handler
        JDBCHandlerBuilder builder = JDBCHandlerBuilder.forOracleAutonomousDescriptor(adbUrl, adbUser, adbPassword);
        JDBCHandler handler = builder.build();
        assertThat(handler)
                .isNotNull()
                .isExactlyInstanceOf(JDBCHandler.class);
        testTables(handler);
    }

    //TODO: make a test for MySQL and Postgres because they require a running server

    private void testTables(JDBCHandler handler) {
        handler.connect();
        assertThat(handler.isConnected())
                .isTrue();

        LOGGER.info("Testing table creation, insertion, and querying for " + handler.getDatabaseType());

        // create a table and insert a record
        HashMap<String, String> columns = new HashMap<>();
        columns.put("ID", "INTEGER PRIMARY KEY");
        columns.put("NAME", "VARCHAR(255)");
        if (handler.tableExists("test_table")) {
            assertTrue(handler.dropTable("test_table"));
        }
        assertTrue(handler.createTable("test_table", columns));

        HashMap<String, Object> values = new HashMap<>();
        values.put("ID", "1");
        values.put("NAME", "Test");
        assertTrue(handler.insertRecord("test_table", values));

        // check if the record was inserted
        List<Map<String, Object>> results = handler.queryRecords("test_table", null, null);
        assertThat(results)
                .isNotNull();

        Object idValue = results.get(0).get("ID");
        assertThat(idValue)
                .isInstanceOf(Number.class); // Ensure it's a number
        assertThat(((Number) idValue).intValue())
                .isEqualTo(1); // Compare its int value

        assertThat(results.get(0).get("NAME"))
                .isEqualTo("Test");

        // Test querying specific columns
        List<Map<String, Object>> nameOnlyResults = handler.queryRecords("test_table", new String[]{"NAME"}, null);
        assertThat(nameOnlyResults)
                .isNotNull();

        assertThat(nameOnlyResults.get(0).containsKey("NAME"))
                .isTrue();
        assertThat(nameOnlyResults.get(0).containsKey("ID"))
                .isFalse();

        // Test querying with a condition
        var filteredResults = handler.queryRecords("test_table", null, "id = ?", 1);
        assertThat(filteredResults)
                .isNotNull();

        assertEquals(1, filteredResults.size());

        // Test no results condition
        var emptyResults = handler.queryRecords("test_table", null, "id = ?", 999);
        assertThat(emptyResults)
                .isNotNull();

        assertTrue(emptyResults.isEmpty());

        // Clean up
        handler.disconnect();
        LOGGER.info("Testing table deletion, insertion, and querying for " + handler.getDatabaseType() + " completed successfully.");
    }
}