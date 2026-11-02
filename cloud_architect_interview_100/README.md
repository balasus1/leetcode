# Senior Cloud Engineer / Lead Cloud Architect Interview Playbook (100 Questions)
*Curated for Balasubramanian Shanmugham (16+ Years Experience, Enterprise & Regulated Cloud Systems)*
*Engineered for Senior Cloud Engineer / Lead Cloud Architect Panels (AWS, EKS, PCF Migration, Terraform, Java/JVM, FinOps, SRE)*

---

## 🎯 Executive Target Role Profile
- **Role:** Senior Cloud Engineer / Lead Cloud Architect (10+ Years Hands-on Depth)
- **Primary Cloud:** Amazon Web Services (AWS) — Solutions Architect / DevOps Professional Depth
- **Core Specializations:** 
  - On-Premises & Pivotal Cloud Foundry (PCF) to AWS EKS/ECS Migrations
  - Containerization (Docker) & High-Performance JVM Tuning in Containers (cgroups v2, GC, Memory Limits)
  - Kubernetes / Amazon EKS Production Orchestration (Karpenter, KEDA, AWS Load Balancer Controller, Cilium/VPC CNI)
  - Infrastructure as Code (Terraform) & GitOps (Argo CD, Flux)
  - Enterprise Cloud Networking (VPC, Transit Gateway, Route 53, PrivateLink, ALB/NLB)
  - Regulated Banking Security & IAM (Least Privilege, IRSA, AWS KMS Envelope Encryption, PCI-DSS, HSM)
  - Java Microservices Cloud Resilience (AWS SDK v2, HikariCP, Resilience4j, Reactive/Kafka)
  - Enterprise Observability (Prometheus, Grafana, CloudWatch, Datadog) & SRE Incident Management
  - FinOps & Cost Optimization (Graviton3 ARM64 migration, Spot, Savings Plans, Bin-Packing)

---

## 📚 Master Index of All 10 Volumes (100 Questions)

