package dev.iseal.sealUtils.systems.jdbc;

import dev.iseal.sealUtils.systems.database.JDBCHandler;
import dev.iseal.sealUtils.systems.database.JDBCHandlerBuilder;
import dev.iseal.sealUtils.systems.database.JDBCHelper;
import dev.iseal.sealUtils.utils.ExceptionHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Blob;
import java.sql.Clob;
import java.time.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class JDBCHelperTest {

    private static JDBCHandler handler;

    private static String dbPath = System.getProperty("user.dir") + "/tests/jdbc/test_helper";
    private static Path dbDirPath = Paths.get(System.getProperty("user.dir") + "/tests/jdbc");


    @BeforeAll
    static void setup() {
        handler = JDBCHandlerBuilder
                .forH2(dbPath)
                .buildAndConnect();
    }

    @AfterAll
    static void tearDown() {
        handler.disconnect();
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

    @Test
    void test_convertTypeToSQLType() {
        Map<Class<?>, String> typeMappings = null;
        try {
            Field typeMapField = JDBCHelper.class.getDeclaredField("TYPE_MAPPINGS");
            typeMapField.setAccessible(true);

            typeMappings = (Map<Class<?>, String>) typeMapField.get(null);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Could not access TYPE_MAPPINGS field: " + e.getMessage());
        }

        // easy test for conversion
        for (Map.Entry<Class<?>, String> entry : typeMappings.entrySet()) {
            Class<?> javaType = entry.getKey();
            String sqlType = entry.getValue();

            // Convert the Java type to SQL type
            String convertedSQLType = JDBCHelper.convertTypeToSQLType(javaType);

            // Assert that the converted SQL type matches the expected SQL type
            assertEquals(sqlType, convertedSQLType, "SQL type mismatch for Java type: " + javaType);
        }

        // add it to the db and check if it gets converted correctly
        for (Map.Entry<Class<?>, String> entry : typeMappings.entrySet()) {
            Class<?> javaType = entry.getKey();
            String sqlType = entry.getValue();
            System.out.println("Testing type: " + javaType + " with SQL type: " + sqlType);

            try {
                String tableName = "TEST_TABLE_" + javaType.getSimpleName().toUpperCase()
                        .replaceAll("[\\[\\]]", "_");
                assertTrue(handler.createTable(tableName, Map.of("id", "INTEGER", "value", sqlType)));

                // Insert a record with appropriate test value based on type
                Object testValue = createTestValueForType(javaType);
                if (testValue != null) {
                    handler.insertRecord(tableName, Map.of("id", 1, "value", testValue));

                    // Query the value back
                    List<Map<String, Object>> results = handler.queryRecords(tableName, Map.of("id", 1));

                    // Assert that we got results and the value is not null
                    assertFalse(results.isEmpty(), "No results returned for Java type: " + javaType);
                    Map<String, Object> record = results.get(0);
                    assertNotNull(record.get("value"), "Value is null for Java type: " + javaType);
                }
            } catch (Exception e) {
                fail("Failed to test database operations for type " + javaType.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Creates a test value appropriate for each type we want to test
     */
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
}