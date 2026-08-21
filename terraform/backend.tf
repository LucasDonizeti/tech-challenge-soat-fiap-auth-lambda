terraform {
  backend "s3" {
    bucket       = "bucket-tfstate-1029"
    key          = "auth-lambda/terraform.tfstate"
    region       = "us-east-1"
    dynamodb_table = "meu-terraform-state-lock"
    encrypt      = true
  }
}
