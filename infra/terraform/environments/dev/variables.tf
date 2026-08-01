variable "aws_region" {
  description = "Região AWS do ambiente dev."
  type        = string
}

variable "owner" {
  description = "Responsável pelo ambiente."
  type        = string
  default     = "Luca5Eckert"
}

variable "availability_zone" {
  description = "AZ da subnet e do volume. Null usa a zona 'a' da regiao."
  type        = string
  default     = null
  nullable    = true
}

variable "vpc_cidr" {
  description = "CIDR da VPC dedicada."
  type        = string
  default     = "10.20.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR da subnet publica unica."
  type        = string
  default     = "10.20.1.0/24"
}

variable "instance_type" {
  description = "Tipo da EC2. t3.medium e a hipotese economica inicial de 4 GiB."
  type        = string
  default     = "t3.medium"
}

variable "root_volume_size" {
  description = "Tamanho do root volume gp3 em GiB."
  type        = number
  default     = 16
}

variable "data_volume_size" {
  description = "Tamanho do volume persistente em GiB."
  type        = number
  default     = 50
}

variable "data_volume_type" {
  description = "Tipo do volume persistente."
  type        = string
  default     = "gp3"
}

variable "data_volume_iops" {
  description = "IOPS do volume gp3."
  type        = number
  default     = 3000
}

variable "data_volume_throughput" {
  description = "Throughput do volume gp3 em MiB/s."
  type        = number
  default     = 125
}

variable "ecr_max_release_images" {
  description = "Numero de imagens com tag mantidas por repositorio."
  type        = number
  default     = 20
}

variable "docker_compose_version" {
  description = "Versao fixa do Docker Compose plugin instalada no host."
  type        = string
  default     = "2.39.1"
}
