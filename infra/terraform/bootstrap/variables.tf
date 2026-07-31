variable "aws_region" {
  description = "Região AWS escolhida para a fundação do Kairos."
  type        = string
}

variable "state_bucket_name" {
  description = "Nome globalmente único do bucket S3 que armazenará o state."
  type        = string
}

variable "budget_limit" {
  description = "Limite mensal do Budget em USD."
  type        = number
  default     = 10
}

variable "budget_email_addresses" {
  description = "Destinatários dos alertas do Budget."
  type        = list(string)
  default     = []
}

variable "environment" {
  description = "Ambiente ao qual a fundação se refere."
  type        = string
  default     = "dev"
}

variable "owner" {
  description = "Responsável pelos recursos."
  type        = string
  default     = "Luca5Eckert"
}
