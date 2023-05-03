package dev.vertcode.vertstorage.util;

import dev.vertcode.vertstorage.StorageObject;
import dev.vertcode.vertstorage.annotations.StorageField;
import dev.vertcode.vertstorage.annotations.StorageId;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.*;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@UtilityClass
public class StorageSQLUtil {

    private static final Map<Class<?>, String> SQL_TYPE_MAP = new HashMap<>();

    static {
        SQL_TYPE_MAP.put(String.class, "VARCHAR(255)");
        SQL_TYPE_MAP.put(UUID.class, "VARCHAR(36)");
        SQL_TYPE_MAP.put(int.class, "INT");
        SQL_TYPE_MAP.put(Integer.class, "INT");
        SQL_TYPE_MAP.put(long.class, "BIGINT");
        SQL_TYPE_MAP.put(Long.class, "BIGINT");
        SQL_TYPE_MAP.put(double.class, "DOUBLE");
        SQL_TYPE_MAP.put(Double.class, "DOUBLE");
        SQL_TYPE_MAP.put(float.class, "FLOAT");
        SQL_TYPE_MAP.put(Float.class, "FLOAT");
        SQL_TYPE_MAP.put(boolean.class, "BOOLEAN");
        SQL_TYPE_MAP.put(Boolean.class, "BOOLEAN");
        SQL_TYPE_MAP.put(byte.class, "TINYINT");
        SQL_TYPE_MAP.put(Byte.class, "TINYINT");
        SQL_TYPE_MAP.put(short.class, "SMALLINT");
        SQL_TYPE_MAP.put(Short.class, "SMALLINT");
        SQL_TYPE_MAP.put(char.class, "CHAR(1)");
        SQL_TYPE_MAP.put(Character.class, "CHAR(1)");
        SQL_TYPE_MAP.put(byte[].class, "BLOB");
        SQL_TYPE_MAP.put(Byte[].class, "BLOB");
        SQL_TYPE_MAP.put(Date.class, "DATETIME");
        SQL_TYPE_MAP.put(Instant.class, "BIGINT");
    }

    /**
     * Generates the type definition for the provided StorageObject.
     *
     * @return The table definition for the StorageObject
     */
    public static String generateSQLTypeDefinition(Class<? extends StorageObject> clazz) {

        try {
            StringBuilder builder = new StringBuilder();
            // Generate an instance of the StorageObject, so we can get the default values for the fields
            StorageObject storageObject = clazz.getConstructor().newInstance();

            boolean isFirst = true;
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(StorageField.class)) {
                    continue;
                }

                StorageField storageField = field.getAnnotation(StorageField.class);
                String fieldName = storageField.columnName();
                String sqlType = getSQLType(field.getType());

                // If this isn't the first field, add a comma and space before adding the next field
                if (!isFirst) {
                    builder.append(", ");
                }

                // Make sure that the next field isn't the first field
                isFirst = false;

                // If the field isn't the id field, just add it to the table definition (e.g. "name VARCHAR(255)")
                if (!field.isAnnotationPresent(StorageId.class)) {
                    builder.append("`").append(fieldName).append("` ").append(sqlType);

                    // If the field has a default value, add it to the table definition (e.g. "name VARCHAR(255) DEFAULT 'test'")
                    field.setAccessible(true);
                    Object defaultValue = field.get(storageObject);
                    String defaultValueSQL = convertToSQLType(defaultValue);

                    if (defaultValueSQL != null && sqlType != "JSON") {
                        builder.append(" DEFAULT ").append(defaultValueSQL);
                    }
                    continue;
                }

                // If the field is the id field, add it to the table definition and make it the primary key (e.g. "id INT PRIMARY KEY")
                StorageId storageId = field.getAnnotation(StorageId.class);
                if (storageId.automaticallyGenerated() && sqlType.equals("INT")) {
                    builder.append("`").append(fieldName).append("` ").append(sqlType).append(" AUTO_INCREMENT PRIMARY KEY");
                    continue;
                }

                builder.append("`").append(fieldName).append("` ").append(sqlType).append(" PRIMARY KEY");
            }

