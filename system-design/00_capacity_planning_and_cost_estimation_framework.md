# Master Capacity Planning & Cloud Cost Estimation Framework
### The 20-Year Principal Distributed Systems Architect's Universal Blueprint

---

## 1. Executive Philosophy: How to Lead Capacity Planning in Interviews

When evaluating staff and principal engineering candidates, interviewers do not look for memorized numbers; they look for **systematic thought process, dimensional analysis, sanity-checking instincts, and production intuition**.

```
+--------------------------------------------------------------------------------------------------+
| THE 6-STEP CAPACITY PLANNING PIPELINE                                                            |
|                                                                                                  |
| [1] Traffic QPS       -->  [2] Storage Sizing   -->  [3] Memory & Cache (80/20)                  |
|     (Read/Write, Peak)     (Payload + Indexes +      (Hot Working Set in RAM)                    |
|                             Replication + Snapshots)                                             |
|                                     │                                                            |
|                                     ▼                                                            |
| [4] Network Bandwidth -->  [5] Compute Sizing   -->  [6] Cloud Cost Model                        |
|     (Ingress/Egress Gbps)  (Little's Law Cores,      (Compute, Storage, RAM,                     |
|                             Pod Replicas, ThreadPool) Egress Network Tolls)                      |
+--------------------------------------------------------------------------------------------------+
```

### Interview Dialogue Framework (How to Start Small & Explain)
1. **State Baseline & Conversions First**: "Before designing the components, let's establish our traffic baseline, compute read/write ratios, and translate monthly volume into per-second transactional throughput."
2. **Separate Reads from Writes**: Never lump reads and writes together. Writes dictate storage growth, ACID contention, and write-ahead-log (WAL) disk IOPS; reads dictate cache sizing, CDN offload, and read-replica scaling.
3. **Apply the 3x Peak Multiplier**: Real-world traffic is diurnal (peaks during midday, valleys at night). Standard production sizing accounts for at least a $3\times$ peak-to-average ratio (or $5\times$ for flash sales/breaking news).
4. **Account for the "Hidden Multipliers"**:
   - **Replication Factor**: Data is rarely stored once; production databases use at least $3\times$ replication across Availability Zones (AZs).
   - **Indexing Overhead**: B-Tree indices, primary keys, and metadata add $20\% - 30\%$ on top of raw payload bytes.
   - **Network Egress Tolls**: Cloud providers (AWS, GCP, Azure) charge heavily for outbound internet egress ($~0.08 - 0.09 per GB); this is often the single highest line item on high-traffic systems.

---

## 2. Universal Constants & Conversions Cheatsheet

Always use these standard constants for clean calculations:

```text
================================================================================
TIME CONSTANTS & POWER-OF-10 CONVERSIONS
================================================================================
1 Minute                 = 60 Seconds
1 Hour                   = 3,600 Seconds ≈ 3.6 * 10^3 Seconds
1 Day                    = 24 * 3,600 = 86,400 Seconds ≈ 8.64 * 10^4 Seconds
                         (Rule of thumb for mental math: ≈ 10^5 Seconds)
1 Month (30 Days)        = 30 * 86,400 = 2,592,000 Seconds ≈ 2.592 * 10^6 Seconds
                         (Rule of thumb: ≈ 2.6 Million Seconds)
1 Year (365 Days)        = 365 * 86,400 = 31,536,000 Seconds ≈ 3.15 * 10^7 Seconds
                         (Rule of thumb: ≈ 31.5 Million Seconds)
5 Years                  = 5 * 31.536M = 157,680,000 Seconds ≈ 1.58 * 10^8 Seconds
10 Years                 = 10 * 31.536M = 315,360,000 Seconds ≈ 3.15 * 10^8 Seconds

================================================================================
DATA STORAGE & BANDWIDTH CONVERSIONS
================================================================================
1 Byte (B)               = 8 bits (b)
1 Kilobyte (KB)          = 1,000 Bytes (10^3 B)  [or 1,024 Bytes in binary KiB]
1 Megabyte (MB)          = 1,000 KB = 10^6 Bytes
1 Gigabyte (GB)          = 1,000 MB = 10^9 Bytes
1 Terabyte (TB)          = 1,000 GB = 10^12 Bytes
1 Petabyte (PB)          = 1,000 TB = 10^15 Bytes

Bandwidth Conversion Rule:
  Throughput in Bytes/sec to Megabits/sec (Mbps):
  Throughput (Mbps) = (Bytes/sec * 8) / 1,000,000
================================================================================
```

