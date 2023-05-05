package dev.vertcode.vertstorage.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import dev.vertcode.vertstorage.annotations.StorageField;
import dev.vertcode.vertstorage.util.StorageUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class StorageObjectTypeAdapter<T> extends TypeAdapter<T> {

    private final Class<T> clazz;
    private final Map<String, Field> fieldMappings = new HashMap<>();

    public StorageObjectTypeAdapter(Class<T> clazz) {
        this.clazz = clazz;

        for (Field field : clazz.getDeclaredFields()) {
            // Check if the field isn't annotated with StorageField
            if (!field.isAnnotationPresent(StorageField.class)) {
                continue;
            }

            // Get the StorageField annotation & the column name
            StorageField storageField = field.getAnnotation(StorageField.class);
            String columnName = storageField.columnName();

            // Put the field into the field mappings
            fieldMappings.put(columnName, field);
        }
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        // Begin the object
        out.beginObject();

        // Loop through the field mappings
        for (Map.Entry<String, Field> entry : fieldMappings.entrySet()) {
            // Get the column name & the field
            String columnName = entry.getKey();
            Field field = entry.getValue();

            // Set the field accessible
            field.setAccessible(true);

            try {

                // Get the field value
                Object fieldValue = field.get(value);

                // Write the column name
                out.name(columnName);

                // Parse the fieldValue to a String
                String json = StorageUtil.getGson().toJson(fieldValue);

                // Write the field value
                out.jsonValue(json);
            } catch (Exception ignored) {
            }
        }

        // End the object
        out.endObject();
    }

    @Override
    public T read(JsonReader in) throws IOException {
        try {
            T instance = clazz.getConstructor().newInstance();

            // Read the object
            in.beginObject();
            // Iterate through the object
            while (in.hasNext()) {
                String columnName = in.nextName();
                Field field = fieldMappings.get(columnName);
                // Check if the field is null
                if (field == null) {
                    in.skipValue();
                    continue;
                }

                // Set the field accessible
                field.setAccessible(true);

                try {
                    // Get the generic type of the field
                    Type genericType = field.getGenericType();
                    // Get the field value
                    Object fieldValue = StorageUtil.getGson().fromJson(in, genericType);

                    // Set the field value
                    field.set(instance, fieldValue);
                } catch (Exception ignored) {
                }
            }

            // End the object
            in.endObject();

            // Return the instance
            return instance;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

}
