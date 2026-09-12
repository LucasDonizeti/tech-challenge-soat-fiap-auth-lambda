# Comandos Úteis

Referência rápida de Maven, Docker, AWS CLI e Terraform para operar a Lambda Authorizer.

---

## Maven

```bash
# Trabalhar sempre na pasta do projeto Java
cd lambda-authorizer/

# Compilar e testar (ciclo completo)
mvn verify

# Apenas compilar
mvn compile

# Apenas testes
mvn test

# Gerar fat JAR (maven-shade-plugin)
mvn package

# Gerar JAR pulando testes
mvn package -DskipTests

# Limpar target/
mvn clean

# Limpar + gerar JAR
mvn clean package -DskipTests

# Ver dependências em árvore
mvn dependency:tree

# Verificar vulnerabilidades OWASP (demora ~2 min)
mvn verify -Dowasp.skip=false
```

---

## Docker

```bash
# Build da imagem (a partir da raiz do repo)
docker build \
  -f lambda-authorizer/Dockerfile \
  -t auth-lambda:local \
  ./lambda-authorizer

# Rodar localmente como Lambda
docker run --rm -p 9000:8080 \
  -e DB_URL="jdbc:mysql://localhost:3306/oficina" \
  -e DB_USER="user" \
  -e DB_PASSWORD="password" \
  -e JWT_SECRET="minha-chave-secreta-256-bits" \
  -e SPRING_SECURITY_USER_NAME="admin" \
  -e SPRING_SECURITY_USER_PASSWORD="secret123" \
  auth-lambda:local

# Testar o container Lambda local (em outro terminal)
curl -s -X POST http://localhost:9000/2015-03-31/functions/function/invocations \
  -H "Content-Type: application/json" \
  -d '{
    "httpMethod": "POST",
    "path": "/v1/auth/login",
    "headers": {"Content-Type": "application/json"},
    "body": "{\"username\":\"admin\",\"password\":\"secret123\"}"
  }' | python3 -m json.tool

# Listar imagens locais
docker images | grep auth-lambda

# Remover imagem local
docker rmi auth-lambda:local
```

---

## AWS CLI — ECR

```bash
# Obter URL do repositório ECR
ECR_URL=$(aws ecr describe-repositories \
  --repository-names auth-lambda \
  --region us-east-1 \
  --query 'repositories[0].repositoryUri' \
  --output text)

echo "ECR URL: $ECR_URL"

# Login no ECR
aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin "$ECR_URL"

# Listar imagens no ECR
aws ecr list-images \
  --repository-name auth-lambda \
  --region us-east-1 \
  --query 'imageIds[*].{Tag:imageTag}' \
  --output table

# Push manual de imagem
docker tag auth-lambda:local "$ECR_URL:latest"
docker push "$ECR_URL:latest"
```

---

## AWS CLI — Lambda

```bash
# Ver configuração da Lambda
aws lambda get-function \
  --function-name oficina-auth-lambda \
  --region us-east-1

# Ver apenas status e imagem
aws lambda get-function \
  --function-name oficina-auth-lambda \
  --region us-east-1 \
  --query 'Configuration.{State:State,LastModified:LastModified,MemorySize:MemorySize,Timeout:Timeout}' \
  --output table

# Ver variáveis de ambiente (apenas nomes, não valores sensíveis)
aws lambda get-function-configuration \
  --function-name oficina-auth-lambda \
  --region us-east-1 \
  --query 'Environment.Variables' \
  --output table

# Ver configuração de VPC
aws lambda get-function-configuration \
  --function-name oficina-auth-lambda \
  --region us-east-1 \
  --query 'VpcConfig' \
  --output table

# Invocar a Lambda diretamente via CLI (para debug)
aws lambda invoke \
  --function-name oficina-auth-lambda \
  --region us-east-1 \
  --cli-binary-format raw-in-base64-out \
  --payload '{
    "version": "2.0",
    "routeKey": "POST /v1/auth/login",
    "rawPath": "/v1/auth/login",
    "headers": {"content-type": "application/json"},
    "body": "{\"username\":\"admin\",\"password\":\"secret123\"}",
    "isBase64Encoded": false
  }' \
  /tmp/lambda-response.json && cat /tmp/lambda-response.json | python3 -m json.tool

# Ver logs da Lambda no CloudWatch (últimas 10 mensagens)
LOG_GROUP="/aws/lambda/oficina-auth-lambda"

aws logs get-log-events \
  --log-group-name "$LOG_GROUP" \
  --log-stream-name "$(aws logs describe-log-streams \
    --log-group-name "$LOG_GROUP" \
    --order-by LastEventTime \
    --descending \
    --limit 1 \
    --query 'logStreams[0].logStreamName' \
    --output text)" \
  --limit 20 \
  --query 'events[*].message' \
  --output text

# Forçar atualização da imagem (sem mudar a tag)
aws lambda update-function-code \
  --function-name oficina-auth-lambda \
  --image-uri "$ECR_URL:latest" \
  --region us-east-1
```

