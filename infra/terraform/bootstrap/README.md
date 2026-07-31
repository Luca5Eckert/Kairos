# Bootstrap AWS do Kairos

Este módulo cria somente a fundação compartilhada do Terraform: o bucket S3 protegido para state e um Budget mensal. Ele começa com state local porque o bucket ainda não existe.

## Pré-requisitos

- Terraform entre 1.9 e 2.0;
- AWS CLI autenticada com uma identidade administrativa não-root;
- região e nome globalmente único para o bucket definidos;
- MFA habilitado no root e Budget revisado antes do `apply`.

## Execução

```bash
cp terraform.tfvars.example terraform.tfvars
# edite terraform.tfvars e substitua os valores de exemplo
terraform init
terraform fmt -check
terraform validate
terraform plan -out bootstrap.tfplan
terraform apply bootstrap.tfplan
```

O primeiro `apply` deve ser revisado conscientemente. O bucket possui versionamento, criptografia SSE-S3, bloqueio de acesso público, ownership control, negação de transporte inseguro e proteção contra destruição pelo Terraform.

## Próximo passo: migrar o state

Depois que o bucket existir, inicialize o ambiente `dev` informando o bucket por configuração parcial:

```bash
cd ../environments/dev
terraform init \
  -backend-config="bucket=NOME_DO_BUCKET" \
  -backend-config="region=REGIAO_ESCOLHIDA" \
  -backend-config="key=kairos/dev/terraform.tfstate" \
  -migrate-state
```

Não coloque credenciais, tokens, `terraform.tfvars` ou arquivos de state no Git.
