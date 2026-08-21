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

    public static DataSource createDataSource() {
        String dbUrl = System.getenv("DB_URL");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");

        // Se DB_URL for fornecida (ex: jdbc:mysql://endpoint:3306/oficina)
        if (dbUrl != null && !dbUrl.isBlank()) {
            return createDataSourceFromUrl(dbUrl, user, password);
        }

        // Fallback usando variáveis individuais
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_PORT", "3306");
        String dbName = System.getenv().getOrDefault("DB_NAME", "oficina");

        return createDataSource(host, port, dbName, user, password);
    }

    private static DataSource createDataSourceFromUrl(String jdbcUrl, String user, String password) {
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
            public Connection getConnection(java.lang.String username, java.lang.String password) throws SQLException {
                return getConnection();
            }

            @Override public java.io.PrintWriter getLogWriter() { return null; }
            @Override public void setLogWriter(java.io.PrintWriter out) {}
            @Override public void setLoginTimeout(int seconds) {}
            @Override public int getLoginTimeout() { return 0; }
            @Override public java.util.logging.Logger getParentLogger() { return null; }
            @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Not a wrapper"); }
            @Override public boolean isWrapperFor(Class<?> iface) { return false; }
        };
    }

    public static DataSource createDataSource(String host, String port, String dbName, String user, String password) {
        String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s", host, port, dbName);
        return createDataSourceFromUrl(jdbcUrl, user, password);
    }
}