output "instance_id" { value = aws_instance.this.id }
output "public_ip" { value = aws_eip.this.public_ip }
output "data_volume_id" { value = aws_ebs_volume.data.id }
