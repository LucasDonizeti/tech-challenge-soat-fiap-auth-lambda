# tech-challenge-soat-fiap-auth-lambda

Projeto de infraestrutura Terraform para deploy da Lambda de autenticação que integra com o API Gateway do projeto k8s-infra.

## Estrutura

- `lambda-authorizer/` - Código fonte da Lambda em Java (Spring Boot)
- `terraform/` - Infraestrutura Terraform para deploy da Lambda

## Como Funciona

1. Consulta o estado remoto do projeto `k8s-infra` via S3 para obter:
   - URL do repositório ECR (`ecr_auth_lambda_url`)
   - ID do API Gateway (`api_gateway_id`)
   - ARN de execução do API Gateway (`api_gateway_execution_arn`)

2. Cria a função AWS Lambda usando imagem de container do ECR

3. Configura permissões IAM para o API Gateway invocar a Lambda

4. Cria integração da Lambda com o API Gateway

5. Adiciona rota `POST /v1/auth` no API Gateway que direciona para a Lambda

## Pipeline CI/CD

A pipeline do GitHub Actions executa os seguintes passos:

1. **Build & Test**: Compila o projeto Java com Maven e executa testes
2. **Build & Push Docker Image**: Cria a imagem Docker usando o JAR pré-compilado e publica no ECR
3. **Deploy Terraform**: Aplica a infraestrutura Terraform para criar/atualizar a Lambda

**Nota Técnica**: O Dockerfile está localizado em `lambda-authorizer/Dockerfile` e usa o JAR pré-compilado pelo Job 1. O comando `docker build` usa a flag `-f` para especificar explicitamente o caminho do Dockerfile.

## Rotas do API Gateway

- `POST /v1/auth` → Lambda de autenticação
- `ANY /{proxy+}` → EKS (aplicação principal)

## Pré-requisitos

- Terraform >= 1.6.0
- AWS CLI configurado
- Bucket S3 com estado do k8s-infra
- Imagem da Lambda publicada no ECR

## Como Usar

1. Configure as variáveis:
```bash
cp terraform/terraform.tfvars.example terraform/terraform.tfvars
# Edite terraform.tfvars com seus valores reais
```

2. Inicialize o Terraform:
```bash
cd terraform
terraform init
```

3. Planeje e aplique:
```bash
terraform plan
terraform apply
```

## Variáveis Principais

- `db_url`, `db_user`, `db_password` - Credenciais do banco de dados
- `jwt_secret` - Segredo para geração de tokens JWT
- `image_tag` - Tag da imagem no ECR a ser utilizada
- `s3_bucket_k8s_infra` - Bucket S3 com estado do k8s-infra (default: bucket-tfstate-1029)
- `s3_key_k8s_infra` - Caminho do estado do k8s-infra no bucket (default: k8s/terraform.tfstate)
- `subnet_ids` - IDs das subnets onde a Lambda será executada (opcional, para acesso VPC)
- `security_group_ids` - IDs dos security groups para a Lambda (opcional, para acesso VPC)

## Configuração de VPC

A Lambda pode ser configurada de duas formas:

1. **Sem VPC (padrão)**: A Lambda roda fora da VPC e acessa o RDS via internet
   - Deixe `subnet_ids` e `security_group_ids` vazios (`[]`)
   - Apenas a política `AWSLambdaBasicExecutionRole` é anexada

2. **Com VPC**: A Lambda roda dentro da VPC para acesso direto ao RDS em subnets privadas
   - Preencha `subnet_ids` com as subnets privadas do k8s-infra
   - Preencha `security_group_ids` ou deixe vazio para criar automaticamente
   - A política `AWSLambdaVPCAccessExecutionRole` também é anexada
   - Um security group é criado automaticamente se `security_group_ids` estiver vazio