package com.techchallenge.lambda.authorizer.application.usecases;

import com.techchallenge.lambda.authorizer.application.usecases.commands.AutenticarUsuarioCommand;
import com.techchallenge.lambda.authorizer.application.usecases.ports.input.AutenticarUsuarioInput;
import com.techchallenge.lambda.authorizer.application.usecases.ports.output.ClienteGateway;
import com.techchallenge.lambda.authorizer.application.usecases.responses.AuthResponse;
import com.techchallenge.lambda.authorizer.domain.model.Cliente;
import com.techchallenge.lambda.authorizer.infrastructure.security.JwtTokenUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AutenticarUsuarioUseCase implements AutenticarUsuarioInput {
    private static final Logger logger = LoggerFactory.getLogger(AutenticarUsuarioUseCase.class);
    
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
        
        logger.info("AutenticarUsuarioUseCase inicializado - adminUsername: {}", maskSensitiveData(adminUsername));
    }

    @Override
    public AuthResponse execute(AutenticarUsuarioCommand command) {
        String normalizedUsername = normalizeUsername(command.getUsername());
        String password = command.getPassword();

        logger.info("Iniciando autenticação - username: {}, normalizedUsername: {}", 
                maskSensitiveData(command.getUsername()), maskSensitiveData(normalizedUsername));

        if (isAdminUser(normalizedUsername, password)) {
            logger.info("Autenticação como ADMIN bem-sucedida - username: {}", maskSensitiveData(normalizedUsername));
            String role = "ADMIN";
            String token = jwtTokenUtil.generateAdminToken(normalizedUsername);
            return new AuthResponse(token, "Bearer", normalizedUsername, role);
        }

        logger.debug("Buscando cliente por CPF/CNPJ - normalizedUsername: {}", maskSensitiveData(normalizedUsername));
        Optional<Cliente> clienteOpt = clienteGateway.findByCpf(normalizedUsername)
                .or(() -> clienteGateway.findByCnpj(normalizedUsername));

        if (clienteOpt.isEmpty()) {
            logger.warn("Cliente não encontrado - normalizedUsername: {}", maskSensitiveData(normalizedUsername));
            throw new IllegalArgumentException("Usuário ou senha inválidos");
        }

        Cliente cliente = clienteOpt.get();
        logger.debug("Cliente encontrado - clienteId: {}, nome: {}", cliente.getId(), cliente.getNome());

        boolean senhaValida = BCrypt.checkpw(password, cliente.getSenhaHash());
        if (!senhaValida) {
            logger.warn("Senha inválida para o cliente - clienteId: {}, normalizedUsername: {}", 
                    cliente.getId(), maskSensitiveData(normalizedUsername));
            throw new IllegalArgumentException("Usuário ou senha inválidos");
        }

        String role = resolveRole(cliente);
        logger.info("Senha válida - clienteId: {}, role: {}", cliente.getId(), role);
        
        String token = generateTokenForUser(role, normalizedUsername, cliente);
        logger.info("Token gerado com sucesso - clienteId: {}, role: {}, username: {}", 
                cliente.getId(), role, maskSensitiveData(normalizedUsername));

        return new AuthResponse(token, "Bearer", normalizedUsername, role);
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            logger.warn("Tentativa de normalizar username nulo");
            return null;
        }

        String digitsOnly = username.replaceAll("\\D", "");
        if (digitsOnly.length() == 11 || digitsOnly.length() == 14) {
            logger.debug("Username normalizado para CPF/CNPJ - original: {}, normalizado: {}", 
                    maskSensitiveData(username), maskSensitiveData(digitsOnly));
            return digitsOnly;
        }

        logger.debug("Username mantido como está - original: {}", maskSensitiveData(username));
        return username;
    }

    private String resolveRole(Cliente cliente) {
        return "CLIENTE";
    }

    private String generateTokenForUser(String role, String normalizedUsername, Cliente userDetails) {
        if ("CLIENTE".equals(role)) {
            logger.debug("Gerando token para CLIENTE - normalizedUsername: {}", maskSensitiveData(normalizedUsername));
            Optional<Cliente> clienteOpt = clienteGateway.findByCpf(normalizedUsername)
                    .or(() -> clienteGateway.findByCnpj(normalizedUsername));
            String nome = clienteOpt.map(Cliente::getNome).orElse("");
            String clienteId = clienteOpt.map(cliente -> cliente.getId() != null ? cliente.getId().toString() : "").orElse("");
            return jwtTokenUtil.generateClienteToken(normalizedUsername, nome, clienteId);
        }

        logger.debug("Gerando token para não-CLIENTE - normalizedUsername: {}", maskSensitiveData(normalizedUsername));
        return jwtTokenUtil.generateAdminToken(normalizedUsername);
    }

    private boolean isAdminUser(String username, String password) {
        if (this.adminUsername.equals(username)) {
            logger.debug("Verificando credenciais de ADMIN - username: {}", maskSensitiveData(username));
            boolean isValid = BCrypt.checkpw(password, this.adminPasswordHash);
            if (isValid) {
                logger.info("Credenciais de ADMIN válidas");
            } else {
                logger.warn("Credenciais de ADMIN inválidas - senha incorreta");
            }
            return isValid;
        }
        logger.debug("Usuário não é ADMIN - username: {}", maskSensitiveData(username));
        return false;
    }
    
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "***";
        }
        return data.substring(0, 2) + "***" + data.substring(data.length() - 2);
    }
}
