package com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories;

import com.techchallenge.lambda.authorizer.domain.model.Cliente;
import com.techchallenge.lambda.authorizer.domain.repositories.ClienteRepository;
import com.techchallenge.lambda.authorizer.domain.model.valueobjects.CNPJ;
import com.techchallenge.lambda.authorizer.domain.model.valueobjects.CPF;
import com.techchallenge.lambda.authorizer.infrastructure.persistence.mappers.ClienteJpaMapper;

import java.util.Optional;

public class ClienteRepositoryImpl implements ClienteRepository {
    private final ClienteJpaRepository jpaRepository;
    private final ClienteJpaMapper mapper;

    public ClienteRepositoryImpl(ClienteJpaRepository jpaRepository, ClienteJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Cliente> findByCPF(CPF cpf) {
        return jpaRepository.findByCpf(cpf.getValor())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Cliente> findByCNPJ(CNPJ cnpj) {
        return jpaRepository.findByCnpj(cnpj.getValor())
                .map(mapper::toDomain);
    }
}
