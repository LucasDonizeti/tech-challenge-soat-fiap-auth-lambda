package com.techchallenge.lambda.authorizer.infrastructure.security;

import com.techchallenge.lambda.authorizer.application.usecases.ports.output.ClienteGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UserDetailsService para autenticação de clientes via CPF ou CNPJ.
 *
 * O "username" neste contexto é o CPF ou CNPJ (apenas dígitos).
 * A senha é o hash BCrypt armazenado em clientes.senha_hash.
 * Somente clientes com status ATIVO podem autenticar.
 */
@Service("clienteUserDetailsService")
@RequiredArgsConstructor
@Slf4j
public class ClienteUserDetailsService implements UserDetailsService {

    private final ClienteGateway clienteGateway;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Carregando cliente por CPF/CNPJ: {}", username);

        var entity = clienteGateway.findByCpf(username)
                .or(() -> clienteGateway.findByCnpj(username))
                .orElseThrow(() -> new UsernameNotFoundException("Cliente não encontrado com CPF/CNPJ: " + username));

        if (entity.getStatus() == null ||
                !entity.getStatus().name().equals("ATIVO")) {
            throw new UsernameNotFoundException("Cliente inativo: " + username);
        }

        if (entity.getSenhaHash() == null || entity.getSenhaHash().isBlank()) {
            throw new UsernameNotFoundException("Cliente sem senha cadastrada: " + username);
        }

        return User.builder()
                .username(username)
                .password(entity.getSenhaHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")))
                .build();
    }
}
