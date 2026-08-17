package com.techchallenge.lambda.authorizer.web.dto;

import com.techchallenge.lambda.authorizer.application.usecases.responses.AuthResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de autenticação com token JWT")
public class AuthResponseDto {

    @Schema(description = "Token JWT para autenticação nas requisições subsequentes")
    private String token;

    @Schema(description = "Tipo do token", example = "Bearer")
    private String type;

    @Schema(description = "Nome de usuário autenticado")
    private String username;

    @Schema(description = "Role do usuário autenticado", example = "ADMIN")
    private String role;

    public static AuthResponseDto from(AuthResponse response) {
        AuthResponseDto dto = new AuthResponseDto();
        dto.token = response.getToken();
        dto.type = response.getType();
        dto.username = response.getUsername();
        dto.role = response.getRole();
        return dto;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

