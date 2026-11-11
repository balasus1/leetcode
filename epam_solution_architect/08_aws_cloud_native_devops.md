# 08: AWS, Cloud-Native, Kubernetes/OpenShift & DevOps (Basic → Advanced)

> JD: *"Hands-on experience scaling production workloads on Cloud (AWS/Azure/GCP); cloud-native development; collaborate with DevOps."* Resume: AWS, Docker, Kubernetes, OpenShift, GitHub Actions, Harbor, Prometheus, Grafana, OpenTelemetry, CloudWatch, Vercel/Railway/VPS.
> GenAI-specific AWS architecture: [genai/04_aws_specific_genai_architecture.md](../genai/04_aws_specific_genai_architecture.md).

| Level | Questions |
|---|---|
| Basic | B1–B10 |
| Medium | M1–M14 |
| Advanced | A1–A7 |

---

## Basic

### B1. Regions, AZs, and why they matter?
🎤 "A **region** is a geographic area with several isolated **availability zones**, each one or more data centres with independent power and networking. Design for HA by spreading across **at least 2–3 AZs**. Use multiple regions for DR, latency or data residency. Some services are regional (S3, DynamoDB), some zonal (EC2, EBS)."

### B2. EC2 vs ECS vs EKS vs Fargate vs Lambda: when?
🎤 "**Lambda**: event-driven, short (≤15 min), spiky workloads, zero ops; watch cold starts and per-invocation cost at high steady load. **ECS/Fargate**: containers without managing Kubernetes, the simplest container platform on AWS. **EKS**: when you need Kubernetes portability, its ecosystem (operators, Helm, service mesh) or have a multi-cloud strategy. **ROSA** if the organization standardizes on OpenShift, as our scheduler did. **EC2**: full control, special hardware, legacy."

### B3. RDS vs Aurora vs DynamoDB?
🎤 "**RDS**: managed Postgres, MySQL and others; familiar and cost-effective. **Aurora**: cloud-native storage replicated six ways across three AZs, faster failover, up to 15 read replicas, Global Database, Serverless v2; best for demanding relational workloads. **DynamoDB**: serverless key-value and document store, single-digit-ms latency at any scale; requires access-pattern-first modelling, and has limited ad-hoc queries."

### B4. S3 storage classes and lifecycle?
🎤 "Standard (hot), Intelligent-Tiering (unknown access patterns), Standard-IA and One Zone-IA (infrequent), Glacier Instant/Flexible/Deep Archive (archive). Lifecycle rules move objects between classes and expire them. Add versioning, **Object Lock** for WORM compliance, SSE-KMS encryption, Block Public Access, and bucket policies requiring TLS."

### B5. VPC fundamentals?
🎤 "A private network per region: **public subnets** (route to an internet gateway, for load balancers), **private subnets** (apps, reaching out via NAT gateways, one per AZ for HA), isolated data subnets. Security groups are stateful, instance-level; NACLs are stateless, subnet-level. **VPC endpoints** (gateway endpoints for S3/DynamoDB, interface endpoints/PrivateLink for other services) keep traffic off the internet. Transit Gateway connects many VPCs and on-prem."

### B6. IAM: users, roles, policies?
🎤 "Policies are JSON permission documents. **Roles** are assumable identities with temporary credentials: use them for workloads and federated humans. Avoid IAM users with long-lived keys. Use least privilege, conditions, permission boundaries, and SCPs at the organization level."

### B7. Load balancers: ALB vs NLB vs API Gateway?
🎤 "**ALB**: layer 7 HTTP routing (path or host), WebSockets, gRPC, OIDC authentication, WAF integration. **NLB**: layer 4 TCP/UDP, ultra-low latency, static IPs, PrivateLink. **API Gateway**: managed API front door with throttling, API keys and usage plans, authorizers, and direct Lambda integration. Great for serverless and partner APIs; per-request cost at very high volume."

### B8. Docker best practices?
🎤 "Multi-stage builds; small base images (distroless or Alpine, or `jlink` runtimes for Java); **non-root user**; a read-only filesystem where possible; pin versions and digests; one process per container; healthchecks; no secrets in images or layers; `.dockerignore`; layer ordering for caching (dependencies before code, or Spring Boot layered jars); **image scanning** in a registry. I used **Harbor** with vulnerability scanning and signed images."

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -q -DskipTests package && java -Djarmode=tools -jar target/app.jar extract --layers --launcher --destination extracted

FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app
COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

