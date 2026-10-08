# 10. Microservices Resilience, Rate Limiting, Circuit Breakers & Production RCA

A senior architectural guide covering rate limiting algorithms, Resilience4j circuit breakers, service mesh vs API gateway, eventual consistency, and production latency RCA playbooks.

---

## 📑 Topics Index
1. [Rate Limiting: Concepts, Architectures & Algorithms](#1-rate-limiting-concepts-architectures--algorithms)
2. [Comparing Rate Limiting Algorithms (Token Bucket vs Sliding Window)](#2-comparing-rate-limiting-algorithms-token-bucket-vs-sliding-window)
3. [Circuit Breaker Design Pattern (Resilience4j Internals)](#3-circuit-breaker-design-pattern-resilience4j-internals)
4. [API Gateway vs Service Mesh (Istio / Envoy)](#4-api-gateway-vs-service-mesh-istio--envoy)
5. [Handling Eventual Consistency in Distributed Systems](#5-handling-eventual-consistency-in-distributed-systems)
6. [Senior Engineer Playbook: Troubleshooting Slow APIs in Production](#6-senior-engineer-playbook-troubleshooting-slow-apis-in-production)

---

### 1. Rate Limiting: Concepts, Architectures & Algorithms
#### 🎙️ 60-Second Verbal Script
> "Rate Limiting controls the rate of incoming network traffic to protect backend services from resource exhaustion, cascading failures, brute-force attacks, and noisy neighbors.
>
> In our microservices architecture, we enforce rate limiting at two layers:
> 1. **At the API Gateway / Edge**: Using **Redis-backed Token Bucket / Sliding Window algorithms** (e.g. Spring Cloud Gateway RequestRateLimiter / Bucket4j). This drops excessive requests early with HTTP `429 Too Many Requests` before hitting internal services.
> 2. **At the Service Layer**: Enforcing tenant/tier limits (e.g., Free Tier: 100 req/min; Enterprise Tier: 10,000 req/min)."

---

### 2. Comparing Rate Limiting Algorithms
#### 🎙️ 60-Second Verbal Script
> "| Algorithm | How it Works | Pros | Cons |
> |---|---|---|---|
> | **Token Bucket** | Tokens added at fixed rate up to capacity; each request consumes a token. | Allows short bursts of traffic; memory efficient | Need to tune refill rate & capacity |
> | **Leaky Bucket** | Requests enter a queue; processed at constant steady rate. | Smooths traffic spikes into constant flow | Delays burst requests |
> | **Fixed Window Counter** | Counter resets every fixed minute (e.g. 100/min). | Trivial to implement | Burst at window boundary can allow $2\times$ rate |
> | **Sliding Window Log / Counter** | Weights past window + current window. | **Most accurate**, eliminates boundary bursts | Slightly higher Redis compute |"

```java
// Redis-backed Token Bucket / Bucket4j Configuration Example:
@Configuration
public class RateLimiterConfig {

    public Bucket createNewBucket(String apiKey) {
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
```

---

### 3. Circuit Breaker Design Pattern (Resilience4j Internals)
#### 🎙️ 60-Second Verbal Script
> "The **Circuit Breaker Pattern** prevents cascading failure across microservices when a downstream service becomes unresponsive or degraded.
>
> It operates in 3 distinct states:
> 1. **CLOSED (Normal Operation)**: All requests flow to the downstream dependency. Resilience4j tracks call outcomes (success/failure/slow calls) in a sliding window (e.g. last 100 calls).
> 2. **OPEN (Tripped / Failing)**: If failure rate exceeds the threshold (e.g. >50% errors), the circuit trips to **OPEN**. All incoming calls **immediately fail fast** (or invoke a fallback method) without making network calls, giving the downstream service time to recover.
> 3. **HALF_OPEN (Trial State)**: After a configured wait duration (e.g., 30s), the circuit transitions to HALF_OPEN, allowing a limited number of trial probe requests (e.g. 10 calls). If they succeed, it transitions back to **CLOSED**; if any fail, it resets to **OPEN**."

```java
@Service
public class OrderService {

    private final PaymentClient paymentClient;

    public OrderService(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentService")
    public PaymentResponse processPayment(PaymentRequest request) {
        return paymentClient.charge(request);
    }

    // Graceful Fallback Method
    public PaymentResponse paymentFallback(PaymentRequest request, Throwable t) {
        return new PaymentResponse("QUEUED", "Payment provider unavailable. Request queued for offline retry.");
    }
}
```

---

### 4. API Gateway vs Service Mesh (Istio / Envoy)
#### 🎙️ 60-Second Verbal Script
> "While both handle microservice networking, they operate at different scopes:
>
> - **API Gateway (North-South Traffic)**: Manages traffic **entering the cluster from external clients** (web/mobile). Focuses on client-facing concerns: API composition, authentication/JWT verification, rate limiting, and SSL termination.
> - **Service Mesh (East-West Traffic, e.g. Istio / Linkerd)**: Manages **internal service-to-service communication** within the cluster using sidecar proxies (Envoy) injected into every Pod. Focuses on internal infrastructure concerns: mutual TLS (mTLS) encryption, fine-grained canary traffic routing, and distributed telemetry."

---

### 5. Handling Eventual Consistency in Distributed Systems
#### 🎙️ 60-Second Verbal Script
> "In distributed microservices with private databases per service, ACID transactions across services are impossible without slow, blocking two-phase commits. We achieve **Eventual Consistency** using:
>
> 1. **Transactional Outbox Pattern**: Saves domain state and events atomically in the same local database transaction. Debezium CDC publishes the event to Kafka, ensuring zero message loss.
> 2. **Saga Pattern (Orchestration or Choreography)**: Decomposes a multi-service workflow into local transactions. If step 3 fails, compensating transactions are executed in reverse order to rollback previous steps.
> 3. **Idempotent Consumers**: Every consumer verifies message UUIDs against a processed-events deduplication table, ensuring duplicate message deliveries have zero adverse side-effects."

---

### 6. Senior Engineer Playbook: Troubleshooting Slow APIs in Production
#### 🎙️ 60-Second Verbal Script
> "When an API is experiencing elevated latency in production, I execute a structured 5-step diagnostic playbook:
>
> 1. **Inspect APM & Distributed Tracing (Jaeger / AWS X-Ray)**: Open the Trace ID for the slowest 99th percentile (p99) requests to pinpoint whether time is spent in internal business logic, external REST calls, or database queries.
> 2. **Database & Connection Pool Metrics**: Check HikariCP connection pool metrics (`hikaricp.connections.pending`, `hikaricp.connections.active`). If pending connections spike, inspect PostgreSQL slow query logs (`pg_stat_statements`) for table lock contention or missing indexes.
> 3. **JVM Thread Contention & Garbage Collection**: Check Grafana for GC pause spikes (STW events) or run `jstack` / Arthas to detect thread deadlocks and lock contention.
> 4. **Downstream Dependencies & Network**: Verify if external third-party APIs or microservices are degraded (triggering timeout retries).
> 5. **Mitigate & Remediate**: Enable circuit breaker fallbacks, scale replicas via Kubernetes HPA, or adjust connection pool sizes before deploying a permanent code/index fix."
