# Ambiente AWS dev

Este root module cria a infraestrutura minima do Kairos: VPC com uma subnet publica, ECR, identidade de runtime, uma EC2 Amazon Linux 2023, Elastic IP e um EBS de dados montado em `/srv/kairos`. Ele nao executa os containers da aplicacao.

## Decisoes

- A subnet unica fica, por padrao, na zona `a` da regiao. Defina `availability_zone` explicitamente quando essa zona nao estiver disponivel na conta.
- A EC2 usa Amazon Linux 2023, obtida por filtro controlado de AMIs oficiais da Amazon.
- `t3.medium` (4 GiB) e a hipotese economica inicial, mas tem pouca margem para JVM, ONNX, PostgreSQL e Neo4j GDS. `t3.large` (8 GiB) e a opcao recomendada para o primeiro runtime completo. CPU, memoria e disco devem ser medidos depois do deploy.
- Um Elastic IP fornece endereco estavel antes da futura configuracao de DNS e HTTPS.
- O EBS `gp3` de 50 GiB e independente da EC2. O attachment nao o apaga quando a instancia e substituida. Entretanto, `terraform destroy` inclui o volume: crie um snapshot e remova o volume do state quando os dados precisarem sobreviver a destruicao completa do ambiente.
- A saida permite HTTPS para ECR, SSM, Gemini e atualizacoes, SMTP com TLS nas portas 465/587 e DNS somente para o resolver da VPC. Nao existem NAT Gateway ou VPC endpoints; restricao por destino exige endpoints/proxy e pertence a um recorte posterior.
- As portas publicas de entrada sao somente 80 e 443. Nao ha SSH, e 8080, 5432, 7474 e 7687 nao sao expostas.
- Participar do grupo `docker` equivale, na pratica, a ter privilegio de root. Somente o usuario operacional `kairos` e adicionado.

## Inicializacao e plano

O backend usa S3 com lockfile nativo. O bucket permanece como configuracao parcial e nunca deve ser gravado em arquivos versionados:

```bash
cp terraform.tfvars.example terraform.tfvars
terraform init \
  -backend-config="bucket=NOME_DO_BUCKET" \
  -backend-config="region=us-east-1"
terraform fmt -check -recursive ../../
terraform validate
terraform plan -out dev.tfplan
terraform show dev.tfplan
```

Revise especialmente substituicoes, destruicoes, o limite de 20 imagens da lifecycle policy do ECR e o custo antes do `apply`. Tags de release devem ser o SHA completo do commit; `latest` nao e uma identidade de release nem de rollback. Como os repositorios sao imutaveis, uma tag publicada nao pode ser sobrescrita.

## Custo antes do apply

Estimativa registrada em 2026-08-01 para o padrao em `us-east-1`, 730 horas/mes, sem impostos, trafego, snapshots ou creditos gratuitos:

| Recurso | Hipotese | Estimativa mensal |
| --- | --- | ---: |
| EC2 | `t3.medium` a USD 0,0416/h | USD 30,37 |
| EBS gp3 | 66 GiB (16 root + 50 data) a USD 0,08/GiB-mes, no baseline | USD 5,28 |
| Elastic IP | 1 IPv4 a USD 0,005/h | USD 3,65 |
| ECR | variavel, USD 0,10/GB-mes | USD 0,10 por GB |
| **Base, antes do ECR** | | **USD 39,30/mes** |

Com `t3.large`, a base sobe para aproximadamente USD 69,66/mes. As referencias sao as paginas AWS para [EC2 T3](https://docs.aws.amazon.com/prescriptive-guidance/latest/optimize-costs-microsoft-workloads/right-size-selection.html), [EBS gp3](https://aws.amazon.com/ebs/general-purpose/), [IPv4 publico](https://aws.amazon.com/vpc/pricing/) e [ECR](https://aws.amazon.com/ecr/pricing/). Consulte novamente a calculadora imediatamente antes do `apply`, pois precos variam.

Uma EC2 ligada continuamente domina o custo. Parar a EC2 interrompe compute, mas EBS, ECR e IPv4 alocado continuam cobrados. O Budget da fundacao e apenas alerta e nao bloqueia gastos.

## Validacao apos apply

Obtenha os IDs com `terraform output` e confira o node no Systems Manager. Nao e criada key pair:

```bash
aws ssm describe-instance-information \
  --filters Key=InstanceIds,Values=$(terraform output -raw instance_id)
aws ssm start-session --target $(terraform output -raw instance_id)
```

Na sessao, valide bootstrap, Docker, mount e reinicio:

```bash
sudo tail -n 200 /var/log/kairos-bootstrap.log
docker version
docker compose version
findmnt /srv/kairos
ls -ld /srv/kairos /srv/kairos/{postgres,neo4j,runtime,backups,logs}
sudo reboot
```

Apos o reboot, abra nova sessao e repita `findmnt /srv/kairos`. Para testar a role sem credenciais estaticas, publique previamente uma imagem descartavel com tag SHA e execute no host:

```bash
aws ecr get-login-password --region REGIAO | \
  docker login --username AWS --password-stdin ID_DA_CONTA.dkr.ecr.REGIAO.amazonaws.com
docker pull URL_DO_REPOSITORIO@sha256:DIGEST
```

O pull deve funcionar. Um push deve falhar, pois a role nao possui `ecr:PutImage` nem permissoes de upload. O host tambem nao recebe acesso ao bucket de state, Secrets Manager, Parameter Store ou APIs de infraestrutura.

## Reexecucao e recuperacao

O bootstrap fica em `/var/lib/cloud/instance/scripts/part-001` e registra logs em `/var/log/kairos-bootstrap.log`. Ele pode ser reexecutado com `sudo bash` nesse arquivo. O script detecta filesystem existente, usa UUID no `/etc/fstab`, evita entradas duplicadas e preserva os dados.

Para substituir a EC2 com dados reais: tire snapshot do EBS, confira o plano, mantenha o volume fora de qualquer destruicao planejada e somente entao associe-o ao novo host. O modulo usa o ID do volume para descobri-lo, inclusive quando Nitro apresenta o disco como NVMe. Nunca teste substituicao destrutiva sem snapshot ou dados descartaveis.

## Destruicao segura

Antes de destruir, revise `terraform plan -destroy`. Se os dados forem descartaveis, aplique o destroy normalmente. Se precisarem sobreviver, crie e valide um snapshot e retire o EBS do gerenciamento deste state antes do destroy. EBS persistente nao e backup. Verifique tambem imagens de rollback que serao removidas junto com os repositorios ECR.
