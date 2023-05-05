package dev.vertcode.vertstorage.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.vertcode.vertstorage.IStorageDatabase;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * This StorageDatabase is used to connect to a MySQL database.
 */
public class MySQLStorageDatabase implements IStorageDatabase {

    private final DataSource dataSource;

    public MySQLStorageDatabase(HikariConfig hikariConfig) {
        this.dataSource = hikariConfig.getDataSource();
    }

    public MySQLStorageDatabase(String host, int port, String username, String password, String database) {
        HikariConfig hikariConfig = new HikariConfig();

        // Some default settings, people should use the constructor with the HikariConfig parameter if they want to change these.
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setPoolName("VertStoragePool - " + database);

        // Optimized settings for VertStorage
        hikariConfig.setMaximumPoolSize(8);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setIdleTimeout(300000);
        hikariConfig.setAutoCommit(false);

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * This method is used to get a connection to the database.
     *
     * @return A connection to the database
     */
    public @Nullable Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
