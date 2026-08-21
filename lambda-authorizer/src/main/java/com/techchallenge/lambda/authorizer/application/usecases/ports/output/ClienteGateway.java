package com.techchallenge.lambda.authorizer.application.usecases.ports.output;

import com.techchallenge.lambda.authorizer.domain.model.Cliente;

import java.util.Optional;

public interface ClienteGateway {
    Optional<Cliente> findByCpf(String cpf);

    Optional<Cliente> findByCnpj(String cnpj);
}
