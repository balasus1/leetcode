# Volume 1: PCF (Pivotal Cloud Foundry) to AWS Migration & Architecture (Q1 - Q10)
*Targeting Senior Cloud Engineer & Cloud Architect Panels (10+ Years Experience)*
*Candidate Context: Balasubramanian Shanmugham (Enterprise Banking, High-Throughput Distributed Cloud Systems)*

---

## Question 1: Migrating PCF Diego Cells & Gorouters to AWS ALB + EKS
**Interviewer:** *"In Pivotal Cloud Foundry (PCF/Tanzu), developers rely on the Gorouter for dynamic route registration, TLS termination, and Diego Cells for container isolation. When architecting the target AWS infrastructure on Amazon EKS, how do you map these core PCF components to AWS primitives, and how do you handle dynamic service registration and traffic routing without losing sub-millisecond edge routing capabilities?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In PCF, the **Gorouter** acts as a reverse proxy that queries the NATS message bus to maintain a dynamic routing table of application instances running across Diego Cells. When migrating to Amazon EKS, we replace this centralized routing tier with a decoupled, cloud-native architecture combining the **AWS Application Load Balancer (ALB)**, the **AWS Load Balancer Controller**, and **Kubernetes Ingress / Service objects**.
>
> Instead of PCF's Gorouter polling NATS, the AWS Load Balancer Controller monitors Kubernetes Ingress events via the API server. In production, we configure the ALB in **IP target mode** using the AWS VPC CNI. This bypasses the traditional `kube-proxy` double-hop (NodePort) and registers Pod IPs directly as target group members in the ALB. This eliminates network latency, preserves client source IP addresses, and allows AWS ALB to perform native Layer-7 health checks directly on Spring Boot Actuator endpoints (`/actuator/health/readiness`).
>
> For dynamic service discovery between microservices (internal east-west traffic), we replace PCF's route-service model with **CoreDNS** and an **AWS App Mesh** or **Cilium Service Mesh** using eBPF. This avoids hair-pinning internal traffic through external load balancers.
>
> The key architectural trade-off is AWS target group registration latency: while PCF Gorouter updates routes in memory within hundreds of milliseconds via NATS, AWS ALB target group registration and health check warm-up take approximately 15 to 30 seconds during Pod scale-out. We mitigate this by tuning target group deregistration delays, using pre-warmed ALBs, and implementing Kubernetes pre-stop lifecycle hooks to ensure zero dropped packets during rolling deployments."

### Working Kubernetes Ingress & TargetGroupBinding Manifest
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: banking-core-ingress
  namespace: core-banking
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip # Direct Pod IP targeting (bypasses NodePort)
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS":443}]'
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:ap-south-1:123456789012:certificate/abc-123-def
    alb.ingress.kubernetes.io/ssl-policy: ELBSecurityPolicy-TLS13-1-2-2021-06
    alb.ingress.kubernetes.io/healthcheck-path: /actuator/health/readiness
    alb.ingress.kubernetes.io/healthcheck-interval-seconds: "10"
    alb.ingress.kubernetes.io/healthy-threshold-count: "2"
    alb.ingress.kubernetes.io/unhealthy-threshold-count: "2"
    alb.ingress.kubernetes.io/target-group-attributes: deregistration_delay.timeout_seconds=30
spec:
  rules:
    - host: api.banking.internal.com
      http:
        paths:
          - path: /api/v1/payments
            pathType: Prefix
            backend:
              service:
                name: payment-service
                port:
                  number: 8080
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
  namespace: core-banking
