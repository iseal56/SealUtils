package dev.iseal.sealUtils.systems.jdbc;

import dev.iseal.sealUtils.systems.database.JDBCHandler;
import dev.iseal.sealUtils.systems.database.JDBCHandlerBuilder;
import dev.iseal.sealUtils.systems.database.JDBCHelper;
import dev.iseal.sealUtils.utils.ExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;

public class JDBCHelperTest {

    private JDBCHandler handler;
    private static final String BASE_DB_PATH = System.getProperty("user.dir") + "/tests/jdbc/";
    private static final Path DB_DIR_PATH = Paths.get(BASE_DB_PATH);

    @AfterEach
    void cleanup() {
        if (handler != null && handler.isConnected()) {
            handler.disconnect();
        }

        try {
            Files.walkFileTree(DB_DIR_PATH, new SimpleFileVisitor<>() {
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

    @Test
    void test_convertTypeToSQLType_allDBs() {
        List<DBConfig> dbConfigs = List.of(
                new DBConfig("sqlite", JDBCHandlerBuilder.forSqlite(BASE_DB_PATH + "test_helper_sqlite")),
                new DBConfig("h2", JDBCHandlerBuilder.forH2(BASE_DB_PATH + "test_helper_h2")),
                new DBConfig("hsqldb", JDBCHandlerBuilder.forHSQLDB(BASE_DB_PATH + "test_helper_hsqldb"))
        );

        for (DBConfig config : dbConfigs) {
            System.out.println("Testing with DB: " + config.name);

            handler = config.builder.buildAndConnect();
            assertTrue(handler.isConnected(), "Handler should be connected for " + config.name);

            Map<Class<?>, String> typeMappings = null;
            try {
                Field typeMapField = JDBCHelper.class.getDeclaredField("TYPE_MAPPINGS");
                typeMapField.setAccessible(true);
                typeMappings = (Map<Class<?>, String>) typeMapField.get(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                fail("Could not access TYPE_MAPPINGS field: " + e.getMessage());
            }

            for (Map.Entry<Class<?>, String> entry : typeMappings.entrySet()) {
                Class<?> javaType = entry.getKey();
                String sqlType = entry.getValue();
                String tableName = "TEST_" + javaType.getSimpleName().toUpperCase().replaceAll("[", "_").replaceAll("]", "_");

                try {
                    assertTrue(handler.createTable(tableName, Map.of("id", "INTEGER", "value", sqlType)));

                    Object testValue = createTestValueForType(javaType);
                    if (testValue != null) {
                        handler.insertRecord(tableName, Map.of("id", 1, "value", testValue));
                        List<Map<String, Object>> results = handler.queryRecords(tableName, Map.of("id", 1));
                        assertFalse(results.isEmpty(), "No results returned for " + javaType + " on " + config.name);
                        assertNotNull(results.get(0).get("value"), "Value is null for " + javaType + " on " + config.name);
                    }
                } catch (Exception e) {
                    fail("DB error with " + config.name + " and type " + javaType + ": " + e.getMessage());
                }
            }

            handler.disconnect();
        }
    }

    private Object createTestValueForType(Class<?> type) {
        if (type == Integer.class) return 1;
        if (type == String.class) return "test";
        if (type == Double.class) return 1.0;
        if (type == Boolean.class) return true;
        if (type == Long.class) return 1L;
        if (type == Float.class) return 1.0f;
        if (type == Short.class) return (short)1;
        if (type == Byte.class) return (byte)1;
        if (type == Character.class) return 'A';
        if (type == UUID.class) return UUID.randomUUID();
        return null;
    }

    private static class DBConfig {
        String name;
        JDBCHandlerBuilder builder;

        DBConfig(String name, JDBCHandlerBuilder builder) {
            this.name = name;
            this.builder = builder;
        }
    }
}