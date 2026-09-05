# Databases, Storage Engines & Partitioning
### Master Architecture Guide for Senior & Principal Interviews

---

## 1. Database Taxonomy: SQL vs NoSQL vs NewSQL

```
+---------------------------------------------------------------------------------------------------+
| THE DATABASE TAXONOMY SPECTRUM                                                                    |
|                                                                                                   |
| Relational (SQL)              NoSQL (Document/Key-Value/Wide-Column)      NewSQL (Distributed SQL)|
| [ PostgreSQL / MySQL ]        [ DynamoDB / Cassandra / MongoDB ]          [ Spanner / CockroachDB ]|
|  - Strict ACID Transactions    - Highly Partitioned Horizontal Scale       - Global Distributed ACID|
|  - Normalized Schemas          - Denormalized JSON / Key-Value             - Multi-Region Consensus|
|  - Complex Multi-Table JOINs   - Tunable Eventual Consistency              - TrueTime / Raft Sync  |
|  - Scale-Up Primary + Replicas - Massive Write Throughput ($1M+$ QPS)      - No Sharding Pain      |
+---------------------------------------------------------------------------------------------------+
```

### Comprehensive Comparison Matrix

| Dimension | Relational (RDBMS) | Key-Value / Wide-Column | Document Store | Distributed NewSQL |
| :--- | :--- | :--- | :--- | :--- |
| **Examples** | PostgreSQL, MySQL 8, MariaDB | DynamoDB, Apache Cassandra, ScyllaDB | MongoDB, Couchbase | Google Cloud Spanner, CockroachDB, YugabyteDB |
| **Primary Data Model** | Tables with typed columns & foreign keys. | Key $\rightarrow$ Binary Blob, or Row-Key $\rightarrow$ Column Family. | Hierarchical BSON/JSON documents. | Relational tables with distributed sharding. |
| **Transaction Guarantee** | Full ACID (Serializable / Read Committed). | Single-row ACID; multi-row transactions costly. | Single-document ACID; multi-doc transactions limited. | Globally distributed serializable ACID. |
| **Scaling Architecture** | Single write primary + read replicas. | Multi-master decentralized ring (Leaderless). | Primary-secondary replica sets with mongos routers. | Raft/Paxos consensus groups per shard. |
| **Best Fit Use Cases** | Core financial ledgers, inventory, complex relational reporting. | High-throughput telemetry, user sessions, chat message history. | Dynamic catalogs, user profiles, content management. | Global banking, multi-region SaaS requiring strong ACID. |

---

## 2. Storage Engine Internals: B+Tree vs LSM-Tree vs WAL

The underlying storage engine dictates whether a database is optimized for **blazing reads** (B+Trees) or **massive sequential write throughput** (LSM-Trees).

```
+---------------------------------------------------------------------------------------------------+
| B+TREE (READ-OPTIMIZED) vs LSM-TREE (WRITE-OPTIMIZED)                                             |
|                                                                                                   |
| B+TREE (Used in Postgres, MySQL InnoDB, SQLite):                                                  |
| ┌─────────────────────────────────────────────────────────────┐                                   |
| │                      [ Root Page: 100, 200 ]                │                                   |
| │                       /         |          \                │  * O(log N) point & range reads.  |
| │            [ 10, 50 ]       [ 120, 160 ]     [ 220, 300 ]   │  * Random in-place page writes    |
| │             /    \           /      \         /      \      │    cause random disk I/O & write  |
| │          [Data]◄─►[Data]◄──►[Data]◄──►[Data]◄─►[Data]◄──►[Data]   │    amplification.                 |
| └─────────────────────────────────────────────────────────────┘                                   |
|                                                                                                   |
| LSM-TREE (Used in Cassandra, RocksDB, LevelDB, DynamoDB):                                         |
| ┌─────────────────────────────────────────────────────────────┐                                   |
| │ 1. Write -> WAL (Disk Append-Only) + MemTable (RAM SkipList)│  * O(1) in-memory writes.         |
| │ 2. Flush -> Immutable SSTables on Disk (Sorted String Table)│  * Zero random in-place writes.   |
| │ 3. Background Compaction -> Merges sorted SSTables         │  * Reads require Bloom filters &  |
| │ 4. Bloom Filter -> Fast O(1) probe before disk lookup       │    probing multiple SSTable levels|
| └─────────────────────────────────────────────────────────────┘                                   |
+---------------------------------------------------------------------------------------------------+
```

