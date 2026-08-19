package com.techchallenge.lambda.authorizer.application.usecases;

import com.techchallenge.lambda.authorizer.application.usecases.commands.AutenticarUsuarioCommand;
import com.techchallenge.lambda.authorizer.application.usecases.ports.input.AutenticarUsuarioInput;
import com.techchallenge.lambda.authorizer.application.usecases.ports.output.ClienteGateway;
import com.techchallenge.lambda.authorizer.application.usecases.responses.AuthResponse;
import com.techchallenge.lambda.authorizer.domain.model.Cliente;
import com.techchallenge.lambda.authorizer.infrastructure.security.JwtTokenUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

public class AutenticarUsuarioUseCase implements AutenticarUsuarioInput {
    private final JwtTokenUtil jwtTokenUtil;
    private final ClienteGateway clienteGateway;

    private final String adminUsername;
    private final String adminPasswordHash;

    public AutenticarUsuarioUseCase(JwtTokenUtil jwtTokenUtil, ClienteGateway clienteGateway) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.clienteGateway = clienteGateway;

        // Lê as variáveis de ambiente na inicialização
        this.adminUsername = System.getenv().getOrDefault("SPRING_SECURITY_USER_NAME", "admin");

        String rawAdminPassword = System.getenv().getOrDefault("SPRING_SECURITY_USER_PASSWORD", "admin123");

        // Gera o hash da senha do admin para permitir comparação segura via BCrypt
        this.adminPasswordHash = BCrypt.hashpw(rawAdminPassword, BCrypt.gensalt());
    }

    @Override
    public AuthResponse execute(AutenticarUsuarioCommand command) {
        String normalizedUsername = normalizeUsername(command.getUsername());
        String password = command.getPassword();


        if (isAdminUser(normalizedUsername, password)) {
            String role = "ADMIN";
            String token = jwtTokenUtil.generateAdminToken(normalizedUsername);
            return new AuthResponse(token, "Bearer", normalizedUsername, role);
        }

        Optional<Cliente> clienteOpt = clienteGateway.findByCpf(normalizedUsername)
                .or(() -> clienteGateway.findByCnpj(normalizedUsername));

        Cliente cliente = clienteOpt.orElseThrow(() ->
                new IllegalArgumentException("Usuário ou senha inválidos"));

        boolean senhaValida = BCrypt.checkpw(password, cliente.getSenhaHash());
        if (!senhaValida) {
            throw new IllegalArgumentException("Usuário ou senha inválidos");
        }

        String role = resolveRole(cliente);
        String token = generateTokenForUser(role, normalizedUsername, cliente);

        return new AuthResponse(token, "Bearer", normalizedUsername, role);
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }

        String digitsOnly = username.replaceAll("\\D", "");
        if (digitsOnly.length() == 11 || digitsOnly.length() == 14) {
            return digitsOnly;
        }

        return username;
    }

    private String resolveRole(Cliente cliente) {
        return "CLIENTE";
    }

    private String generateTokenForUser(String role, String normalizedUsername, Cliente userDetails) {
        if ("CLIENTE".equals(role)) {
            Optional<Cliente> clienteOpt = clienteGateway.findByCpf(normalizedUsername)
                    .or(() -> clienteGateway.findByCnpj(normalizedUsername));
            String nome = clienteOpt.map(Cliente::getNome).orElse("");
            String clienteId = clienteOpt.map(cliente -> cliente.getId() != null ? cliente.getId().toString() : "").orElse("");
            return jwtTokenUtil.generateClienteToken(normalizedUsername, nome, clienteId);
        }

        return jwtTokenUtil.generateAdminToken(normalizedUsername);
    }

    private boolean isAdminUser(String username, String password) {
        if (this.adminUsername.equals(username)) {
            return BCrypt.checkpw(password, this.adminPasswordHash);
        }
        return false;
    }
}
