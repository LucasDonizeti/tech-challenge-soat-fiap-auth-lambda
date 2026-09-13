# Provisionamento Manual

Siga este guia para provisionar a Lambda e a integração com o API Gateway localmente, sem a pipeline.

> **Pré-requisitos:**
> - [k8s-infra](../../tech-challenge-soat-fiap-k8s-infra) provisionado (ECR `auth-lambda` + API Gateway criados)
> - [db-infra](../../tech-challenge-soat-fiap-db-infra) provisionado (RDS disponível)
> - Imagem Docker da Lambda publicada no ECR `auth-lambda`

---

## Passo 1 — Compilar e buildar a imagem

```bash
cd lambda-authorizer/

# Compilar e gerar o fat JAR
mvn package -DskipTests

cd ..
```

---

## Passo 2 — Publicar a imagem no ECR

```bash
# Obter a URL do ECR
ECR_URL=$(aws ecr describe-repositories \
  --repository-names auth-lambda \
  --region us-east-1 \
  --query 'repositories[0].repositoryUri' \
  --output text)

echo "ECR URL: $ECR_URL"

# Login no ECR
aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin "$ECR_URL"

# Build da imagem
docker build \
  -f lambda-authorizer/Dockerfile \
  -t "$ECR_URL:latest" \
  ./lambda-authorizer

# Push
docker push "$ECR_URL:latest"
```

---

## Passo 3 — Preparar variáveis do Terraform

```bash
# Obter o Account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Obter o endpoint do RDS
RDS_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier oficina-rds \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

# Obter a senha do banco via Secrets Manager
SECRET_ARN=$(aws rds describe-db-instances \
  --db-instance-identifier oficina-rds \
  --query 'DBInstances[0].MasterUserSecret.SecretArn' \
  --output text)

DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id "$SECRET_ARN" \
  --query 'SecretString' \
  --output text \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['password'])")

echo "Account:  $ACCOUNT_ID"
echo "RDS Host: $RDS_ENDPOINT"
```

---

## Passo 4 — Inicializar o Terraform

```bash
cd terraform/

terraform init
```

O `backend.tf` já tem o bucket e a key configurados. O `terraform init` vai configurar automaticamente.

---

## Passo 5 — Planejar

```bash
terraform plan \
  -var="account_id=$ACCOUNT_ID" \
  -var="db_url=jdbc:mysql://${RDS_ENDPOINT}:3306/oficina" \
  -var="db_user=admindb" \
  -var="db_password=$DB_PASSWORD" \
  -var="jwt_secret=SUA_CHAVE_JWT" \
  -var="image_tag=latest"
```

Revise o plano. Espere ver a criação de:
- `aws_lambda_function.auth_lambda`
- `aws_lambda_permission.apigw_lambda`
- `aws_apigatewayv2_integration.auth_lambda_integration`
- `aws_apigatewayv2_route.auth_route`

---

## Passo 6 — Aplicar

```bash
terraform apply \
  -var="account_id=$ACCOUNT_ID" \
  -var="db_url=jdbc:mysql://${RDS_ENDPOINT}:3306/oficina" \
  -var="db_user=admindb" \
  -var="db_password=$DB_PASSWORD" \
  -var="jwt_secret=SUA_CHAVE_JWT" \
  -var="image_tag=latest"
```

Confirme com `yes`.

> ⏱️ **Tempo esperado:** 1–3 minutos.

---

## Passo 7 — Verificar o deploy

```bash
# Ver outputs
terraform output

# Status da Lambda
aws lambda get-function \
  --function-name oficina-auth-lambda \
  --query 'Configuration.{State:State,LastModified:LastModified,MemorySize:MemorySize}' \
  --output table

# Testar o endpoint
API_URL=$(aws apigatewayv2 get-apis \
  --region us-east-1 \
  --query 'Items[?Name==`oficina-api-gateway`].ApiEndpoint' \
  --output text)

curl -s -X POST "$API_URL/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"secret123"}' \
  | python3 -m json.tool
```

**Resposta esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "username": "admin"
}
```

> ⚠️ **Cold start:** A primeira chamada após um período de inatividade pode levar 2–5 segundos por causa da inicialização da JVM. As chamadas subsequentes são rápidas.

---

## Atualizar a Lambda (nova versão da imagem)

```bash
# 1. Rebuildar e fazer push de nova imagem com tag específica
SHORT_SHA=$(git rev-parse --short HEAD)

docker build -f lambda-authorizer/Dockerfile -t "$ECR_URL:$SHORT_SHA" ./lambda-authorizer
docker push "$ECR_URL:$SHORT_SHA"

# 2. Re-aplicar o Terraform com a nova tag
cd terraform/
terraform apply \
  -var="account_id=$ACCOUNT_ID" \
  -var="db_url=jdbc:mysql://${RDS_ENDPOINT}:3306/oficina" \
  -var="db_user=admindb" \
  -var="db_password=$DB_PASSWORD" \
  -var="jwt_secret=SUA_CHAVE_JWT" \
  -var="image_tag=$SHORT_SHA"
```

---

## Destruir

```bash
cd terraform/
terraform destroy \
  -var="account_id=$ACCOUNT_ID" \
  -var="db_url=jdbc:mysql://${RDS_ENDPOINT}:3306/oficina" \
  -var="db_user=admindb" \
  -var="db_password=$DB_PASSWORD" \
  -var="jwt_secret=SUA_CHAVE_JWT" \
  -var="image_tag=latest"
```

> Destruir a Lambda remove também a rota `POST /v1/auth/login` do API Gateway — o endpoint de login deixará de funcionar.
