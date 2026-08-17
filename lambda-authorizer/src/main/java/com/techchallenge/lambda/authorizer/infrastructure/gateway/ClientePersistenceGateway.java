package com.techchallenge.lambda.authorizer.infrastructure.gateway;

import com.techchallenge.lambda.authorizer.application.usecases.ports.output.ClienteGateway;
import com.techchallenge.lambda.authorizer.domain.model.Cliente;
import com.techchallenge.lambda.authorizer.domain.model.valueobjects.CNPJ;
import com.techchallenge.lambda.authorizer.domain.model.valueobjects.CPF;
import com.techchallenge.lambda.authorizer.domain.repositories.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClientePersistenceGateway implements ClienteGateway {

    private final ClienteRepository clienteRepository;

    @Override
    public Optional<Cliente> findByCpf(String cpf) {
        return clienteRepository.findByCPF(CPF.of(cpf));
    }

    @Override
    public Optional<Cliente> findByCnpj(String cnpj) {
        return clienteRepository.findByCNPJ(CNPJ.of(cnpj));
    }
}
