# Pipeline CI/CD

A pipeline é acionada em todo push para `develop` e executa três jobs encadeados.

---

## Fluxo

```
Push → develop
         │
         ▼
┌─────────────────────────┐
│  Job 1: build           │  mvn verify (compilação + testes)
│  Build & Test           │  Sobe o JAR como artefato
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│  Job 2: push-image-ecr  │  Docker build + push para ECR auth-lambda
│  Build & Push           │  Tag: SHORT_SHA + latest
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────┐
│  Job 3: deploy                                      │
│  Deploy Terraform → AWS Lambda                      │
│  ├── Obtém Account ID via sts get-caller-identity   │
│  ├── Busca endpoint do RDS                          │
│  ├── Recupera senha do RDS no Secrets Manager       │
│  ├── terraform init + plan + apply                  │
│  └── Verifica status da Lambda (State == Active)    │
└─────────────────────────────────────────────────────┘
```

---

## GitHub Secrets obrigatórios

Acesse: **Repositório → Settings → Secrets and variables → Actions → New repository secret**

| Secret | Descrição | Como obter |
|--------|-----------|-----------|
| `AWS_ACCESS_KEY_ID` | ID da chave de acesso AWS | AWS Academy → **AWS Details** → `aws_access_key_id` |
| `AWS_SECRET_ACCESS_KEY` | Chave secreta de acesso AWS | AWS Academy → **AWS Details** → `aws_secret_access_key` |
| `AWS_SESSION_TOKEN` | Token de sessão temporário | AWS Academy → **AWS Details** → `aws_session_token` |
| `DB_USERNAME` | Usuário master do RDS | O mesmo configurado no db-infra (padrão: `admindb`) |
| `JWT_SECRET` | Chave de assinatura JWT | Deve ser **idêntico** ao secret `JWT_SECRET` do repo principal |

> ⚠️ **AWS Academy:** Os três secrets AWS expiram a cada ~4h. Atualize-os antes de cada pipeline.
>
> ⚠️ **JWT_SECRET crítico:** Se este valor for diferente do `JWT_SECRET` configurado no `oficina-api`, todos os tokens gerados pela Lambda serão rejeitados pelo Spring Security.

---

## Detalhes de cada job

### Job 1 — Build & Test

1. Checkout do código
2. Instala JDK 21 (Amazon Corretto) com cache Maven
3. Executa `mvn -B verify` na pasta `lambda-authorizer/`
   - Compila o código
   - Executa todos os testes unitários (JUnit 5 + Mockito)
   - Gera o fat JAR via `maven-shade-plugin`
4. Sobe o JAR como artefato `lambda-jar` (retenção: 1 dia)

### Job 2 — Build & Push Docker Image

1. Baixa o artefato `lambda-jar` do Job 1
2. Configura credenciais AWS e faz login no ECR
3. Define a tag como SHORT_SHA (7 chars do commit hash)
4. Build da imagem Docker a partir do `lambda-authorizer/Dockerfile`
5. Push com duas tags: `SHORT_SHA` e `latest`
6. Exporta `image_tag` e `ecr_registry` como outputs para o Job 3

### Job 3 — Deploy Terraform → Lambda

1. Configura credenciais AWS
2. Instala Terraform (latest)
3. **Obtém Account ID** via `aws sts get-caller-identity`
4. **Busca endpoint do RDS** via `aws rds describe-db-instances`
5. **Recupera senha do RDS** via AWS Secrets Manager — mascarada com `::add-mask::`
6. `terraform init` (sem flags de backend — usa `backend.tf` direto)
7. `terraform plan` com todas as variáveis sensíveis
8. `terraform apply -auto-approve` com as mesmas variáveis
9. **Verifica status da Lambda**: `aws lambda get-function` → espera `State == Active`

---

## Como a senha do banco é usada

A pipeline **nunca armazena a senha** — ela é recuperada dinamicamente a cada execução:

```
Secrets Manager → pipeline → terraform -var="db_password=..."
                                       → Lambda env var DB_PASSWORD
```

Fluxo no Job 3:
```bash
SECRET_ARN=$(aws rds describe-db-instances \
  --db-instance-identifier oficina-rds \
  --query 'DBInstances[0].MasterUserSecret.SecretArn' \
  --output text)

DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id "$SECRET_ARN" \
  --query 'SecretString' --output text \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['password'])")

echo "::add-mask::$DB_PASSWORD"   # Mascara nos logs
```

---

## Atualizar secrets do AWS Academy

1. Acesse o laboratório → **AWS Details**
2. Copie os três valores AWS
3. No GitHub: **Settings → Secrets and variables → Actions**
4. Atualize: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`
5. Faça push ou re-run do workflow

---

## Verificar o resultado da pipeline

```bash
# Status da Lambda após o deploy
aws lambda get-function \
  --function-name oficina-auth-lambda \
  --region us-east-1 \
  --query 'Configuration.{State:State,ImageUri:Code.ImageUri}' \
  --output table

# Testar o endpoint de login
API_URL=$(aws apigatewayv2 get-apis \
  --region us-east-1 \
  --query 'Items[?Name==`oficina-api-gateway`].ApiEndpoint' \
  --output text)

curl -s -X POST "$API_URL/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"secret123"}'
```
