package com.techchallenge.lambda.authorizer.web.dto;

import com.techchallenge.lambda.authorizer.application.usecases.commands.AutenticarUsuarioCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requisição de autenticação com credenciais do usuário")
public class AuthRequestDto {

    @NotBlank(message = "Nome de usuário é obrigatório")
    @Schema(description = "Nome de usuário ou documento do cliente (CPF/CNPJ)", example = "admin", required = true)
    private String username;

    @NotBlank(message = "Senha é obrigatória")
    @Schema(description = "Senha do usuário", example = "password", required = true)
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AutenticarUsuarioCommand toCommand() {
        return new AutenticarUsuarioCommand(username, password);
    }
}
