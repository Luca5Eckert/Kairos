variable "repository_names" {
  description = "Nomes dos repositorios privados."
  type        = set(string)
}

variable "max_release_images" {
  description = "Quantidade de imagens com tag preservadas para rollback."
  type        = number
  default     = 20
}
