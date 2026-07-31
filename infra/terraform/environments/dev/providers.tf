provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      project     = "kairos"
      environment = "dev"
      managed-by  = "terraform"
      owner       = var.owner
    }
  }
}
