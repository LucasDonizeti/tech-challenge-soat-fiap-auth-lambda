package com.techchallenge.lambda.authorizer.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Long.parseLong;

public class JwtTokenUtil {

    private String secret;

    private Long expiration;

    public JwtTokenUtil(){
        this.secret = System.getenv().getOrDefault("JWT_SECRET", "mySuperSecretKeyForJWTTokenGenerationThatIsSecureEnough");
        this.expiration = parseLong(System.getenv().getOrDefault("JWT_EXPIRATION", "86400"));
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // -------------------------------------------------------------------------
    // Extração de claims
    // -------------------------------------------------------------------------

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public String getRoleFromToken(String token) {
        return getAllClaimsFromToken(token).get("role", String.class);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(getAllClaimsFromToken(token));
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return getAllClaimsFromToken(token).getExpiration().before(new Date());
    }

    // -------------------------------------------------------------------------
    // Geração de tokens
    // -------------------------------------------------------------------------

    /**
     * Gera token para admin (fluxo username/password existente).
     */
    public String generateAdminToken(String username) {
        return buildToken(username, Map.of("role", "ADMIN"));
    }

    /**
     * Gera token para cliente (CPF como subject, role = CLIENTE).
     */
    public String generateClienteToken(String cpf, String clienteNome, String clienteId) {
        return buildToken(cpf, Map.of(
                "role", "CLIENTE",
                "nome", clienteNome != null ? clienteNome : "",
                "clienteId", clienteId != null ? clienteId : ""
        ));
    }

    private String buildToken(String subject, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey())
                .compact();
    }
}