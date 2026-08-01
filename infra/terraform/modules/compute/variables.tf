variable "name" { type = string }
variable "availability_zone" { type = string }
variable "subnet_id" { type = string }
variable "security_group_id" { type = string }
variable "instance_profile_name" { type = string }
variable "instance_type" { type = string }
variable "root_volume_size" { type = number }
variable "data_volume_size" { type = number }
variable "data_volume_type" { type = string }
variable "docker_compose_version" { type = string }

variable "data_volume_iops" {
  type    = number
  default = 3000
}

variable "data_volume_throughput" {
  type    = number
  default = 125
}
