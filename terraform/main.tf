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

# ------------------------------------------------------------------------------
# 2. Função AWS Lambda (Container Image)
# ------------------------------------------------------------------------------
resource "aws_lambda_function" "auth_lambda" {
  function_name = "${var.app_name}-auth-lambda"
  role          = "arn:aws:iam::${var.account_id}:role/LabRole"
  package_type  = "Image"

  image_uri = "${data.terraform_remote_state.k8s_infra.outputs.ecr_auth_lambda_url}:${var.image_tag}"

  memory_size = 512
  timeout     = 15

  vpc_config {
    subnet_ids         = data.terraform_remote_state.k8s_infra.outputs.private_subnets
    security_group_ids = [data.terraform_remote_state.k8s_infra.outputs.eks_node_security_group_id]
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
# 3. Permissão para o API Gateway Invocar a Lambda
# ------------------------------------------------------------------------------
resource "aws_lambda_permission" "apigw_lambda" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.auth_lambda.function_name
  principal     = "apigateway.amazonaws.com"

  source_arn = "${data.terraform_remote_state.k8s_infra.outputs.api_gateway_execution_arn}/*/*"
}

# ------------------------------------------------------------------------------
# 4. Integração da Lambda com o API Gateway
# ------------------------------------------------------------------------------
resource "aws_apigatewayv2_integration" "auth_lambda_integration" {
  api_id                 = data.terraform_remote_state.k8s_infra.outputs.api_gateway_id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.auth_lambda.invoke_arn
  payload_format_version = "2.0"
}

# ------------------------------------------------------------------------------
# 5. Rota POST /v1/auth no API Gateway
# ------------------------------------------------------------------------------
resource "aws_apigatewayv2_route" "auth_route" {
  api_id    = data.terraform_remote_state.k8s_infra.outputs.api_gateway_id
  route_key = "POST /v1/auth/login"
  target    = "integrations/${aws_apigatewayv2_integration.auth_lambda_integration.id}"
}