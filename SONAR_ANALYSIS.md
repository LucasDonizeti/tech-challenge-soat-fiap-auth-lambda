# Análise Estática de Código com SonarQube

Este documento descreve como configurar e executar a análise estática de código do projeto lambda-authorizer usando SonarQube.

## Visão Geral

O SonarQube é uma plataforma de análise estática de código que detecta:
- Bugs e vulnerabilidades de segurança
- Code smells e dívida técnica
- Duplicação de código
- Complexidade ciclomática excessiva
- Problemas de conformidade com padrões

## Configuração

### Plugin Maven

O projeto possui o plugin `sonar-maven-plugin` configurado no `pom.xml` (versão 3.9.1.2184), alinhado com o projeto principal, para facilitar a execução de análises manuais.

### Execução Local

#### 1. Iniciar o SonarQube

Use o Docker Compose do projeto principal:

```bash
cd ../tech-challenge-soat-fiap
docker-compose up -d sonar sonar_db
```

Acesse: http://localhost:9000 (admin/admin)

#### 2. Gerar Token de Acesso

No painel do SonarQube:
1. Vá em My Account → Security
2. Clique em "Generate Token"
3. Dê um nome ao token (ex: "lambda-authorizer")
4. Copie o token gerado

#### 3. Executar Análise

```bash
export SONAR_TOKEN=seu_token_gerado
export SONAR_HOST_URL=http://localhost:9000

cd lambda-authorizer
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=lambda-authorizer \
  -Dsonar.projectName='lambda-authorizer' \
  -Dsonar.host.url=$SONAR_HOST_URL \
  -Dsonar.token=$SONAR_TOKEN
```

## Configurações Adicionais

### Exclusões de Arquivos

Para excluir determinados arquivos/diretórios da análise:

```bash
-Dsonar.exclusions=**/generated/**,**/dto/**,**/config/**
-Dsonar.coverage.exclusions=**/dto/**,**/config/**
```

### Configuração de Java

```bash
-Dsonar.java.source=21
-Dsonar.java.target=21
```

### Níveis de Log

```bash
-Dsonar.verbose=true
```

## Integração CI/CD

Atualmente, a análise SonarQube não está configurada no pipeline CI/CD automatizado. A análise deve ser executada manualmente conforme descrito na seção "Execução Local".

Para implementar análise automatizada no futuro, consulte a documentação do SonarQube sobre integração com GitHub Actions.

## Troubleshooting

### Erro de Conexão

```bash
# Verifique se o SonarQube está rodando
docker-compose ps sonar

# Verifique os logs
docker-compose logs sonar
```

### Token Inválido

Gere um novo token no painel do SonarQube e atualize a variável `SONAR_TOKEN`.

### Timeout na Análise

Aumente o timeout do Maven:

```bash
mvn -Dmaven.test.failure.ignore=true -Dsonar.timeout=600 clean verify sonar:sonar
```

## Métricas e Quality Gates

### Métricas Principais

- **Coverage**: Cobertura de testes (alvo: ≥80%)
- **Bugs**: Defeitos no código
- **Vulnerabilities**: Problemas de segurança
- **Code Smells**: Dívida técnica
- **Duplications**: Código duplicado
- **Complexity**: Complexidade ciclomática

### Quality Gates Padrão

- Novo código: Coverage ≥ 80%
- Zero vulnerabilidades críticas
- Zero bugs críticos
- Code smells controlados

## Referências

- [SonarQube Documentation](https://docs.sonarqube.org/)
- [SonarScanner for Maven](https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-maven/)
- [SonarQube Quality Gates](https://docs.sonarqube.org/latest/user-guide/quality-gates/)
