# AWS Deployment Plan — Phase 1: EC2, Docker Compose and Terraform

**Status:** planned  
**Created:** 2026-07-31  
**Target environment:** development/laboratory  
**Objective:** deploy Kairos on AWS with a reproducible infrastructure and an understandable delivery process before introducing ECS, RDS or high availability.

## 1. Objective

The first cloud deployment will run the complete Kairos stack on a single EC2 instance:

- Kairos Spring Boot API;
- PostgreSQL 16 with pgvector;
- Neo4j 5.26 with Graph Data Science;
- an HTTPS reverse proxy.

The infrastructure will be declared with Terraform. Docker Compose will remain responsible for the runtime topology. GitHub Actions will publish versioned images to Amazon ECR and, after the first manual deployment is validated, will trigger deployments through AWS Systems Manager.

This phase is intentionally not a production high-availability architecture. Its goal is to establish the operational foundations that can later support the migration of the API to ECS and PostgreSQL to RDS.

## 2. Architecture

```text
                         GitHub Actions
                               |
                      OIDC temporary identity
                               |
                  +------------+------------+
                  |                         |
                  v                         v
            Amazon ECR             Systems Manager
          versioned images            Run Command
                  |                         |
                  +------------+------------+
                               |
                               v
Internet                  Public EC2
   |                           |
   |                    HTTPS reverse proxy
   |                           |
   +--------- 443 -------------+
                               |
                 +-------------+-------------+
                 |             |             |
              Kairos       PostgreSQL      Neo4j
              :8080          :5432          :7687
                 |             |             |
                 +-------------+-------------+
                               |
                        Dedicated EBS volume
```

Only ports 80 and 443 will be reachable from the internet. Ports 8080, 5432, 7474 and 7687 will remain internal to the Docker network. SSH will not be exposed; administration will use Systems Manager.

## 3. Responsibility boundaries

### Terraform

Terraform owns AWS infrastructure:

- VPC and subnet;
- Internet Gateway and route table;
- security groups;
- EC2 instance;
- EBS volume and attachment;
- Elastic IP when adopted;
- ECR repositories;
- IAM roles and instance profile;
- GitHub OIDC provider and trust policies;
- Systems Manager permissions;
- secret containers or parameter paths;
- logs and alarms added during the observability milestone.

Terraform does not deploy each Kairos release and does not store runtime secret values.

### Docker Compose

Docker Compose owns the processes running on the EC2 instance:

- reverse proxy;
- Kairos application;
- PostgreSQL;
- Neo4j;
- internal networks;
- volumes and bind mounts;
- health checks;
- restart policies;
- runtime resource limits;
- log rotation.

### Amazon ECR

ECR stores immutable images:

- `kairos/app:<commit-sha>`;
- `kairos/neo4j:<version-or-commit-sha>`.

The deployed application must always reference a traceable tag. `latest` may exist as a convenience alias but must not be the source of truth for releases or rollback.

### GitHub Actions

GitHub Actions owns validation and delivery:

- run the existing CI checks;
- authenticate to AWS using OIDC;
- publish images to ECR;
- request a deployment through Systems Manager;
- wait for the deployment result;
- report success or failure.

### AWS Systems Manager

Systems Manager is the control channel for the EC2 host:

- interactive maintenance without public SSH;
- execution of the deployment script;
- retrieval of deployment command status;
- operational troubleshooting.

## 4. Manual versus automated work

### Manual account bootstrap

The following actions are performed manually because they belong to account ownership or precede Terraform access:

- create or activate the AWS account;
- enable MFA for the root user;
- configure billing and credit alerts;
- create an AWS Budget;
- select the deployment region;
- obtain the initial administrative access used for Terraform bootstrap;
- insert secret values after their containers are created;
- configure a domain when HTTPS with a public hostname is introduced.

### Terraform-managed resources

All technical resources must be created through Terraform after bootstrap. The AWS console may be used for inspection and learning, but the resulting production of resources must be represented in code.

### First application deployment

The first deploy will be executed manually through Systems Manager. This validates the actual runtime steps before automating them.

### Subsequent deployments

After the manual sequence and rollback are proven, GitHub Actions will execute the same deployment process automatically.

## 5. Proposed repository structure

