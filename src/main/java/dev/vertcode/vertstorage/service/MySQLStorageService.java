package dev.vertcode.vertstorage.service;

import dev.vertcode.vertstorage.StorageObject;
import dev.vertcode.vertstorage.StorageService;
import dev.vertcode.vertstorage.annotations.StorageField;
import dev.vertcode.vertstorage.annotations.StorageId;
import dev.vertcode.vertstorage.annotations.StorageMetadata;
import dev.vertcode.vertstorage.database.MySQLStorageDatabase;
import dev.vertcode.vertstorage.util.StorageSQLUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is the storage service for MySQL for the provided StorageObject.
 *
 * @param <T> The StorageObject this service is for.
 */
public class MySQLStorageService<T extends StorageObject> extends StorageService<T> {

    private final MySQLStorageDatabase storageDatabase;

    public MySQLStorageService(MySQLStorageDatabase storageDatabase, Class<T> clazz) {
        super(clazz);
        this.storageDatabase = storageDatabase;
    }

    @Override
    public void startupService() {
        // Ensure that the table is correct
        ensureCorrectTable();
    }

    @Override
    public T createInstance() {
        try {
            // Create the instance of the StorageObject
            T instance = this.clazz.getDeclaredConstructor().newInstance();

            // Loop through all the fields in the class
            for (Field field : this.clazz.getDeclaredFields()) {
                // We only want to populate the ID field
                if (!field.isAnnotationPresent(StorageId.class)) {
                    continue;
                }

                // We only want to populate the ID field if it is a number and is automatically generated
                if (!field.getType().equals(int.class) && !field.getType().equals(Integer.class)) {
                    continue;
                }

                StorageId annotation = field.getAnnotation(StorageId.class);
                // Check if the ID should be automatically generated
                if (!annotation.automaticallyGenerated()) {
                    continue;
                }

                // Get the next id
                int nextId = (int) getNextId();

                // Set the id field
                field.setAccessible(true);
                field.set(instance, nextId);

                // Break the loop, since we only want to populate the ID field
                break;
            }

            // Return the instance
            return instance;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create a new instance of " + clazz.getName() + "!", ex);
        }
    }

    @Nullable
    @Override
    public T find(Object id) {
        StorageMetadata metadata = getMetadata();

        return findOne(metadata.idColumnName(), id);
    }

    @Nullable
    @Override
    public T findOne(String fieldName, Object value) {
        // Get the connection
        Connection connection = storageDatabase.getConnection();
        if (connection == null) {
            throw new IllegalStateException("The connection to the database is null!");
        }

        // Get the metadata
        StorageMetadata metadata = getMetadata();
        String tableName = metadata.tableName();
        // Create the SQL query
        String sqlQuery = "SELECT * FROM `" + tableName + "` WHERE `" + fieldName + "` = ?";
        // Create the prepared statement
        try (PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
            StorageSQLUtil.insertValueIntoPrepStatement(statement, 1, value);

            // Execute the query
            try (ResultSet resultSet = statement.executeQuery()) {
                // Check if no matching row was found, if so return null.
                if (!resultSet.next()) {
                    return null;
                }

                // Create a new instance of the StorageObject
                return createFromResultSet(resultSet);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute query " + sqlQuery + "!", e);
        }
    }

    @Override
    public List<T> findAll() {
        // Get the connection
        Connection connection = storageDatabase.getConnection();
        if (connection == null) {
            throw new IllegalStateException("The connection to the database is null!");
        }

        // Get the metadata
        StorageMetadata metadata = getMetadata();
        String tableName = metadata.tableName();
        // Create the SQL query
        String sqlQuery = "SELECT * FROM `" + tableName + "`";
        // Create the prepared statement
        try (PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
            // Execute the query
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> storageObjects = new ArrayList<>();
                while (resultSet.next()) {
                    storageObjects.add(createFromResultSet(resultSet));
                }
                return storageObjects;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute query " + sqlQuery + "!", e);
        }
    }

    @Override
    public List<T> findAll(String fieldName, Object value) {
        // Get the connection
        Connection connection = storageDatabase.getConnection();
        if (connection == null) {
            throw new IllegalStateException("The connection to the database is null!");
        }

        // Get the metadata
        StorageMetadata metadata = getMetadata();
        String tableName = metadata.tableName();
        // Create the SQL query
        String sqlQuery = "SELECT * FROM `" + tableName + "` WHERE `" + fieldName + "` = ?";
        // Create the prepared statement
        try (PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
            StorageSQLUtil.insertValueIntoPrepStatement(statement, 1, value);

            // Execute the query
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> storageObjects = new ArrayList<>();
                while (resultSet.next()) {
                    storageObjects.add(createFromResultSet(resultSet));
                }
                return storageObjects;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute query " + sqlQuery + "!", e);
        }
    }

    @Override
    public void upsert(T object) {
        // Get the connection
        Connection connection = storageDatabase.getConnection();
        if (connection == null) {
            throw new IllegalStateException("The connection to the database is null!");
        }

        StorageMetadata metadata = getMetadata();
        String tableName = metadata.tableName();
        int index = 1;
        List<Object> values = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder("INSERT INTO `" + tableName + "` (");
        StringBuilder updateQueryBuilder = new StringBuilder("ON DUPLICATE KEY UPDATE ");

        // Loop through all the fields in the StorageObject
        for (Field field : clazz.getDeclaredFields()) {
            // Check if the field is a StorageField
            if (!field.isAnnotationPresent(StorageField.class)) {
                continue;
            }

            StorageField storageField = field.getAnnotation(StorageField.class);
            String fieldName = storageField.columnName();

            // If it's not the first field, add a comma
            if (index != 1) {
                queryBuilder.append(", ");
                updateQueryBuilder.append(", ");
            }

            // Add the field name to the query
            queryBuilder.append("`").append(fieldName).append("`");
            updateQueryBuilder.append("`").append(fieldName).append("`").append(" = ?");

            // Add the value to the list
            try {
                field.setAccessible(true);

                // Get the value of the field
                Object value = field.get(object);

                // Add the value to the list
                values.add(value);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to get the value of field " + fieldName + " in class " + clazz.getName() + "!", ex);
            }

            // Increment the index
            index++;
        }

        // Add the values to the query
        queryBuilder.append(") VALUES (");
        for (int i = 0; i < values.size(); i++) {
            if (i != 0) {
                queryBuilder.append(", ");
            }
            queryBuilder.append("?");
        }
        queryBuilder.append(") ").append(updateQueryBuilder);

        // Create the prepared statement
        try (PreparedStatement statement = connection.prepareStatement(queryBuilder.toString())) {
            // Loop through all the values and set them in the prepared statement
            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);

                StorageSQLUtil.insertValueIntoPrepStatement(statement, i + 1, value);
            }
            // Add the same values for the update part of the query
            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);

