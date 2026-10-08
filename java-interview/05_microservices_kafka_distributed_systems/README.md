# 05. Microservices Architecture, Kafka & Distributed Systems Mastery

A comprehensive guide covering inter-service communication, distributed tracing, Kafka internals, consumer lag troubleshooting, and service resilience.

---

## 📑 Topics Index
1. [Synchronous vs Asynchronous Communication in Microservices](#1-synchronous-vs-asynchronous-communication-in-microservices)
2. [Code Implementation: Asynchronous Communication via Kafka](#2-code-implementation-asynchronous-communication-via-kafka)
3. [Distributed Tracing: Trace ID & Span ID Propagation (OpenTelemetry)](#3-distributed-tracing-trace-id--span-id-propagation-opentelemetry)
4. [Kafka Partitions: Sizing, Ordering & Limits](#4-kafka-partitions-sizing-ordering--limits)
5. [Troubleshooting Stuck Kafka Consumers & Consumer Lag in Production](#5-troubleshooting-stuck-kafka-consumers--consumer-lag-in-production)
6. [Kafka Fault Tolerance: Replication Factor, Leader, Follower & ISR](#6-kafka-fault-tolerance-replication-factor-leader-follower--isr)
7. [API Gateway: Role & Core Responsibilities](#7-api-gateway-role--core-responsibilities)
8. [Service Discovery with Netflix Eureka & Client-Side Load Balancing](#8-service-discovery-with-netflix-eureka--client-side-load-balancing)
9. [Enterprise Rule Engines (Drools / Easy Rules)](#9-enterprise-rule-engines-drools--easy-rules)

---

### 1. Synchronous vs Asynchronous Communication in Microservices
#### 🎙️ 60-Second Verbal Script
> "In microservices architectures, communication patterns fall into two categories:
>
> 1. **Synchronous Communication (REST / gRPC / GraphQL)**: The calling service sends a request and blocks/waits for an immediate response. Ideal for real-time queries (e.g., querying product inventory during checkout). Drawbacks include **tight temporal coupling** and risk of cascading failures.
> 2. **Asynchronous Communication (Event-Driven via Kafka / RabbitMQ / AWS SQS)**: The calling service emits an event or message to a broker and immediately returns. Downstream services consume the message at their own pace. This provides **loose coupling, temporal decoupling, peak-load buffering, and high fault tolerance**."

---

### 2. Code Implementation: Asynchronous Communication via Kafka
#### 🎙️ 60-Second Verbal Script
> "To implement async communication between Service A (Order Service) and Service B (Email Service):
>
> 1. **Service A (Producer)** uses `KafkaTemplate.send()` to publish an `OrderPlacedEvent` to topic `order-events` with `orderId` as the partition key.
> 2. **Service B (Consumer)** uses `@KafkaListener` to consume the event, process email dispatch, and commit the offset manually via `Acknowledgment.acknowledge()`."

```java
// === Service A: Producer ===
@Service
public class OrderEventPublisher {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderPlaced(OrderEvent event) {
        kafkaTemplate.send("order-events", event.orderId(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    System.out.println("Published to offset: " + result.getRecordMetadata().offset());
                } else {
                    System.err.println("Failed to publish: " + ex.getMessage());
                }
            });
    }
}

// === Service B: Consumer ===
@Service
public class EmailNotificationConsumer {

    @KafkaListener(topics = "order-events", groupId = "email-service-group")
    public void consume(OrderEvent event, Acknowledgment ack) {
        try {
            System.out.println("Sending confirmation email for Order: " + event.orderId());
            sendEmail(event);
            ack.acknowledge(); // Manual commit
        } catch (Exception e) {
            // Log & let error handler send to DLQ (Dead-Letter Queue)
            throw new RuntimeException("Email dispatch failed", e);
        }
    }
}
```

---

### 3. Distributed Tracing: Trace ID & Span ID Propagation (OpenTelemetry)
#### 🎙️ 60-Second Verbal Script
> "In a distributed call chain ($A \to B \to C$), **Distributed Tracing** allows us to track the entire request lifecycle using **Trace IDs and Span IDs**:
>
> - **Trace ID**: A globally unique identifier generated at the API Gateway for an entire end-to-end user transaction.
> - **Span ID**: Represents a single unit of work within an individual microservice.
>
> **How It Propagates**:
> Modern frameworks use the **W3C Trace Context standard** via HTTP headers (specifically `traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01`).
> In Spring Boot 3, **Micrometer Tracing + OpenTelemetry** automatically intercepts HTTP clients (Feign, RestClient) and Kafka message headers, injecting the Trace ID into MDC logs (`[order-service,traceId,spanId]`) and exporting to tools like **Jaeger, Zipkin, or AWS X-Ray**."

---

### 4. Kafka Partitions: Sizing, Ordering & Limits
#### 🎙️ 60-Second Verbal Script
> "In Kafka, **Partitions are the fundamental unit of parallelism and scale**:
>
> - **Ordering Guarantee**: Kafka guarantees message ordering **strictly within a single partition**, NOT across the entire topic. By providing a record key (e.g. `customerId`), Kafka hashes the key to ensure all events for that customer land in the exact same partition.
> - **Consumer Group Scaling**: You cannot have more active consumers in a consumer group than partitions in a topic. Extra consumers will sit idle.
> - **How many partitions?**: A single topic can easily support dozens or hundreds of partitions. However, excessive partitions (e.g. >10,000 per cluster) increase broker leader election time, file descriptors, and client memory."

---

### 5. Troubleshooting Stuck Kafka Consumers & Consumer Lag in Production
#### 🎙️ 60-Second Verbal Script
> "When **Consumer Lag** spikes or a consumer stops processing:
>
> 1. **Check `kafka-consumer-groups.sh --describe`**: Identify which specific partition has high lag and whether the consumer instance is `ACTIVE` or `REBALANCING`.
> 2. **Inspect JVM Thread Dumps (`jstack`)**: Check if consumer worker threads are blocked on database deadlocks, slow external HTTP calls, or infinite loops.
> 3. **Evaluate `max.poll.interval.ms`**: If message processing time exceeds `max.poll.interval.ms` (default 5 mins), the coordinator considers the consumer dead, triggers a **Rebalance Storm**, and revokes its partitions.
>    - *Fix*: Increase `max.poll.interval.ms`, decrease `max.poll.records`, or offload heavy processing to worker pools.
> 4. **Check for Poison Pills**: A malformed message failing deserialization. Configure `ErrorHandlingDeserializer` and route unprocessable records to a **Dead-Letter Queue (DLQ)**."

---

### 6. Kafka Fault Tolerance: Replication Factor, Leader, Follower & ISR
#### 🎙️ 60-Second Verbal Script
> "Kafka achieves fault tolerance through **Partition Replication**:
>
> 1. **Replication Factor (RF)**: Each partition has $N$ copies across brokers (typically `RF=3` in production).
> 2. **Leader Replica**: One broker acts as the Leader for a partition, handling all producer writes and consumer reads.
> 3. **Follower Replicas**: Passive copies on other brokers that continuously fetch and replicate records from the Leader.
> 4. **In-Sync Replicas (ISR)**: The subset of follower replicas that are caught up with the Leader within `replica.lag.time.max.ms`.
>
> **Zero Data Loss Configuration**:
> We set Producer `acks=all` (or `-1`), Topic `min.insync.replicas=2`, and `replication.factor=3`. If the Leader crashes, ZooKeeper/KRaft automatically elects a new Leader from the ISR set with zero message loss."

---

### 7. API Gateway: Role & Core Responsibilities
#### 🎙️ 60-Second Verbal Script
> "An **API Gateway (e.g. Spring Cloud Gateway, Kong, AWS API Gateway)** serves as the single entry point for all client traffic into the microservices ecosystem.
>
> Its core responsibilities include:
> 1. **Request Routing & Path Rewriting**: Routing `/api/v1/orders/**` to the Order microservice.
> 2. **Authentication & Authorization**: Validating JWT tokens and OAuth2 scopes at the edge.
> 3. **Rate Limiting & Throttling**: Protecting backend services using Token Bucket / Redis rate limiters.
> 4. **Cross-Cutting Concerns**: SSL termination, CORS handling, compression, and request/response logging.
> 5. **Resilience**: Integration with Circuit Breakers to return graceful fallback responses during service downtime."

---

### 8. Service Discovery with Netflix Eureka & Client-Side Load Balancing
#### 🎙️ 60-Second Verbal Script
> "**Eureka Service Discovery** eliminates hardcoded IP addresses in dynamic cloud environments:
>
> 1. **Registration**: When a microservice instance boots up, it registers its hostname, IP, and port with the **Eureka Server**.
> 2. **Heartbeats**: Microservices send periodic heartbeats (every 30s) to renew leases. If heartbeats stop, Eureka evicts the instance.
> 3. **Client-Side Load Balancing**: The calling service (via Spring Cloud LoadBalancer or OpenFeign) caches the registry locally and performs Round-Robin load balancing directly on the client side, avoiding extra network hops through a central hardware load balancer."

---

### 9. Enterprise Rule Engines (Drools / Easy Rules)
#### 🎙️ 60-Second Verbal Script
> "A **Rule Engine** (like **Drools** or **Easy Rules**) is an expert system that separates complex, frequently changing business rules from application code.
>
> In our underwriting / loan approval service, instead of thousands of hardcoded `if-else` branches, we write rules in declarative DRL (Drools Rule Language) or decision tables (Excel). The Drools engine uses the **Rete Algorithm** to pattern-match facts (e.g. credit score, income) against rules in memory, allowing business analysts to modify approval thresholds without requiring code deployments."
