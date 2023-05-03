package dev.vertcode.vertstorage;

public abstract class StorageObject<T> {

    /**
     * Get the ID of the object.
     *
     * @return The ID of the object
     */
    public abstract T getIdentifier();

}
