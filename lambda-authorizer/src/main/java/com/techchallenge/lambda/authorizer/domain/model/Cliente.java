package com.techchallenge.lambda.authorizer.domain.model;

import com.techchallenge.lambda.authorizer.domain.model.valueobjects.StatusCliente;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {
    private UUID id;

    private String nome;

    private String senhaHash;

    private StatusCliente status = StatusCliente.ATIVO;
}