            return builder.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate SQL type definition for " + clazz.getName(), ex);
        }
    }

    /**
     * Inserts the value into the prepared statement.
     *
     * @param statement The prepared statement
     * @param index     The index of the value
     * @param value     The value
     * @throws SQLException If an error occurs while setting the value
     */
    public static void insertValueIntoPrepStatement(PreparedStatement statement, int index, Object value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.NULL);
            return;
        }

        Class<?> type = value.getClass();
        if (type == String.class) {
            statement.setString(index, (String) value);
            return;
        }

        if (type == UUID.class) {
            statement.setString(index, value.toString());
            return;
        }

        if (type == int.class || type == Integer.class) {
            statement.setInt(index, (int) value);
            return;
        }

        if (type == long.class || type == Long.class) {
            statement.setLong(index, (long) value);
            return;
        }

        if (type == double.class || type == Double.class) {
            statement.setDouble(index, (double) value);
            return;
        }

        if (type == float.class || type == Float.class) {
            statement.setFloat(index, (float) value);
            return;
        }

        if (type == boolean.class || type == Boolean.class) {
            statement.setBoolean(index, (boolean) value);
            return;
        }

        if (type == byte.class || type == Byte.class) {
            statement.setByte(index, (byte) value);
            return;
        }

        if (type == short.class || type == Short.class) {
            statement.setShort(index, (short) value);
            return;
        }

        if (type == char.class || type == Character.class) {
            statement.setString(index, value.toString());
            return;
        }

        if (type == byte[].class || type == Byte[].class) {
            statement.setBytes(index, (byte[]) value);
            return;
        }

        if (type == Date.class) {
            statement.setTimestamp(index, new Timestamp(((Date) value).getTime()));
            return;
        }

        if (type == Instant.class) {
            statement.setLong(index, ((Instant) value).getEpochSecond());
            return;
        }

        // Convert the object to JSON
        statement.setString(index, StorageUtil.getGson().toJson(value));
    }

    /**
     * Returns the SQL type of the provided class.
     *
     * @param type The class to get the SQL type for
     * @return The SQL type
     */
    public static String getSQLType(Class<?> type) {
        return SQL_TYPE_MAP.getOrDefault(type, "JSON");
    }

    /**
     * Converts the provided object to the SQL type.
     *
     * @param object The object to convert
     * @return The SQL type
     */
    public static String convertToSQLType(Object object) {
        if (object == null) {
            return "NULL";
        }

        Class<?> type = object.getClass();
        if (type == String.class || type == UUID.class) {
            return "'" + object + "'";
        }

        if (type == Date.class) {
            return "'" + ((Date) object).toInstant().toString() + "'";
        }

        if (type == Instant.class) {
            return Long.toString(((Instant) object).getEpochSecond());
        }

        // Get the SQL type
        String sqlType = getSQLType(type);
        if (!sqlType.equals("JSON")) {
            return object.toString();
        }

        return "'" + StorageUtil.getGson().toJson(object) + "'";
    }

    /**
     * Gets all the column names and their data types from the provided table.
     *
     * @param connection the connection to the database
     * @param tableName  the name of the table to get columns from
     * @return a Map containing the column names as keys and their data types as values
     */
    public static Map<String, String> getTableColumns(Connection connection, String tableName) {
        Map<String, String> tableFields = new HashMap<>();

        try {
            // Get the columns from the table
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, tableName, null);

            // Loop through the columns and add them to the map
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");

                tableFields.put(columnName, columnType);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get table columns", ex);
        }

        return tableFields;
    }

    public static Object convertSQLValueToJavaValue(Object value, Type type) {
        if (value == null) {
            return null;
        }

        if (type == String.class) {
            return value;
        }

        if (type == UUID.class) {
            return UUID.fromString((String) value);
        }

        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value.toString());
        }

        if (type == long.class || type == Long.class) {
            return Long.parseLong(value.toString());
        }

        if (type == double.class || type == Double.class) {
            return Double.parseDouble(value.toString());
        }

        if (type == float.class || type == Float.class) {
            return Float.parseFloat(value.toString());
        }

        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value.toString());
        }

        if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(value.toString());
        }

        if (type == short.class || type == Short.class) {
            return Short.parseShort(value.toString());
        }

        if (type == char.class || type == Character.class) {
            return value.toString().charAt(0);
        }

        if (type == byte[].class || type == Byte[].class) {
            return value;
        }

        if (type == Date.class) {
            return new Date(Timestamp.valueOf(value.toString()).getTime());
        }

        if (type == Instant.class) {
            return Instant.ofEpochSecond(Long.parseLong(value.toString()));
        }

        return StorageUtil.getGson().fromJson(value.toString(), type);
    }
}