---

## 3. Latency Numbers Every Systems Architect Must Know

These physical hardware timings dictate why multi-tiered caching, memory lookups, and connection pooling are mandatory:

```text
================================================================================
LATENCY REFERENCE SHEET (Peter Norvig / Jeff Dean Scale)
================================================================================
Operation                                       Latency (Real Time)
--------------------------------------------------------------------------------
L1 CPU Cache Reference                          0.5 ns
Branch Mispredict                               5.0 ns
L2 CPU Cache Reference                          7.0 ns
Mutex Lock / Unlock                             25.0 ns
Main Memory (DRAM) Reference                    100.0 ns  (0.1 µs)
Compress 1 KB with Zstandard                    2.0 µs
Read 1 MB sequentially from Memory (RAM)        3.0 µs
Read 1 MB sequentially from NVMe SSD            50.0 µs   (0.05 ms)
Read 1 MB randomly from NVMe SSD                100.0 µs  (0.1 ms)
Round-trip in same Data Center (LAN)            500.0 µs  (0.5 ms)
Disk Seek (Traditional HDD Mechanical)          10,000.0 µs (10 ms)
Read 1 MB sequentially from HDD                 20,000.0 µs (20 ms)
Cross-Continent WAN Round-Trip (US East to West)40,000.0 µs (40 ms)
Transatlantic WAN (US East to Europe)           80,000.0 µs (80 ms)
================================================================================
```

---

## 4. The Universal 6-Step Calculation Blueprint

```
+-------------------------------------------------------------------------------+
| INPUT PARAMETERS (Interview Baseline Variables)                               |
|                                                                               |
| N_write = Monthly Writes (e.g., 1 Million, 100 Million, 1 Billion)           |
| R       = Read-to-Write Ratio (e.g., 10:1, 100:1, 5:1)                        |
| S_raw   = Raw Record Payload Size in Bytes (e.g., 500 Bytes, 2 KB)            |
| Y       = Retention Period in Years (e.g., 5 Years, 10 Years)                 |
| P_mult  = Peak Traffic Multiplier (Standard = 3x)                             |
+-------------------------------------------------------------------------------+
```

---

### Step 1: Traffic Estimation (Write QPS, Read QPS, Peak QPS)

```text
================================================================================
STEP 1: TRAFFIC & QPS FORMULAE
================================================================================
Average Write QPS:
  QPS_write_avg = N_write / 2,592,000

Peak Write QPS (3x Multiplier):
  QPS_write_peak = QPS_write_avg * 3

Monthly Read Volume:
  N_read = N_write * R

Average Read QPS:
  QPS_read_avg = N_read / 2,592,000 = (N_write * R) / 2,592,000

Peak Read QPS (3x Multiplier):
  QPS_read_peak = QPS_read_avg * 3

Total Combined Average QPS:
  QPS_total_avg = QPS_write_avg + QPS_read_avg

Total Combined Peak QPS:
  QPS_total_peak = QPS_write_peak + QPS_read_peak
================================================================================
```

---

### Step 2: Storage Sizing (Raw, Indexed, Replicated, 5 & 10-Year Projections)

```text
================================================================================
STEP 2: STORAGE CONSUMPTION FORMULAE
================================================================================
Per-Row Effective Size (with 25% B-Tree Index & Metadata Overhead):
  S_effective = S_raw * 1.25

Monthly Storage Growth:
  Storage_month = N_write * S_effective

1-Year Storage Growth:
  Storage_1yr = Storage_month * 12

5-Year Storage Total:
  Storage_5yr = Storage_month * 12 * 5

10-Year Storage Total:
  Storage_10yr = Storage_month * 12 * 10

Production Replicated Storage (Replication Factor = 3 for High Availability):
  Storage_5yr_replicated = Storage_5yr * 3
  Storage_backup_snapshots = Storage_5yr * 0.5  (Incremental daily WAL snapshots)
  Total_Usable_Storage = Storage_5yr_replicated + Storage_backup_snapshots
================================================================================
```

