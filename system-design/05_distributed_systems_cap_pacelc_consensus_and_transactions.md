# Distributed Systems: CAP, PACELC, Consensus & Transactions
### Master Architecture Guide for Senior & Principal Interviews

---

## 1. CAP Theorem & PACELC: The Real Architectural Trade-Offs

The **CAP Theorem (Eric Brewer / Seth Gilbert & Nancy Lynch proof)** states that in any asynchronous network subject to network partitions ($P$), a distributed system can guarantee at most **two** of the following three properties:

```
+---------------------------------------------------------------------------------------------------+
| THE CAP THEOREM TRIANGLE                                                                          |
|                                                                                                   |
|                              Consistency (C)                                                      |
|                             /               \                                                     |
|                            /                 \                                                    |
|                           /                   \                                                   |
|                          /                     \                                                  |
|                  CP Systems                   CA Systems (Non-distributed)                        |
|            (Spanner, HBase, ZooKeeper)         (Single-node Postgres/MySQL)                       |
|                        /                         \                                                |
|                       /                           \                                               |
|                      /                             \                                              |
|      Partition Tolerance (P) ───────────────────── Availability (A)                               |
|                                   AP Systems                                                      |
|                          (DynamoDB, Cassandra, CouchDB)                                           |
+---------------------------------------------------------------------------------------------------+
```

### Why "CA" Does Not Exist in Real Distributed Systems

In real physical networks, fiber cuts, router crashes, and packet drops make network partitions ($P$) **inevitable**. Therefore, the choice in distributed systems is never between $C, A,$ and $P$; the real choice is:

$$\textbf{When a network partition occurs, do you choose Consistency (CP) or Availability (AP)?}$$

- **CP (Consistency / Partition Tolerant)**: If Node 1 cannot talk to Node 2, reject writes or return errors. Preserves strict consistency at the cost of availability (e.g., Banking, Stock Exchanges, etcd).
- **AP (Availability / Partition Tolerant)**: Both nodes accept local reads/writes, returning potentially stale data. Preserves availability at the cost of eventual consistency (e.g., Social Feeds, Shopping Carts, DNS).

---

### PACELC Theorem (Daniel Abadi): Extending CAP for Normal Operation

CAP only describes behavior **during a failure (Partition)**. What happens during the **$99.99\%$ of time when the network is healthy**?

$$\text{If } \mathbf{P} \text{ (Partition): Choose } \mathbf{A} \text{ or } \mathbf{C}; \quad \text{Else } (\mathbf{E}): \text{ Choose } \mathbf{L} \text{ (Latency) or } \mathbf{C} \text{ (Consistency)}$$

```
+---------------------------------------------------------------------------------------------------+
| PACELC CLASSIFICATION OF MODERN SYSTEMS                                                           |
|                                                                                                   |
| Database / System         | Classification | Trade-off Description                                |
| ------------------------- | -------------- | ---------------------------------------------------- |
| Google Cloud Spanner      | PC/EC          | Consistent during partitions; Consistent over Latency|
| MongoDB (Majority Write)  | PC/EC          | High consistency at expense of write latency         |
| Apache Cassandra          | PA/EL          | Available during partitions; Low Latency over Consist |
| DynamoDB (Default)        | PA/EL          | Eventual consistency & sub-10ms latency              |
+---------------------------------------------------------------------------------------------------+
```

---

## 2. Consistency Models: From Linearizable to Eventual

```
+---------------------------------------------------------------------------------------------------+
| CONSISTENCY HIERARCHY (STRONGEST TO WEAKEST)                                                      |
|                                                                                                   |
| [ Linearizable / Strict Serializable ]                                                            |
|   ▲  * Real-time global clock ordering (TrueTime). Operations appear atomic instantaneously.      |
|   │                                                                                               |
| [ Sequential Consistency ]                                                                        |
|   ▲  * Operations order consistent across all observers, but may lag behind real-time clock.      |
|   │                                                                                               |
| [ Causal Consistency ]                                                                            |
|   ▲  * Causally related events (Comment -> Post) ordered correctly; concurrent events unordered.  |
|   │                                                                                               |
| [ Read-Your-Writes Consistency ]                                                                  |
|   ▲  * A user always sees updates made by themselves (Sticky session to write primary).           |
|   │                                                                                               |
| [ Eventual Consistency ]                                                                          |
|      * If no further updates occur, all replicas will eventually converge to the same state.      |
+---------------------------------------------------------------------------------------------------+
```

---

## 3. Distributed Consensus: Raft vs Paxos