### B9. Kubernetes core objects?
🎤 "**Pod** (one or more containers), **Deployment** (replicas and rolling updates), **StatefulSet** (stable identity and storage), **DaemonSet** (one per node), **Job/CronJob**, **Service** (stable virtual IP and DNS), **Ingress/Route/Gateway API** (external routing; Routes on OpenShift), **ConfigMap/Secret**, **HPA**, **PDB**, **NetworkPolicy**, **ServiceAccount + RBAC**, **Namespace**."

### B10. What's CI/CD, and what's in your pipeline? ✅
🎤 "CI: on every PR, build, unit and integration tests (Testcontainers), static analysis, **coverage thresholds**, dependency and image **security scans**, contract and breaking-change checks. CD: build an immutable image once, sign it, push it to the registry, deploy to dev → staging → prod by promotion (GitOps with ArgoCD, or pipeline stages), with automated smoke tests, canary analysis and one-click rollback. That's the security and quality gate approach I used in my OpenHands work: Jest and JUnit 5 coverage thresholds and dependency scanning before merge."

---

## Medium

### M1. Design a highly available 3-tier Java application on AWS
🎤 (~1.5 min) "**Edge**: Route 53, then CloudFront (static React assets from S3 with OAC, plus API caching where safe), then AWS WAF and Shield. **API layer**: ALB across 3 AZs (or API Gateway for partner APIs), then **EKS or ECS Fargate** running Spring Boot services, with HPA and Karpenter or cluster autoscaler, PDBs and topology spread across AZs. **Data**: **Aurora PostgreSQL** Multi-AZ with RDS Proxy for connection pooling, **ElastiCache Redis** (cluster mode, Multi-AZ), **MSK** (3 AZs, RF=3), S3 for documents. **Security**: private subnets, VPC endpoints, IRSA for pods, Secrets Manager, KMS everywhere. **Observability**: CloudWatch plus OpenTelemetry (ADOT) to X-Ray or Managed Prometheus and Grafana. **DR**: Aurora Global Database or cross-region snapshots, S3 CRR, IaC to recreate the stack. Everything in Terraform, deployed via GitHub Actions with OIDC."

### M2. EKS vs ROSA vs ECS: how would you advise a client? ✅
🎤 (~1 min) "**ECS/Fargate** if they want containers with minimal platform engineering and are AWS-committed. **EKS** if they need Kubernetes (portability, operators, Helm ecosystem, multi-cloud skills) and have a platform team. **ROSA** if they already run OpenShift on-prem, which was our situation: same developer experience (Routes, BuildConfigs, operators, built-in security like SCCs), Red Hat support, and an easier lift-and-shift of the scheduler. The decision factors are team skills, existing investment, compliance and the support model, and total cost including people, not just node prices."

### M3. SQS vs SNS vs EventBridge vs MSK vs Kinesis?
🎤 (~1 min) "**SQS**: queues for decoupling and buffering (standard or FIFO). **SNS**: pub/sub fan-out (often SNS → many SQS queues). **EventBridge**: an event bus with content-based rules, SaaS integrations, schema registry, archive and replay. **MSK**: managed Kafka for an event backbone, replay, stream processing, CDC; the choice when migrating existing Kafka workloads like ours. **Kinesis Data Streams**: AWS-native streaming with shard-based scaling, a simpler ops model than Kafka, but fewer ecosystem tools."

### M4. Autoscaling on Kubernetes and AWS?
🎤 (~1 min) "**HPA** on CPU or memory, or better, **custom metrics** (requests per second, latency). **KEDA** for event-driven scaling on Kafka lag, SQS depth, or cron. **VPA** for right-sizing recommendations. **Cluster scaling** via **Karpenter**, which provisions right-sized nodes quickly and uses Spot. Set requests correctly, or the scheduler and HPA make bad decisions. Watch scale-up latency versus traffic spikes, and pre-scale for known peaks like the morning booking rush."

### M5. AWS cost optimization levers?
🎤 (~1 min) "**Right-size** (Compute Optimizer, VPA), **Graviton** ARM instances (often 20–40% better price-performance for Java), **Savings Plans or Reserved Instances** for the baseline, **Spot** for stateless and batch work (Karpenter interruption handling), auto-scale down off-peak, S3 lifecycle tiers, gp3 instead of gp2, cut cross-AZ and NAT data transfer (VPC endpoints), cache to reduce database size, delete idle resources, and **tag everything** for showback (FinOps). Review cost in architecture decisions, not after the bill."