spec:
  replicas: 5
  selector:
    matchLabels:
      app: payment-service
  template:
    metadata:
      labels:
        app: payment-service
    spec:
      terminationGracePeriodSeconds: 45
      containers:
        - name: payment-service
          image: 123456789012.dkr.ecr.ap-south-1.amazonaws.com/banking/payment-service:v2.1.0
          lifecycle:
            preStop:
              exec:
                command: ["/bin/sh", "-c", "sleep 15"] # Allow ALB target deregistration before SIGTERM
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 5
```

---

## Question 2: Translating PCF `manifest.yml` to Kubernetes Manifests & Helm Charts
**Interviewer:** *"PCF developers are accustomed to running `cf push` using a declarative `manifest.yml` specifying buildpacks, memory limits, instances, and service bindings. How do you construct an automated migration pipeline that refactors these `manifest.yml` files into standardized Helm charts, and what hidden configuration discrepancies (such as memory overhead and env vars) must you guard against?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In PCF, `cf push` abstracts the container runtime completely: the Java Buildpack automatically inspects the droplet memory limit (e.g., `memory: 2G`), calculates JVM heap, metaspace, thread stacks, and native memory, and dynamically injects `-Xmx` flags. In Kubernetes, if you simply map `memory: 2G` in `manifest.yml` to a Kubernetes `resources.limits.memory: 2Gi`, your Java application will almost certainly suffer from Kubernetes **OOMKilled (Exit Code 137)**.
>
> When designing our migration factory, we create an automated parsing tool that inspects `manifest.yml` and generates parameterized Helm templates. We decompose the PCF manifest into four distinct Kubernetes constructs:
> 1. **Resource Mapping:** We decouple requests from limits. For a PCF `memory: 2G`, we allocate a Kubernetes limit of `2.5Gi` and configure the container JVM with `-XX:MaxRAMPercentage=75.0`. This reserves 25% (~600MB) for off-heap allocations, Metaspace, GC overhead, and thread stacks.
> 2. **Service Bindings:** PCF injects `VCAP_SERVICES` as a massive JSON blob. In EKS, we unwrap `VCAP_SERVICES` into Kubernetes `Secret` and `ConfigMap` objects populated dynamically via the **AWS External Secrets Operator (ESO)** from AWS Secrets Manager.
> 3. **Application Routing:** The PCF `routes` block translates directly to an AWS Ingress resource with custom ALB annotations.
> 4. **Health Checks:** PCF `health-check-type: http` maps directly to Kubernetes `livenessProbe` and `readinessProbe` pointing to Spring Boot Actuator."

### Production Helm `values.yaml` vs PCF Translation
```yaml
# Target Helm values.yaml generated from PCF manifest.yml
replicaCount: 3

image:
  repository: 123456789012.dkr.ecr.ap-south-1.amazonaws.com/banking/account-service
  tag: "1.4.2"
  pullPolicy: IfNotPresent

resources:
  requests:
    cpu: "500m"
    memory: "1536Mi"
  limits:
    cpu: "2000m"
    memory: "2560Mi" # Buffer over PCF 2G to prevent Linux OOM-killer on JVM off-heap

jvm:
  maxRamPercentage: "75.0"
  initialRamPercentage: "50.0"
  metaspaceSize: "256m"
  maxMetaspaceSize: "384m"

envFromSecrets:
  - secretRef: "aurora-db-credentials"
  - secretRef: "redis-auth-token"

probes:
  readiness:
    path: /actuator/health/readiness
    initialDelaySeconds: 25
    periodSeconds: 10
  liveness:
    path: /actuator/health/liveness
    initialDelaySeconds: 40
    periodSeconds: 15
```

---

## Question 3: Replacing Spring Cloud Services (Eureka, Config Server) with AWS Native Solutions
**Interviewer:** *"Many legacy PCF Java workloads leverage Spring Cloud Eureka for client-side discovery, Spring Cloud Config Server for centralized Git-backed configuration, and Spring Cloud Hystrix for circuit breaking. As we move to AWS, do we keep running these Spring Cloud servers on EKS or migrate them? Justify your architectural choice."*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "I strongly recommend **decommissioning Eureka and Spring Cloud Config Server** in favor of Kubernetes-native and AWS-managed primitives. 
>
> Running Eureka on Kubernetes introduces architectural redundancy and anti-patterns:
> 1. Kubernetes already provides a battle-tested, kernel-level service registry via **CoreDNS and `kube-proxy` (or eBPF/Cilium)**. Running Eureka introduces a second, desynchronized registry. Eureka relies on heartbeat polling (every 30 seconds), causing stale routing tables during rapid container scale-downs, whereas Kubernetes endpoints update instantly via API server watches.
> 2. For Spring Cloud Config Server, maintaining a dedicated cluster backed by Git introduces an operational failure domain. Instead, we transition to **AWS Secrets Manager and AWS Systems Manager Parameter Store**, integrated seamlessly into EKS via the **External Secrets Operator (ESO)** or the AWS Secrets Store CSI Driver. Applications read native environment variables or mounted files, eliminating Spring bootstrap latency and external network dependencies during startup.
> 3. For Hystrix (which is deprecated), we refactor applications to **Resilience4j**, which integrates natively with Spring Boot Actuator and Prometheus, or offload circuit breaking and retries entirely to an **Envoy-based service mesh**.
>
> The trade-off is configuration reload immediacy: Spring Cloud Bus enabled instant `/actuator/bus-refresh` via RabbitMQ. With AWS Parameter Store and ESO, secret synchronization has a configurable reconciliation window (e.g., 60 seconds). For zero-downtime property updates without container restarts, we use Spring Cloud AWS Parameter Store integration with dynamic `@RefreshScope`."

### Spring Boot 3 `application.yml` with AWS Parameter Store
```yaml
spring:
  application:
    name: trade-settlement-service
  config:
    import:
      # Native AWS SSM Parameter Store resolution without Config Server
      - "aws-parameterstore:/config/trade-settlement-service/"
      - "aws-secretsmanager:/secrets/trade-settlement-service/db"