```text
Kairos/
├── src/
├── pom.xml
├── Dockerfile
├── Dockerfile.neo4j
├── docker-compose.yml
│
├── docs/
│   └── deployment/
│       └── aws-ec2-phase-1.md
│
├── infra/
│   ├── terraform/
│   │   ├── bootstrap/
│   │   ├── modules/
│   │   │   ├── network/
│   │   │   ├── ecr/
│   │   │   ├── ec2/
│   │   │   ├── iam/
│   │   │   ├── secrets/
│   │   │   └── observability/
│   │   └── environments/
│   │       └── dev/
│   │
│   └── runtime/
│       ├── docker-compose.production.yml
│       ├── proxy/
│       └── scripts/
│           ├── bootstrap-host.sh
│           ├── deploy.sh
│           └── rollback.sh
│
└── .github/
    └── workflows/
        ├── ci.yml
        ├── terraform-plan.yml
        ├── terraform-apply.yml
        └── deploy-dev.yml
```

The structure is the target organization. It should be introduced incrementally instead of creating empty modules and scripts before their responsibilities are implemented.

## 6. Terraform state bootstrap

Terraform requires persistent state before the main environment can be managed safely.

The bootstrap process will:

1. initialize a small Terraform root module using local state;
2. create an S3 bucket dedicated to Terraform state;
3. enable bucket versioning;
4. enable state locking using the supported S3 backend mechanism;
5. configure encryption and public access blocking;
6. restrict bucket access to the infrastructure role;
7. migrate the local state to the remote backend;
8. remove any local state files from the working directory and ensure they are ignored by Git.

The bootstrap root module must remain separate from the main environment because it creates the backend required by that environment.

## 7. Network design

Phase 1 uses one public subnet to avoid NAT Gateway costs and complexity.

### Resources

- one VPC;
- one public subnet;
- one Internet Gateway;
- one public route table;
- one security group for the EC2 instance.

### Inbound rules

- TCP 80 from the internet, only to redirect to HTTPS;
- TCP 443 from the internet;
- no public SSH;
- no public access to the application or databases.

### Outbound rules

The EC2 host needs outbound HTTPS access for:

- Amazon ECR;
- Systems Manager;
- Gemini API;
- SMTP provider;
- package and container image downloads.

The security group and route design must not expose PostgreSQL or Neo4j.

## 8. EC2 sizing and host layout

Kairos runs a JVM, ONNX Runtime, PostgreSQL and Neo4j GDS on the same host. The initial instance size must therefore be selected based on memory measurements rather than only HTTP traffic.

### Initial sizing hypothesis

- 4 GiB RAM: cost-oriented experiment, requiring reduced Neo4j memory values and close monitoring;
- 8 GiB RAM: safer initial laboratory configuration for ingestion, graph operations and local embeddings.

The chosen instance type must be documented as an assumption and reviewed after measuring:

- idle memory;
- memory during source ingestion;
- memory during graph search and PageRank;
- CPU during ONNX inference;
- PostgreSQL and Neo4j disk growth;
- container restart or OOM events.

### Filesystem layout

A dedicated EBS volume will be mounted at `/srv/kairos`:

```text
/srv/kairos/
├── postgres/
├── neo4j/
├── runtime/
├── backups/
└── logs/
```

The operating system disk and application data disk must have separate responsibilities. Replacing the EC2 instance must not require discarding the data volume.

## 9. ECR strategy

Create two repositories:

- `kairos/app`;
- `kairos/neo4j`.

Recommended controls:

- scan images on push when available;
- use lifecycle rules to remove old unreferenced images while preserving rollback candidates;
- prevent accidental tag overwrite for immutable release tags;
- tag the application image with the complete or shortened commit SHA;
- record the deployed tag on the EC2 host.

The Neo4j image has a separate lifecycle because it changes less frequently than the application image.

## 10. IAM and identity design

### EC2 instance role

Required capabilities:

- Systems Manager managed instance access;
- ECR image pull;
- read access to the Kairos runtime secrets or parameters;
- CloudWatch log and metric publication when implemented;
- restricted backup access to an S3 path when implemented.

The EC2 role must not create infrastructure or push images.

### GitHub deployment role

Required capabilities:

- ECR authentication and image push to Kairos repositories;
- send Systems Manager commands only to the Kairos development instance;
- read command execution status.

The role must not read application secrets or modify VPC, EC2, IAM or billing resources.

### GitHub infrastructure role

Required capabilities:

- execute Terraform plan against the declared environment;
- apply infrastructure only through an explicitly protected workflow or environment.

The trust policy must restrict access to `Luca5Eckert/Kairos` and to the intended branch or GitHub Environment.