| Volume | Focus Domain | Key Topics Covered | File Link |
| :--- | :--- | :--- | :--- |
| **Volume 1** | **PCF to AWS Workload Migration (Q1 - Q10)** | Diego Cells/Gorouter to ALB+EKS, `manifest.yml` to Helm/K8s, Spring Cloud Config/Eureka to AWS App Mesh & Parameter Store, User-Provided Services (UPSI) to RDS/ElastiCache, Zero-Downtime Database CDC via AWS DMS, Blue-Green/Canary Cutover Rehearsals, PCF Loggregator to Fluent Bit/CloudWatch, Rollback Runbooks. | [Volume 1](./volume_01_pcf_to_aws_migration.md) |
| **Volume 2** | **Containerization & JVM Container Optimization (Q11 - Q20)** | Multi-Stage Distroless Java Dockerfiles, cgroups v1 vs v2 Memory Limits, Eliminating K8s OOMKilled (Exit Code 137), `-XX:MaxRAMPercentage` & Metaspace/Off-Heap Sizing, Graviton3 Multi-Arch Builds (Buildx), Non-Root Security Profiles, Spring Boot Actuator Liveness/Readiness Probes, Graceful `SIGTERM` Draining, JVM DNS Caching & Alpine Musl Gotchas. | [Volume 2](./volume_02_docker_jvm_container_optimization.md) |
| **Volume 3** | **Kubernetes & Amazon EKS Production (Q21 - Q30)** | EKS Control Plane Architecture, Managed Node Groups vs Bottlerocket, AWS Load Balancer Controller (IP vs Instance Mode TargetGroupBinding), Pod Autoscaling via KEDA & Prometheus, Karpenter v1 Consolidation & Spot Interruption, EBS/EFS CSI IAM Roles, Topology Spread Constraints across Multi-AZ, VPC CNI `/28` Prefix Delegation, Zero-Downtime EKS Upgrades. | [Volume 3](./volume_03_kubernetes_eks_architecture.md) |
| **Volume 4** | **Terraform Infrastructure as Code & Governance (Q31 - Q40)** | Clean Module Separation (Network, Data, Compute), S3 Remote State with DynamoDB Locking, Production Drift Detection Pipelines (`detailed-exitcode`), Zero-Downtime Resource Replacement (`create_before_destroy`), Non-Leaking Secrets in State, Refactoring via `moved` Blocks, Policy as Code (OPA/Checkov), Multi-Account AWS Provider Aliasing. | [Volume 4](./volume_04_terraform_iac_drift_governance.md) |
| **Volume 5** | **Cloud Networking, Load Balancing & DNS (Q41 - Q50)** | Hub-and-Spoke Transit Gateway Topologies, Subnet CIDR Planning & Secondary CIDRs, AWS PrivateLink Interface/Gateway Endpoints eliminating NAT Costs, ALB (L7 Path/Host, mTLS) vs NLB (L4 Ultra-low Latency, Static IP), Direct Connect with VPN Failover & BGP, Route 53 Hybrid Inbound/Outbound Resolvers, Egress Filtering for PCI-DSS. | [Volume 5](./volume_05_aws_networking_load_balancing_dns.md) |
| **Volume 6** | **Cloud Security, IAM, KMS & Regulated Compliance (Q51 - Q60)** | IAM Permission Boundaries & SCPs, EKS IRSA & Pod Identity Associations, AWS KMS Multi-Region Envelope Encryption & Key Rotation, Secrets Manager Auto-Rotation vs External Secrets Operator (ESO), PCI-DSS Cardholder Data Environment (CDE) Isolation, S3 Object Lock (WORM), AWS WAF Layer-7 Defense, GuardDuty Automated Remediation. | [Volume 6](./volume_06_cloud_security_iam_kms_pcidss.md) |
| **Volume 7** | **Java Microservices Cloud Resilience & SDKs (Q61 - Q70)** | AWS SDK for Java v2 Non-Blocking Netty Async, HikariCP Connection Pool Sizing for Aurora PostgreSQL, Redis Distributed Locks (Redisson) for Financial Idempotency, Resilience4j Circuit Breaker & Retries with Jitter, OpenTelemetry Distributed Tracing (W3C), Transactional Outbox with Debezium/Kafka, Clustered Quartz to EventBridge/ECS. | [Volume 7](./volume_07_java_microservices_resilience_cloud_sdk.md) |
| **Volume 8** | **CI/CD Pipelines, GitOps & Release Automation (Q71 - Q80)** | Argo CD Pull-Based GitOps & ApplicationSets, GitLab CI / GitHub Actions OIDC Federation (Zero Long-Lived AWS Keys), Argo Rollouts Canary Traffic Shifting with ALB, Liquibase Pre-Sync Database Migrations in Kubernetes, Ephemeral Feature-Branch EKS Environments, Cosign/Sigstore Image Signing, Automated Rollbacks via Prometheus Error Budgets. | [Volume 8](./volume_08_cicd_gitops_argo_automation.md) |
| **Volume 9** | **Observability, SRE Incident Response & High Availability (Q81 - Q90)** | Fluent Bit Daemonsets to CloudWatch/OpenSearch at Scale, CloudWatch Embedded Metric Format (EMF) for Java, Continuous Profiling for JVM Lock Contention, SLI/SLO Multi-Window Multi-Burn-Rate Alerting, Severity-1 AZ Outage Live Triage & Remediation, AWS Fault Injection Simulator (FIS) Chaos Drills, Correlating TraceId/SpanId into SLF4J MDC. | [Volume 9](./volume_09_observability_logging_sre_incidents.md) |
| **Volume 10** | **FinOps, Cost Optimization & Executive Cloud Leadership (Q91 - Q100)** | FinOps Tagging & Cost Allocation, EKS Spot with Karpenter Bin-Packing, AWS Graviton3 (ARM64) Java Migration (40% Price-Performance Gain), Aurora Serverless v2 Scaling, PCF Licensing vs AWS TCO Financial Modeling, Multi-Region Active-Active DR with Route 53 ARC, AWS Well-Architected Framework Reviews, Engineering Mentorship & CCoE. | [Volume 10](./volume_10_finops_cost_optimization_executive_leadership.md) |

---

## 🎙️ The 3-Step Vetting Formula for Senior Cloud Architect Responses
Every response in these 100 questions is structured for an impactful **1.5 to 2-minute verbal delivery**:
1. **The Architecture Context & Failure Mode (15-20s):** Directly pinpoint the technical trade-off, architectural constraint, or risk (e.g., cgroup v2 memory throttling, Gorouter session affinity loss, state drift, or NAT Gateway bandwidth tax).
2. **The Mechanism, Implementation & Invariants (45-60s):** Explain the exact cloud-native implementation, APIs, protocols, and configuration invariants without hand-waving.
3. **Production Realism & Architectural Trade-offs (30-45s):** Address failure scenarios, operational observability, rollback strategies, cost implications, and security boundaries.
