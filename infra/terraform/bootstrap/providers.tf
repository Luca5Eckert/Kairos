provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      project     = "kairos"
      environment = var.environment
      managed-by  = "terraform"
      owner       = var.owner
    }
  }
}
