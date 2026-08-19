package com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteEntity {
    private UUID id;

    private String nome;

    private String cpf;

    private String cnpj;

    private String email;

    private String senhaHash;

    private StatusClienteEntity status;

    public enum StatusClienteEntity {
        ATIVO,
        INATIVO
    }
}
