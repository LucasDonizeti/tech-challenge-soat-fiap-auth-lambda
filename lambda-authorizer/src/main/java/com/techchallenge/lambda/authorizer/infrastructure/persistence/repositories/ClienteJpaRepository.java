package com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories;

import com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories.entities.ClienteEntity;

import java.util.Optional;

public interface ClienteJpaRepository {
    Optional<ClienteEntity> findByCpf(String cpf);

    Optional<ClienteEntity> findByCnpj(String cnpj);
}