Consensus is the process of getting a cluster of distributed nodes to agree on a sequence of state machine transitions (a shared log) in the presence of node failures.

```
+---------------------------------------------------------------------------------------------------+
| RAFT CONSENSUS PROTOCOL OVERVIEW                                                                  |
|                                                                                                   |
|              ┌─────────────────────────────────────────────────────────────┐                      |
|              ▼                                                             │                      |
|      ┌───────────────┐   Election Timeout Expired    ┌─────────────────┐   │ Discovers Leader     |
|      │   FOLLOWER    │ ────────────────────────────► │    CANDIDATE    │   │ with higher term     |
|      └───────────────┘                               └─────────────────┘   │                      |
|              ▲                                                │            │                      |
|              │                                                │ Receives   │                      |
|              │                                                ▼ Majority   │                      |
|              │                                       ┌─────────────────┐   │                      |
|              └────────────────────────────────────── │     LEADER      │ ──┘                      |
|                       Steps down if heartbeat fails  └─────────────────┘                          |
+---------------------------------------------------------------------------------------------------+
```

### Raft Protocol Stages

1. **Leader Election**:
   - Nodes start as `Follower`. If a follower receives no heartbeat within a randomized election timeout ($150\text{ms} - 300\text{ms}$), it increments term number and becomes `Candidate`.
   - Candidate requests votes (`RequestVote RPC`). If it collects votes from a **majority (Quorum $= \lfloor \frac{N}{2} \rfloor + 1$)**, it becomes the `Leader`.
2. **Log Replication**:
   - Client sends command to Leader. Leader appends entry to its local log.
   - Leader sends `AppendEntries RPC` to all followers.
   - Once a majority of followers acknowledge writing the log entry to non-volatile disk, the entry is **committed**.
   - Leader executes command on its state machine and returns result to client.
3. **Safety Invariants**:
   - **Election Restriction**: A follower will reject a candidate's vote request if the candidate's log is less up-to-date than its own (`Candidate.LastLogTerm < Follower.LastLogTerm` or shorter log). This guarantees a new leader already contains **all committed entries** from previous terms.

---

## 4. Distributed Transactions: 2PC vs Saga Pattern

In microservice architectures with separate databases per service, classical database ACID transactions across network boundaries are impractical.

```
+---------------------------------------------------------------------------------------------------+
| SAGA PATTERN: ORCHESTRATION vs CHOREOGRAPHY                                                       |
|                                                                                                   |
| 1. CHOREOGRAPHY (Event-Driven / Pub-Sub):                                                          |
|    [ Order Service ] ──(OrderCreated Event)──► [ Payment Service ] ──(PaymentSuccess)──► [Inventory]|
|    * Problem: Complex cyclic dependencies; difficult to visualize and trace distributed rollbacks.|
|                                                                                                   |
| 2. ORCHESTRATION (Central State Machine Engine via Temporal / Cadence / Step Functions):          |
|                                                                                                   |
|                                 ┌─────────────────────────┐                                       |
|                                 │     SAGA ORCHESTRATOR   │                                       |
|                                 │ (Tracks State & Retries)│                                       |
|                                 └─┬─────────┬─────────┬───┘                                       |
|                                   │         │         │                                           |
|                  1. Create Order  │         │         │ 3. Reserve Stock                          |
|                                   ▼         ▼         ▼                                           |
|                           [Order Svc]  [Payment Svc]  [Inventory Svc]                             |
|                                             ▲                                                     |
|                                             │ 2. Authorize Card (If FAILS: Trigger               |
|                                             └─ Compensating Transaction "Cancel Order")          |
+---------------------------------------------------------------------------------------------------+
```

### Two-Phase Commit (2PC) vs Saga Comparison

| Attribute | Two-Phase Commit (2PC) | Saga Pattern (Orchestration) |
| :--- | :--- | :--- |
| **Consistency Model** | Strict ACID (Immediate Consistency). | Eventual Consistency (Compensating actions). |
| **Resource Locking** | Locks database rows across all nodes during phase 1 & 2. | Zero distributed locking. Each local service commits locally. |
| **Failure Mode** | **Blocking**. If coordinator crashes during phase 2, participants hold locks indefinitely. | **Non-blocking**. Failures trigger backward compensating transactions. |
| **Throughput** | Low throughput; high latency ($P99 > 1000\text{ms}$). | Extreme throughput ($100\text{k}+$ QPS). |
| **Production Fit** | Legacy single-datacenter relational clusters. | Modern distributed microservices & cloud architectures. |

---

## 5. Transactional Outbox Pattern & Change Data Capture (CDC)

