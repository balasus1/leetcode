# Volume 3: Kubernetes & AWS EKS Production Architecture (Q21 - Q30)
*Targeting Senior Cloud Engineer & Cloud Architect Panels (10+ Years Experience)*
*Candidate Context: Balasubramanian Shanmugham (Enterprise Banking, High-Throughput Distributed Cloud Systems)*

---

## Question 21: EKS Control Plane Architecture & Private Endpoint Security
**Interviewer:** *"Walk me through the underlying architecture of an Amazon EKS managed control plane. How does AWS isolate control planes across tenants, how do worker nodes communicate with the API server, and how do you configure endpoint access for PCI-DSS banking compliance?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Amazon EKS provides a fully managed, highly available Kubernetes control plane running across at least three Availability Zones in an AWS-managed account. Each EKS cluster has dedicated **etcd** instances and Kubernetes API servers that scale automatically based on query load. AWS guarantees 99.95% SLA for the EKS control plane.
>
> Communication between the AWS-managed control plane and customer VPC worker nodes is established via **Cross-Account Elastic Network Interfaces (ENIs)**. During cluster provisioning, EKS provisions managed ENIs inside the private subnets of your VPC. The `kubelet` and `kube-proxy` running on worker nodes communicate securely with the API server through these ENIs.
>
> In a PCI-DSS or banking environment, public internet exposure of the Kubernetes API server is an immediate audit failure. We enforce **Private-Only API Endpoint Access**:
> 1. We disable public endpoint access: `endpoint_public_access = false` and enable `endpoint_private_access = true`.
> 2. The API server DNS (`*.yl4.ap-south-1.eks.amazonaws.com`) resolves strictly to private IP addresses residing within the cluster VPC.
> 3. Administrative access (developers running `kubectl` or CI/CD runners) is permitted only from designated corporate networks via **AWS Direct Connect, Transit Gateway, or an internal bastion/VPN host** with strict Security Group whitelisting."

### Terraform: Hardened Private EKS Cluster Endpoint
```hcl
resource "aws_eks_cluster" "banking_eks" {
  name     = "banking-core-prod-eks"
  role_arn = aws_iam_role.eks_cluster_role.arn
  version  = "1.30"

  vpc_config {
    subnet_ids              = var.private_subnet_ids
    endpoint_private_access = true
    endpoint_public_access  = false # Zero public exposure for PCI-DSS compliance
    security_group_ids      = [aws_security_group.eks_cluster_sg.id]
  }

  enabled_cluster_log_types = [
    "api",
    "audit",
    "authenticator",
    "controllerManager",
    "scheduler"
  ]

  encryption_config {
    provider {
      key_arn = var.kms_key_arn # KMS envelope encryption for Kubernetes Secrets
    }
    resources = ["secrets"]
  }
}
```

---

## Question 22: Worker Node OS: Bottlerocket vs Amazon Linux 2023
**Interviewer:** *"Why is AWS pushing Bottlerocket OS for EKS worker nodes over traditional Amazon Linux? What are the architectural differences, security advantages, and operational trade-offs?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Bottlerocket is an open-source, purpose-built Linux distribution engineered by AWS specifically for hosting containers. In traditional distributions like Amazon Linux 2023 or Ubuntu, the OS includes a full package manager (yum/apt), systemd services, Python runtimes, and an interactive shell.
>
> Bottlerocket fundamentally rethinks node security through three architectural mechanisms:
> 1. **Immutable Root Filesystem:** Bottlerocket's root filesystem is mounted as read-only. Package managers do not exist; you cannot run `yum install` or modify core binaries. Updates are applied via automated atomic partition flips (A/B partitioning) that roll back automatically if the new image fails health checks.
> 2. **Minimal Attack Surface:** Bottlerocket contains *only* the essential software required to run containers (`containerd`, `kubelet`, and AWS CNI). This drastically reduces the number of OS-level CVEs by over 80%.
> 3. **Kernel Hardening via SELinux:** SELinux is enabled in enforcing mode by default, strictly preventing container escapes from altering host state.
> 4. **No Direct SSH Access:** Traditional SSH is disabled. For emergency troubleshooting, Bottlerocket provides an out-of-band ephemeral admin container running in a separate user namespace accessed via AWS Systems Manager (SSM) Session Manager.
>
> The trade-off is debugging ergonomics: platform engineers cannot simply SSH and run ad-hoc shell commands. However, for a compliant, automated production environment, Bottlerocket is our undisputed gold standard."

