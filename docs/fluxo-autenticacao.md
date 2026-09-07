# Fluxo de Autenticação

Documentação do funcionamento interno da Lambda Authorizer.

---

## Visão geral

A Lambda é o único responsável pela **geração de tokens JWT** na plataforma. O `oficina-api` (Spring Boot no EKS) apenas **valida** os tokens recebidos — nunca os gera.

```
[Cliente]
    │
    │  POST /v1/auth/login
    │  { "username": "admin", "password": "secret123" }
    ▼
[API Gateway HTTP v2]
    │  Rota dedicada: POST /v1/auth/login → AWS_PROXY → Lambda
    ▼
[Auth Lambda]
    │
    ├─ Extrai/gera x-correlation-id
    ├─ Parse JSON → AuthRequestDto
    │
    ├─ [SE username == ADMIN_USERNAME (env var)]
    │       └─ BCrypt.checkpw(password, hash_em_memória)
    │           ├─ ✅ → generateAdminToken(username)
    │           └─ ❌ → 401 "Credenciais inválidas"
    │
    └─ [SE CPF (11 dígitos) ou CNPJ (14 dígitos)]
            └─ clienteGateway.findByCpf/Cnpj() → JDBC → RDS MySQL
                ├─ Não encontrado → 401
                └─ Encontrado → BCrypt.checkpw(password, hash_banco)
                    ├─ ✅ → generateClienteToken(username, nome, clienteId)
                    └─ ❌ → 401 "Credenciais inválidas"
    │
    ▼
HTTP 200: { "token": "eyJ...", "type": "Bearer", "username": "..." }
           Headers: x-correlation-id: <uuid>
```

---

## Dois perfis de usuário

### Admin

- Identificado pelo `username` igual à variável de ambiente `SPRING_SECURITY_USER_NAME`
- A senha é lida de `SPRING_SECURITY_USER_PASSWORD` e hasheada em memória no startup da Lambda
- **Não consulta o RDS** — autenticação 100% em memória
- Recebe role `ADMIN` no token JWT

### Cliente (CPF/CNPJ)

- Identificado por CPF (11 dígitos) ou CNPJ (14 dígitos) — formatação é normalizada (remove pontuação)
- Consulta a tabela `clientes` no RDS via JDBC direto (sem pool de conexões)
- A senha é comparada com o hash BCrypt armazenado no banco
- Recebe role `CLIENTE` no token JWT com claims extras: `nome` e `clienteId`

---

## Estrutura do token JWT

| Campo | Admin | Cliente |
|-------|-------|---------|
| Algoritmo | HS256 | HS256 |
| `sub` | username (ex: `admin`) | CPF/CNPJ normalizado |
| `role` | `ADMIN` | `CLIENTE` |
| `nome` | — | Nome do cliente |
| `clienteId` | — | UUID do cliente |
| `iat` | Timestamp de emissão | Timestamp de emissão |
| `exp` | Agora + 1h (3600s) | Agora + 24h |

**Exemplo — payload Admin:**
```json
{
  "sub": "admin",
  "role": "ADMIN",
  "iat": 1725494400,
  "exp": 1725498000
}
```

**Exemplo — payload Cliente:**
```json
{
  "sub": "12345678901",
  "role": "CLIENTE",
  "nome": "João da Silva",
  "clienteId": "550e8400-e29b-41d4-a716-446655440000",
  "iat": 1725494400,
  "exp": 1725580800
}
```

---

## Validação do token no oficina-api

O `oficina-api` (Spring Boot) possui um `JwtAuthenticationFilter` que intercepta todas as requisições:

1. Extrai o token do header `Authorization: Bearer <token>`
2. Chama `jwtTokenUtil.getUsernameFromToken(token)` → extrai `sub`
3. Chama `jwtTokenUtil.getRoleFromToken(token)` → extrai `role`
4. Se `role == ADMIN` → usa `adminUserDetailsService`
5. Se `role == CLIENTE` → usa `clienteUserDetailsService`
6. Chama `jwtTokenUtil.validateToken(token, userDetails)` → verifica assinatura e expiração
7. Define `SecurityContextHolder.setAuthentication(...)` com as authorities corretas

> ⚠️ **Atenção:** O `JWT_SECRET` deve ser **exatamente o mesmo** na Lambda e no `oficina-api`. Se forem diferentes, todos os tokens serão inválidos.

---

## Mapeamento de endpoints (públicos vs protegidos)

| Endpoint | Acessível sem token | Observação |
|----------|--------------------|----|
| `POST /v1/auth/login` | ✅ Sim | Roteado para esta Lambda pelo API Gateway |
| `GET /v1/os/**` | ✅ Sim | Acompanhamento de OS pelo cliente |
| `GET /actuator/**` | ✅ Sim | Health checks para Kubernetes e New Relic |
| `GET /swagger-ui/**` | ✅ Sim | Documentação da API |
| `GET /v3/api-docs/**` | ✅ Sim | Spec OpenAPI JSON |
| `/v1/admin/**` | ❌ Requer `ADMIN` | CRUD completo de clientes, veículos, serviços, OS |

---

## Tratamento de erros

| Cenário | Status | Body |
|---------|--------|------|
| Body da requisição vazio | `400` | `{"error": "Body da requisição é obrigatório"}` |
| Credenciais inválidas (admin) | `401` | `{"error": "Credenciais inválidas"}` |
| Usuário não encontrado no RDS | `401` | `{"error": "Credenciais inválidas"}` |
| Senha incorreta (cliente) | `401` | `{"error": "Credenciais inválidas"}` |
| Erro interno / exceção inesperada | `500` | `{"error": "Erro interno ao processar autenticação"}` |

> Todas as respostas incluem o header `x-correlation-id` para rastreamento de logs.

---

## Segurança — decisões de implementação

- **BCrypt**: todas as senhas são comparadas com hash BCrypt — nunca texto claro
- **Mascaramento de logs**: usernames são mascarados (`ab***cd`) em todos os logs
- **Correlation ID**: cada requisição recebe um UUID para correlação de logs distribuídos
- **Sem pool de conexões**: a Lambda abre/fecha conexão JDBC a cada invocação — aceitável para o volume de logins esperado
- **Env vars para credenciais**: `DB_URL`, `DB_USER`, `DB_PASSWORD` e `JWT_SECRET` são injetados como variáveis de ambiente pela Lambda (via Terraform), nunca em código