### Deep Dive: Log-Structured Merge-Tree (LSM-Tree) Write & Read Path

1. **Write Path ($O(1)$ RAM + Sequential Disk)**:
   - Incoming write is appended sequentially to the **Write-Ahead Log (WAL)** on disk for crash durability ($O(1)$ sequential I/O).
   - The key-value pair is inserted into an in-memory sorted structure called the **MemTable** (implemented via Concurrent SkipList).
   - Once MemTable reaches threshold size ($\sim 64\text{MB}$), it becomes immutable and is flushed to disk as an immutable **SSTable (Sorted String Table)** at Level 0 ($L_0$).
2. **Read Path (Multi-Level Probe)**:
   - Check MemTable in RAM. If found, return.
   - Check Immutable MemTables flushing to disk.
   - For disk SSTables: Query **Bloom Filter** in memory for each SSTable. If Bloom filter says *no*, the key is guaranteed not in that SSTable (zero disk read). If *yes*, perform binary search on SSTable sparse index.
3. **Compaction (Size-Tiered vs Leveled)**:
   - Background threads continuously merge smaller SSTables into larger sorted runs at higher levels ($L_1, L_2, \dots$), eliminating duplicate overwritten keys and deleted items marked with **Tombstones**.

---

## 3. Database Indexing: Mechanics & Query Optimization

```
+---------------------------------------------------------------------------------------------------+
| INDEX TYPES AND TRADE-OFFS                                                                        |
|                                                                                                   |
| [ Clustered Index ]     ──► The physical order of data rows on disk matches index order.          |
|                             (Only 1 per table; e.g. InnoDB Primary Key).                          |
| [ Non-Clustered Index ] ──► Separate B+Tree whose leaf nodes point to the Clustered Key.          |
|                             Requires 2-step lookup ("Index Double Hop" / Bookmark Lookup).        |
| [ Covering Index ]      ──► Non-clustered index containing ALL columns requested in SELECT query. |
|                             Avoids reading table heap entirely ("Index Only Scan").               |
| [ Inverted Index ]      ──► Maps Term/Word -> Posting List of Doc IDs (Elasticsearch/Lucene).     |
| [ GIN / GiST Index ]    ──► Generalized Inverted/Search Trees for JSONB, Arrays, PostGIS Geo.     |
+---------------------------------------------------------------------------------------------------+
```

### The Leftmost Prefix Rule for Composite Indexes

If you create a composite index on `(tenant_id, status, created_at)`:
- `WHERE tenant_id = 'A' AND status = 'ACTIVE'` $\rightarrow$ **FULL INDEX SCAN USED** (High efficiency).
- `WHERE tenant_id = 'A' AND created_at > '2026-01-01'` $\rightarrow$ **PARTIAL SCAN** (Uses index for `tenant_id`, filters `created_at` in memory).
- `WHERE status = 'ACTIVE'` $\rightarrow$ **INDEX UNUSABLE** (Breaks leftmost prefix; triggers full table scan).

---

## 4. Partitioning & Sharding Strategies

When database size exceeds a single machine's storage capacity or write IOPS, data must be partitioned across $N$ physical database nodes.

