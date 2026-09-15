# Messaging, Streaming & Event-Driven Architectures
### Master Architecture Guide for Senior & Principal Interviews

---

## 1. Message Queues (RabbitMQ / SQS) vs Distributed Streaming (Kafka / Kinesis)

Understanding when to choose a point-to-point **Message Queue** versus an append-only **Distributed Event Stream** is a core distinction tested in distributed system interviews.

```
+---------------------------------------------------------------------------------------------------+
| MESSAGE QUEUE (RABBITMQ / SQS) vs DISTRIBUTED EVENT STREAM (KAFKA / KINESIS)                      |
|                                                                                                   |
| MESSAGE QUEUE (Smart Broker, Dumb Consumer):                                                      |
| [ Producer ] ──► [ Queue Router ] ──► [ Msg 1 | Msg 2 | Msg 3 ] ──► [ Worker 1 ] [ Worker 2 ]     |
|  * Messages deleted immediately upon Consumer ACK.                                                |
|  * Broker tracks individual consumer acknowledgments and redeliveries.                            |
|  * Best for: Transient asynchronous job processing, individual task routing, delayed tasks.      |
|                                                                                                   |
| DISTRIBUTED STREAM (Dumb Broker, Smart Consumer):                                                 |
| [ Producer ] ──► [ Partition Log: (Offset 0)(1)(2)(3)(4)(5)(6)... (Disk Append-Only) ]             |
|                                     ▲                     ▲                                       |
|                                     │ Offset: 2           │ Offset: 5                             |
|                           [ Analytics Consumer ]    [ Billing Consumer ]                          |
|  * Messages immutable and retained on disk for days/months (Replayable).                          |
|  * Consumers independently track their own read offset pointers.                                  |
|  * Best for: High-throughput event streaming, CDC pipelines, event sourcing, replayability.       |
+---------------------------------------------------------------------------------------------------+
```

### Architectural Comparison Matrix

| Dimension | Message Queue (RabbitMQ / SQS) | Event Stream (Apache Kafka / Kinesis) |
| :--- | :--- | :--- |
| **Ordering Guarantee** | Guaranteed strictly on single queue; lost if multiple competing consumers. | Guaranteed strictly **per partition** (by partition key hash). |
| **Throughput Capacity** | $10\text{k} - 50\text{k}$ msgs/sec per broker. | **$1\text{M}+$ msgs/sec** via zero-copy sequential disk I/O. |
| **Message Retention** | Deleted after successful consumer acknowledgement. | Retained for configurable time (e.g. 7 days) or compacted indefinitely. |
| **Replayability** | **No**. Cannot rewind time to reprocess past messages. | **Yes**. Consumers can rewind offset to `0` or any timestamp. |
| **Backpressure / Fanout**| Push-based flow control; broker can overwhelm consumer. | Pull-based (Consumer polls at its own controlled processing pace). |

---

## 2. Apache Kafka Internals: Partitions, Offsets & Zero-Copy I/O

Why is Apache Kafka capable of processing millions of records per second on commodity hardware?

```
+---------------------------------------------------------------------------------------------------+
| KAFKA ZERO-COPY OS NETWORK TRANSFER ARCHITECTURE                                                  |
|                                                                                                   |
| TRADITIONAL DATA PATH (4 Context Switches + 4 Buffer Copies):                                     |
| Disk File ──► OS PageCache ──► App Heap Buffer ──► Socket Buffer ──► NIC Buffer ──► Network       |
|                                                                                                   |
| KAFKA ZERO-COPY (sendfile Syscall - 2 Context Switches + Zero CPU Data Copying):                  |
| Disk File ──► OS PageCache ───────────────────────────────────────► NIC Buffer ──► Network       |
|  * CPU never touches user-space memory! Bytes transferred via DMA directly from OS PageCache.     |
+---------------------------------------------------------------------------------------------------+
```

### Kafka Partitioning & Consumer Group Rebalancing

```
+---------------------------------------------------------------------------------------------------+
| TOPIC PARTITIONS & CONSUMER GROUP ASSIGNMENT                                                      |
|                                                                                                   |
| Topic: "orders" (4 Partitions)                                                                    |
| ┌───────────────┐ ┌───────────────┐ ┌───────────────┐ ┌───────────────┐                           |
| │  Partition 0  │ │  Partition 1  │ │  Partition 2  │ │  Partition 3  │                           |
| └───────┬───────┘ └───────┬───────┘ └───────┬───────┘ └───────┬───────┘                           |
|         │                 │                 │                 │                                   |
|         ▼                 ▼                 ▼                 ▼                                   |
| ┌───────────────────────────────┐   ┌───────────────────────────────┐                             |
| │  Consumer Group A - Worker 1  │   │  Consumer Group A - Worker 2  │                             |
| │  (Reads Partition 0 & 1)      │   │  (Reads Partition 2 & 3)      │                             |
| └───────────────────────────────┘   └───────────────────────────────┘                             |
|                                                                                                   |
| * Number of active concurrent workers in a group is BOUNDED by total partitions (Max 4 workers).  |
| * Adding a 5th worker leaves it IDLE in standby.                                                  |
+---------------------------------------------------------------------------------------------------+
```

