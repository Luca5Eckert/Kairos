output "state_bucket_name" {
  description = "Nome do bucket S3 do state."
  value       = aws_s3_bucket.terraform_state.id
}

output "state_bucket_arn" {
  description = "ARN do bucket S3 do state."
  value       = aws_s3_bucket.terraform_state.arn
}
