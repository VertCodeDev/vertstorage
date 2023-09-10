package dev.vertcode.vertstorage.service.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import dev.vertcode.vertstorage.StorageObject;
import dev.vertcode.vertstorage.annotations.StorageField;
import dev.vertcode.vertstorage.annotations.StorageId;
import dev.vertcode.vertstorage.annotations.StorageMetadata;
import dev.vertcode.vertstorage.service.StorageService;
import dev.vertcode.vertstorage.util.StorageUtil;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author Elmar Blume - 20/08/2023
 */
public class MongoStorageService<T extends StorageObject> extends StorageService<T> {

	private final MongoDatabase mongoDatabase;

	public MongoStorageService(Class<T> clazz, ConnectionString connectionString) {
		super(clazz);

		// Check if the connection string provides a database name
		if (connectionString.getDatabase() == null)
			throw new IllegalArgumentException("No database was specified in connection string");

		// Create a new mongo client connection
		MongoClient mongoClient = MongoClients.create(connectionString);
		this.mongoDatabase = mongoClient.getDatabase(connectionString.getDatabase());
	}

	@Override
	public T createInstance() {
		try {
			// Create the instance of the StorageObject
			T instance = this.clazz.getDeclaredConstructor().newInstance();

			// Loop through all the fields in the class
			for (Map.Entry<Field, StorageField> entry : this.fieldMappings.entrySet()) {
				Field field = entry.getKey();
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
	public @Nullable T findInDatabase(Object id) {
		StorageMetadata metadata = getMetadata();

		return findOneInDatabase(metadata.idColumnName(), id);
	}

	@Override
	public @Nullable T findOneInDatabase(String fieldName, Object value) {
		// Get the collection
		final MongoCollection<Document> collection = this.getCollection();

		// Find the first document matching the field name with the value
		final Document document = collection.find(Filters.eq(fieldName, value instanceof UUID ? String.valueOf(value) : value)).first();
		if (document == null) return null;

		// Serialize the document to the storage object type
		return StorageUtil.getGson().fromJson(document.toJson(), this.clazz);
	}

	@Override
	public List<T> findAllInDatabase() {
		// Get the collection
		final MongoCollection<Document> collection = this.getCollection();

		// Create a new list for the storageObjects
		final List<T> storageObjects = new ArrayList<>();

		// Loop through all the documents
		try (MongoCursor<Document> iterator = collection.find().iterator()) {
			while (iterator.hasNext()) {
				Document document = iterator.next();

				// Serialize the storageObject to the storageObject type
				T storageObject = StorageUtil.getGson().fromJson(document.toJson(), this.clazz);
				storageObjects.add(storageObject);
			}
		}

		// Return the storageObjects
		return storageObjects;
	}

	@Override
	public List<T> findAllInDatabase(String fieldName, Object value) {
		// Get the collection
		final MongoCollection<Document> collection = this.getCollection();

		// Create a new list for the storageObjects
		final List<T> storageObjects = new ArrayList<>();

		// Loop through all the documents
		try (MongoCursor<Document> iterator = collection.find(Filters.eq(fieldName, value instanceof UUID ? String.valueOf(value) : value)).iterator()) {
			while (iterator.hasNext()) {
				Document document = iterator.next();

				// Serialize the storageObject to the storageObject type
				T storageObject = StorageUtil.getGson().fromJson(document.toJson(), this.clazz);
				storageObjects.add(storageObject);
			}
		}

		// Return the storageObjects
		return storageObjects;
	}

	@Override
	public void upsert(T object) {
		// Get the collection
		final MongoCollection<Document> collection = this.getCollection();

		// Serialize and parse the storageObject to a document
		final Document document = Document.parse(StorageUtil.getGson().toJson(object));

		// Insert the document into the collection
		collection.replaceOne(
				Filters.eq(getMetadata().idColumnName(), object.getIdentifier()),
				document, new ReplaceOptions().upsert(true)
		);
	}

	@Override
	public void delete(T object) {
		// Get the collection
		final MongoCollection<Document> collection = this.getCollection();

		// Delete the document from the collection
		collection.deleteOne(
				Filters.eq(getMetadata().idColumnName(), object.getIdentifier() instanceof UUID ? String.valueOf(object.getIdentifier()) : object.getIdentifier())
		);
	}

	@Override
	public Object getNextId() {
		return this.getCollection().find().into(new ArrayList<>()).size() + 1;
	}

	/**
	 * Get the collection from the database
	 *
	 * @return The collection
	 */
	private @NotNull MongoCollection<Document> getCollection() {
		return this.mongoDatabase.getCollection(this.getMetadata().tableName());
	}
}
