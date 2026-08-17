package com.techchallenge.lambda.authorizer.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT que suporta dois contextos de autenticação:
 *   - ADMIN: token gerado via /v1/auth/login (role=ADMIN)
 *   - CLIENTE: token gerado via /v1/auth/login (role=CLIENTE)
 *
 * A seleção do UserDetailsService correto é feita pela claim "role" do token.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger jwtLogger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService adminUserDetailsService;
    private final UserDetailsService clienteUserDetailsService;

    /**
     * Construtor completo (dois UserDetailsService).
     */
    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil,
                                   UserDetailsService adminUserDetailsService,
                                   UserDetailsService clienteUserDetailsService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.adminUserDetailsService = adminUserDetailsService;
        this.clienteUserDetailsService = clienteUserDetailsService;
    }

    /**
     * Construtor de retrocompatibilidade (apenas admin) — mantido para não quebrar testes existentes.
     */
    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, UserDetailsService userDetailsService) {
        this(jwtTokenUtil, userDetailsService, userDetailsService);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String tokenHeader = request.getHeader(AUTH_HEADER);

        if (tokenHeader == null || !tokenHeader.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String jwtToken = tokenHeader.substring(BEARER_PREFIX.length());
        String username = extractUsername(jwtToken);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateUser(username, jwtToken, request);
        }

        chain.doFilter(request, response);
    }

    private String extractUsername(String token) {
        try {
            return jwtTokenUtil.getUsernameFromToken(token);
        } catch (Exception e) {
            jwtLogger.warn("Token JWT inválido ou expirado: {}", e.getMessage());
            return null;
        }
    }

    private void authenticateUser(String username, String token, HttpServletRequest request) {
        // Determina qual UserDetailsService usar baseado na claim "role"
        String role = extractRole(token);
        UserDetailsService uds = "CLIENTE".equals(role) ? clienteUserDetailsService : adminUserDetailsService;

        try {
            UserDetails userDetails = uds.loadUserByUsername(username);

            if (jwtTokenUtil.validateToken(token, userDetails)) {
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                jwtLogger.debug("Usuário autenticado via JWT: {} [role={}]", username, role);
            } else {
                jwtLogger.warn("Falha na validação do token JWT para: {}", username);
            }
        } catch (UsernameNotFoundException e) {
            jwtLogger.warn("Usuário não encontrado no UserDetailsService: {}", username);
        }
    }

    private String extractRole(String token) {
        try {
            return jwtTokenUtil.getRoleFromToken(token);
        } catch (Exception e) {
            return "ADMIN"; // padrão para tokens legados sem a claim role
        }
    }
}
