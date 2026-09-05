# Cloud-Native Infrastructure, Kubernetes & Object Storage
### Master Architecture Guide for Senior & Principal Interviews

---

## 1. Kubernetes (K8s) Architecture & Networking Primitives

Kubernetes is the industry-standard orchestrator for automating deployment, scaling, and operational lifecycle of containerized workloads.

```
+---------------------------------------------------------------------------------------------------+
| KUBERNETES CONTROL PLANE & WORKER NODE ARCHITECTURE                                               |
|                                                                                                   |
| [ CONTROL PLANE / MASTER NODES ]                                                                  |
| ┌───────────────────────────────────────────────────────────────────────────────────────────────┐ |
| │  [ kube-apiserver ] (REST API & Auth Hub) ◄──► [ etcd ] (Raft-based Distributed State Store)   │ |
| │           ▲                                                                                   │ |
| │           ├──► [ kube-scheduler ] (Assigns Unscheduled Pods -> Best Worker Node)              │ |
| │           └──► [ kube-controller-manager ] (Deployment, ReplicaSet, Node Lifecycle Loops)    │ |
| └───────────────────────────────────────────────┬───────────────────────────────────────────────┘ |
|                                                 │                                                 |
| ┌───────────────────────────────────────────────┴───────────────────────────────────────────────┐ |
| │ [ WORKER NODE ]                                                                               │ |
| │  - [ kubelet ] (Agent that communicates with API Server & drives Container Runtime CRI)       │ |
| │  - [ kube-proxy ] (Manages iptables / IPVS packet forwarding rules for Services)             │ |
| │  - [ Pod 1 (App Container) ]   [ Pod 2 (App Container + Envoy Sidecar) ]                      │ |
| └───────────────────────────────────────────────────────────────────────────────────────────────┘ |
+---------------------------------------------------------------------------------------------------+
```

### Kubernetes Service Networking & Traffic Routing

```
+---------------------------------------------------------------------------------------------------+
| SERVICE NETWORKING TAXONOMY                                                                       |
|                                                                                                   |
| 1. ClusterIP (Default):                                                                           |
|    - Internal Virtual IP accessible ONLY within the K8s cluster.                                  |
|    - Load-balances across healthy matching Pod endpoints via kube-proxy iptables/IPVS.           |
|                                                                                                   |
| 2. NodePort:                                                                                      |
|    - Exposes service on a static high port (30000 - 32767) on EVERY worker node's physical IP.    |
|                                                                                                   |
| 3. LoadBalancer:                                                                                  |
|    - Provisions external Cloud Provider Load Balancer (AWS NLB/ALB, GCP Cloud LB) pointing to node|
|                                                                                                   |
| 4. Ingress Controller (Nginx / Traefik / Envoy / AWS ALB Controller):                             |
|    - Single entrypoint L7 reverse proxy managing TLS termination, path routing (`/api` vs `/app`)|
|      and virtual host name routing across internal ClusterIP services.                            |
+---------------------------------------------------------------------------------------------------+
```

---

## 2. Service Mesh Architecture: Istio & Envoy Sidecars

When hundreds of microservices communicate over internal networks, cross-cutting concerns (Mutual TLS encryption, circuit breaking, distributed tracing, retries) pollute application code.

```
+---------------------------------------------------------------------------------------------------+
| SERVICE MESH SIDECAR PATTERN (ENVOY PROXY)                                                        |
|                                                                                                   |
|   [ Pod: Order Service ]                               [ Pod: Payment Service ]                   |
| ┌─────────────────────────┐                         ┌─────────────────────────┐                   |
| │ [ Order App Container ] │                         │[ Payment App Container ]│                   |
| │         │               │                         │            ▲            │                   |
| │         ▼ Localhost     │                         │            │ Localhost  │                   |
| │ ┌─────────────────────┐ │     Encrypted mTLS      │ ┌─────────────────────┐ │                   |
| │ │ Envoy Sidecar Proxy │ │ ──────────────────────► │ │ Envoy Sidecar Proxy │ │                   |
| │ └─────────────────────┘ │ (Auto Retry, Trace ID,  │ └─────────────────────┘ │                   |
| └─────────────────────────┘  Rate Limit, Metrics)   └─────────────────────────┘                   |
|                                                                                                   |
| * Transparent to application code. Provides Zero-Trust security and end-to-end telemetry.        |
+---------------------------------------------------------------------------------------------------+
```

