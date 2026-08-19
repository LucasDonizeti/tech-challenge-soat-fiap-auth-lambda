package com.techchallenge.lambda.authorizer.infrastructure.persistence.config;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Configuration class for creating DataSource instances for database connections.
 * This class uses environment variables to configure the database connection,
 * making it suitable for AWS Lambda deployment.
 */
public class DataSourceConfig {

    /**
     * Creates a DataSource instance configured from environment variables.
     * 
     * Environment variables used:
     * - DB_HOST: Database host (default: localhost)
     * - DB_PORT: Database port (default: 3306)
     * - DB_NAME: Database name (default: techchallenge)
     * - DB_USER: Database username (default: root)
     * - DB_PASSWORD: Database password (default: empty)
     * 
     * @return configured DataSource instance
     */
    public static DataSource createDataSource() {
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_PORT", "3306");
        String dbName = System.getenv().getOrDefault("DB_NAME", "oficina");
        String user = System.getenv().getOrDefault("DB_USER", "user");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "password");
        
        return createDataSource(host, port, dbName, user, password);
    }

    /**
     * Creates a DataSource instance with custom connection parameters.
     * 
     * @param host database host
     * @param port database port
     * @param dbName database name
     * @param user database username
     * @param password database password
     * @return configured DataSource instance
     */
    public static DataSource createDataSource(String host, String port, String dbName, String user, String password) {
        String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s", host, port, dbName);
        
        return new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                Properties props = new Properties();
                props.setProperty("user", user);
                props.setProperty("password", password);
                props.setProperty("connectTimeout", "10000");
                props.setProperty("socketTimeout", "30000");
                return DriverManager.getConnection(jdbcUrl, props);
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                Properties props = new Properties();
                props.setProperty("user", username);
                props.setProperty("password", password);
                props.setProperty("connectTimeout", "10000");
                props.setProperty("socketTimeout", "30000");
                return DriverManager.getConnection(jdbcUrl, props);
            }

            @Override
            public java.io.PrintWriter getLogWriter() throws SQLException {
                return DriverManager.getLogWriter();
            }

            @Override
            public void setLogWriter(java.io.PrintWriter out) throws SQLException {
                DriverManager.setLogWriter(out);
            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {
                DriverManager.setLoginTimeout(seconds);
            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return DriverManager.getLoginTimeout();
            }

            @Override
            public java.util.logging.Logger getParentLogger() {
                return java.util.logging.Logger.getLogger(DataSourceConfig.class.getName());
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                throw new SQLException("Not a wrapper");
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return false;
            }
        };
    }
}