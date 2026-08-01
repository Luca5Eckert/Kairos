locals {
  name              = "kairos-dev"
  availability_zone = coalesce(var.availability_zone, "${var.aws_region}a")
  ecr_repositories  = toset(["kairos/app", "kairos/neo4j"])
}

module "network" {
  source = "../../modules/network"

  name               = local.name
  vpc_cidr           = var.vpc_cidr
  public_subnet_cidr = var.public_subnet_cidr
  availability_zone  = local.availability_zone
}

module "ecr" {
  source = "../../modules/ecr"

  repository_names   = local.ecr_repositories
  max_release_images = var.ecr_max_release_images
}

module "iam_instance" {
  source = "../../modules/iam-instance"

  name                = "${local.name}-host"
  ecr_repository_arns = module.ecr.repository_arns
}

module "compute" {
  source = "../../modules/compute"

  name                   = "${local.name}-host"
  availability_zone      = local.availability_zone
  subnet_id              = module.network.public_subnet_id
  security_group_id      = module.network.security_group_id
  instance_profile_name  = module.iam_instance.instance_profile_name
  instance_type          = var.instance_type
  root_volume_size       = var.root_volume_size
  data_volume_size       = var.data_volume_size
  data_volume_type       = var.data_volume_type
  data_volume_iops       = var.data_volume_iops
  data_volume_throughput = var.data_volume_throughput
  docker_compose_version = var.docker_compose_version
}
