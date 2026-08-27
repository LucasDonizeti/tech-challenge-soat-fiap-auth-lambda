package com.techchallenge.lambda.authorizer.infrastructure.persistence.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    public static DataSource createDataSource() {
        logger.info("Iniciando configuração do DataSource");
        
        String dbUrl = System.getenv("DB_URL");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");

        // Se DB_URL for fornecida (ex: jdbc:mysql://endpoint:3306/oficina)
        if (dbUrl != null && !dbUrl.isBlank()) {
            logger.info("Usando DB_URL fornecida para configuração do DataSource");
            logger.debug("DB_URL configurada - user: {}", maskSensitiveData(user));
            return createDataSourceFromUrl(dbUrl, user, password);
        }

        // Fallback usando variáveis individuais
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_PORT", "3306");
        String dbName = System.getenv().getOrDefault("DB_NAME", "oficina");

        logger.info("Usando variáveis individuais para configuração do DataSource - host: {}, port: {}, dbName: {}", 
                host, port, dbName);
        logger.debug("Configuração individual - user: {}", maskSensitiveData(user));
        
        return createDataSource(host, port, dbName, user, password);
    }

    private static DataSource createDataSourceFromUrl(String jdbcUrl, String user, String password) {
        logger.debug("Criando DataSource a partir de URL JDBC");
        
        return new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                long startTime = System.currentTimeMillis();
                logger.debug("Obtendo conexão do banco de dados - url: {}", maskJdbcUrl(jdbcUrl));
                
                try {
                    Properties props = new Properties();
                    props.setProperty("user", user);
                    props.setProperty("password", password);
                    props.setProperty("connectTimeout", "10000");
                    props.setProperty("socketTimeout", "30000");
                    
                    Connection connection = DriverManager.getConnection(jdbcUrl, props);
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("Conexão com banco de dados estabelecida - duration: {}ms", duration);
                    return connection;
                } catch (SQLException e) {
                    long duration = System.currentTimeMillis() - startTime;
                    logger.error("Erro ao obter conexão com banco de dados - duration: {}ms, erro: {}, sqlState: {}", 
                            duration, e.getMessage(), e.getSQLState());
                    throw e;
                }
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
        logger.debug("Construindo URL JDBC - host: {}, port: {}, dbName: {}", host, port, dbName);
        return createDataSourceFromUrl(jdbcUrl, user, password);
    }
    
    private static String maskSensitiveData(String data) {
        if (data == null || data.length() <= 2) {
            return "***";
        }
        return data.substring(0, 1) + "***" + data.substring(data.length() - 1);
    }
    
    private static String maskJdbcUrl(String url) {
        if (url == null) {
            return "***";
        }
        // Remove credenciais da URL se presentes
        return url.replaceAll("://[^@]+@", "://***@");
    }
}