aws:
  paramstore:
    enabled: true
    prefix: /config
    default-context: application
    profile-separator: _
    fail-fast: true
  secretsmanager:
    enabled: true
    prefix: /secrets

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,env
  endpoint:
    health:
      probes:
        enabled: true
```

---

## Question 4: PCF User-Provided Service Instances (UPSI) & Tiles to AWS Managed Services
**Interviewer:** *"In PCF, stateful services were bound using Pivotal Marketplace tiles (e.g., Pivotal MySQL, RabbitMQ tile) or User-Provided Service Instances (UPSI). How do you plan the migration of these data stores to AWS RDS Aurora and Amazon MSK, and how do you handle credential rotation and VPC isolation?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In PCF, service bindings inject connection credentials into the container's environment via `VCAP_SERVICES`. When migrating to AWS, we replace tile-managed datastores with enterprise-grade AWS managed services: Pivotal MySQL moves to **Amazon Aurora PostgreSQL or MySQL with Multi-AZ**, RabbitMQ tiles move to **Amazon MQ (RabbitMQ) or Amazon MSK (Managed Streaming for Kafka)**, and Redis tiles move to **Amazon ElastiCache Redis Cluster**.
>
> The architectural migration follows three strict phases:
> 1. **VPC & Subnet Isolation:** In PCF, apps and tiles frequently co-existed in large shared CIDRs. In AWS, we enforce a strict 3-tier VPC architecture: EKS Pods reside in private application subnets, while Aurora and MSK clusters reside in dedicated, isolated database subnets with no internet gateways or NAT routes. Security groups strictly whitelist traffic from the EKS node/pod security groups on port 5432 or 9092.
> 2. **Credential Decoupling & IAM Authentication:** Instead of static passwords, we migrate to **AWS IAM Database Authentication** for Aurora where feasible, allowing Spring Boot to fetch short-lived IAM auth tokens. For services requiring passwords, credentials reside in **AWS Secrets Manager** with automated Lambda rotation every 30 days.
> 3. **Spring Boot Autowiring:** We eliminate `cf-java-client` / Cloud Foundry Spring connectors and replace them with standard Spring Boot datasource starters backed by HikariCP, reading JDBC connection URLs directly from decrypted secrets."

### Terraform: Isolated Aurora PostgreSQL Subnet Group & Security Group
```hcl
resource "aws_db_subnet_group" "aurora_subnet_group" {
  name       = "aurora-db-subnet-group"
  subnet_ids = var.database_subnet_ids

  tags = {
    Environment = var.environment
    Compliance  = "PCI-DSS"
  }
}

