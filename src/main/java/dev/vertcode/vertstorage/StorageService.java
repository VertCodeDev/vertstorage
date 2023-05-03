package dev.vertcode.vertstorage;

import dev.vertcode.vertstorage.annotations.StorageMetadata;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A storage service handles the database & caching for a specific StorageObject.
 *
 * @param <T> The StorageObject this service is for
 */
public abstract class StorageService<T extends StorageObject> {

    protected final Class<T> clazz;

    public StorageService(Class<T> clazz) {
        this.clazz = clazz;

        // We make sure the clazz has a @StorageMetadata annotation
        if (!clazz.isAnnotationPresent(StorageMetadata.class)) {
            throw new IllegalArgumentException(String.format("The class %s does not have a @StorageMetadata annotation!", clazz.getName()));
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
     * Queries the database for the object with the given id.
     *
     * @param id The id of the object to query
     * @return The object with the given id
     */
    public abstract @Nullable T find(Object id);

    /**
     * Asynchronously queries the database for the object with the given id.
     *
     * @param id The id of the object to query
     * @return The object with the given id
     */
    public CompletableFuture<T> findAsync(Object id) {
        return CompletableFuture.supplyAsync(() -> find(id));
    }

    /**
     * Queries the database for the object with the given field name and value.
     *
     * @param fieldName The name of the field to query
     * @param value     The value of the field to query
     * @return The object with the given field name and value
     */
    public abstract @Nullable T findOne(String fieldName, Object value);

    /**
     * Asynchronously queries the database for the object with the given field name and value.
     *
     * @param fieldName The name of the field to query
     * @param value     The value of the field to query
     * @return The object with the given field name and value
     */
    public CompletableFuture<T> findOneAsync(String fieldName, Object value) {
        return CompletableFuture.supplyAsync(() -> findOne(fieldName, value));
    }

    /**
     * Queries the database for all objects of the given class.
     *
     * @return All objects of the given class
     */
    public abstract List<T> findAll();

    /**
     * Asynchronously queries the database for all objects of the given class.
     *
     * @return All objects of the given class
     */
    public CompletableFuture<List<T>> findAllAsync() {
        return CompletableFuture.supplyAsync(() -> findAll());
    }

    /**
     * Queries the database for all objects of the given class with the given field name and value.
     *
     * @param fieldName The name of the field to query
     * @param value     The value of the field to query
     * @return All objects of the given class with the given field name and value
     */
    public abstract List<T> findAll(String fieldName, Object value);

    /**
     * Asynchronously queries the database for all objects of the given class with the given field name and value.
     *
     * @param fieldName The name of the field to query
     * @param value     The value of the field to query
     * @return All objects of the given class with the given field name and value
     */
    public CompletableFuture<List<T>> findAllAsync(String fieldName, Object value) {
        return CompletableFuture.supplyAsync(() -> findAll(fieldName, value));
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

}