How do you atomically update a database and publish an event to Kafka without dual-write inconsistency?

```
+---------------------------------------------------------------------------------------------------+
| TRANSACTIONAL OUTBOX PATTERN WITH DEBEZIUM CDC                                                    |
|                                                                                                   |
|  [ Order Service ]                                                                                |
|         │                                                                                         |
|         │ 1. Atomic Local ACID Transaction                                                        |
|         ▼                                                                                         |
|  ┌──────────────────────────────────────────────────┐                                             |
|  │ PostgreSQL Database                              │                                             |
|  │  - INSERT INTO orders (id, amount, status);      │                                             |
|  │  - INSERT INTO outbox_events (event_id, payload);│                                             |
|  └──────────────────────┬───────────────────────────┘                                             |
|                         │                                                                         |
|                         │ 2. Reads WAL (Write-Ahead Log) Stream                                   |
|                         ▼                                                                         |
|            [ Debezium CDC Engine ]                                                                |
|                         │                                                                         |
|                         │ 3. Guaranteed Exactly-Once Publishing                                   |
|                         ▼                                                                         |
|             [ Apache Kafka Cluster ]                                                              |
|                         │                                                                         |
|            ┌────────────┴────────────┐                                                            |
|            ▼                         ▼                                                            |
|     [ Payment Consumer ]     [ Notification Consumer ]                                            |
+---------------------------------------------------------------------------------------------------+
```

---

## 6. Distributed Unique ID Generators: Snowflake vs UUIDv7

Distributed systems require globally unique, 64-bit/128-bit identifiers that are **k-sortable** (ordered roughly by generation timestamp for high B+Tree index write performance).

```
+---------------------------------------------------------------------------------------------------+
| TWITTER SNOWFLAKE ID BIT LAYOUT (64 BITS TOTAL)                                                   |
|                                                                                                   |
|  1 Bit      41 Bits (Timestamp in Milliseconds)         10 Bits (Worker Node ID)    12 Bits (Seq) |
| [ 0 ] [ 10100101101010010101010100101010101010101 ]   [ 0101010101 ]              [ 000000000001 ]|
|                                                                                                   |
|  - Bit 0: Sign bit (always 0 for positive integer).                                               |
|  - 41 Bits Timestamp: 2^41 ms ≈ 69.7 Years from custom epoch.                                     |
|  - 10 Bits Worker ID: Supports 1,024 unique generator servers (5-bit Datacenter + 5-bit Worker).  |
|  - 12 Bits Sequence: Supports 4,096 unique IDs per millisecond per node (4.096M IDs/sec/node).     |
+---------------------------------------------------------------------------------------------------+
```

```typescript
// Production Snowflake ID Generator in TypeScript
export class SnowflakeIdGenerator {
  private readonly epoch: bigint = 1704067200000n; // Custom Epoch: 2024-01-01T00:00:00.000Z
  private readonly workerIdBits = 10n;
  private readonly sequenceBits = 12n;
  private readonly maxWorkerId = (1n << this.workerIdBits) - 1n; // 1023
  private readonly maxSequence = (1n << this.sequenceBits) - 1n; // 4095

  private readonly workerIdShift = this.sequenceBits; // 12
  private readonly timestampLeftShift = this.sequenceBits + this.workerIdBits; // 22

  private sequence = 0n;
  private lastTimestamp = -1n;
  private readonly workerId: bigint;

  constructor(workerId: number) {
    if (workerId < 0 || BigInt(workerId) > this.maxWorkerId) {
      throw new Error(`Worker ID must be between 0 and ${this.maxWorkerId}`);
    }
    this.workerId = BigInt(workerId);
  }

  public nextId(): string {
    let timestamp = BigInt(Date.now());

    if (timestamp < this.lastTimestamp) {
      throw new Error(`Clock moved backwards! Refusing to generate ID for ${this.lastTimestamp - timestamp}ms`);
    }

    if (timestamp === this.lastTimestamp) {
      this.sequence = (this.sequence + 1n) & this.maxSequence;
      if (this.sequence === 0n) {
        // Sequence exhausted in current millisecond; spin-wait for next millisecond
        while (timestamp <= this.lastTimestamp) {
          timestamp = BigInt(Date.now());
        }
      }
    } else {
      this.sequence = 0n;
    }

    this.lastTimestamp = timestamp;

    const id = ((timestamp - this.epoch) << this.timestampLeftShift) |
               (this.workerId << this.workerIdShift) |
               this.sequence;

    return id.toString();
  }
}
```