### M6. Terraform vs CloudFormation vs CDK?
🎤 (~45 s) "**Terraform**: multi-cloud, large provider ecosystem, mature modules, state management needed (S3 + DynamoDB lock). **CloudFormation**: AWS-native, no state to manage, drift detection, sometimes lags new features. **CDK**: write infrastructure in TypeScript or Java and synthesize to CloudFormation; great for developers and abstractions. I default to Terraform for multi-account and multi-cloud estates, with reusable modules, policy-as-code checks (Checkov or OPA), and plan reviews in PRs."

### M7. GitHub Actions to AWS securely? ✅
🎤 (~1 min) "Use **OIDC federation**: GitHub's OIDC token assumes an IAM role whose trust policy is restricted to the specific repo and branch or environment. No long-lived AWS keys in secrets. Pin actions by SHA, use least-privilege roles per environment, protected environments with required reviewers for production, artifact attestation and signing (Sigstore/cosign), and gitleaks and dependency scanning in the pipeline."

```yaml
permissions: { id-token: write, contents: read }
jobs:
  deploy:
    environment: prod
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::111122223333:role/gha-deploy-prod
          aws-region: ap-south-1
      - run: ./scripts/deploy.sh   # e.g. update image tag in GitOps repo, ArgoCD syncs
```

### M8. Deployment strategies: rolling vs blue-green vs canary?
🎤 (~1 min) "**Rolling**: replace pods gradually; cheap, but old and new versions run together, so you need backward compatibility. **Blue-green**: two full environments, switch traffic at once, instant rollback, double cost during the switch. **Canary**: shift a small percentage of traffic (5%, 25%, 100%) with automated analysis of error rate and latency (Argo Rollouts, Flagger, App Mesh or ALB weighted target groups); lowest risk. Use **feature flags** to decouple deploy from release. DB changes follow expand–contract regardless of strategy."

### M9. Kubernetes probes and graceful shutdown? ✅
🎤 (~1 min) "**Startup probe** for slow-starting JVMs, so liveness doesn't kill them during boot. **Readiness** gates traffic: fail it when the app can't serve (warming up, or a critical dependency is down). **Liveness** restarts a hung process. It should *not* check external dependencies, or a DB blip restart-loops everything. Graceful shutdown: on SIGTERM, readiness fails so endpoints are removed, a preStop sleep of a few seconds handles propagation lag, Spring's `server.shutdown=graceful` finishes in-flight requests, and Kafka consumers commit and close, all within `terminationGracePeriodSeconds`. Quartz in the scheduler also needed `waitForJobsToCompleteOnShutdown`."

### M10. Observability stack on AWS vs self-managed? ✅
🎤 (~1 min) "Self-managed (what I ran on getConsenso and OpenShift): Prometheus, Grafana, Loki, Tempo or Jaeger, with OpenTelemetry. Full control, but you operate it. AWS-managed: **CloudWatch** (logs, metrics, alarms, Application Signals), **X-Ray**, **Amazon Managed Prometheus** and **Managed Grafana**, plus the **ADOT** collector, which keeps OpenTelemetry as the portable instrumentation layer. I instrument with OTel and Micrometer regardless, so the backend is swappable."

### M11. Handling configuration and secrets on EKS?
🎤 (~45 s) "ConfigMaps via GitOps for non-secret config; **Secrets Store CSI Driver** or External Secrets Operator syncing from Secrets Manager or Parameter Store; **IRSA or EKS Pod Identity** for AWS API access without keys; KMS envelope encryption for Kubernetes secrets at rest; feature flags in AppConfig."

### M12. Multi-account strategy (landing zone)?
🎤 (~1 min) "AWS Organizations with Control Tower: separate accounts for security/log archive, shared networking, shared services (CI, registries), and **per-environment workload accounts** (dev, stage, prod), sometimes per team or tenant tier. **SCPs** as guardrails (deny region usage outside approved regions, deny disabling CloudTrail), centralized logging and GuardDuty and Security Hub, IAM Identity Center for SSO. Account boundaries are the strongest blast-radius and billing isolation."

### M13. Container image supply-chain security? ✅
🎤 (~45 s) "Build in CI only. Scan images (Trivy, or the **Harbor** built-in scanner, which we used, or ECR scanning), block on critical CVEs, **sign images** (cosign) and verify signatures at admission (Kyverno or Gatekeeper policies), generate SBOMs, use minimal base images, rebuild regularly to pick up patches, and pull only from approved registries."

### M14. OpenShift specifics vs vanilla Kubernetes? ✅
🎤 (~1 min) "OpenShift adds **Routes** (before Ingress was mature), **Security Context Constraints** (pods run as arbitrary non-root UIDs by default, so images must support that), integrated image registry and builds (S2I, BuildConfigs), **OperatorHub**, a built-in monitoring stack, OAuth-integrated console, and an opinionated, supported distribution. Practically, for our Quartz scheduler, it meant non-root, read-only-friendly images, Routes with TLS edge termination, and using the cluster monitoring stack for Prometheus scraping."

