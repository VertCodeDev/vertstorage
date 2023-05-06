package dev.vertcode.vertstorage.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import dev.vertcode.vertstorage.IStorageDatabase;

public class MongoStorageDatabase implements IStorageDatabase {

    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;

    public MongoStorageDatabase(String uri) {
        // Get the connection string
        ConnectionString connectionString = new ConnectionString(uri);
        MongoClientURI mongoClientURI = new MongoClientURI(connectionString.getConnectionString());

        // Set the mongo client
        this.mongoClient = new MongoClient(mongoClientURI);

        // Get the database name
        String databaseName = connectionString.getDatabase();
        // Check if the database name is null
        if (databaseName == null) {
            throw new NullPointerException("Your connection string does not specify a database");
        }

        // Set the mongo database
        this.mongoDatabase = this.mongoClient.getDatabase(databaseName);
    }

    @Override
    public void shutdown() {
        // Shutdown the mongo client
        this.mongoClient.close();
    }

    /**
     * Get the mongo client
     *
     * @return the mongo client
     */
    public MongoClient getMongoClient() {
        return mongoClient;
    }

    /**
     * Get the mongo database
     *
     * @return the mongo database
     */
    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

}