```
+---------------------------------------------------------------------------------------------------+
| SHARDING STRATEGIES                                                                               |
|                                                                                                   |
| 1. Range-Based Sharding:                                                                          |
|    - Shard 1: [A - G], Shard 2: [H - P], Shard 3: [Q - Z]                                         |
|    - Flaw: Extreme hotspotting on monotonically increasing keys (e.g. timestamp primary keys).    |
|                                                                                                   |
| 2. Hash-Based / Key-Based Sharding:                                                               |
|    - Shard ID = MurmurHash(user_id) % Total_Shards                                                |
|    - Uniform traffic distribution. Cross-shard range queries require scatter-gather broadcast.    |
|                                                                                                   |
| 3. Directory-Based / Lookup Sharding:                                                             |
|    - Centralized routing service (backed by ZooKeeper/etcd/Redis) maps Tenant_ID -> Shard_Node.   |
|    - High flexibility; enables moving a high-volume VIP tenant to a dedicated physical shard.     |
+---------------------------------------------------------------------------------------------------+
```

### Cross-Shard Joins & Scatter-Gather Query Overhead

- **Single-Shard Query**: `SELECT * FROM orders WHERE user_id = 42;` $\rightarrow$ Router hashes `user_id`, sends request to **exactly 1 shard**. Latency: $\sim 2\text{ms}$.
- **Cross-Shard (Scatter-Gather)**: `SELECT * FROM orders WHERE status = 'PENDING';` $\rightarrow$ Router must broadcast query to **all 100 shards simultaneously**, await all responses, and merge results in memory. Latency: **P99 of the slowest shard in the fleet**.

---

## 5. Replication Models: Single-Leader vs Multi-Leader vs Leaderless

```
+---------------------------------------------------------------------------------------------------+
| REPLICATION TOPOLOGIES                                                                             |
|                                                                                                   |
| 1. Single-Leader (Primary-Replica):                                                               |
|    [ Primary (Writes) ] ─── Async / Semi-Sync WAL Stream ───► [ Replica 1 ]  [ Replica 2 ] (Reads)|
|    - Clean serialization. Failover requires election (Split-Brain risk if network partitions).    |
|                                                                                                   |
| 2. Multi-Leader (Active-Active Multi-Region):                                                      |
|    [ Region US Leader ] ◄──── Bi-directional Asynchronous Sync ────► [ Region EU Leader ]         |
|    - High write availability. Requires conflict resolution: Last-Write-Wins (LWW) or CRDTs.     |
|                                                                                                   |
| 3. Leaderless (Dynamo / Cassandra):                                                               |
|    Client writes directly to multiple replica nodes (Quorum R + W > N).                           |
+---------------------------------------------------------------------------------------------------+
```

### Quorum Consensus Mechanics in Leaderless Systems ($R + W > N$)

Let:
- $N$ = Total replication factor (e.g., $N = 3$).
- $W$ = Number of successful write acknowledgments required to consider a write committed.
- $R$ = Number of nodes queried for a read.

$$\text{Strong Consistency Condition:} \quad W + R > N$$

```
+---------------------------------------------------------------------------------------------------+
| QUORUM OVERLAP PROOF (N=3, W=2, R=2)                                                              |
|                                                                                                   |
| Node 1: [ Version 2 (Latest Write) ] ◄── Write Ack 1                                             |
| Node 2: [ Version 2 (Latest Write) ] ◄── Write Ack 2 & Read Query 1 (OVERLAP NODE)                |
| Node 3: [ Version 1 (Stale)        ] ◄── Read Query 2                                             |
|                                                                                                   |
| * Because W + R = 4 > 3, at least ONE node in the read set (Node 2) is guaranteed to contain      |
|   the latest write. The client compares vector clocks or timestamps and returns Version 2.        |
+---------------------------------------------------------------------------------------------------+
```

### Anti-Entropy with Merkle Trees & Read Repair

1. **Read Repair**: When a client performs a quorum read ($R=2$) and detects Node 3 has stale data, the client or coordinator asynchronously pushes the latest version to Node 3.
2. **Anti-Entropy with Merkle Trees**: Background synchronization processes compare cryptographic Merkle Trees (Hash Trees) representing key ranges. Nodes only transfer ranges where tree root hashes differ, reducing replica sync bandwidth by $99.9\%$.
