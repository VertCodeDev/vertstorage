package dev.vertcode.vertstorage.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.vertcode.vertstorage.StorageObject;
import dev.vertcode.vertstorage.StorageService;
import dev.vertcode.vertstorage.adapters.StorageObjectTypeAdapter;
import dev.vertcode.vertstorage.annotations.StorageId;
import dev.vertcode.vertstorage.annotations.StorageMetadata;
import dev.vertcode.vertstorage.util.StorageUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class JsonStorageService<T extends StorageObject> extends StorageService<T> {

    private final File dataFolder;
    private final File tableFolder;
    private final File nextIdFile;
    private final Gson gson;
    private int nextId = 1;

    public JsonStorageService(Class<T> clazz, File dataFolder) {
        super(clazz);


        StorageMetadata metadata = getMetadata();
        String folderName = metadata.tableName();

        this.dataFolder = dataFolder;
        this.tableFolder = new File(this.dataFolder, folderName);
        this.nextIdFile = new File(this.tableFolder, "nextId.json");

        GsonBuilder gsonBuilder = StorageUtil.getGsonBuilder();

        // Register the StorageObject type adapter
        gsonBuilder.registerTypeAdapter(clazz, new StorageObjectTypeAdapter<>(clazz));

        this.gson = gsonBuilder.create();
    }

    @Override
    public void startupService() {
        // Ensure the table folder exists
        if (this.tableFolder.exists()) {
            // Load the next id from nextId.json
            if (nextIdFile.exists()) {
                try (FileReader reader = new FileReader(nextIdFile)) {
                    this.nextId = this.gson.fromJson(reader, Integer.class);
                } catch (Exception ignored) {
                }
            }

            return;
        }

        // Create the table folder
        this.tableFolder.mkdirs();
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

    @Override
    public @Nullable T find(Object id) {
        File file = new File(this.tableFolder, id.toString() + ".json");
        if (!file.exists()) {
            return null;
        }

        return readObject(file);
    }

    @Override
    public @Nullable T findOne(String fieldName, Object value) {
        if (!this.tableFolder.exists()) {
            return null;
        }

        File[] files = this.tableFolder.listFiles();
        // If no files are found, return an empty list
        if (files == null) {
            return null;
        }

        // Loop through all files and read them
        for (File file : files) {
            T object = readObject(file);
            // Check if the object is null, if so, skip it
            if (object == null) {
                continue;
            }

            try {
                Field field = this.clazz.getField(fieldName);

                // Make the field accessible
                field.setAccessible(true);

                // Get the value of the field
                Object fieldValue = field.get(object);

                // Check if the value is equal to the given value, if not, skip it
                if (fieldValue != value) {
                    continue;
                }

                // Return the object
                return object;
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    @Override
    public List<T> findAll() {
        List<T> objects = new ArrayList<>();
        // If the table folder doesn't exist, return an empty list
        if (!this.tableFolder.exists()) {
            return objects;
        }

        File[] files = this.tableFolder.listFiles();
        // If no files are found, return an empty list
        if (files == null) {
            return objects;
        }

        // Loop through all files and read them
        for (File file : files) {
            try {
                T value = readObject(file);
                // Check if the value is null, if so, skip it
                if (value == null) {
                    continue;
                }

                // Add the value to the list
                objects.add(value);
            } catch (Exception ignored) {
            }
        }

        // Return the list
        return objects;
    }

    @Override
    public List<T> findAll(String fieldName, Object value) {
        List<T> objects = new ArrayList<>();
        // If the table folder doesn't exist, return an empty list
        if (!this.tableFolder.exists()) {
            return objects;
        }

        File[] files = this.tableFolder.listFiles();
        // If no files are found, return an empty list
        if (files == null) {
            return objects;
        }

        // Loop through all files and read them
        for (File file : files) {
            T object = readObject(file);
            // Check if the object is null, if so, skip it
            if (object == null) {
                continue;
            }

            try {
                Field field = this.clazz.getField(fieldName);

                // Make the field accessible
                field.setAccessible(true);

                // Get the value of the field
                Object fieldValue = field.get(object);

                // Check if the value is equal to the given value, if not, skip it
                if (fieldValue != value) {
                    continue;
                }

                // Add the object to the list
                objects.add(object);
            } catch (Exception ignored) {
            }
        }

        // Return the list
        return objects;
    }

    @Override
    public void upsert(T object) {
        File file = new File(this.tableFolder, object.getIdentifier() + ".json");

        try (Writer writer = new FileWriter(file)) {
            this.gson.toJson(object, writer);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void delete(T object) {
        File file = new File(this.tableFolder, object.getIdentifier() + ".json");
        // If the file doesn't exist, return
        if (!file.exists()) {
            return;
        }

        // Delete the file
        file.delete();
    }

    @Override
    public Object getNextId() {
        int nextId = this.nextId;

        // Increment the next id & save it
        try (Writer writer = new FileWriter(this.nextIdFile)) {
            this.nextId++;

            this.gson.toJson(this.nextId, writer);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return nextId;
    }

    /**
     * Reads an object from a file.
     *
     * @param file The file to read from
     * @return The object read from the file
     */
    private T readObject(File file) {
        try (FileReader reader = new FileReader(file)) {
            return this.gson.fromJson(reader, this.clazz);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

}