---

### Step 3: Memory & Distributed Cache Sizing (80/20 Pareto Working Set)

```text
================================================================================
STEP 3: MEMORY & CACHE SIZING (80/20 PARETO RULE)
================================================================================
Daily Read Volume:
  Daily_reads = N_read / 30

Daily Unique Active Objects (20% Pareto Hot Working Set):
  Hot_objects_daily = Daily_reads * 0.20

Daily Hot Cache Memory:
  Cache_RAM_daily = Hot_objects_daily * S_effective

Weekly Hot Cache Memory (Multi-Day Buffer with 20% Redis Key Metadata Overhead):
  Cache_RAM_weekly = Cache_RAM_daily * 7 * 1.20

Recommended Cluster Sizing:
  Target Memory Utilization = 70% (To prevent Redis OOM and swap thrashing)
  Total_Allocated_Cache_RAM = Cache_RAM_weekly / 0.70
================================================================================
```

---

### Step 4: Network Bandwidth (Ingress & Egress)

```text
================================================================================
STEP 4: NETWORK BANDWIDTH FORMULAE
================================================================================
[1] Ingress Bandwidth (Inbound Traffic):
    - Write Payload Ingress: QPS_write_avg * S_raw
    - Read Request Header Ingress: QPS_read_avg * 100 Bytes (HTTP GET headers)
    Ingress_Bytes_sec = (QPS_write_avg * S_raw) + (QPS_read_avg * 100)
    Ingress_Mbps = (Ingress_Bytes_sec * 8) / 1,000,000
    Peak_Ingress_Mbps = Ingress_Mbps * 3

[2] Egress Bandwidth (Outbound Traffic):
    - Write Response Egress: QPS_write_avg * 200 Bytes (JSON Ack / Short URL)
    - Read Response Payload Egress: QPS_read_avg * S_response
    Egress_Bytes_sec = (QPS_write_avg * 200) + (QPS_read_avg * S_response)
    Egress_Mbps = (Egress_Bytes_sec * 8) / 1,000,000
    Peak_Egress_Mbps = Egress_Mbps * 3
================================================================================
```

---

### Step 5: Compute Sizing via Little's Law & Concurrency Models

How do you calculate exactly how many servers, CPU cores, and thread pools you need?

```text
================================================================================
STEP 5: COMPUTE & INSTANCE SIZING (LITTLE'S LAW)
================================================================================
Little's Law Formula:
  L = λ * W
  Where:
    L = Concurrency (Average number of simultaneous requests in flight)
    λ = Arrival Rate (Peak QPS)
    W = Average Latency / Service Time per Request (in Seconds)

Example:
  Peak QPS (λ)                 = 1,000 requests/sec
  Average Latency (W)          = 20 ms = 0.020 seconds
  Concurrent Requests (L)      = 1,000 * 0.020 = 20 concurrent requests

Instance Sizing Calculation:
  - 1 vCPU with non-blocking I/O (e.g. Netty / Spring WebFlux / Go) handles 
    ~500 - 1,000 concurrent light connections.
  - 1 vCPU with traditional synchronous thread-per-request (Tomcat / Django) 
    handles ~50 - 100 concurrent worker threads.

Total vCPUs Required:
  vCPU_required = (Peak_QPS * Latency_sec) / Throughput_per_core

Pod / Instance Sizing:
  Using standard 4 vCPU / 8 GB RAM Nodes (e.g., AWS c6i.xlarge):
  Node_Count = ceil(vCPU_required / 4) * 2  (2x Multiplier for N+1 AZ Redundancy)
================================================================================
```

---

## 5. Production Cloud Cost Estimation Model (AWS / GCP Baseline)

Use these standard cloud pricing rates to provide realistic monthly and annual cost estimates:

