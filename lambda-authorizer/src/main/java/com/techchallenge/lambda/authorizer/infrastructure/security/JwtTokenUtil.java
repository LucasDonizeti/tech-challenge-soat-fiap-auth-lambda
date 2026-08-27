package com.techchallenge.lambda.authorizer.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Long.parseLong;

public class JwtTokenUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    private String secret;
    private Long expiration;

    public JwtTokenUtil(){
        this.secret = System.getenv().getOrDefault("JWT_SECRET", "mySuperSecretKeyForJWTTokenGenerationThatIsSecureEnough");
        this.expiration = parseLong(System.getenv().getOrDefault("JWT_EXPIRATION", "86400"));
        
        logger.info("JwtTokenUtil inicializado - expiration: {}s, secretConfigured: {}", 
                expiration, secret != null && !secret.isEmpty());
    }

    private SecretKey getSigningKey() {
        logger.debug("Obtendo signing key para JWT");
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // -------------------------------------------------------------------------
    // Extração de claims
    // -------------------------------------------------------------------------

    public String getUsernameFromToken(String token) {
        logger.debug("Extraindo username do token");
        return getClaimFromToken(token, Claims::getSubject);
    }

    public String getRoleFromToken(String token) {
        logger.debug("Extraindo role do token");
        return getAllClaimsFromToken(token).get("role", String.class);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        logger.debug("Extraindo claim genérico do token");
        return claimsResolver.apply(getAllClaimsFromToken(token));
    }

    private Claims getAllClaimsFromToken(String token) {
        logger.debug("Parseando e validando token JWT");
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            logger.debug("Token JWT parseado com sucesso - subject: {}, expiration: {}", 
                    claims.getSubject(), claims.getExpiration());
            return claims;
        } catch (Exception e) {
            logger.error("Erro ao parsear token JWT - erro: {}", e.getMessage());
            throw e;
        }
    }

    private boolean isTokenExpired(String token) {
        logger.debug("Verificando se token está expirado");
        boolean expired = getAllClaimsFromToken(token).getExpiration().before(new Date());
        if (expired) {
            logger.warn("Token JWT está expirado");
        }
        return expired;
    }

    // -------------------------------------------------------------------------
    // Geração de tokens
    // -------------------------------------------------------------------------

    /**
     * Gera token para admin (fluxo username/password existente).
     */
    public String generateAdminToken(String username) {
        logger.info("Gerando token ADMIN - username: {}", maskSensitiveData(username));
        String token = buildToken(username, Map.of("role", "ADMIN"));
        logger.info("Token ADMIN gerado com sucesso - username: {}", maskSensitiveData(username));
        return token;
    }

    /**
     * Gera token para cliente (CPF como subject, role = CLIENTE).
     */
    public String generateClienteToken(String cpf, String clienteNome, String clienteId) {
        logger.info("Gerando token CLIENTE - cpf: {}, clienteId: {}, nome: {}", 
                maskSensitiveData(cpf), clienteId, clienteNome);
        String token = buildToken(cpf, Map.of(
                "role", "CLIENTE",
                "nome", clienteNome != null ? clienteNome : "",
                "clienteId", clienteId != null ? clienteId : ""
        ));
        logger.info("Token CLIENTE gerado com sucesso - cpf: {}, clienteId: {}", 
                maskSensitiveData(cpf), clienteId);
        return token;
    }

    private String buildToken(String subject, Map<String, Object> extraClaims) {
        logger.debug("Construindo token JWT - subject: {}, claims: {}", 
                maskSensitiveData(subject), extraClaims.keySet());
        
        Date issuedAt = new Date(System.currentTimeMillis());
        Date expiration = new Date(System.currentTimeMillis() + this.expiration * 1000);
        
        String token = Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(getSigningKey())
                .compact();
        
        logger.debug("Token JWT construído - issuedAt: {}, expiration: {}", issuedAt, expiration);
        return token;
    }
    
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "***";
        }
        return data.substring(0, 2) + "***" + data.substring(data.length() - 2);
    }
}