---

## 3. Object Storage Deep Dive: AWS S3, GCP Cloud Storage & Erasure Coding

Object storage systems are optimized for storing petabytes of unstructured binary objects (images, videos, backups, machine learning checkpoints) with $99.999999999\%$ (11 Nines) durability.

```
+---------------------------------------------------------------------------------------------------+
| ERASURE CODING IN DISTRIBUTED OBJECT STORAGE (REED-SOLOMON 8+4 CONFIGURATION)                     |
|                                                                                                   |
| Original File (800MB)                                                                             |
| ┌────────┬────────┬────────┬────────┬────────┬────────┬────────┬────────┐                         |
| │ Chunk1 │ Chunk2 │ Chunk3 │ Chunk4 │ Chunk5 │ Chunk6 │ Chunk7 │ Chunk8 │ (8 Data Chunks, 100MB)  |
| └────────┴────────┴────────┴────────┴────────┴────────┴────────┴────────┘                         |
|                                        │                                                          |
|                                        ▼ Reed-Solomon Parity Matrix Calculation                   |
| ┌────────┬────────┬────────┬────────┐                                                             |
| │Parity 1│Parity 2│Parity 3│Parity 4│ (4 Parity Chunks, 100MB)                                    |
| └────────┴────────┴────────┴────────┘                                                             |
|                                                                                                   |
| * All 12 chunks are written across 12 independent failure domains (Separate Racks / Datacenters). |
| * Durability Guarantee: The system can SURVIVE THE LOSS OF ANY 4 DRIVES/RACKS SIMULTANEOUSLY      |
|   without losing a single byte of customer data!                                                  |
| * Storage Overhead: 12 / 8 = 1.5x (50% overhead) vs 3.0x (200% overhead) for 3-way replication!   |
+---------------------------------------------------------------------------------------------------+
```

### High-Scale S3 Upload Patterns: Presigned URLs & Multipart Uploads

1. **Multipart Upload ($> 100\text{MB}$ Objects)**:
   - Client splits large file into $5\text{MB} - 50\text{MB}$ parts.
   - Uploads parts in parallel across multiple worker threads.
   - Failed parts retry independently without re-uploading the entire multi-gigabyte file.
2. **Direct-to-S3 Uploads via Presigned URLs (Bypassing App Servers)**:
   - Application server authenticates user and generates a time-limited cryptographically signed URL (`s3.getSignedUrlPromise('putObject', ...)`).
   - Client uploads raw media bytes **directly from browser/mobile to S3 bucket**.
   - Preserves application server CPU, RAM, and network bandwidth.

---

## 4. Cloud Native Super-Databases: Google Spanner vs AWS DynamoDB

```
+---------------------------------------------------------------------------------------------------+
| GOOGLE SPANNER TRUETIME & EXTERNAL CONSISTENCY                                                    |
|                                                                                                   |
| The Hard Problem in Distributed Systems: Physical clocks drift (NTP skew ±100ms - 500ms).         |
| Google Spanner Solution: TrueTime API backed by GPS Receivers + Atomic Clocks in every DC!        |
|                                                                                                   |
| TrueTime.now() returns time interval: [ t.earliest, t.latest ] where uncertainty ε <= 7ms.        |
| Commit Wait Rule: Spanner waits 2 * ε before committing a transaction.                            |
| Result: Globally Linearizable (Serializable) ACID transactions across continents without locking! |
+---------------------------------------------------------------------------------------------------+
```

### AWS vs GCP Distributed Cloud Services Comparison

| Functional Area | AWS Ecosystem | Google Cloud Platform (GCP) |
| :--- | :--- | :--- |
| **Distributed NoSQL** | DynamoDB (Single-table, Partition Keys, Global Secondary Indexes). | Cloud Bigtable (HBase API, Petabyte analytics/IoT). |
| **Global NewSQL** | Amazon Aurora Multi-Master / Global Database. | Cloud Spanner (TrueTime, Distributed Global ACID). |
| **Data Warehouse** | Amazon Redshift (Columnar, MPP). | BigQuery (Serverless, Dremel, Capacitor Storage). |
| **Event Streaming** | Amazon Kinesis Data Streams. | Cloud Pub/Sub (Global routing, automatic partition autoscaling). |
| **Serverless Compute** | AWS Lambda. | Cloud Run (Container-based, automatic 0-to-N scale). |