                StorageSQLUtil.insertValueIntoPrepStatement(statement, i + values.size() + 1, value);
            }

            // Execute the query
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to execute query " + queryBuilder + "!", e);
        }
    }

    @Override
    public void delete(T object) {
        // Get the metadata
        StorageMetadata metadata = getMetadata();
        String tableName = metadata.tableName();
        Connection connection = storageDatabase.getConnection();
        if (connection == null) {
            throw new IllegalStateException("The connection to the database is null!");
        }

        // Create the SQL query
        String sqlQuery = "DELETE FROM `" + tableName + "` WHERE `" + metadata.idColumnName() + "` = ?";
        // Create the prepared statement
        try (PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
            Object identifier = object.getIdentifier();
            if (identifier == null) {
                throw new IllegalStateException("The identifier of the object is null!");
            }

            // Add the identifier to the prepared statement
            StorageSQLUtil.insertValueIntoPrepStatement(statement, 1, identifier);

            // Execute the query
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to execute query " + sqlQuery + "!", e);
        }
    }

    @Override
    public Object getNextId() {
        // Get the connection
        Connection connection = storageDatabase.getConnection();
        if (connection == null) {
            throw new IllegalStateException("The connection to the database is null!");
        }

        // Get the metadata
        StorageMetadata metadata = getMetadata();
        String tableName = metadata.tableName();
        // Create the SQL query
        String sqlQuery = "SELECT `" + metadata.idColumnName() + "` FROM `" + tableName + "` ORDER BY `" + metadata.idColumnName() + "` DESC LIMIT 1";
        // Create the prepared statement
        try (PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
            // Execute the query
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return 1;
                }

                int currentId = resultSet.getInt(metadata.idColumnName());
                return currentId + 1;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to execute query " + sqlQuery + "!", e);
        }
    }


    /**
     * This method creates a new StorageObject from the provided ResultSet.
     *
     * @param resultSet The ResultSet to create the StorageObject from
     * @return The created StorageObject
     * @throws Exception If an error occurs while creating the StorageObject
     */
    @NotNull
    private T createFromResultSet(ResultSet resultSet) throws Exception {
        // Create a new instance of the StorageObject
        T object = clazz.getDeclaredConstructor().newInstance();

        // Loop through all the fields in the StorageObject
        for (Field field : clazz.getDeclaredFields()) {
            // Check if the field is a StorageField
            if (!field.isAnnotationPresent(StorageField.class)) {
                continue;
            }

            StorageField storageField = field.getAnnotation(StorageField.class);
            String fieldName = storageField.columnName();

            // Get the value from the result set
            Object value = resultSet.getObject(fieldName);
            Object convertedValue = StorageSQLUtil.convertSQLValueToJavaValue(value, field.getGenericType());

            // Set the value of the field in the StorageObject
            field.setAccessible(true);
            field.set(object, convertedValue);
        }

        return object;
    }

    /**
     * Ensures that the table for the StorageObject is correct
     * and creates it if it doesn't exist.
     */
    private void ensureCorrectTable() {
        StorageMetadata metadata = getMetadata();
        String tableName = metadata.tableName();
        Connection connection = storageDatabase.getConnection();
        if (connection == null) {
            throw new IllegalStateException("The connection to the database is null!");
        }

        // Check if the table exists
        if (!tableExists(connection, tableName)) {
            try (Statement statement = connection.createStatement()) {
                // Create the table
                String tableTypeDefinition = StorageSQLUtil.generateSQLTypeDefinition(this.clazz);
                statement.executeUpdate("CREATE TABLE `" + tableName + "` (" + tableTypeDefinition + ")");
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to create table " + tableName + "!", e);
            }
            return;
        }

        // Update the table
        updateTable(connection, tableName);
    }

    /**
     * Updates the table to match the StorageObject.
     *
     * @param connection The connection to the database
     * @param tableName  The name of the table
     */
    private void updateTable(Connection connection, String tableName) {
        // Get all the fields in the table
        Map<String, String> tableFields = StorageSQLUtil.getTableColumns(connection, tableName); // <Field name, Field type>
        List<String> objectFields = new ArrayList<>();

        try (Statement statement = connection.createStatement()) {
            // Begin with the SQL transaction, since we are doing a bunch of queries
            statement.addBatch("START TRANSACTION");

            // Generate an instance of the StorageObject, so we can get the default values for the fields
            StorageObject storageObject = clazz.getDeclaredConstructor().newInstance();

            // Loop through all the fields in the StorageObject and make sure they are all correct in the table
            for (Field field : this.clazz.getDeclaredFields()) {
                // Check if the field is a StorageField, if not, skip it
                if (!field.isAnnotationPresent(StorageField.class)) {
                    continue;
                }

                StorageField storageField = field.getAnnotation(StorageField.class);
                String fieldName = storageField.columnName();
                String sqlType = StorageSQLUtil.getSQLType(field.getType());

                // Add the field to the list of fields in the StorageObject
                objectFields.add(fieldName);

                // Check if the field exists
                if (!tableFields.containsKey(fieldName)) {
                    StringBuilder builder = new StringBuilder();
                    // Append the statement to add the column
                    builder.append("ALTER TABLE `").append(tableName).append("` ADD COLUMN `").append(fieldName).append("` ").append(sqlType);

                    if (!field.isAnnotationPresent(StorageId.class) && sqlType != "JSON") {
                        // Make the field accessible
                        field.setAccessible(true);

                        // Get the default value for the field
                        Object defaultValue = field.get(storageObject);
                        String defaultValueSQL = StorageSQLUtil.convertToSQLType(defaultValue);

                        // Append the default value to the statement
                        builder.append(" DEFAULT ").append(defaultValueSQL);
                    }

                    // Add the query to the batch
                    statement.addBatch(builder.toString());
                    continue;
                }

                // Check if the field type is correct, if it's correct, skip it
                if (tableFields.get(fieldName).equalsIgnoreCase(sqlType)) {
                    continue;
                }

                StringBuilder builder = new StringBuilder();

                // Append the statement to modify the column
                builder.append("ALTER TABLE `").append(tableName).append("` MODIFY COLUMN `").append(fieldName).append("` ").append(sqlType);

                if (!field.isAnnotationPresent(StorageId.class) && sqlType != "JSON") {
                    // Make the field accessible
                    field.setAccessible(true);

                    // Get the default value for the field
                    Object defaultValue = field.get(storageObject);
                    String defaultValueSQL = StorageSQLUtil.convertToSQLType(defaultValue);

                    // Append the default value to the statement
                    builder.append(" DEFAULT ").append(defaultValueSQL);
                }

                // Add the query to the batch
                statement.addBatch(builder.toString());
            }

            // Remove all fields that are in the table but not in the StorageObject
            for (String fieldName : tableFields.keySet()) {
                if (objectFields.contains(fieldName)) {
                    continue;
                }

                statement.addBatch("ALTER TABLE `" + tableName + "` DROP COLUMN `" + fieldName + "`");
            }

            // Execute the batch & commit the transaction
            statement.executeBatch();
            statement.addBatch("COMMIT");
            statement.executeBatch();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to update table " + tableName + "!", ex);
        }
    }

    /**
     * Checks if the table exists.
     *
     * @param connection The connection to the database
     * @param tableName  The name of the table
     * @return True if the table exists, false otherwise
     */
    private boolean tableExists(@NotNull Connection connection, @NotNull String tableName) {
        try {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet result = databaseMetaData.getTables(null, null, tableName, null);

            return result.next();
        } catch (Exception ignored) {
            return false;
        }
    }

}
