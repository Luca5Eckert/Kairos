variable "name" {
  description = "Prefixo de nome dos recursos de rede."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR da VPC."
  type        = string
}

variable "public_subnet_cidr" {
  description = "CIDR da subnet publica."
  type        = string
}

variable "availability_zone" {
  description = "Availability Zone da subnet publica unica."
  type        = string
}