---

## 3. Message Delivery Semantics: Exactly-Once Processing (EOS)

```
+---------------------------------------------------------------------------------------------------+
| DELIVERY SEMANTIC GUARANTEES                                                                      |
|                                                                                                   |
| 1. At-Most-Once:                                                                                  |
|    - Producer sends without ACK (acks=0), or Consumer commits offset BEFORE processing.           |
|    - Message loss possible during crash. Zero duplicates.                                         |
|                                                                                                   |
| 2. At-Least-Once (Default Production Standard):                                                   |
|    - Producer requires all ISR ACKs (acks=all). Consumer commits offset AFTER processing.         |
|    - Zero message loss. Network retries can cause duplicate deliveries.                           |
|                                                                                                   |
| 3. Exactly-Once Processing (Idempotency + Deduplication):                                         |
|    - Producer: Idempotent Producer (`enable.idempotence=true`, Sequence IDs per batch).          |
|    - Consumer: Atomic storage deduplication key (INSERT ... ON CONFLICT DO NOTHING) or Redis set.|
+---------------------------------------------------------------------------------------------------+
```

### Production Idempotent Consumer Pattern (TypeScript)

```typescript
export async function processOrderEvent(
  db: any, 
  redis: any, 
  event: { eventId: string; orderId: string; amount: number }
): Promise<void> {
  const dedupKey = `event_processed:${event.eventId}`;

  // 1. Atomic Redis check-and-set with 24-hour TTL
  const isNew = await redis.set(dedupKey, 'PROCESSED', 'NX', 'EX', 86400);
  if (!isNew) {
    console.log(`[DUPLICATE EVENT DETECTED] Event ${event.eventId} already processed. Skipping.`);
    return;
  }

  // 2. Execute business logic inside idempotent DB transaction
  await db.transaction(async (trx: any) => {
    await trx('processed_events').insert({ event_id: event.eventId });
    await trx('account_balances')
      .where({ order_id: event.orderId })
      .increment('balance', event.amount);
  });
}
```

---

## 4. Event Sourcing & CQRS (Command Query Responsibility Segregation)

```
+---------------------------------------------------------------------------------------------------+
| CQRS + EVENT SOURCING ARCHITECTURE                                                                |
|                                                                                                   |
| [ Write Command: CreateOrder ] ──► [ Command Handler ] ──► [ Event Store: Append-Only Immutable ] |
|                                                                 │ (OrderCreated, Paid, Shipped)   |
|                                                                 ▼                                 |
|                                                    [ Kafka / Event Stream ]                       |
|                                                                 │                                 |
|                                                                 ▼ Asynchronous Projection Worker  |
| [ Read Query: GetOrderDetails ] ◄── [ Elastic / Postgres View ] ◄─────────────────────────────────┘
|  (Optimized denormalized Read Model)                                                              |
+---------------------------------------------------------------------------------------------------+
```

---

## 5. Real-Time Communication Protocols: Comparison

```
+---------------------------------------------------------------------------------------------------+
| REAL-TIME PROTOCOL SPECTRUM                                                                       |
|                                                                                                   |
| Protocol       | Transport | Directionality    | Connection Overhead | Best Use Case              |
| -------------- | --------- | ----------------- | ------------------- | -------------------------- |
| Polling        | HTTP/1.1  | Half-Duplex       | High (New TCP/TLS)  | Low-frequency batch checks  |
| Long Polling   | HTTP/1.1  | Half-Duplex (Hold)| Medium              | Legacy fallback             |
| Server-Sent    | HTTP/2    | Unidirectional    | Low (Multiplexed)   | Stock Tickers, AI Streaming|
| Events (SSE)   | (TCP)     | (Server -> Client)|                     | LLM Token responses (ChatGPT|
| WebSockets     | TCP       | Full-Duplex Bi-dir| Low (Persistent)    | Chat, Multiplayer gaming   |
| WebTransport   | HTTP/3    | Bi-dir Multiplexed| Ultra-Low (Zero-RTT)| Live video streaming,       |
|                | (QUIC/UDP)| (Unreliable + Rel)|                     | Cloud gaming, VR / AR      |
+---------------------------------------------------------------------------------------------------+
```