---

## Advanced

### A1. Migrate the on-prem OpenShift scheduler to AWS: plan it ✅
🎤 (~2 min) "**Assess**: dependencies (Postgres, Redis, Kafka, downstream services still on-prem?), data volumes, network latency requirements, compliance. **Target**: **ROSA** (lowest friction: same manifests, SCCs, Routes) or EKS (if the organization prefers vanilla Kubernetes). **Aurora PostgreSQL** for the Quartz JDBC store (Quartz supports Postgres; test lock behaviour and failover timing), ElastiCache Redis, **MSK** for Kafka. **Connectivity**: Direct Connect or VPN to on-prem services during the transition. Latency matters because jobs call downstream services. **Migration**: replicate data with AWS DMS or logical replication; run the scheduler in AWS in **standby mode** (Quartz clustered nodes with triggers paused) and validate; cut over in a quiet window by pausing on-prem schedulers, final sync, then enabling AWS nodes; misfire policies cover the gap. Use MirrorMaker 2 for Kafka topics. **Rollback**: keep on-prem warm, ready for reverse replication, for a defined period. **Post-move**: IRSA for permissions, CloudWatch and AMP for observability, Graviton nodes for cost, multi-AZ Aurora for the DB. And document it all as transition architectures in the SAD."

### A2. Design DR for a critical service on AWS
🎤 (~1.5 min) "Set RTO/RPO with the business first. Say RTO 30 minutes, RPO 1 minute: a **warm standby** in a second region. **Aurora Global Database** (typically sub-second replication lag, managed failover), DynamoDB global tables for AP data, S3 CRR, MSK replication via MirrorMaker 2 (or design topics to be regenerable from the outbox), container images replicated in ECR, infrastructure via Terraform, and the secondary EKS cluster running at reduced size, scaled up on failover. **Route 53** health checks with failover routing. **Runbooks and automated failover scripts**, plus **game days** each quarter. Remember the hard parts: in-flight Saga state, idempotency stores, and secrets or keys being available in both regions (multi-region KMS keys)."

### A3. Well-Architected review: how do you run one?
🎤 (~1 min) "Use the six pillars: **operational excellence, security, reliability, performance efficiency, cost optimization, sustainability**, plus lenses (serverless, SaaS, generative AI). Interview the team with the Well-Architected Tool questions, collect evidence (IaC, dashboards, runbooks), identify high and medium risks, and agree a prioritized improvement plan with owners. It maps naturally into an EPAM-style SARR: findings, risks, actions."

### A4. Serverless vs containers for a new service?
🎤 (~1 min) "Serverless (Lambda, API Gateway, DynamoDB, SQS) for spiky or unpredictable traffic, event-driven glue, small teams, and pay-per-use economics. Containers for steady high throughput (cheaper per request), long-running or stateful processing, custom runtimes, low-latency Java without cold starts (though SnapStart helps), and portability. Often hybrid: the core APIs on EKS, event glue and scheduled maintenance on Lambda."

### A5. Kubernetes multi-tenancy?
🎤 (~1 min) "**Soft tenancy** (namespace per team or tenant, with RBAC, ResourceQuotas, LimitRanges, NetworkPolicies default-deny, Pod Security admission, separate service accounts and IAM roles) works for trusted internal teams. **Hard tenancy** (separate clusters, or virtual clusters like vcluster, or separate accounts) for untrusted or regulated tenants. Node isolation with taints and dedicated node pools for sensitive workloads."

### A6. Platform engineering: what would you build for 20 product teams?
🎤 (~1 min) "**Paved roads**: service templates (a Spring Boot starter with security, observability and resilience defaults), golden pipelines, a self-service portal (Backstage) with a catalogue and docs, standard Helm charts or Kustomize bases, policy-as-code guardrails, managed data services via Crossplane or Terraform modules, and dashboards and alerts generated per service. Measure adoption and DORA metrics. The goal is that the secure, observable, compliant way is the fastest way to ship."

### A7. Cloud decisions that could be better ✅
🎤 (~1 min) "We self-managed Prometheus, Loki and Grafana for getConsenso, which was right for cost at small scale, but it's operational work for a solo founder. Managed Grafana Cloud or AMP/AMG would buy back time. On one project the NAT gateway data-transfer costs surprised us because we lacked VPC endpoints for S3 and ECR. And early container images ran as root, which OpenShift's SCCs forced us to fix. Now it's non-root by default in the templates."