---

## Question 23: AWS Load Balancer Controller: IP Mode vs Instance Mode
**Interviewer:** *"When provisioning an AWS Application Load Balancer via the AWS Load Balancer Controller, you must choose between `target-type: instance` and `target-type: ip`. Walk me through the network packet path for both, and why IP mode is required for high-throughput banking workloads."*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "The choice between `instance` mode and `ip` mode dictates whether traffic traverses Kubernetes **NodePort / `kube-proxy`** or routes directly to the container network interface.
>
> **Instance Mode (Legacy NodePort):**
> 1. The ALB registers the EC2 worker node instances as targets on a random high-port (NodePort 30000-32767).
> 2. When an HTTP request hits the ALB, it forwards the packet to *any* EC2 node in the target group.
> 3. If the destination Pod does not live on that specific node, the Linux kernel's `iptables` or IPVS rules (`kube-proxy`) rewrite the destination IP and forward the packet across the internal VPC network to the node where the Pod actually resides (**the second hop**).
> 4. **Consequences:** Double network latency, uneven load balancing (nodes with 5 pods receive the same traffic as nodes with 1 pod), and loss of original client IP without `externalTrafficPolicy: Local`.
>
> **IP Mode (AWS VPC CNI Native):**
> 1. Because the AWS VPC CNI assigns native VPC IP addresses directly to each Pod (via Secondary ENIs or Prefix Delegation), the ALB registers **Pod IPs directly as target group members**.
> 2. The ALB routes traffic directly from the ALB ENI to the Pod's veth interface in a **single hop**.
> 3. **Benefits:** Eliminates `kube-proxy` iptables overhead, ensures perfectly balanced traffic distribution at the individual Pod level, and preserves the real client IP address in HTTP request headers."

```
INSTANCE MODE (Double Hop):
Client -> ALB -> Node 1 (NodePort) -> [iptables/kube-proxy Hop] -> Node 2 (Pod)

IP MODE (Direct Single Hop):
Client -> ALB ---------------------- Direct Pod IP --------------------> Pod
```

---

## Question 24: Advanced Pod Autoscaling with KEDA & Prometheus Metrics
**Interviewer:** *"Horizontal Pod Autoscaler (HPA) based on CPU and Memory is insufficient for asynchronous payment event consumers. Why does CPU-based scaling fail here, and how do you implement event-driven autoscaling with KEDA based on Kafka consumer lag?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Scaling asynchronous consumer microservices based on CPU or Memory metrics is an anti-pattern because of **inverse scaling dynamics and metric lag**.
>
> When an asynchronous message broker (like Apache Kafka or Amazon SQS) experiences a sudden surge of 500,000 payment events, the existing 5 consumer pods become completely saturated. If each message processing thread blocks on I/O (e.g., waiting for database responses), CPU utilization may remain low (e.g., 25%), preventing the standard HPA from triggering a scale-out. Conversely, when the backlog is cleared, CPU might briefly spike during garbage collection, preventing down-scaling.
>
> We solve this using **KEDA (Kubernetes Event-driven Autoscaling)**. KEDA installs lightweight Custom Resource Definitions (CRDs) and an external metrics adapter that bridges external event sources directly to the Kubernetes HPA.
>
> For Kafka, KEDA queries the Kafka broker's consumer group lag or queries Prometheus via PromQL. If `consumer_lag > 50` messages per partition, KEDA instructs the HPA to immediately scale the consumer deployment from 5 to 50 replicas. Furthermore, KEDA can scale deployments **to and from zero**, saving significant compute costs for batch jobs."

### KEDA ScaledObject Manifest (Kafka Consumer Lag)
```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: payment-consumer-scaler
  namespace: payment-cde
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: payment-event-consumer
  minReplicaCount: 2
  maxReplicaCount: 60
  cooldownPeriod: 300
  pollingInterval: 15
  triggers:
    - type: kafka
      metadata:
        bootstrapServers: b-1.bankingmsk.ap-south-1.kafka.amazonaws.com:9096
        consumerGroup: payment-settlement-group
        topic: payment-transactions
        lagThreshold: "100" # Scale 1 pod per 100 lag
        offsetResetPolicy: latest
      authenticationRef:
        name: keda-msk-iam-auth
```

