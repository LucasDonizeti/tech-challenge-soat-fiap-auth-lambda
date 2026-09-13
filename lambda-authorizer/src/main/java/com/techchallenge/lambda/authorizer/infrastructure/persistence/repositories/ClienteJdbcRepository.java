package com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories;

import com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories.entities.ClienteEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class ClienteJdbcRepository implements ClienteJpaRepository {
    private static final Logger logger = LoggerFactory.getLogger(ClienteJdbcRepository.class);

    private final DataSource dataSource;

    public ClienteJdbcRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        logger.info("ClienteJdbcRepository inicializado com DataSource");
    }

    @Override
    public Optional<ClienteEntity> findByCpf(String cpf) {
        logger.debug("Executando query SQL para buscar cliente por CPF - cpf: {}", maskSensitiveData(cpf));
        String sql = "SELECT * FROM clientes WHERE cpf = ?";
        return executeQuery(sql, cpf);
    }

    @Override
    public Optional<ClienteEntity> findByCnpj(String cnpj) {
        logger.debug("Executando query SQL para buscar cliente por CNPJ - cnpj: {}", maskSensitiveData(cnpj));
        String sql = "SELECT * FROM clientes WHERE cnpj = ?";
        return executeQuery(sql, cnpj);
    }

    private Optional<ClienteEntity> executeQuery(String sql, String parameter) {
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            logger.debug("Obtendo conexão com banco de dados e preparando statement");
            stmt.setString(1, parameter);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ClienteEntity entity = mapResultSetToEntity(rs);
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("Query executada com sucesso - clienteId: {}, duration: {}ms", entity.getId(), duration);
                    return Optional.of(entity);
                }
            }
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Query executada sem resultados - duration: {}ms", duration);
        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Erro SQL ao executar query - sql: {}, parameter: {}, duration: {}ms, erro: {}, sqlState: {}, errorCode: {}", 
                    sql, maskSensitiveData(parameter), duration, e.getMessage(), e.getSQLState(), e.getErrorCode());
            throw new RuntimeException("Erro ao buscar cliente no banco de dados", e);
        }

        return Optional.empty();
    }

    private ClienteEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        logger.debug("Mapeando ResultSet para ClienteEntity");
        
        // Trata a conversão do UUID (se a coluna for BINARY(16) no MySQL)
        UUID id = null;
        byte[] bytes = rs.getBytes("id");
        if (bytes != null) {
            id = bytesToUuid(bytes); // Ou UUID.fromString(rs.getString("id")) se for VARCHAR
        }

        String statusStr = rs.getString("status");
        ClienteEntity.StatusClienteEntity status = statusStr != null
                ? ClienteEntity.StatusClienteEntity.valueOf(statusStr)
                : null;

        ClienteEntity entity = ClienteEntity.builder()
                .id(id)
                .nome(rs.getString("nome"))
                .cpf(rs.getString("cpf"))
                .cnpj(rs.getString("cnpj"))
                .email(rs.getString("email"))
                .senhaHash(rs.getString("senha_hash"))
                .status(status)
                .build();
        
        logger.debug("ClienteEntity mapeado com sucesso - clienteId: {}, nome: {}", id, entity.getNome());
        return entity;
    }

    // Utilitário caso o UUID seja armazenado como BINARY(16) no MySQL
    private UUID bytesToUuid(byte[] bytes) {
        java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.wrap(bytes);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();
        return new UUID(high, low);
    }
    
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "***";
        }
        return data.substring(0, 2) + "***" + data.substring(data.length() - 2);
    }
}