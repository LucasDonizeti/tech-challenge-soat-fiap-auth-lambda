package com.techchallenge.lambda.authorizer.infrastructure.persistence.repositories.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clientes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteEntity {
    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "nome", length = 100, nullable = false)
    private String nome;

    @Column(name = "cpf", length = 11, unique = true)
    private String cpf;

    @Column(name = "cnpj", length = 14, unique = true)
    private String cnpj;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", length = 255, nullable = true)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private StatusClienteEntity status;

    @Column(name = "criado_em", columnDefinition = "DATETIME(6)", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", columnDefinition = "DATETIME(6)", nullable = false)
    private LocalDateTime atualizadoEm;

    public enum StatusClienteEntity {
        ATIVO,
        INATIVO
    }
}
