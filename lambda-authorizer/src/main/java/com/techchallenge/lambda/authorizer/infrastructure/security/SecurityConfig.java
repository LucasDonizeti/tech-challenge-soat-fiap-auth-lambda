package com.techchallenge.lambda.authorizer.infrastructure.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Configuração de segurança com dois fluxos de autenticação:
 *
 * 1. ADMIN — usuário in-memory (username/password via /v1/auth/login) com ROLE_ADMIN
 *    → Protege /v1/admin/**, /v1/os/** (gestão interna)
 *
 * 2. CLIENTE — autenticação via CPF/CNPJ + senha (ClienteUserDetailsService) com ROLE_CLIENTE
 *    → Protege /api/cliente/** (endpoints públicos do cliente)
 *    → Login via /v1/auth/login
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.user.name}")
    private String adminUsername;

    @Value("${spring.security.user.password}")
    private String adminPassword;

    @Value("${spring.security.user.roles}")
    private String adminRoles;

    // -------------------------------------------------------------------------
    // SecurityFilterChain
    // -------------------------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    // -------------------------------------------------------------------------
    // PasswordEncoder
    // -------------------------------------------------------------------------

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // -------------------------------------------------------------------------
    // UserDetailsService — Admin (in-memory)
    // -------------------------------------------------------------------------

    @Bean
    @Primary
    @Qualifier("adminUserDetailsService")
    public UserDetailsService adminUserDetailsService(PasswordEncoder passwordEncoder) {
        var adminUser = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles(adminRoles.split(","))
                .build();
        return new InMemoryUserDetailsManager(adminUser);
    }

    // -------------------------------------------------------------------------
    // AuthenticationManager — admin dedicado (não primário)
    // -------------------------------------------------------------------------

    /**
     * AuthenticationManager dedicado apenas a administradores.
     *
     * Este bean não é o AuthenticationManager primário usado pelo login unificado,
     * mas permanece disponível para fluxos de autenticação separados, se necessário.
     */
    @Bean
    public AuthenticationManager adminAuthenticationManager(
            @Qualifier("adminUserDetailsService") UserDetailsService adminUDS,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider adminProvider = new DaoAuthenticationProvider(adminUDS);
        adminProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(List.of(adminProvider));
    }

    /**
     * AuthenticationManager primário usado pelo AuthController para login unificado.
     *
     * Combina dois providers:
     *   - adminProvider  → InMemoryUserDetailsManager (ROLE_ADMIN)
     *   - clienteProvider → ClienteUserDetailsService (ROLE_CLIENTE)
     *
     * O Spring Security tenta cada provider na ordem até um retornar autenticação.
     */
    @Bean
    @Primary
    public AuthenticationManager authenticationManager(
            @Qualifier("adminUserDetailsService") UserDetailsService adminUDS,
            ClienteUserDetailsService clienteUDS,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider adminProvider = new DaoAuthenticationProvider(adminUDS);
        adminProvider.setPasswordEncoder(passwordEncoder);

        DaoAuthenticationProvider clienteProvider = new DaoAuthenticationProvider(clienteUDS);
        clienteProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(List.of(adminProvider, clienteProvider));
    }

    // -------------------------------------------------------------------------
    // JwtAuthenticationFilter
    // -------------------------------------------------------------------------

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenUtil jwtTokenUtil,
            @Qualifier("adminUserDetailsService") UserDetailsService adminUDS,
            ClienteUserDetailsService clienteUDS) {
        return new JwtAuthenticationFilter(jwtTokenUtil, adminUDS, clienteUDS);
    }
}
