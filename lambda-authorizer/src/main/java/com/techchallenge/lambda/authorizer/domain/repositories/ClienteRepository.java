package com.techchallenge.lambda.authorizer.domain.repositories;

import com.techchallenge.lambda.authorizer.domain.model.Cliente;
import com.techchallenge.lambda.authorizer.domain.model.valueobjects.CNPJ;
import com.techchallenge.lambda.authorizer.domain.model.valueobjects.CPF;

import java.util.Optional;

public interface ClienteRepository {
    Optional<Cliente> findByCPF(CPF cpf);

    Optional<Cliente> findByCNPJ(CNPJ cnpj);
}
