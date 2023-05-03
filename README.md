<p align="center">
    <img src="https://cdn.vertcodedevelopment.com/logo-text.png" alt="VCD Banner"/>
    <h1 align="center">VertStorage | An easy to use storage system.</h1>
</p>

## Features

- Easy to use
- MongoDB, MySQL & JSON Support
- Possibility to use different databases per StorageObject

## Planned Features

- Linking objects together by their id's (e.g. linking a user to a guild and just having the Guild object having the
  user object inside it when loaded)
- More database support (e.g. PostgreSQL, SQLite, etc.)

## Example

```java

public class Example {

  public static void main(String[] args) {
    System.out.println("------------ [ Example ] ------------");

    // Create the database instance, this manages the connection to the database
    MySQLStorageDatabase database = new MySQLStorageDatabase("localhost", 3306, "root", "password", "test");
    // Create the service instance for the TestObject
    MySQLStorageService<TestObject> service = new MySQLStorageService<>(database, TestObject.class);

    // Startup the service so it can do some stuff that needs to be done before it can be used
    service.startupService();

    // Create a new TestObject instance using the service
    TestObject testObject = service.createInstance();

    // Let's log the object before we save it
    System.out.println("Before save: " + testObject);
    // Save the object to the database
    service.upsert(testObject);

    // Now Let's set the testObject to null so we can load it from the database (first we get the id)
    int id = testObject.getId();

    // Set the testObject to null
    testObject = null;

    // Let's log the object before we load it
    System.out.println("Before load: " + testObject);

    // Load the object from the database
    testObject = service.find(id);

    // Let's log the object after we loaded it
    System.out.println("After load: " + testObject);

    // Now change the name of the object
    testObject.setName("VertCode");
    // Now let's save the object again
    service.upsert(testObject);

    // Let's log the object after we saved it
    System.out.println("After save: " + testObject);

    // Now we set the object to null again and load it again to show that the name has changed in the database
    testObject = null;

    // Load the object from the database
    testObject = service.find(id);
    // Let's log the object after we loaded it
    System.out.println("After load: " + testObject);

    // Now we know this works, let's delete the object from the database
    service.delete(testObject);

    // To show that the object is deleted, we set it to null again and load it again
    testObject = null;

    // Load the object from the database
    testObject = service.find(id);

    // Let's log the object after we loaded it
    System.out.println("After load: " + testObject);

    System.out.println("------------ [ Example ] ------------");
  }

  @NoArgsConstructor(force = true)
  @Getter
  @Setter
  @StorageMetadata(
          tableName = "example"
  )
  public static class TestObject extends StorageObject<Integer> {

    @StorageId(
            automaticallyGenerated = true
    )
    @StorageField(
            columnName = "id"
    )
    @Setter(AccessLevel.NONE)
    private int id;

    @StorageField(
            columnName = "name"
    )
    private String name;

    public TestObject(String name) {
      this.name = name;
    }

    @Override
    public Integer getIdentifier() {
      return this.id;
    }

    @Override
    public String toString() {
      return "TestObject{" +
              "id=" + id +
              ", name='" + name + '\'' +
              '}';
    }
  }

}
```