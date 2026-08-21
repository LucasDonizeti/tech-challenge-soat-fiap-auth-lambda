variable "region" {
  description = "Região AWS"
  type        = string
  default     = "us-east-1"
}

variable "app_name" {
  description = "Nome da aplicação (usado como prefixo nos recursos)"
  type        = string
  default     = "oficina"
}

variable "environment" {
  description = "Ambiente de execução"
  type        = string
  default     = "prod"
}

# Variáveis de ambiente que a Lambda em Java precisa
variable "db_url" {
  description = "URL de conexão com o banco de dados"
  type        = string
  sensitive   = true
}

variable "db_user" {
  description = "Usuário do banco de dados"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "Senha do banco de dados"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Segredo para geração de tokens JWT"
  type        = string
  sensitive   = true
}

variable "image_tag" {
  description = "Tag da imagem no ECR gerada pelo pipeline"
  type        = string
  default     = "latest"
}

variable "s3_bucket_k8s_infra" {
  description = "Nome do bucket S3 onde está o terraform.tfstate do k8s-infra"
  type        = string
  default     = "bucket-tfstate-1029"
}

variable "s3_key_k8s_infra" {
  description = "Caminho do estado do k8s-infra no bucket S3"
  type        = string
  default     = "k8s/terraform.tfstate"
}

variable "subnet_ids" {
  description = "IDs das subnets onde a Lambda será executada (para acesso ao RDS)"
  type        = list(string)
  default     = []
}

variable "security_group_ids" {
  description = "IDs dos security groups para a Lambda (para acesso ao RDS)"
  type        = list(string)
  default     = []
}

variable "account_id" {
  description = "ID da Conta AWS"
  type        = string
}