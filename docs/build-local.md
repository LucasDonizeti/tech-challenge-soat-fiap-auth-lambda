# Build e Testes Locais

---

## Compilar e testar com Maven

```bash
cd lambda-authorizer/

# Compilar e executar todos os testes
mvn verify

# Apenas compilar (sem testes)
mvn compile

# Apenas testes
mvn test

# Gerar o fat JAR (necessário para build Docker)
mvn package

# Pular testes ao gerar o JAR
mvn package -DskipTests
```

O JAR gerado fica em `lambda-authorizer/target/lambda.authorizer-0.0.1-SNAPSHOT.jar`.

---

## Build da imagem Docker

```bash
# A partir da raiz do repositório
docker build \
  -f lambda-authorizer/Dockerfile \
  -t auth-lambda:local \
  ./lambda-authorizer

# Verificar se a imagem foi criada
docker images | grep auth-lambda
```

O `Dockerfile` usa a imagem base oficial da AWS Lambda para Java 21:

```dockerfile
FROM public.ecr.aws/lambda/java:21
COPY target/*.jar ${LAMBDA_TASK_ROOT}/lib/
CMD [ "com.techchallenge.lambda.authorizer.application.ports.AuthLambdaHandler::handleRequest" ]
```

---

## Testar a Lambda localmente com Docker

O runtime Lambda da AWS pode ser emulado localmente com o container oficial:

```bash
# 1. Buildar a imagem
docker build -f lambda-authorizer/Dockerfile -t auth-lambda:local ./lambda-authorizer

# 2. Rodar o container (o runtime escuta na porta 9000)
docker run --rm -p 9000:8080 \
  -e DB_URL="jdbc:mysql://localhost:3306/oficina" \
  -e DB_USER="user" \
  -e DB_PASSWORD="password" \
  -e JWT_SECRET="minha-chave-secreta-256-bits" \
  -e SPRING_SECURITY_USER_NAME="admin" \
  -e SPRING_SECURITY_USER_PASSWORD="secret123" \
  auth-lambda:local

# 3. Em outro terminal, enviar uma requisição de teste (login admin)
curl -s -X POST http://localhost:9000/2015-03-31/functions/function/invocations \
  -H "Content-Type: application/json" \
  -d '{
    "httpMethod": "POST",
    "path": "/v1/auth/login",
    "headers": {"Content-Type": "application/json"},
    "body": "{\"username\":\"admin\",\"password\":\"secret123\"}"
  }' | python3 -m json.tool
```

**Resposta esperada (200 OK):**
```json
{
  "statusCode": 200,
  "headers": {
    "Content-Type": "application/json",
    "x-correlation-id": "uuid-gerado"
  },
  "body": "{\"token\":\"eyJhbGciOiJIUzI1NiJ9...\",\"type\":\"Bearer\",\"username\":\"admin\"}"
}
```

**Teste com credenciais inválidas:**
```bash
curl -s -X POST http://localhost:9000/2015-03-31/functions/function/invocations \
  -H "Content-Type: application/json" \
  -d '{
    "httpMethod": "POST",
    "path": "/v1/auth/login",
    "headers": {"Content-Type": "application/json"},
    "body": "{\"username\":\"admin\",\"password\":\"senha-errada\"}"
  }' | python3 -m json.tool
```

**Resposta esperada (401):**
```json
{
  "statusCode": 401,
  "body": "{\"error\": \"Credenciais inválidas\"}"
}
```

---

## Testar via API Gateway (após deploy)

Após o deploy completo, use o endpoint real do API Gateway:

```bash
# Obter URL do API Gateway
API_URL=$(aws apigatewayv2 get-apis \
  --region us-east-1 \
  --query 'Items[?Name==`oficina-api-gateway`].ApiEndpoint' \
  --output text)

echo "API URL: $API_URL"

# Login como admin
curl -s -X POST "$API_URL/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"secret123"}' \
  | python3 -m json.tool

# Salvar o token para uso subsequente
TOKEN=$(curl -s -X POST "$API_URL/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"secret123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

echo "Token: $TOKEN"

# Usar o token em um endpoint protegido
curl -s "$API_URL/v1/admin/clientes" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool
```

---

## Análise de qualidade com SonarQube (local)

O projeto tem integração com SonarQube via `sonar-maven-plugin`. Para rodar localmente, use o Docker Compose do repositório principal:

```bash
# A partir do repositório principal (tech-challenge-soat-fiap)
docker-compose up sonar -d

# Aguarde o SonarQube inicializar (~1–2 min) e acesse: http://localhost:9000
# Usuário: admin / Senha: admin (troque no primeiro acesso)

# Rodar análise da Lambda
cd lambda-authorizer/
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<TOKEN_DO_SONAR>
```