## 11. Secret management

Runtime secrets include:

- PostgreSQL password;
- Neo4j password;
- Gemini API key;
- authentication session secret;
- SMTP password;
- administrator bootstrap credentials when temporarily required.

Terraform may create secret resources or parameter paths, but the values should be inserted separately to avoid unnecessarily persisting plaintext values in Terraform state.

The EC2 instance role reads the secrets at deploy or startup time. GitHub Actions does not need access to database passwords, the Gemini key or the JWT signing secret.

For the AWS environment:

```text
KAIROS_ADMIN_BOOTSTRAP_ENABLED=false
```

Any controlled administrator creation process must be designed separately. Local default credentials must never be used on a public environment.

## 12. Production Docker Compose requirements

The existing local Compose file remains the development baseline. The AWS runtime will use a production override or a dedicated production Compose file.

Required differences:

- use ECR images instead of local `build` directives;
- receive the application image tag through a deployment variable;
- persist PostgreSQL and Neo4j using bind mounts on EBS;
- expose only the reverse proxy ports;
- remove host publishing for PostgreSQL, Neo4j and Kairos;
- disable local administrative bootstrap defaults;
- configure container memory limits after measurement;
- configure log rotation;
- preserve the current health checks;
- use restart policies suitable for host reboot;
- keep the application container stateless.

## 13. Host bootstrap

The EC2 `user_data` or host bootstrap script is responsible for:

1. installing required operating system packages;
2. installing Docker Engine and the Compose plugin;
3. creating the deployment user and groups;
4. mounting the EBS volume at `/srv/kairos`;
5. creating runtime directories;
6. applying restrictive filesystem permissions;
7. enabling Docker on boot;
8. validating Systems Manager connectivity;
9. preparing a location for the Compose files and deployment scripts.

The bootstrap must not embed:

- application image tags;
- database passwords;
- Gemini credentials;
- JWT signing secrets;
- complete application deployment logic.

## 14. First manual deployment

The first deployment is a learning and validation procedure.

### Sequence

1. run the existing CI successfully;
2. build the Kairos and Neo4j images;
3. tag the images using immutable identifiers;
4. publish them to ECR;
5. connect to the EC2 instance through Systems Manager;
6. confirm the EBS volume and runtime directories;
7. configure the production Compose files;
8. retrieve runtime secrets using the EC2 role;
9. authenticate Docker to ECR using the EC2 role;
10. pull the required images;
11. start PostgreSQL and wait for health;
12. start Neo4j and wait for health;
13. start Kairos and wait for `/actuator/health`;
14. start the HTTPS proxy;
15. test authentication, source ingestion, asynchronous progress and retrieval;
16. inspect logs and resource usage;
17. deploy a known-invalid application image or configuration in a controlled test;
18. restore the previously healthy image.

The first deployment is complete only after rollback has been demonstrated.

## 15. Automated deployment flow

After the manual procedure is stable, `deploy-dev.yml` will:

1. run only after the required CI jobs pass;
2. authenticate to AWS using OIDC;
3. publish `kairos/app:<commit-sha>` to ECR;
4. record the intended release identifier;
5. invoke the EC2 deployment script through Systems Manager;
6. pass the image tag as a non-secret argument;
7. wait for command completion;
8. fail the workflow when the deployment script reports failure;
9. retain deployment logs as a workflow artifact or CloudWatch log;
10. expose the deployed commit in the workflow summary.

The EC2 deployment script will:

1. acquire a deployment lock;
2. read the current healthy version;
3. authenticate to ECR;
4. pull the new image;
5. update the runtime version file;
6. replace only the Kairos application container;
7. wait for the container health check;
8. execute an external or host-level smoke test;
9. persist the new healthy version on success;
10. restore the previous version on failure;
11. release the deployment lock.

## 16. Database migrations

Kairos uses Flyway and Hibernate schema validation.

During Phase 1, only one application instance is deployed, so Flyway may continue to execute during application startup. However, every migration must be evaluated for rollback compatibility.

Use expand/contract principles:

1. add compatible structures;
2. deploy code that supports old and new structures;
3. migrate data when necessary;
4. remove old structures in a later release.

A rollback of the container must not be assumed safe if a migration removed or changed data required by the previous application version.

## 17. Backup and recovery

Persistence on EBS is not a complete backup strategy.

The phase must eventually include:

- EBS snapshots;
- PostgreSQL logical backups;
- Neo4j backup or export compatible with the selected edition and operational model;
- retention limits;
- restore documentation;
- at least one tested restoration.

The project currently treats PostgreSQL as the durable source of truth and Neo4j as a derived projection, but a fully supported graph rebuild operation is not yet available. Neo4j data must therefore be protected until that rebuild capability exists and is tested.

## 18. Observability baseline

Before the phase is considered operationally complete, add:

- application and container logs with bounded retention;
- disk utilization monitoring;
- EC2 CPU and memory monitoring;
- container health visibility;
- alerts for API unavailability;
- alerts for low disk space;
- deployment failure reporting;
- version identifier in application logs or metadata;
- a simple runbook for common failures.

## 19. Cost controls

Before creating resources:

- enable billing alerts;
- create an AWS Budget;
- monitor credit consumption;
- select a single region;
- use consistent tags;
- avoid NAT Gateway, load balancer, Multi-AZ and duplicated always-on environments during Phase 1;
- configure ECR lifecycle rules;
- configure log and snapshot retention;
- document `terraform destroy` and identify resources containing persistent data before destruction.

Recommended tags:

```text
project     = kairos
environment = dev
managed-by  = terraform
owner       = Luca5Eckert
```

## 20. Milestones

### Milestone 0 — Account safety

- root MFA enabled;
- billing and credit alerts enabled;
- AWS Budget created;
- region selected;
- initial administrative access tested.

### Milestone 1 — Terraform bootstrap

- remote state bucket created;
- versioning and locking enabled;
- state migrated from local to remote;
- Git excludes local state and plan artifacts;
- GitHub OIDC provider and initial roles designed.

### Milestone 2 — Minimum AWS infrastructure

- VPC and public subnet created;
- security group exposes only 80 and 443;
- ECR repositories created;
- EC2 and dedicated EBS created;
- EC2 visible in Systems Manager;
- Docker and Compose installed;
- EBS mounted persistently.

### Milestone 3 — Manual runtime deployment

- production Compose created;
- secrets configured outside Git;
- PostgreSQL, Neo4j and Kairos healthy;
- functional API flow validated;
- restart after EC2 reboot validated;
- rollback validated.

### Milestone 4 — HTTPS

- domain or selected hostname strategy defined;
- reverse proxy deployed;
- certificate issued and renewed automatically;
- HTTP redirected to HTTPS;
- only proxy ports publicly exposed.

### Milestone 5 — Automated CD

- GitHub deployment role active through OIDC;
- image published by commit SHA;
- Systems Manager deploy command automated;
- health validation automated;
- rollback automated;
- deployment status visible in GitHub Actions.

### Milestone 6 — Backup and observability

- logs centralized or retained with limits;
- health and disk alarms active;
- backup schedules active;
- restore procedure tested;
- operational runbook documented.

## 21. Phase acceptance criteria

Phase 1 is complete when:

- infrastructure can be recreated from Terraform;
- Terraform state is remote, versioned and locked;
- no AWS long-lived access keys are stored in GitHub;
- no application secret is committed to Git;
- EC2 has no public SSH access;
- PostgreSQL and Neo4j have no public ports;
- all persistent data is located on the dedicated EBS volume;
- images are pulled from ECR using immutable tags;
- a merge to the selected branch can deploy a validated image;
- an unhealthy release automatically returns to the previous healthy version;
- HTTPS is enabled;
- backups exist and at least one restore has been tested;
- costs and resource ownership are observable through tags and budgets.

## 22. Explicitly out of scope

The following items belong to later phases:

- ECS or EKS;
- RDS migration;
- high availability;
- Multi-AZ;
- horizontal autoscaling;
- managed load balancer;
- NAT Gateway and private application subnets;
- multi-account AWS Organizations structure;
- blue/green deployment across multiple application instances;
- automatic Neo4j rebuild from PostgreSQL;
- production security audit.

## 23. Implementation order

The order is intentional:

```text
Account safety
    ↓
Terraform backend
    ↓
Network, IAM, ECR, EC2 and EBS
    ↓
First manual deployment
    ↓
HTTPS
    ↓
Automated deployment
    ↓
Backup and observability
```

Automating deployment before the manual process and rollback are understood would hide operational failures behind the pipeline. Building the infrastructure manually before declaring it in Terraform would make the environment difficult to reproduce. The selected order keeps each new layer testable before the next layer is introduced.
