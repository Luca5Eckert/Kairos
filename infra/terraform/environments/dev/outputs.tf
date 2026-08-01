output "vpc_id" {
  description = "ID da VPC dedicada."
  value       = module.network.vpc_id
}

output "public_subnet_id" {
  description = "ID da subnet publica."
  value       = module.network.public_subnet_id
}

output "security_group_id" {
  description = "ID do security group da EC2."
  value       = module.network.security_group_id
}

output "instance_id" {
  description = "ID da EC2."
  value       = module.compute.instance_id
}

output "public_ip" {
  description = "Elastic IP do host."
  value       = module.compute.public_ip
}

output "data_volume_id" {
  description = "ID do EBS persistente."
  value       = module.compute.data_volume_id
}

output "app_ecr_repository_url" {
  description = "URL do ECR da aplicacao."
  value       = module.ecr.repository_urls["kairos/app"]
}

output "neo4j_ecr_repository_url" {
  description = "URL do ECR do Neo4j."
  value       = module.ecr.repository_urls["kairos/neo4j"]
}

output "instance_role_arn" {
  description = "ARN da role exclusiva do runtime."
  value       = module.iam_instance.role_arn
}
