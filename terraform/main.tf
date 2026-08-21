# ------------------------------------------------------------------------------
# 1. Leitura do Estado Remoto do k8s-infra
# ------------------------------------------------------------------------------
data "terraform_remote_state" "k8s_infra" {
  backend = "s3"

  config = {
    bucket = var.s3_bucket_k8s_infra
    key    = var.s3_key_k8s_infra
    region = var.region
  }
}


# Security Group para a Lambda (se precisar acessar recursos na VPC)
resource "aws_security_group" "lambda_sg" {
  count       = length(var.subnet_ids) > 0 && length(var.security_group_ids) == 0 ? 1 : 0
  name        = "${var.app_name}-auth-lambda-sg"
  description = "Security group para Lambda de autenticação"
  vpc_id      = data.terraform_remote_state.k8s_infra.outputs.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Project     = var.app_name
    Environment = var.environment
  }
}

locals {
  lambda_security_group_ids = length(var.security_group_ids) > 0 ? var.security_group_ids : (length(aws_security_group.lambda_sg) > 0 ? [aws_security_group.lambda_sg[0].id] : [])
}

# ------------------------------------------------------------------------------
# 3. Função AWS Lambda (Container Image)
# ------------------------------------------------------------------------------
resource "aws_lambda_function" "auth_lambda" {
  function_name = "${var.app_name}-auth-lambda"
  role          = "arn:aws:iam::${var.account_id}:role/LabRole"
  package_type  = "Image"

  # Usa a URL do ECR exportada pelo k8s-infra
  image_uri = "${data.terraform_remote_state.k8s_infra.outputs.ecr_auth_lambda_url}:${var.image_tag}"

  memory_size = 512
  timeout     = 15

  # Configuração de VPC (opcional - para acesso ao RDS em subnets privadas)
  dynamic "vpc_config" {
    for_each = length(var.subnet_ids) > 0 ? [1] : []
    content {
      subnet_ids         = var.subnet_ids
      security_group_ids = local.lambda_security_group_ids
    }
  }

  environment {
    variables = {
      DB_URL      = var.db_url
      DB_USER     = var.db_user
      DB_PASSWORD = var.db_password
      JWT_SECRET  = var.jwt_secret
    }
  }

  tags = {
    Project     = var.app_name
    Environment = var.environment
  }
}

# ------------------------------------------------------------------------------
# 4. Permissão para o API Gateway Invocar a Lambda
# ------------------------------------------------------------------------------
resource "aws_lambda_permission" "apigw_lambda" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.auth_lambda.function_name
  principal     = "apigateway.amazonaws.com"

  # Libera chamadas vindas do API Gateway do k8s-infra
  source_arn = "${data.terraform_remote_state.k8s_infra.outputs.api_gateway_execution_arn}/*/*"
}

# ------------------------------------------------------------------------------
# 5. Integração da Lambda com o API Gateway
# ------------------------------------------------------------------------------
resource "aws_apigatewayv2_integration" "auth_lambda_integration" {
  api_id                 = data.terraform_remote_state.k8s_infra.outputs.api_gateway_id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.auth_lambda.invoke_arn
  payload_format_version = "2.0"
}

# ------------------------------------------------------------------------------
# 6. Rota POST /v1/auth no API Gateway
# ------------------------------------------------------------------------------
resource "aws_apigatewayv2_route" "auth_route" {
  api_id    = data.terraform_remote_state.k8s_infra.outputs.api_gateway_id
  route_key = "POST /v1/auth"
  target    = "integrations/${aws_apigatewayv2_integration.auth_lambda_integration.id}"
}
