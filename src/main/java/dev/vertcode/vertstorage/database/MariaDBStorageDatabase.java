package dev.vertcode.vertstorage.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * This StorageDatabase is used to connect to a MariaDB database.
 */
public class MariaDBStorageDatabase extends SQLStorageDatabase {

    private final DataSource dataSource;

    public MariaDBStorageDatabase(HikariConfig hikariConfig) {
        this.dataSource = hikariConfig.getDataSource();
    }

    public MariaDBStorageDatabase(String host, int port, String username, String password, String database) {
        HikariConfig hikariConfig = new HikariConfig();

        // Some default settings, people should use the constructor with the HikariConfig parameter if they want to change these.
        hikariConfig.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database);
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

    @Override
    public @Nullable Connection getConnection() {
        try {
            return this.dataSource.getConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public void shutdown() {
        // Check if the dataSource is a HikariDataSource
        if (!(this.dataSource instanceof HikariDataSource hikariDataSource)) {
            return;
        }

        // Shutdown the HikariDataSource
        hikariDataSource.close();
    }
}