---

## Question 25: Node Autoscaling: Karpenter v1 vs Kubernetes Cluster Autoscaler
**Interviewer:** *"Cluster Autoscaler has been the default Kubernetes node scaler for years. Why has AWS shifted to Karpenter as the recommended autoscaler for EKS, how does Karpenter's consolidation algorithm work, and how does it handle Spot instance interruptions?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Cluster Autoscaler was designed around cloud-agnostic abstractions: it does not manage EC2 instances directly, but instead adjusts the `DesiredCapacity` of AWS **Auto Scaling Groups (ASGs)**.
>
> This creates three major production bottlenecks:
> 1. **ASG Rigidity:** You must create dozens of specialized ASGs (one for each instance type, AZ, and capacity type), creating management overhead.
> 2. **Slow Provisioning Times:** When a pod goes into `Pending`, Cluster Autoscaler takes 3 to 6 minutes to calculate ASG needs, trigger the AWS ASG, wait for EC2 launch, and bootstrap the node.
> 3. **Inefficient Bin-Packing:** Cluster Autoscaler struggles with multi-resource bin-packing, often leaving nodes 40% empty.
>
> **Karpenter v1** bypasses ASGs entirely, communicating directly with the **AWS EC2 Fleet API**. Karpenter evaluates all pending Pod requests, calculates the exact aggregate CPU/memory/architecture requirements, and provisions a custom-tailored EC2 instance in **under 45 seconds**.
>
> **Consolidation Algorithm:** Karpenter continuously analyzes your cluster. If it notices that three pods across two $100/month nodes can safely fit onto a single $60/month node, it launches the cheaper node, cordons and drains the old nodes, and terminates them automatically.
>
> **Spot Interruption Handling:** Karpenter monitors AWS Spot Interruption Notifications via an SQS queue. When AWS issues a 2-minute Spot termination warning, Karpenter immediately provisions a replacement node and begins graceful Pod draining before the Spot instance is reclaimed."

---

## Question 26: Storage in EKS: AWS EBS CSI Driver vs EFS CSI Driver (ReadWriteMany)
**Interviewer:** *"When stateful workloads in EKS require persistent storage, how do you choose between the AWS EBS CSI Driver and the AWS EFS CSI Driver, and what are the concurrency, performance, and multi-AZ trade-offs?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Choosing between EBS and EFS in Amazon EKS comes down to **Access Modes, Latency Requirements, and Multi-AZ Availability**.
>
> **AWS EBS CSI Driver (ReadWriteOnce - RWO):**
> - **Access Model:** An EBS volume can only be attached to a *single EC2 node at a time*.
> - **Performance:** Delivers high IOPS and sub-millisecond block storage latency (using `gp3` or `io2` with Provisioned IOPS).
> - **Limitation:** EBS volumes are strictly tied to a single Availability Zone (e.g. `ap-south-1a`). If a pod scheduled on an EBS volume crashes and the scheduler attempts to restart it in `ap-south-1b`, the pod will remain stuck in `ContainerCreating` with a volume attachment failure.
>
> **AWS EFS CSI Driver (ReadWriteMany - RWX):**
> - **Access Model:** EFS is a managed NFS file system that can be mounted concurrently by hundreds of Pods across all worker nodes and across all Availability Zones simultaneously.
> - **Performance:** Higher latency (typically 2 to 7 milliseconds) because it is a distributed network file system.
> - **Use Cases:** Ideal for shared media assets, configuration stores, and legacy applications that require concurrent multi-pod file sharing.
>
> **Our Enterprise Rule:** For high-throughput transactional databases (like PostgreSQL, Kafka, or Elasticsearch running in containers), we mandate **EBS gp3** with Kubernetes Topology Spread Constraints pinning the stateful set to a specific AZ. For shared read/write directories, we use **EFS with provisioned throughput**."

---

