package com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories;

import com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories.entities.ClienteEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class ClienteJdbcRepository implements ClienteJpaRepository {

    private final DataSource dataSource;

    public ClienteJdbcRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<ClienteEntity> findByCpf(String cpf) {
        String sql = "SELECT * FROM clientes WHERE cpf = ?";
        return executeQuery(sql, cpf);
    }

    @Override
    public Optional<ClienteEntity> findByCnpj(String cnpj) {
        String sql = "SELECT * FROM clientes WHERE cnpj = ?";
        return executeQuery(sql, cnpj);
    }

    private Optional<ClienteEntity> executeQuery(String sql, String parameter) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, parameter);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ClienteEntity entity = mapResultSetToEntity(rs);
                    return Optional.of(entity);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar cliente no banco de dados", e);
        }

        return Optional.empty();
    }

    private ClienteEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
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

        return ClienteEntity.builder()
                .id(id)
                .nome(rs.getString("nome"))
                .cpf(rs.getString("cpf"))
                .cnpj(rs.getString("cnpj"))
                .email(rs.getString("email"))
                .senhaHash(rs.getString("senha_hash"))
                .status(status)
                .build();
    }

    // Utilitário caso o UUID seja armazenado como BINARY(16) no MySQL
    private UUID bytesToUuid(byte[] bytes) {
        java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.wrap(bytes);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();
        return new UUID(high, low);
    }
}