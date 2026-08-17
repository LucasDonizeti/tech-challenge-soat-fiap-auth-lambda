package com.techchallenge.lambda.authorizer.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenUtil {

    @Value("${spring.security.jwt.secret:mySuperSecretKeyForJWTTokenGenerationThatIsSecureEnough}")
    private String secret;

    @Value("${spring.security.jwt.expiration:86400}")
    private Long expiration;

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
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
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
    public String generateToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), Map.of("role", "ADMIN"));
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

    // -------------------------------------------------------------------------
    // Validação
    // -------------------------------------------------------------------------

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
}