resource "aws_security_group" "aurora_sg" {
  name        = "aurora-banking-sg"
  description = "Strict DB access from EKS Pods only"
  vpc_id      = var.vpc_id

  ingress {
    description     = "PostgreSQL from EKS Node Security Group"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.eks_nodes_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_rds_cluster" "aurora_cluster" {
  cluster_identifier      = "banking-core-aurora"
  engine                  = "aurora-postgresql"
  engine_version          = "15.4"
  database_name           = "bankingdb"
  master_username         = "dbadmin"
  manage_master_user_password = true # Auto-managed in AWS Secrets Manager
  db_subnet_group_name    = aws_db_subnet_group.aurora_subnet_group.name
  vpc_security_group_ids  = [aws_security_group.aurora_sg.id]
  storage_encrypted       = true
  kms_key_id              = var.kms_key_arn
  deletion_protection     = true
  backup_retention_period = 35
}
```

---

## Question 5: Zero-Downtime Database Migration during Cutover: Dual-Write vs AWS DMS
**Interviewer:** *"We have a critical financial ledger database running in on-premise Oracle/MySQL backing a PCF application. We cannot afford more than 2 minutes of maintenance window during the cutover to AWS Aurora. How do you design the database migration strategy, and when do you choose Change Data Capture (CDC via AWS DMS) over application-level dual-writes?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "For a mission-critical financial database with a strict 2-minute maintenance window, **Change Data Capture (CDC) using AWS Database Migration Service (DMS) combined with AWS Direct Connect is significantly superior to application-level dual-writes**.
>
> Application-level dual-writes introduce severe distributed systems hazards: dual-writes cannot guarantee atomicity across high-latency hybrid network links without distributed 2-Phase Commit (2PC), which introduces locking bottlenecks. If the secondary write to AWS fails or experiences network timeout, your application must handle complex compensation logic, local queuing, or risk catastrophic data drift.
>
> Our CDC architecture consists of:
> 1. **Initial Full Load:** AWS DMS extracts table snapshots from on-premises over a 10Gbps AWS Direct Connect private VIF into Amazon Aurora.
> 2. **Continuous CDC Replication:** DMS reads the source transaction logs (Oracle Redo Logs or MySQL binlogs), streaming changes continuously to Aurora with sub-second replication lag.
> 3. **Data Validation:** We run AWS DMS Data Validation in parallel to verify checksums and record counts row-by-row.
> 4. **Cutover Window (Under 60 Seconds):** During the cutover window, we gracefully scale down PCF application instances (`cf scale app -i 0`), allow in-flight transactions to drain, wait for DMS replication lag to reach zero (verified via CloudWatch `CDCLatencySource`), stop the DMS task, repoint DNS/traffic to AWS EKS, and start the new microservices."

```
[On-Premises Database] (MySQL/Oracle)
         │
         │ (Transaction Logs / Binlog)
         ▼
[AWS Direct Connect (DX)] 10 Gbps Private VIF
         │
         ▼
[AWS DMS Replication Instance] (Multi-AZ, Memory-Optimized)
         │
         │ (Sub-second CDC Streaming)
         ▼
[AWS Amazon Aurora] (Multi-AZ Target)
```

---

## Question 6: PCF Loggregator / Firehose to AWS CloudWatch & OpenSearch
**Interviewer:** *"In PCF, application logs, metrics, and router events flow through the Loggregator Firehose (Loggregator Doppler -> Traffic Controller). How do you architect a high-throughput, compliant logging architecture on AWS EKS that matches or exceeds PCF's logging pipeline without crashing under 50,000 logs/sec?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In PCF, developers run `cf logs` to tail application logs stream-aggregated by the Loggregator Firehose. However, Firehose nozzles often become scaling bottlenecks under massive log surges, dropping logs during network backpressure.
>
> On AWS EKS, we replace Loggregator with a decoupled, high-performance logging pipeline using **Fluent Bit as a DaemonSet**, **Amazon Kinesis Data Streams**, and **Amazon OpenSearch Service (or Datadog)**, with cold archival to **Amazon S3 Glacier**.
>
> Here is the architecture:
> 1. Applications log strictly to `stdout`/`stderr` in structured JSON format (Logstash/SLF4J layout).
> 2. A **Fluent Bit** DaemonSet runs on every EKS worker node, mounting `/var/log/containers/`. Fluent Bit is written in C and consumes less than 30MB of RAM and 1% CPU per node—far more efficient than Java-based Logstash or Fluentd.
> 3. Fluent Bit enriches logs with Kubernetes metadata (pod name, namespace, container ID), filters health-check spam, and flushes directly to an **Amazon Kinesis Data Stream**.
> 4. Kinesis acts as an elastic buffer absorbing traffic spikes up to 100,000 logs/second without backpressuring application pods. A Kinesis Firehose delivery stream batches records and streams them to **Amazon OpenSearch** for hot 14-day indexing, and dumps compressed Parquet batches to an encrypted **S3 bucket** with lifecycle rules for 7-year regulatory compliance."

### Production Fluent Bit `fluent-bit.conf` ConfigMap
```ini
[SERVICE]
    Flush         1
    Log_Level     info
    Daemon        off
    Parsers_File  parsers.conf

[INPUT]
    Name              tail
    Tag               kube.*
    Path              /var/log/containers/*.log
    Parser            docker
    DB                /var/log/flb_kube.db
    Mem_Buf_Limit     50MB
    Skip_Long_Lines   On
    Refresh_Interval  10

[FILTER]
    Name                kubernetes
    Match               kube.*
    Kube_URL            https://kubernetes.default.svc:443
    Kube_CA_File        /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
    Kube_Token_File     /var/run/secrets/kubernetes.io/serviceaccount/token
    Merge_Log           On
    Keep_Log            Off
    K8S-Logging.Parser  On
    K8S-Logging.Exclude On

[OUTPUT]
    Name            kinesis_streams
    Match           kube.*
    region          ap-south-1
    stream          banking-eks-log-stream
    time_key        @timestamp
    auto_retry      On
    retry_limit     5
```

---

## Question 7: PCF Autoscaling (`cf scale`) vs EKS Karpenter & Kubernetes HPA/KEDA
**Interviewer:** *"PCF makes scaling trivial with `cf scale` and the PCF App Autoscaler tile. How do you implement a dual-layer autoscaling strategy in EKS that handles both horizontal Pod scaling and rapid EC2 worker node provisioning, especially when sudden flash-crowd traffic hits our services?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In PCF, autoscaling operates on a pre-provisioned pool of Diego Cells: `cf scale` simply schedules additional container Garden instances onto existing VMs. If the Diego cell pool runs out of memory, developers face capacity errors until infrastructure admins manually scale the BOSH director.
>
> On AWS EKS, we implement a **dual-layer reactive and event-driven autoscaling architecture**:
> 1. **Pod Layer (Horizontal Pod Autoscaler + KEDA):** For HTTP traffic, the standard Kubernetes HPA scales Pod replicas based on CPU/Memory or custom Prometheus metrics (e.g., requests per second from the ALB). For event-driven batch consumers, we deploy **KEDA (Kubernetes Event-driven Autoscaling)**. KEDA monitors external queues (like Amazon SQS queue depth or Kafka consumer lag) and scales Pods from zero to hundreds in seconds, bypassing CPU metric lag.
> 2. **Node Layer (Karpenter v1):** Legacy Kubernetes Cluster Autoscaler relies on AWS Auto Scaling Groups (ASGs), taking 3 to 5 minutes to launch, join, and bootstrap an EC2 node. We replace Cluster Autoscaler with **Karpenter**. Karpenter communicates directly with the AWS EC2 fleet API, bypassing ASGs entirely. It inspects unschedulable Pod resource requests and provisions right-sized EC2 instances (mixing Graviton3 ARM64 and Spot instances) in **under 45 seconds**.
> 3. Karpenter also performs continuous **consolidation**: when traffic subsides, it bin-packs pods onto fewer nodes and terminates underutilized EC2 instances, reducing compute waste by up to 35%."

### Karpenter v1 `NodePool` & `EC2NodeClass`
```yaml
apiVersion: karpenter.sh/v1
kind: NodePool
metadata:
  name: banking-workload-pool
spec:
  template:
    spec:
      requirements:
        - key: kubernetes.io/arch
          operator: In
          values: ["arm64", "amd64"]
        - key: karpenter.sh/capacity-type
          operator: In
          values: ["on-demand", "spot"]
        - key: karpenter.k8s.aws/instance-category
          operator: In
          values: ["c", "m", "r"]
        - key: karpenter.k8s.aws/instance-generation
          operator: Gt
          values: ["5"]
      nodeClassRef:
        group: karpenter.k8s.aws
        kind: EC2NodeClass
        name: default-node-class
      expireAfter: 720h # 30-day node rotation for security patching
  disruption:
    consolidationPolicy: WhenEmptyOrUnderutilized
    consolidateAfter: 60s
---
apiVersion: karpenter.k8s.aws/v1
kind: EC2NodeClass
metadata:
  name: default-node-class
spec:
  amiFamily: Bottlerocket # Fast booting, minimal attack surface
  role: KarpenterNodeRole-banking-cluster
  subnetSelectorTerms:
    - tags:
        karpenter.sh/discovery: banking-cluster
  securityGroupSelectorTerms:
    - tags:
        karpenter.sh/discovery: banking-cluster
```

---

## Question 8: Hybrid Cloud Network Interconnect: Direct Connect + Transit Gateway
**Interviewer:** *"During our 12-month phased migration, 40% of microservices will reside in AWS EKS while 60% remain in on-premises PCF. How do you design the hybrid network connectivity to ensure sub-5ms latency, zero IP conflicts, and high throughput between PCF and AWS?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "A phased enterprise migration requires a robust, high-bandwidth, redundant hybrid networking backbone. We design this using **AWS Direct Connect (DX) paired with AWS Transit Gateway (TGW)** and IPsec VPN failover.
>
> Key architectural pillars:
> 1. **Dedicated Redundant Direct Connect:** We provision dual 10 Gbps Direct Connect connections terminating at diverse AWS Direct Connect locations to ensure 99.99% high availability. Dynamic routing is established using eBGP with BFD (Bidirectional Forwarding Detection) for sub-second link failure detection.
> 2. **Hub-and-Spoke Transit Gateway:** All AWS VPCs (EKS VPC, Shared Services VPC, Data VPC) attach to an AWS Transit Gateway. The Direct Connect Gateway attaches directly to the Transit Gateway, enabling transitive routing between any on-premises PCF container and any AWS EKS Pod without point-to-point VPC peering sprawl.
> 3. **Non-Overlapping IP Planning:** On-premises PCF Diego cell subnets and AWS VPC subnets must never collide. We assign AWS VPCs private IPv4 blocks from dedicated RFC 1918 allocations (e.g., `10.200.0.0/16`). If enterprise CIDR space is exhausted, we use **AWS PrivateLink** or Transit Gateway NAT rules to map overlapping CIDRs.
> 4. **Jumbo Frames & MTU Tuning:** We enable 9001 MTU (Jumbo Frames) across Direct Connect and VPCs to maximize network throughput and minimize CPU packet processing overhead for high-volume database replication."

```
[On-Premises Data Center]
  ├── PCF Foundation (Diego Cells / Gorouters)
  └── Enterprise Core Switch
         │
         ├── [10G DX Primary (BGP + BFD)] ──────┐
         └── [10G DX Secondary (Diverse Path)] ──┼──> [Direct Connect Gateway]
                                                 │             │
                                                 │             ▼
                                                 └──> [AWS Transit Gateway]
                                                               │
                                         ┌─────────────────────┼─────────────────────┐
                                         ▼                     ▼                     ▼
                                   [EKS App VPC]        [Aurora DB VPC]      [Shared Security VPC]
```

---

## Question 9: Blue-Green & Canary Cutover using Route 53 & ALB Weighted Target Groups
**Interviewer:** *"When cutover day arrives for our primary consumer banking API, how do you orchestrate the traffic transition from PCF (Blue) to AWS EKS (Green) so that we can validate real-world production traffic with 1% canary shifting before committing 100%?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "To execute a zero-downtime, risk-mitigated cutover, we combine **Amazon Route 53 Weighted DNS Routing** at the global edge with **AWS ALB Weighted Target Groups** at the infrastructure edge.
>
> The execution methodology is structured into 4 stages:
> 1. **Baseline Smoke Testing:** The AWS EKS environment is stood up with identical SSL certificates and configurations. We run automated synthetic integration tests against a private pre-production endpoint (`green-api.banking.com`) validating database connections, latency, and IAM role authorizations.
> 2. **DNS Weighted Canary (1% Traffic):** In Route 53, we establish two weighted record sets for `api.banking.com`: 99% weight pointing to the on-premise PCF F5/Gorouter VIP, and 1% weight pointing to the AWS Application Load Balancer. We monitor synthetic error rates and Datadog APM dashboards for 30 minutes.
> 3. **Incremental Stepped Shift:** We adjust Route 53 weights: 10% -> 25% -> 50% -> 100% over a 4-hour window. If 5xx error spikes or latency degradations occur, Route 53 can revert to 100% PCF within 60 seconds (utilizing a 30-second TTL on the DNS record).
> 4. **Sticky Sessions & Header-Based Overrides:** For internal employee testing during the migration window, ALB routing rules inspect a custom HTTP header (`X-Route-Target: EKS-Green`). This allows QA engineers to route directly to AWS even when public traffic remains 100% on PCF."

### Terraform: Route 53 Weighted DNS Cutover Records
```hcl
resource "aws_route53_record" "pcf_blue" {
  zone_id = var.hosted_zone_id
  name    = "api.banking.com"
  type    = "A"
  ttl     = 30

  weighted_routing_policy {
    weight = 90 # Step 1: 90% traffic to PCF
  }

  set_identifier = "pcf-blue-production"
  records        = [var.onprem_pcf_gorouter_vip]
}

resource "aws_route53_record" "aws_eks_green" {
  zone_id = var.hosted_zone_id
  name    = "api.banking.com"
  type    = "A"

  weighted_routing_policy {
    weight = 10 # Step 1: 10% canary traffic to EKS
  }

  set_identifier = "aws-eks-green-production"

  alias {
    name                   = aws_lb.eks_alb.dns_name
    zone_id                = aws_lb.eks_alb.zone_id
    evaluate_target_health = true
  }
}
```

---

## Question 10: Rollback Engineering & Automated Circuit-Breaker Aborts
**Interviewer:** *"Imagine you are 30 minutes into a live cutover: 25% of traffic is routed to AWS EKS, and suddenly downstream database connection pool timeouts trigger a 4% error spike on payment transactions. What is your automated and manual rollback runbook?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "When orchestrating mission-critical cutovers, having a pre-tested, deterministic rollback runbook is just as important as the deployment plan itself. In our architecture, rollback is both **automated via CloudWatch Alarms** and executable via a **one-click emergency runbook**.
>
> If downstream connection timeouts trigger an error rate exceeding our SLO threshold (>1% 5xx over 3 consecutive minutes):
> 1. **Automated Circuit-Breaker Trigger:** A CloudWatch Alarm monitoring ALB `HTTPCode_Target_5XX_Count` triggers an AWS EventBridge rule. This rule immediately invokes an AWS Lambda function that updates the Route 53 weighted record, setting AWS weight to 0 and PCF weight to 100.
> 2. **Session Draining & In-Flight Reversals:** The AWS ALB target groups initiate connection draining (`deregistration_delay.timeout_seconds=30`), allowing in-flight transactions 30 seconds to complete cleanly while all new requests flow back to PCF.
> 3. **Database State Reconciliation:** Because our database migration was architected with bidirectional synchronization (AWS DMS running reverse CDC from Aurora back to on-premise Oracle/MySQL during the migration window), transactions committed in AWS during the 25% cutover window are already replicated back to on-premises. This completely prevents data loss or reconciliation mismatches.
> 4. **Triage & Incident RCA:** Pod logs and JVM thread dumps are captured automatically using an automated shell script before any pods are restarted."

### Automated Rollback AWS Lambda (Python 3.11)
```python
import boto3
import os

route53 = boto3.client('route53')
HOSTED_ZONE_ID = os.environ['HOSTED_ZONE_ID']
DOMAIN_NAME = "api.banking.com"
ONPREM_VIP = os.environ['ONPREM_VIP']
ALB_DNS = os.environ['ALB_DNS']
ALB_ZONE_ID = os.environ['ALB_ZONE_ID']

def handler(event, context):
    print("CRITICAL: Automated Rollback Triggered by CloudWatch Alarm!")
    
    change_batch = {
        'Comment': 'EMERGENCY ROLLBACK: Revert 100% traffic to PCF',
        'Changes': [
            {
                'Action': 'UPSERT',
                'ResourceRecordSet': {
                    'Name': DOMAIN_NAME,
                    'Type': 'A',
                    'TTL': 30,
                    'SetIdentifier': 'pcf-blue-production',
                    'Weight': 100,
                    'ResourceRecords': [{'Value': ONPREM_VIP}]
                }
            },
            {
                'Action': 'UPSERT',
                'ResourceRecordSet': {
                    'Name': DOMAIN_NAME,
                    'Type': 'A',
                    'SetIdentifier': 'aws-eks-green-production',
                    'Weight': 0,
                    'AliasTarget': {
                        'HostedZoneId': ALB_ZONE_ID,
                        'DNSName': ALB_DNS,
                        'EvaluateTargetHealth': True
                    }
                }
            }
        ]
    }
    
    response = route53.change_resource_record_sets(
        HostedZoneId=HOSTED_ZONE_ID,
        ChangeBatch=change_batch
    )
    print(f"Rollback completed. Route53 Change ID: {response['ChangeInfo']['Id']}")
    return {"status": "SUCCESS", "traffic": "REVERTED_TO_PCF"}
```
