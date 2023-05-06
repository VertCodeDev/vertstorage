package dev.vertcode.vertstorage.service;

import com.mongodb.client.MongoDatabase;
import dev.vertcode.vertstorage.StorageObject;
import dev.vertcode.vertstorage.StorageService;
import dev.vertcode.vertstorage.database.MongoStorageDatabase;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MongoStorageService<T extends StorageObject> extends StorageService<T> {

    private final MongoStorageDatabase mongoStorageDatabase;

    public MongoStorageService(Class<T> clazz, MongoStorageDatabase mongoStorageDatabase) {
        super(clazz);

        this.mongoStorageDatabase = mongoStorageDatabase;
    }

    @Override
    public void startupService() {
        // Ensure the collection exists
        MongoDatabase mongoDatabase = this.mongoStorageDatabase.getMongoDatabase();
        String tableName = getMetadata().tableName();
        // Check if the collection exists
        if (mongoDatabase.listCollectionNames().into(new ArrayList<>()).contains(tableName)) {
            return;
        }

        // Create the collection
        mongoDatabase.createCollection(tableName);

        // Ensure the collection has the correct indexes

    }

    @Override
    public T createInstance() {
        return null;
    }

    @Override
    public @Nullable T find(Object id) {
        return null;
    }

    @Override
    public @Nullable T findOne(String fieldName, Object value) {
        return null;
    }

    @Override
    public List<T> findAll() {
        return null;
    }

    @Override
    public List<T> findAll(String fieldName, Object value) {
        return null;
    }

    @Override
    public void upsert(T object) {

    }

    @Override
    public void delete(T object) {

    }

    @Override
    public Object getNextId() {
        return null;
    }


}