```text
================================================================================
STANDARD CLOUD PRICING MATRIX (AWS US-East Baseline)
================================================================================
Resource Type                   Instance / Tier Specs           Unit Cost
--------------------------------------------------------------------------------
Compute (General Purpose)       c6i.xlarge (4 vCPU, 8 GB RAM)   $0.170 / hour ($124.10 / mo)
Compute (Memory Optimized)      r6i.xlarge (4 vCPU, 32 GB RAM)  $0.252 / hour ($183.96 / mo)
Relational DB (RDS Postgres)    db.m6i.xlarge (4 vCPU, 16 GB)   $0.384 / hour ($280.32 / mo)
                                Multi-AZ 2x multiplier          $560.64 / mo
In-Memory Cache (ElastiCache)   cache.m6g.large (6.38 GB RAM)   $0.136 / hour ($99.28 / mo)
Fast NVMe Block Storage (EBS gp3)Provisioned IOPS & Throughput  $0.080 / GB-month
Object Storage (Amazon S3)      Standard Hot Storage            $0.023 / GB-month
Object Storage (S3 Glacier)     Cold Archive Storage            $0.004 / GB-month
Network Egress (Outbound to Web)First 10 TB/mo                  $0.090 / GB
CDN Edge Delivery (Cloudflare/CF)Edge 301/302 Cache & Requests  $0.010 - $0.020 / GB
================================================================================
```

### Cost Estimation Worksheet Template

```text
================================================================================
MONTHLY INFRASTRUCTURE COST CALCULATION TEMPLATE
================================================================================
[1] Compute Tier (Stateless Application Pods):
    Number of Instances * Cost per Instance
    Example: 4 x c6i.xlarge ($124.10)                           = $496.40 / month

[2] Distributed Cache Tier (Redis / Memcached):
    Number of Cache Nodes * Cost per Node
    Example: 2 x cache.m6g.large ($99.28)                       = $198.56 / month

[3] Database Tier (PostgreSQL Primary + Multi-AZ Standby + Read Replica):
    Primary Multi-AZ ($560.64) + 1 Read Replica ($280.32)       = $840.96 / month

[4] Storage Volume (EBS gp3 + S3 Backup Snapshots):
    Total GB * $0.080 (EBS) + Backup GB * $0.023 (S3)
    Example: 100 GB EBS ($8.00) + 200 GB S3 ($4.60)             = $12.60 / month

[5] Network Egress & CDN Data Transfer:
    Total Monthly Egress GB * $0.090
    Example: 500 GB Egress * $0.090                             = $45.00 / month

[6] Managed Services & Observability (Kafka / DataDog / CloudWatch):
    Standard 15% Operational Surcharge                          = $239.03 / month
--------------------------------------------------------------------------------
TOTAL ESTIMATED MONTHLY CLOUD SPEND                             ≈ $1,832.55 / month
TOTAL ESTIMATED ANNUAL CLOUD SPEND                              ≈ $21,990.60 / year
================================================================================
```

---

## 6. Plug-and-Play Parameter Matrix for Common System Design Scenarios

Use this reference table during interviews to calibrate sizes for different problem archetypes:

```text
+---------------------------------------------------------------------------------------------------------+
| ARCHETYPE SIZING BENCHMARKS                                                                             |
+---------------------+-------------------+-------------------+-------------------+-----------------------+
| System Archetype    | Baseline Writes   | Read:Write Ratio  | Avg Object Size   | Primary Bottleneck    |
+---------------------+-------------------+-------------------+-------------------+-----------------------+
| 1. URL Shortener    | 1M – 100M / mo    | 10:1 to 100:1     | ~600 Bytes        | Read Latency (Cache)  |
| 2. Twitter / X Feed | 500M tweets / day | 100:1             | ~1 KB (text+meta) | Fan-out Write Amplif. |
| 3. WhatsApp / Chat  | 10B msgs / day    | 1:1 to 2:1        | ~500 Bytes        | WebSocket Connections |
| 4. Video Streaming  | 100K uploads / day| 1000:1            | 500 MB (chunks)   | Storage & Egress Band.|
| 5. Distributed Lock | 100M locks / day  | 10:1              | ~128 Bytes        | Consensus & Low Jitter|
| 6. Uber / Ride Match| 1M rides / day    | 50:1 (Geo pings)  | ~256 Bytes        | Geospatial Quadtree   |
+---------------------+-------------------+-------------------+-------------------+-----------------------+
```
