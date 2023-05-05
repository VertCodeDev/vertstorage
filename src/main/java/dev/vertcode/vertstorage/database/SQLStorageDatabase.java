package dev.vertcode.vertstorage.database;

import dev.vertcode.vertstorage.IStorageDatabase;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;

public abstract class SQLStorageDatabase implements IStorageDatabase {

    /**
     * This method is used to get a connection to the database.
     *
     * @return A connection to the database
     */
    @Nullable
    public abstract Connection getConnection();

}
