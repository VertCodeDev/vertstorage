package dev.vertcode.vertstorage.service;

import dev.vertcode.vertstorage.StorageObject;
import dev.vertcode.vertstorage.annotations.StorageField;
import dev.vertcode.vertstorage.annotations.StorageMetadata;
import dev.vertcode.vertstorage.object.ObjectCache;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A storage service handles the database & caching for a specific StorageObject.
 *
 * @param <T> The StorageObject this service is for
 */
public abstract class StorageService<T extends StorageObject> {

    protected final Class<T> clazz;
    protected final ObjectCache<Object, T> cache;
    protected final Map<Field, StorageField> fieldMappings = new HashMap<>();

    public StorageService(Class<T> clazz) {
        this.clazz = clazz;
        this.cache = new ObjectCache<>();

        // We make sure the clazz has a @StorageMetadata annotation
        if (!clazz.isAnnotationPresent(StorageMetadata.class)) {
            throw new IllegalArgumentException(String.format("The class %s does not have a @StorageMetadata annotation!", clazz.getName()));
        }

        // We load the field mappings
        loadFieldMappings();
    }

    public StorageService(Class<T> clazz, long cacheTime, TimeUnit cacheTimeUnit) {
        this.clazz = clazz;
        this.cache = new ObjectCache<>(cacheTime, cacheTimeUnit);

        // We make sure the clazz has a @StorageMetadata annotation
        if (!clazz.isAnnotationPresent(StorageMetadata.class)) {
            throw new IllegalArgumentException(String.format("The class %s does not have a @StorageMetadata annotation!", clazz.getName()));
        }

        // We load the field mappings
        loadFieldMappings();
    }

    /**
     * Loads the field mappings for the StorageObject.
     */
    private void loadFieldMappings() {
        // Loop through all the fields in the class
        for (Field declaredField : this.clazz.getDeclaredFields()) {
            // Check if the field has a @StorageField annotation
            if (!declaredField.isAnnotationPresent(StorageField.class)) {
                continue;
            }

            // Get the StorageField annotation
            StorageField metadata = declaredField.getAnnotation(StorageField.class);

            // Add the field to the field mappings
            this.fieldMappings.put(declaredField, metadata);
        }
    }

    /**
     * Starts up the service.
     */
    public void startupService() {
        // Do nothing by default
    }

    /**
     * Shuts down the service.
     */
    public void shutdownService() {
        // Do nothing by default
    }

    /**
     * Creates a new instance of the StorageObject.
     *
     * @return A new instance of the StorageObject
     */
    public abstract T createInstance();

    /**
     * Gets an object with the provided id in the cache or queries the database for it.
     *
     * @param id The id of the object to find
     * @return The object with the given id
     */
    public @Nullable T find(Object id) {
        // First we get the object from the cache
        T object = this.cache.get(id);
        if (object != null) {
            return object;
        }

        // If the object is not in the cache, we query the database
        object = findInDatabase(id);
        if (object != null) {
            this.cache.put(id, object);
        }

        return object;
    }

    /**
     * Queries the database for the object with the given id.
     *
     * @param id The id of the object to query
     * @return The object with the given id
     */
    public abstract @Nullable T findInDatabase(Object id);

    /**
     * Asynchronously queries the database for the object with the given id.
     *
     * @param id The id of the object to query
     * @return The object with the given id
     */
    public CompletableFuture<T> findInDatabaseAsync(Object id) {
        return CompletableFuture.supplyAsync(() -> findInDatabase(id));
    }

    /**
     * Queries the database for the object with the given field name and value.
     *
     * @param fieldName The name of the field to query
     * @param value     The value of the field to query
     * @return The object with the given field name and value
     */
    public abstract @Nullable T findOneInDatabase(String fieldName, Object value);

    /**
     * Asynchronously queries the database for the object with the given field name and value.
     *
     * @param fieldName The name of the field to query
     * @param value     The value of the field to query
     * @return The object with the given field name and value
     */
    public CompletableFuture<T> findOneInDatabaseAsync(String fieldName, Object value) {
        return CompletableFuture.supplyAsync(() -> findOneInDatabase(fieldName, value));
    }

    /**
     * Finds all the cached objects.
     *
     * @return All the cached objects
     */
    public List<T> findAllCached() {
        return this.cache.getValues();
    }

    /**
     * Queries the database for all objects of the given class.
     *
     * @return All objects of the given class
     */
    public abstract List<T> findAllInDatabase();

    /**
     * Asynchronously queries the database for all objects of the given class.
     *
     * @return All objects of the given class
     */
    public CompletableFuture<List<T>> findAllInDatabaseAsync() {
        return CompletableFuture.supplyAsync(this::findAllInDatabase);
    }

    /**
     * Queries the database for all objects of the given class with the given field name and value.
     *
     * @param fieldName The name of the field to query
     * @param value     The value of the field to query
     * @return All objects of the given class with the given field name and value
     */
    public abstract List<T> findAllInDatabase(String fieldName, Object value);

    /**
     * Asynchronously queries the database for all objects of the given class with the given field name and value.
     *
     * @param fieldName The name of the field to query
     * @param value     The value of the field to query
     * @return All objects of the given class with the given field name and value
     */
    public CompletableFuture<List<T>> findAllInDatabaseAsync(String fieldName, Object value) {
        return CompletableFuture.supplyAsync(() -> findAllInDatabase(fieldName, value));
    }

    /**
     * Upsert the object into the database.
     *
     * @param object The object to upsert
     */
    public abstract void upsert(T object);

    /**
     * Asynchronously upsert the object into the database.
     *
     * @param object The object to upsert
     */
    public CompletableFuture<Void> upsertAsync(T object) {
        return CompletableFuture.runAsync(() -> upsert(object));
    }

    /**
     * Loads the object from the database.
     *
     * @param object The object to load
     */
    public abstract void delete(T object);

    /**
     * Asynchronously loads the object from the database.
     *
     * @param object The object to load
     */
    public CompletableFuture<Void> deleteAsync(T object) {
        return CompletableFuture.runAsync(() -> delete(object));
    }

    /**
     * Caches the object.
     *
     * @param object The object to cache
     */
    public void cacheObject(T object) {
        this.cache.put(object.getIdentifier(), object);
    }

    /**
     * Uncaches the object.
     *
     * @param object The object to uncache
     */
    public void uncacheObject(T object) {
        this.cache.remove(object.getIdentifier());
    }

    /**
     * Uncaches the object with the given id.
     *
     * @param id The id of the object to uncache
     */
    public void uncacheObject(Object id) {
        this.cache.remove(id);
    }

    /**
     * Gets the next id for the StorageObject this service is for.
     *
     * @return The next id for the StorageObject this service is for
     */
    public abstract Object getNextId();

    /**
     * Gets the metadata of the StorageObject this service is for.
     *
     * @return The metadata of the StorageObject this service is for
     */
    public StorageMetadata getMetadata() {
        return this.clazz.getAnnotation(StorageMetadata.class);
    }

    /**
     * Gets the cache of this service.
     *
     * @return The cache of this service
     */
    public ObjectCache<Object, T> getCache() {
        return this.cache;
    }

}
