package dev.iseal.sealUtils.systems.database;

import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Map;
import java.util.UUID;

public class JDBCHelper {

    private static final Map<Class<?>, String> TYPE_MAPPINGS = Map.ofEntries(
            // Numeric types
            Map.entry(Integer.class, "INTEGER"),
            Map.entry(Short.class, "SMALLINT"),
            Map.entry(Long.class, "BIGINT"),
            Map.entry(Double.class, "DOUBLE"),
            Map.entry(Float.class, "FLOAT"),
            Map.entry(Byte.class, "TINYINT"),

            // Text types
            Map.entry(String.class, "VARCHAR(255)"),
            Map.entry(Character.class, "CHAR(1)"),
            Map.entry(char[].class, "VARCHAR(255)"),
            Map.entry(Clob.class, "CLOB"),

            // Boolean type
            Map.entry(Boolean.class, "BOOLEAN"),

            // Binary types
            Map.entry(byte[].class, "BLOB"),
            Map.entry(Blob.class, "BLOB"),

            // Other types
            Map.entry(UUID.class, "VARCHAR(36)")
    );

    /**
     * Converts a Java type to its corresponding SQL type.
     * @param type The Java type to convert
     * @return The SQL type as a string
     */
    public static <T> String convertTypeToSQLType(Class<T> type) {
        String sqlType = TYPE_MAPPINGS.get(type);
        if (sqlType == null) {
            throw new IllegalArgumentException("Unsupported type: " + type.getName());
        }
        return sqlType;
    }
}

