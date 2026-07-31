variable "aws_region" {
  description = "Região AWS do ambiente dev."
  type        = string
}

variable "owner" {
  description = "Responsável pelo ambiente."
  type        = string
  default     = "Luca5Eckert"
}
