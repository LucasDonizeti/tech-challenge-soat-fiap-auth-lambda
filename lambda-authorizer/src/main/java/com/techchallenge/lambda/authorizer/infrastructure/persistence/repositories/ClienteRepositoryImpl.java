package com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories;

import com.techchallenge.lambda.authorizer.domain.model.Cliente;
import com.techchallenge.lambda.authorizer.domain.repositories.ClienteRepository;
import com.techchallenge.lambda.authorizer.infrastructure.persistence.mappers.ClienteJpaMapper;
import lombok.RequiredArgsConstructor;
import com.techchallenge.lambda.authorizer.domain.model.valueobjects.CNPJ;
import com.techchallenge.lambda.authorizer.domain.model.valueobjects.CPF;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClienteRepositoryImpl implements ClienteRepository {
    private final ClienteJpaRepository jpaRepository;
    private final ClienteJpaMapper mapper;

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