## Question 27: Multi-AZ Pod Topology Spread Constraints & Anti-Affinity
**Interviewer:** *"In a multi-AZ EKS cluster, how do you guarantee that a deployment of 6 replicas is evenly distributed across 3 Availability Zones, and that the scheduler does not pack all pods onto the same node or the same AZ during a cluster scale-up?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Historically, teams relied on `podAntiAffinity` with `preferredDuringSchedulingIgnoredDuringExecution`. However, pod anti-affinity is binary: it only checks whether *any* matching pod exists on a failure domain, often resulting in unbalanced clusters where one AZ holds 4 pods and another holds 1 pod.
>
> The modern, Kubernetes-native solution is **`topologySpreadConstraints`**.
>
> `topologySpreadConstraints` enforces mathematical balance across topological domains (such as Availability Zones or individual host nodes) by defining a `maxSkew` parameter:
> - **`maxSkew: 1`** dictates that the difference in pod count between any two zones can never exceed 1.
> - **`topologyKey: topology.kubernetes.io/zone`** defines AWS Availability Zones as the failure domain boundary.
> - **`whenUnsatisfiable: DoNotSchedule` (Hard constraint)** prevents the scheduler from violating the skew rule, while `ScheduleAnyway` (Soft constraint) allows temporary imbalance if an AZ runs out of capacity.
>
> In production, we combine two constraints:
> 1. Even distribution across **Availability Zones** (`topologyKey: topology.kubernetes.io/zone`).
> 2. Even distribution across **Host Nodes** (`topologyKey: kubernetes.io/hostname`) to prevent multiple replicas from co-locating on the same physical VM."

### Deployment with TopologySpreadConstraints
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: core-banking-service
  namespace: banking-prod
spec:
  replicas: 6
  selector:
    matchLabels:
      app: core-banking-service
  template:
    metadata:
      labels:
        app: core-banking-service
    spec:
      topologySpreadConstraints:
        # Constraint 1: Balance evenly across AWS Availability Zones
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: DoNotSchedule
          labelSelector:
            matchLabels:
              app: core-banking-service
        # Constraint 2: Prevent co-locating on the exact same EC2 instance
        - maxSkew: 1
          topologyKey: kubernetes.io/hostname
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: core-banking-service
```

---

## Question 28: AWS VPC CNI & Overcoming Subnet IPv4 Exhaustion
**Interviewer:** *"By default, the AWS VPC CNI assigns an individual VPC private IP address to every single Pod. In large enterprise EKS clusters with hundreds of microservices, subnets quickly run out of IPv4 addresses. How do you resolve subnet IP exhaustion without rebuilding the entire VPC?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Subnet IPv4 exhaustion is the single most common scaling bottleneck when running enterprise EKS clusters. Each EC2 instance type supports a fixed number of ENIs, and each ENI can only attach a fixed number of secondary private IP addresses.
>
> We resolve this using a two-pronged architectural strategy:
> 1. **Prefix Delegation (`/28` IPv4 Prefixes):**
>    In standard mode, the CNI requests individual `/32` IPs from the subnet. By enabling **Prefix Delegation** (`ENABLE_PREFIX_DELEGATION=true` in `aws-node`), AWS assigns entire `/28` IPv4 CIDR blocks (16 IPs per allocation) to each ENI slot. This increases the pod density per node by up to 4x while drastically reducing EC2 API throttling calls to the AWS control plane.
> 2. **Secondary VPC CIDR (100.64.0.0/10 Carrier-Grade NAT):**
>    If the primary VPC CIDR (e.g., `10.0.0.0/16`) is completely exhausted, we attach a **Secondary IPv4 CIDR block** from the RFC 6598 Carrier-Grade NAT range (`100.64.0.0/10`) to the VPC. We create dedicated Pod subnets within this secondary CIDR and configure the AWS VPC CNI in **Custom Networking mode** (`AWS_VPC_K8S_CNI_CUSTOM_NETWORK_CFG=true`).
>    Worker nodes retain primary IPs on `10.0.0.0/16`, while all Pods are dynamically assigned IPs from `100.64.0.0/10`. When Pods communicate with external on-premises systems, AWS SNAT translates the packet to the node's primary IP."

### Enabling Prefix Delegation in AWS VPC CNI
```bash
# Enable IPv4 Prefix Delegation on aws-node DaemonSet
kubectl set env daemonset aws-node -n kube-system ENABLE_PREFIX_DELEGATION=true
kubectl set env daemonset aws-node -n kube-system WARM_PREFIX_TARGET=1