---

## AWS CLI — API Gateway

```bash
# Obter URL do API Gateway
API_URL=$(aws apigatewayv2 get-apis \
  --region us-east-1 \
  --query 'Items[?Name==`oficina-api-gateway`].ApiEndpoint' \
  --output text)

echo "API URL: $API_URL"

# Obter ID do API Gateway
API_ID=$(aws apigatewayv2 get-apis \
  --region us-east-1 \
  --query 'Items[?Name==`oficina-api-gateway`].ApiId' \
  --output text)

# Listar todas as rotas
aws apigatewayv2 get-routes \
  --api-id "$API_ID" \
  --region us-east-1 \
  --query 'Items[*].{RouteKey:RouteKey,Target:Target}' \
  --output table

# Verificar a integração da Lambda
aws apigatewayv2 get-integrations \
  --api-id "$API_ID" \
  --region us-east-1 \
  --query 'Items[*].{Type:IntegrationType,URI:IntegrationUri}' \
  --output table
```

---

## Testar o endpoint de autenticação

```bash
# Obter URL do API Gateway
API_URL=$(aws apigatewayv2 get-apis \
  --region us-east-1 \
  --query 'Items[?Name==`oficina-api-gateway`].ApiEndpoint' \
  --output text)

# Login como admin
curl -s -X POST "$API_URL/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"secret123"}' \
  | python3 -m json.tool

# Login como cliente (CPF)
curl -s -X POST "$API_URL/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"12345678901","password":"senha-do-cliente"}' \
  | python3 -m json.tool

# Salvar o token em variável
TOKEN=$(curl -s -X POST "$API_URL/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"secret123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

echo "Token: ${TOKEN:0:50}..."

# Usar o token em endpoint protegido
curl -s "$API_URL/v1/admin/clientes?page=0&size=5" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool

# Testar com token inválido (deve retornar 401)
curl -s "$API_URL/v1/admin/clientes" \
  -H "Authorization: Bearer token-invalido" \
  | python3 -m json.tool

# Testar sem token (deve retornar 401)
curl -s "$API_URL/v1/admin/clientes" \
  | python3 -m json.tool
```

---

## Terraform

```bash
cd terraform/

# Inicializar
terraform init

# Planejar
terraform plan \
  -var="account_id=$(aws sts get-caller-identity --query Account --output text)" \
  -var="db_url=jdbc:mysql://<RDS_ENDPOINT>:3306/oficina" \
  -var="db_user=admindb" \
  -var="db_password=<SENHA>" \
  -var="jwt_secret=<CHAVE_JWT>" \
  -var="image_tag=latest"

# Aplicar
terraform apply \
  -var="account_id=$(aws sts get-caller-identity --query Account --output text)" \
  -var="db_url=jdbc:mysql://<RDS_ENDPOINT>:3306/oficina" \
  -var="db_user=admindb" \
  -var="db_password=<SENHA>" \
  -var="jwt_secret=<CHAVE_JWT>" \
  -var="image_tag=latest"

# Ver outputs
terraform output

# Destruir
terraform destroy \
  -var="account_id=$(aws sts get-caller-identity --query Account --output text)" \
  -var="db_url=jdbc:mysql://<RDS_ENDPOINT>:3306/oficina" \
  -var="db_user=admindb" \
  -var="db_password=<SENHA>" \
  -var="jwt_secret=<CHAVE_JWT>" \
  -var="image_tag=latest"
```

---

## Cheklist pós-deploy

```bash
# 1. Lambda ativa
aws lambda get-function \
  --function-name oficina-auth-lambda \
  --query 'Configuration.State' \
  --output text
# Esperado: Active

# 2. Rota POST /v1/auth/login existe no API Gateway
API_ID=$(aws apigatewayv2 get-apis --region us-east-1 \
  --query 'Items[?Name==`oficina-api-gateway`].ApiId' --output text)
aws apigatewayv2 get-routes --api-id "$API_ID" \
  --query 'Items[?RouteKey==`POST /v1/auth/login`].RouteKey' \
  --output text
# Esperado: POST /v1/auth/login

# 3. Login funciona
API_URL=$(aws apigatewayv2 get-apis --region us-east-1 \
  --query 'Items[?Name==`oficina-api-gateway`].ApiEndpoint' --output text)
curl -s -o /dev/null -w "%{http_code}" \
  -X POST "$API_URL/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"secret123"}'
# Esperado: 200
```
