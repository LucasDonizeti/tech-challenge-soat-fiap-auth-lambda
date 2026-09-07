# Recursos Provisionados

Este repositório cria **4 recursos** na AWS, todos integrados ao API Gateway e à VPC já existentes do `k8s-infra`.

---

## Visão geral

| Recurso | Nome | Tipo Terraform |
|---------|------|----------------|
| Lambda Function | `oficina-auth-lambda` | `aws_lambda_function` |
| Lambda Permission | — | `aws_lambda_permission` |
| API GW Integration | — | `aws_apigatewayv2_integration` |
| API GW Route | `POST /v1/auth/login` | `aws_apigatewayv2_route` |

---

## Lambda Function — `oficina-auth-lambda`

| Parâmetro | Valor |
|-----------|-------|
| Nome | `oficina-auth-lambda` |
| Runtime | Container Image (Java 21) |
| Imagem | `<account>.dkr.ecr.us-east-1.amazonaws.com/auth-lambda:<tag>` |
| IAM Role | `LabRole` (AWS Academy) |
| Memória | 512 MB |
| Timeout | 15 segundos |
| Arquitetura | x86_64 |
| VPC | Private Subnets do k8s-infra |
| Security Group | SG dos nodes EKS (permite acesso 3306 no RDS) |

**Variáveis de ambiente injetadas:**

| Variável | Origem | Descrição |
|----------|--------|-----------|
| `DB_URL` | Terraform `-var` | `jdbc:mysql://<rds-endpoint>:3306/oficina` |
| `DB_USER` | GitHub Secret `DB_USERNAME` | Usuário master do RDS |
| `DB_PASSWORD` | AWS Secrets Manager (dinâmico) | Senha do banco |
| `JWT_SECRET` | GitHub Secret `JWT_SECRET` | Chave de assinatura do JWT |

---

## Lambda Permission

Permite que o API Gateway invoque a Lambda:

```
Principal: apigateway.amazonaws.com
Action:    lambda:InvokeFunction
Source:    arn:aws:execute-api:us-east-1:<account>:<api-id>/*/*
```

---

## API Gateway — Integração e Rota

A Lambda é integrada ao API Gateway **já existente** (criado pelo k8s-infra) com uma rota dedicada:

| Campo | Valor |
|-------|-------|
| API ID | Lido do remote state do k8s-infra |
| Route key | `POST /v1/auth/login` |
| Integration type | `AWS_PROXY` |
| Payload format version | `2.0` |
| Invoke URL | `https://<api-id>.execute-api.us-east-1.amazonaws.com/v1/auth/login` |

> O stage `$default` com `auto_deploy = true` (criado pelo k8s-infra) garante que a nova rota é automaticamente publicada sem necessidade de redeploy manual do stage.

---

## Topologia de rede

```
Internet
    │
    ▼
API Gateway HTTP v2  (public endpoint)
    │
    │  POST /v1/auth/login → AWS_PROXY
    ▼
Lambda: oficina-auth-lambda
    │  VPC config: Private Subnets (10.0.1.0/24, 10.0.2.0/24)
    │  SG: eks_node_security_group
    │
    └──► RDS MySQL — oficina-rds (port 3306)
         Database Subnets (10.0.201.0/24, 10.0.202.0/24)
```

**Por que a Lambda está na VPC?**
A Lambda precisa de acesso ao RDS MySQL que está em subnets privadas sem roteamento público. Colocar a Lambda nas mesmas private subnets com o security group dos nodes EKS garante que ela consegue abrir conexão JDBC na porta 3306 do RDS.

---

## Outputs exportados

| Output | Descrição |
|--------|-----------|
| `lambda_arn` | ARN completo da função Lambda |
| `lambda_function_name` | Nome da função (`oficina-auth-lambda`) |
| `lambda_invoke_arn` | ARN de invocação (usado pelo API Gateway) |

```bash
# Ver outputs após o apply
cd terraform/
terraform output
```

---

## Custo estimado

| Recurso | Custo |
|---------|-------|
| Lambda | Primeiros 1M requests/mês gratuitos. Depois: $0.20/M requests + $0.0000166667/GB-segundo |
| API Gateway | $1.00/milhão de requisições |
| **Total (volume acadêmico)** | **~$0.00** (dentro do free tier) |

> A Lambda é cobrada apenas quando invocada — sem custo enquanto parada.

---

## Verificar após deploy

```bash
# Status da Lambda
aws lambda get-function \
  --function-name oficina-auth-lambda \
  --region us-east-1 \
  --query 'Configuration.{State:State,LastModified:LastModified}' \
  --output table

# Ver as variáveis de ambiente (nomes, não valores)
aws lambda get-function-configuration \
  --function-name oficina-auth-lambda \
  --region us-east-1 \
  --query 'Environment.Variables' \
  --output table

# Listar rotas do API Gateway
API_ID=$(aws apigatewayv2 get-apis \
  --region us-east-1 \
  --query 'Items[?Name==`oficina-api-gateway`].ApiId' \
  --output text)

aws apigatewayv2 get-routes \
  --api-id "$API_ID" \
  --region us-east-1 \
  --query 'Items[*].{RouteKey:RouteKey,Target:Target}' \
  --output table
# Deve mostrar: POST /v1/auth/login + ANY /{proxy+}
```
