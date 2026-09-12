# Pré-requisitos

## Ferramentas necessárias

| Ferramenta | Versão mínima | Instalação |
|-----------|--------------|-----------|
| Java (Corretto) | 21 | https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html |
| Maven | 3.9.x | https://maven.apache.org/download.cgi |
| Docker | 24+ | https://docs.docker.com/engine/install/ |
| AWS CLI | v2 | https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html |
| Terraform | >= 1.6.0 | https://developer.hashicorp.com/terraform/install |

---

## 1. Dependências de infraestrutura

Este repositório depende de dois outros estarem provisionados:

```bash
# Verificar k8s-infra (ECR auth-lambda + API Gateway)
aws s3 ls s3://bucket-tfstate-1029/k8s/terraform.tfstate

# Verificar db-infra (RDS MySQL)
aws s3 ls s3://bucket-tfstate-1029/db/terraform.tfstate

# Verificar se o RDS está disponível
aws rds describe-db-instances \
  --db-instance-identifier oficina-rds \
  --query 'DBInstances[0].DBInstanceStatus' \
  --output text
# Esperado: available
```

---

## 2. Configurar AWS CLI

### Ambiente normal

```bash
aws configure
```

### AWS Academy (sessão temporária)

```bash
aws configure set aws_access_key_id     "ASIA..."
aws configure set aws_secret_access_key "..."
aws configure set aws_session_token     "..."
aws configure set region                "us-east-1"
```

Ou via variáveis de ambiente:

```bash
export AWS_ACCESS_KEY_ID="ASIA..."
export AWS_SECRET_ACCESS_KEY="..."
export AWS_SESSION_TOKEN="..."
export AWS_DEFAULT_REGION="us-east-1"
```

> ⚠️ **AWS Academy:** credenciais expiram a cada ~4h.

Verifique:

```bash
aws sts get-caller-identity
```

---

## 3. Variáveis do Terraform

Copie o arquivo de exemplo e preencha com os valores reais:

```bash
cp terraform/terraform.tfvars.example terraform/terraform.tfvars
```

Edite `terraform/terraform.tfvars`:

```hcl
# Conta AWS (obrigatório)
account_id = "123456789012"

# Credenciais de banco (obrigatório)
db_url      = "jdbc:mysql://oficina-rds.xyz.us-east-1.rds.amazonaws.com:3306/oficina"
db_user     = "admindb"
db_password = "SenhaGeradaPeloSecretsManager"

# Segredo JWT — deve ser IDÊNTICO ao configurado no oficina-api
jwt_secret  = "minha-chave-secreta-256-bits"

# Tag da imagem — preenchida automaticamente pela pipeline
image_tag = "latest"
```

> O arquivo `terraform.tfvars` está no `.gitignore` — nunca o comite.

---

## 4. Como obter cada valor

### `account_id`

```bash
aws sts get-caller-identity --query Account --output text
```

### `db_url`

```bash
RDS_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier oficina-rds \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)
echo "jdbc:mysql://${RDS_ENDPOINT}:3306/oficina"
```

### `db_user` e `db_password`

```bash
SECRET_ARN=$(aws rds describe-db-instances \
  --db-instance-identifier oficina-rds \
  --query 'DBInstances[0].MasterUserSecret.SecretArn' \
  --output text)

# Usuário
aws secretsmanager get-secret-value --secret-id "$SECRET_ARN" \
  --query 'SecretString' --output text \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['username'])"

# Senha
aws secretsmanager get-secret-value --secret-id "$SECRET_ARN" \
  --query 'SecretString' --output text \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['password'])"
```

### `jwt_secret`

O mesmo valor configurado no GitHub Secret `JWT_SECRET` do repositório principal. Deve ser uma string com pelo menos 32 caracteres para garantir segurança com HS256.

```bash
# Gerar uma chave segura (se ainda não tiver)
openssl rand -base64 32
```

### `image_tag`

Deixe `latest` para o primeiro deploy manual. A pipeline substitui automaticamente pelo SHORT_SHA de cada commit.
