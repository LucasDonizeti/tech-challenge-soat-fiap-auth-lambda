package com.techchallenge.lambda.authorizer.infrastructure.persistence.mappers;

import com.techchallenge.lambda.authorizer.domain.model.Cliente;
import com.techchallenge.lambda.authorizer.domain.model.valueobjects.StatusCliente;
import com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories.entities.ClienteEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClienteJpaMapper {
    public Cliente toDomain(ClienteEntity entity) {
        if (entity == null) {
            return null;
        }

        return Cliente.builder()
                .id(entity.getId())
                .nome(entity.getNome())
                .senhaHash(entity.getSenhaHash())
                .status(toDomainStatus(entity.getStatus()))
                .build();
    }

    private StatusCliente toDomainStatus(ClienteEntity.StatusClienteEntity status) {
        return switch (status) {
            case ATIVO -> StatusCliente.ATIVO;
            case INATIVO -> StatusCliente.INATIVO;
        };
    }
}
