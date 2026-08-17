package com.techchallenge.lambda.authorizer.application.ports;

import com.techchallenge.lambda.authorizer.application.usecases.ports.input.AutenticarUsuarioInput;
import com.techchallenge.lambda.authorizer.application.usecases.responses.AuthResponse;
import com.techchallenge.lambda.authorizer.web.dto.AuthRequestDto;
import com.techchallenge.lambda.authorizer.web.dto.AuthResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação e geração de tokens JWT")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AutenticarUsuarioInput autenticarUsuarioInput;

    public AuthController(AutenticarUsuarioInput autenticarUsuarioInput) {
        this.autenticarUsuarioInput = autenticarUsuarioInput;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário e gerar token JWT",
            description = "Realiza autenticação do usuário com username (admin ou CPF/CNPJ do cliente) e password, retornando um token JWT para uso nas requisições subsequentes")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Autenticação bem-sucedida, token gerado",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Credenciais inválidas",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Credenciais inválidas\"}")))
    })
    public ResponseEntity<?> createAuthenticationToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Credenciais de autenticação",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AuthRequestDto.class)))
            @Valid @RequestBody AuthRequestDto authRequest) {
        logger.info("Tentativa de login para o usuário: {}", authRequest.getUsername());

        try {
            AuthResponse authResponse = autenticarUsuarioInput.execute(authRequest.toCommand());
            AuthResponseDto responseDto = AuthResponseDto.from(authResponse);
            logger.info("Login bem-sucedido para o usuário: {}", authResponse.getUsername());
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            logger.error("Falha na autenticação para o usuário: {}. Erro: {}", authRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Credenciais inválidas"));
        }
    }
}