# Verify Pod allocation density on m5.large (jumps from 29 to 110 pods)
kubectl describe daemonset aws-node -n kube-system | grep ENABLE_PREFIX_DELEGATION
```

---

## Question 29: Zero-Downtime EKS Cluster Upgrades (e.g., 1.28 to 1.30)
**Interviewer:** *"Upgrading an EKS cluster across minor versions (e.g. from 1.28 to 1.29 and 1.30) touches the control plane, CoreDNS, VPC CNI, and hundreds of worker nodes. Walk me through your zero-downtime cluster upgrade playbook and how you prevent dropped traffic."*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Executing an EKS upgrade in a high-availability banking environment requires strict sequencing, pre-upgrade API auditing, and blue-green node group rollouts.
>
> Our 5-step upgrade methodology:
> 1. **API Deprecation Audit:** We run **Pluto** or `kube-no-trouble` against our Helm charts and Git repositories to detect any Kubernetes API versions scheduled for deprecation or removal in the target release (e.g. `v1beta1` to `v1`).
> 2. **Prerequisite Add-ons Upgrade:** Before upgrading the control plane, we update core cluster add-ons—specifically **VPC CNI, CoreDNS, and kube-proxy**—to the versions compatible with both the current and target Kubernetes minor version.
> 3. **Control Plane Upgrade:** We initiate the AWS EKS control plane upgrade via Terraform (`version = "1.29"`). AWS manages this rolling upgrade across the 3 control plane AZs with zero disruption to active API queries.
> 4. **Pod Disruption Budgets (PDB):** We guarantee that every application deployment has an active PDB (`minAvailable: 50%` or `maxUnavailable: 1`). This prevents `kubectl drain` from terminating too many pods concurrently.
> 5. **Blue-Green Node Group Replacement:** Instead of doing in-place upgrades on existing EC2 nodes, we provision a brand new **Target Managed Node Group** running the new EKS AMI version. Once healthy, we cordon and safely drain the old node group using `kubectl drain --ignore-daemonsets --delete-emptydir-data`. Pods gracefully migrate to the new nodes, and the old node group is deleted in Terraform."

### PodDisruptionBudget (PDB) Manifest
```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: payment-api-pdb
  namespace: banking-prod
spec:
  minAvailable: 2 # Guarantees at least 2 pods are always active during node drains
  selector:
    matchLabels:
      app: payment-api
```

---

## Question 30: Multi-Cluster Disaster Recovery with GitOps & Velero
**Interviewer:** *"If AWS experiences a catastrophic multi-AZ regional failure in `ap-south-1` (Mumbai), how do you restore our EKS workloads into `ap-southeast-1` (Singapore) within an RTO of 15 minutes and RPO under 1 minute?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Achieving an RTO under 15 minutes for Kubernetes requires separating **Stateless Application State (GitOps)** from **Persistent Stateful Data (Storage & Backups)**.
>
> Our multi-region DR architecture consists of:
> 1. **GitOps Mirroring via Argo CD:** We do not rely on manual scripts to recreate Kubernetes resources. An active-passive or warm-standby EKS cluster is pre-provisioned via Terraform in Singapore (`ap-southeast-1`). Argo CD monitors the Git repository and continuously reconciles all Deployments, Services, Ingresses, and ConfigMaps identically across both clusters.
> 2. **Stateful Data Synchronization via Velero & AWS Cross-Region Replication:**
>    - For any Kubernetes-native stateful resources (CRDs, Secrets, Persistent Volumes), we schedule automated **Velero** backups every 15 minutes.
>    - Velero encrypts and stores backups in an S3 bucket with **AWS S3 Cross-Region Replication (CRR)** enabled to Singapore. EBS volume snapshots are replicated cross-region via AWS Backup.
> 3. **Database Tier (Aurora Global Database):** Amazon Aurora PostgreSQL Global Database replicates transactions asynchronously from Mumbai to Singapore in **under 1 second (RPO < 1s)**.
> 4. **Failover Execution (RTO < 5m):** When an incident is declared, we execute an automated AWS Route 53 Application Recovery Controller (ARC) routing control, promoting the Singapore Aurora cluster and shifting global DNS traffic to the Singapore ALB."
