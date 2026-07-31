terraform {
  backend "s3" {
    key          = "kairos/dev/terraform.tfstate"
    use_lockfile = true
    encrypt      = true
  }
}
