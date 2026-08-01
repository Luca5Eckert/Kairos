variable "name" {
  description = "Nome da identidade da instancia."
  type        = string
}

variable "ecr_repository_arns" {
  description = "Repositorios dos quais o host pode baixar imagens."
  type        = list(string)
}
