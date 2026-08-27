package com.techchallenge.lambda.authorizer.application.configure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techchallenge.lambda.authorizer.application.usecases.AutenticarUsuarioUseCase;
import com.techchallenge.lambda.authorizer.application.usecases.ports.input.AutenticarUsuarioInput;
import com.techchallenge.lambda.authorizer.application.usecases.ports.output.ClienteGateway;
import com.techchallenge.lambda.authorizer.domain.repositories.ClienteRepository;
import com.techchallenge.lambda.authorizer.infrastructure.gateway.ClientePersistenceGateway;
import com.techchallenge.lambda.authorizer.infrastructure.persistence.config.DataSourceConfig;
import com.techchallenge.lambda.authorizer.infrastructure.persistence.mappers.ClienteJpaMapper;
import com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories.ClienteJdbcRepository;
import com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories.ClienteJpaRepository;
import com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories.ClienteRepositoryImpl;
import com.techchallenge.lambda.authorizer.infrastructure.security.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Factory class responsible for dependency injection following Clean Architecture principles.
 * This class manages the creation and wiring of all dependencies required by the AuthLambdaHandler.
 */
public class InjectionFactory {
    private static final Logger logger = LoggerFactory.getLogger(InjectionFactory.class);

    private static ObjectMapper objectMapper;
    private static JwtTokenUtil jwtTokenUtil;
    private static DataSource dataSource;
    private static ClienteJpaRepository clienteJpaRepository;
    private static ClienteJpaMapper clienteJpaMapper;
    private static ClienteRepository clienteRepository;
    private static ClienteGateway clienteGateway;
    private static AutenticarUsuarioInput autenticarUsuarioInput;

    private InjectionFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates or returns the singleton instance of ObjectMapper.
     * ObjectMapper is used for JSON serialization/deserialization in the Lambda handler.
     */
    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            logger.debug("Criando nova instância de ObjectMapper");
            objectMapper = new ObjectMapper();
            logger.info("ObjectMapper inicializado com sucesso");
        }
        return objectMapper;
    }

    /**
     * Creates or returns the singleton instance of JwtTokenUtil.
     * JwtTokenUtil is responsible for JWT token generation and validation.
     */
    public static JwtTokenUtil getJwtTokenUtil() {
        if (jwtTokenUtil == null) {
            logger.debug("Criando nova instância de JwtTokenUtil");
            jwtTokenUtil = new JwtTokenUtil();
            logger.info("JwtTokenUtil inicializado com sucesso");
        }
        return jwtTokenUtil;
    }


    /**
     * Returns the current DataSource instance.
     * 
     * @return the DataSource instance, or null if not set
     */
    public static DataSource getDataSource() {
        logger.debug("Obtendo DataSource via DataSourceConfig");
        return DataSourceConfig.createDataSource();
    }

    /**
     * Creates or returns the singleton instance of ClienteJpaRepository.
     * This is the infrastructure component that interacts with the database.
     * Uses ClienteJdbcRepository as the implementation.
     * Note: The DataSource must be set before calling this method via setDataSource().
     */
    public static ClienteJpaRepository getClienteJpaRepository() {
        if (clienteJpaRepository == null) {
            logger.debug("Criando nova instância de ClienteJpaRepository");
            if (dataSource == null) {
                logger.debug("DataSource não inicializado, obtendo via getDataSource()");
                dataSource = getDataSource();
                //throw new IllegalStateException("DataSource must be set before creating ClienteJpaRepository. Call setDataSource() first.");
            }
            clienteJpaRepository = new ClienteJdbcRepository(dataSource);
            logger.info("ClienteJpaRepository inicializado com sucesso");
        }
        return clienteJpaRepository;
    }

    /**
     * Creates or returns the singleton instance of ClienteJpaMapper.
     * Mapper is responsible for converting between JPA entities and domain models.
     */
    public static ClienteJpaMapper getClienteJpaMapper() {
        if (clienteJpaMapper == null) {
            logger.debug("Criando nova instância de ClienteJpaMapper");
            clienteJpaMapper = new ClienteJpaMapper();
            logger.info("ClienteJpaMapper inicializado com sucesso");
        }
        return clienteJpaMapper;
    }

    /**
     * Creates or returns the singleton instance of ClienteRepository.
     * This is the domain repository interface implementation.
     */
    public static ClienteRepository getClienteRepository() {
        if (clienteRepository == null) {
            logger.debug("Criando nova instância de ClienteRepository");
            clienteRepository = new ClienteRepositoryImpl(
                    getClienteJpaRepository(),
                    getClienteJpaMapper()
            );
            logger.info("ClienteRepository inicializado com sucesso");
        }
        return clienteRepository;
    }

    /**
     * Creates or returns the singleton instance of ClienteGateway.
     * Gateway acts as an interface between the use case layer and the domain layer.
     */
    public static ClienteGateway getClienteGateway() {
        if (clienteGateway == null) {
            logger.debug("Criando nova instância de ClienteGateway");
            clienteGateway = new ClientePersistenceGateway(
                    getClienteRepository()
            );
            logger.info("ClienteGateway inicializado com sucesso");
        }
        return clienteGateway;
    }

    /**
     * Creates or returns the singleton instance of AutenticarUsuarioInput.
     * This is the use case interface responsible for user authentication logic.
     */
    public static AutenticarUsuarioInput getAutenticarUsuarioInput() {
        if (autenticarUsuarioInput == null) {
            logger.debug("Criando nova instância de AutenticarUsuarioInput");
            autenticarUsuarioInput = new AutenticarUsuarioUseCase(
                    getJwtTokenUtil(),
                    getClienteGateway()
            );
            logger.info("AutenticarUsuarioInput inicializado com sucesso");
        }
        return autenticarUsuarioInput;
    }

    /**
     * Resets all singleton instances.
     * This method is primarily useful for testing purposes to ensure clean state between tests.
     */
    public static void reset() {
        logger.info("Resetando todas as instâncias singleton do InjectionFactory");
        objectMapper = null;
        jwtTokenUtil = null;
        clienteJpaRepository = null;
        clienteJpaMapper = null;
        clienteRepository = null;
        clienteGateway = null;
        autenticarUsuarioInput = null;
        logger.info("Instâncias singleton resetadas com sucesso");
    }
}