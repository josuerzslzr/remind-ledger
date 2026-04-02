output "ecr_repository_url" {
  description = "Push container images here before applying the backend stack."
  value       = aws_ecr_repository.app.repository_url
}

output "cognito_issuer_uri" {
  description = "JWT issuer URI for Spring Security resource server validation."
  value       = local.cognito_issuer_uri
}

output "cognito_user_pool_client_id" {
  description = "Client ID for obtaining tokens (used by frontend / Postman)."
  value       = aws_cognito_user_pool_client.app.id
}
