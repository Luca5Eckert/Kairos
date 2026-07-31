# Ambiente dev

Este diretório ainda não cria VPC, EC2, EBS ou recursos da aplicação. Ele valida que o ambiente consegue usar o backend remoto e gerar um plano vazio/minimal.

## Inicialização

O backend S3 usa configuração parcial para não armazenar o nome do bucket no código:

```bash
terraform init \
  -backend-config="bucket=NOME_DO_BUCKET" \
  -backend-config="region=REGIAO_ESCOLHIDA" \
  -backend-config="key=kairos/dev/terraform.tfstate"
```

Se houver state local existente, use `-migrate-state` após revisar a origem e o destino.

## Validação

```bash
terraform fmt -check
terraform validate
terraform plan
```

O próximo recorte deverá adicionar a rede, o ECR, a IAM Role da EC2, a instância e o volume EBS.
