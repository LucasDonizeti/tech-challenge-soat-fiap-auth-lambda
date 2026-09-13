package com.techchallenge.lambda.authorizer.infrastructure.gateway;

import com.techchallenge.lambda.authorizer.application.usecases.ports.output.ClienteGateway;
import com.techchallenge.lambda.authorizer.domain.model.Cliente;
import com.techchallenge.lambda.authorizer.domain.model.valueobjects.CNPJ;
import com.techchallenge.lambda.authorizer.domain.model.valueobjects.CPF;
import com.techchallenge.lambda.authorizer.domain.repositories.ClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ClientePersistenceGateway implements ClienteGateway {
    private static final Logger logger = LoggerFactory.getLogger(ClientePersistenceGateway.class);

    private final ClienteRepository clienteRepository;

    public ClientePersistenceGateway(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
        logger.info("ClientePersistenceGateway inicializado");
    }

    @Override
    public Optional<Cliente> findByCpf(String cpf) {
        logger.debug("Buscando cliente por CPF no gateway - cpf: {}", maskSensitiveData(cpf));
        try {
            CPF cpfValue = CPF.of(cpf);
            Optional<Cliente> result = clienteRepository.findByCPF(cpfValue);
            
            if (result.isPresent()) {
                logger.info("Cliente encontrado por CPF - clienteId: {}, cpf: {}", 
                        result.get().getId(), maskSensitiveData(cpf));
            } else {
                logger.debug("Cliente não encontrado por CPF - cpf: {}", maskSensitiveData(cpf));
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Erro ao buscar cliente por CPF - cpf: {}, erro: {}", 
                    maskSensitiveData(cpf), e.getMessage());
            throw e;
        }
    }

    @Override
    public Optional<Cliente> findByCnpj(String cnpj) {
        logger.debug("Buscando cliente por CNPJ no gateway - cnpj: {}", maskSensitiveData(cnpj));
        try {
            CNPJ cnpjValue = CNPJ.of(cnpj);
            Optional<Cliente> result = clienteRepository.findByCNPJ(cnpjValue);
            
            if (result.isPresent()) {
                logger.info("Cliente encontrado por CNPJ - clienteId: {}, cnpj: {}", 
                        result.get().getId(), maskSensitiveData(cnpj));
            } else {
                logger.debug("Cliente não encontrado por CNPJ - cnpj: {}", maskSensitiveData(cnpj));
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Erro ao buscar cliente por CNPJ - cnpj: {}, erro: {}", 
                    maskSensitiveData(cnpj), e.getMessage());
            throw e;
        }
    }
    
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "***";
        }
        return data.substring(0, 2) + "***" + data.substring(data.length() - 2);
    }
}
