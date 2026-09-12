# 🔐 tech-challenge-soat-fiap-auth-lambda

Lambda Authorizer do **Sistema de Gestão de Oficina** — responsável pela geração de tokens JWT no endpoint `POST /v1/auth/login`.

Este repositório é o **quarto e último** da cadeia de provisionamento. Ele cria a função Lambda, integra ao API Gateway já existente (provisionado pelo `k8s-infra`) e conecta ao RDS MySQL (provisionado pelo `db-infra`).

---

## 📑 Documentação

| Documento | Descrição |
|-----------|-----------|
| [Pré-requisitos](docs/prerequisitos.md) | Java 21, Maven, Docker, AWS CLI, Terraform e credenciais |
| [Build e Testes Locais](docs/build-local.md) | Como compilar, testar e rodar localmente |
| [Provisionamento Manual](docs/provisionamento-manual.md) | Passo a passo do Terraform para deploy da Lambda |
| [Pipeline CI/CD](docs/pipeline.md) | Como funciona a pipeline e os GitHub Secrets necessários |
| [Recursos Provisionados](docs/recursos-provisionados.md) | Inventário da Lambda e integração API Gateway |
| [Fluxo de Autenticação](docs/fluxo-autenticacao.md) | Como funciona o login, geração de JWT e validação |
| [Comandos Úteis](docs/comandos-uteis.md) | AWS CLI para Lambda, testes de endpoint e Maven |

---

## ⚡ Visão Rápida

```
Cliente → POST /v1/auth/login
              │
              ▼
    API Gateway HTTP v2
    (rota dedicada: POST /v1/auth/login → AWS_PROXY)
              │
              ▼
    Auth Lambda (Java 21, Container Image)
    ├── Admin?  → BCrypt.checkpw(env vars) → JWT ADMIN
    └── CPF/CNPJ? → JDBC → RDS MySQL → BCrypt → JWT CLIENTE
              │
              ▼
    { "token": "eyJ...", "type": "Bearer", "username": "..." }
```

---

## 🔗 Arquitetura

```
Internet
    │
    ▼
API Gateway HTTP v2
    │  POST /v1/auth/login → AWS_PROXY integration
    ▼
Lambda: oficina-auth-lambda
    ├── Runtime: Java 21 (Container Image)
    ├── Memória: 512 MB · Timeout: 15s
    ├── VPC: Private Subnets (acesso ao RDS)
    ├── SG: eks_node_security_group (permite 3306 no RDS)
    └── Env vars: DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET
              │
              ▼  (apenas para login CPF/CNPJ)
    RDS MySQL — oficina-rds:3306
```

---

## 🗂️ Estrutura do Repositório

```
.
├── .github/
│   └── workflows/
│       └── pipeline.yml              # Pipeline CI/CD (Build → ECR → Terraform)
├── lambda-authorizer/                # Código-fonte Java da Lambda
│   ├── src/
│   │   ├── main/java/com/techchallenge/lambda/authorizer/
│   │   │   ├── application/
│   │   │   │   ├── configure/        # InjectionFactory (DI sem Spring)
│   │   │   │   ├── ports/            # AuthLambdaHandler (entry point)
│   │   │   │   └── usecases/         # AutenticarUsuarioUseCase
│   │   │   ├── domain/model/         # Cliente (entidade)
│   │   │   ├── infrastructure/
│   │   │   │   ├── persistence/      # ClienteJdbcGateway (JDBC direto)
│   │   │   │   └── security/         # JwtTokenUtil (JJWT 0.13.0)
│   │   │   └── web/dto/              # AuthRequestDto, AuthResponseDto
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── logback.xml
│   ├── Dockerfile                    # FROM public.ecr.aws/lambda/java:21
│   └── pom.xml                       # Java 21, sem Spring Boot
├── terraform/
│   ├── main.tf                       # Lambda + API GW integration + route
│   ├── variables.tf                  # db_url, db_user, db_password, jwt_secret, image_tag...
│   ├── outputs.tf                    # lambda_arn, lambda_function_name
│   ├── backend.tf                    # State: auth-lambda/terraform.tfstate
│   ├── provider.tf                   # AWS ~> 6.0
│   └── terraform.tfvars.example      # Exemplo de variáveis
└── docs/                             # Documentação detalhada
```

---

## 🔄 Ordem de Provisionamento

```
[1] k8s-infra   →  VPC + EKS + ECR + API GW     ✅ deve estar pronto
[2] db-infra    →  RDS MySQL                      ✅ deve estar pronto
[3] Aplicação   →  Helm → EKS (pode ser paralelo)
[4] auth-lambda →  Lambda + rota API GW           ← ESTE REPOSITÓRIO
```

> ⚠️ O `k8s-infra` deve estar provisionado (para obter ECR URL, API GW ID e subnets). O `db-infra` deve ter o RDS rodando (para o `DB_URL` e senha).

---

## 🛠️ Tecnologias

| Tecnologia | Versão | Uso |
|-----------|--------|-----|
| Java | 21 (Corretto) | Runtime da Lambda |
| Maven | 3.9.x | Build e testes |
| AWS Lambda Java Core | 1.2.3 | Handler e Context |
| AWS Lambda Java Events | 3.11.4 | APIGatewayProxyRequestEvent |
| JJWT | 0.13.0 | Geração de tokens JWT (HS256) |
| jBCrypt | 0.4 | Hash e verificação de senhas |
| MySQL Connector/J | 9.7.0 | Conexão JDBC ao RDS |
| Jackson Databind | 2.17.0 | Serialização JSON |
| Lombok | 1.18.44 | Redução de boilerplate |
| SLF4J + Logback | 2.0.17 / 1.4.14 | Logging |
| maven-shade-plugin | 3.6.2 | Fat JAR para a Lambda |
| Terraform | >= 1.6.0 | Provisionamento da Lambda |
| AWS Provider (TF) | ~> 6.0 | Recursos AWS |

---

## 🔗 Links Relacionados

- [k8s-infra](../tech-challenge-soat-fiap-k8s-infra) — cria o API Gateway e ECR `auth-lambda`
- [db-infra](../tech-challenge-soat-fiap-db-infra) — cria o RDS acessado pela Lambda
- [Repositório principal — oficina-api](../tech-challenge-soat-fiap) — valida os tokens JWT gerados aqui
- [Swagger UI](https://<API_GATEWAY_URL>/swagger-ui/index.html) *(após deploy da app)*
