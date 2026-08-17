package com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories;

import com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories.entities.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteJpaRepository  extends JpaRepository<ClienteEntity, UUID> {
    Optional<ClienteEntity> findByCpf(String cpf);

    Optional<ClienteEntity> findByCnpj(String cnpj);
}
