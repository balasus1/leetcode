# 🎯 30 Scenario-Based Senior Java Backend & Distributed Systems Deep-Dives

> **Master Guide for Senior / Lead / Principal Java Backend Interviews**  
> Each scenario is broken down into:  
> 1. 🎙️ **2-Minute High-Impact Verbal Script**: Conversational pitch for interviewers and AI avatars.  
> 2. 🧠 **Core Engineering Basics & Internals**: JMM, JVM bytecode, OS kernel, database engines, network protocols.  
> 3. 💥 **Real-World Failure Modes & Cascading Implications**: What breaks, why it breaks, and trade-offs.  
> 4. 💻 **Production-Grade Working Code & Architecture**: Runnable Java, Spring Boot, SQL, Kafka, and AWS configurations.

---

## 📑 Table of Contents

- [Part 1: Java & Concurrency (Scenarios 1–5)](#-part-1-java--concurrency)
  - [01. Multiple threads access a HashMap. What can go wrong?](#01-multiple-threads-access-a-hashmap-what-can-go-wrong)
  - [02. A race condition causes duplicate payments. How would you solve it?](#02-a-race-condition-causes-duplicate-payments-how-would-you-solve-it)
  - [03. Thousands of threads are created and CPU usage spikes. What would you do?](#03-thousands-of-threads-are-created-and-cpu-usage-spikes-what-would-you-do)
  - [04. A REST API takes 30 seconds because of 3 downstream calls. How would you optimize it?](#04-a-rest-api-takes-30-seconds-because-of-3-downstream-calls-how-would-you-optimize-it)
  - [05. Multiple threads update the same record. How would you maintain consistency?](#05-multiple-threads-update-the-same-record-how-would-you-maintain-consistency)
- [Part 2: Spring Boot & JPA/Hibernate (Scenarios 6–10)](#-part-2-spring-boot--jpahibernate)
  - [06. Startup time increases from 15 seconds to 2 minutes. How would you investigate?](#06-startup-time-increases-from-15-seconds-to-2-minutes-how-would-you-investigate)
  - [07. Circular dependency appears after deployment. How would you resolve it?](#07-circular-dependency-appears-after-deployment-how-would-you-resolve-it)
  - [08. API works locally but fails in production with LazyInitializationException.](#08-api-works-locally-but-fails-in-production-with-lazyinitializationexception)
  - [09. Transaction partially updates data even though rollback was expected. Why?](#09-transaction-partially-updates-data-even-though-rollback-was-expected-why)
  - [10. Application suddenly throws OutOfMemoryError. How would you debug it?](#10-application-suddenly-throws-outofmemoryerror-how-would-you-debug-it)
- [Part 3: Microservices & Distributed Architecture (Scenarios 11–15)](#-part-3-microservices--distributed-architecture)
  - [11. Service A → B → C, and C is down. How do you prevent cascading failure?](#11-service-a--b--c-and-c-is-down-how-do-you-prevent-cascading-failure)
  - [12. Same payment gets processed twice. How do you prevent duplicate processing?](#12-same-payment-gets-processed-twice-how-do-you-prevent-duplicate-processing)
  - [13. One microservice becomes slow and affects the entire system. What patterns would you use?](#13-one-microservice-becomes-slow-and-affects-the-entire-system-what-patterns-would-you-use)
  - [14. How would you trace a request across 15 microservices?](#14-how-would-you-trace-a-request-across-15-microservices)
  - [15. REST, Kafka or gRPC — which would you choose and why?](#15-rest-kafka-or-grpc--which-would-you-choose-and-why)
- [Part 4: Apache Kafka & Event-Driven Systems (Scenarios 16–20)](#-part-4-apache-kafka--event-driven-systems)
  - [16. Kafka consumer processes the same message twice. How would you handle it?](#16-kafka-consumer-processes-the-same-message-twice-how-would-you-handle-it)
  - [17. One partition has much higher traffic than others. How would you fix it?](#17-one-partition-has-much-higher-traffic-than-others-how-would-you-fix-it)
  - [18/19. A message fails repeatedly during processing. What should happen next?](#1819-a-message-fails-repeatedly-during-processing-what-should-happen-next)
  - [20. How would you guarantee message ordering for a customer?](#20-how-would-you-guarantee-message-ordering-for-a-customer)
- [Part 5: Database Engineering & SQL Optimization (Scenarios 21–25)](#-part-5-database-engineering--sql-optimization)
  - [21. A query that took 50 ms now takes 10 seconds. How would you troubleshoot it?](#21-a-query-that-took-50-ms-now-takes-10-seconds-how-would-you-troubleshoot-it)
  - [22. Database CPU reaches 100% during peak hours. What steps would you take?](#22-database-cpu-reaches-100-during-peak-hours-what-steps-would-you-take)
  - [23. Two transactions update the same row simultaneously. How would you handle concurrency?](#23-two-transactions-update-the-same-row-simultaneously-how-would-you-handle-concurrency)
  - [24. A table has hundreds of millions of records. How would you improve performance?](#24-a-table-has-hundreds-of-millions-of-records-how-would-you-improve-performance)
  - [25. Optimistic or pessimistic locking for an inventory system — which one and why?](#25-optimistic-or-pessimistic-locking-for-an-inventory-system--which-one-and-why)
- [Part 6: System Design, AWS & Production Reliability (Scenarios 26–30)](#-part-6-system-design-aws--production-reliability)
  - [26. API receives 100,000 requests/minute. How would you scale it?](#26-api-receives-100000-requestsminute-how-would-you-scale-it)
  - [27. Users are abusing your API. How would you implement rate limiting?](#27-users-are-abusing-your-api-how-would-you-implement-rate-limiting)
  - [28. Redis crashes unexpectedly. How should your application behave?](#28-redis-crashes-unexpectedly-how-should-your-application-behave)
  - [29. EC2 needs secure access to S3. How would you configure it without storing credentials?](#29-ec2-needs-secure-access-to-s3-how-would-you-configure-it-without-storing-credentials)
  - [30. A deployment causes increased latency. How would you find the root cause and roll back safely?](#30-a-deployment-causes-increased-latency-how-would-you-find-the-root-cause-and-roll-back-safely)

---

# ☕ Part 1: Java & Concurrency

---

### 01. Multiple threads access a HashMap. What can go wrong?

#### 🎙️ 2-Minute Verbal Script
> "Standard `java.util.HashMap` is fundamentally not thread-safe. When multiple threads perform concurrent write operations or concurrent read/writes without external synchronization, three major catastrophic failure modes occur:
> 1. **Data Loss and Silent Corruption**: Concurrent `put()` operations computing the same bucket index will overwrite the internal node reference, causing one entry to disappear silently without throwing any exception.
> 2. **Broken Internal State (`size` drift and null returns)**: The internal `size` counter is non-atomic (`size++`). Simultaneous increments lead to lost updates, corrupting threshold calculations. Reads (`get()`) during table resizing can traverse partially linked buckets and return `null` even when the key exists.
> 3. **Infinite Loops & 100% CPU (Java 7 Legacy & Treeify Race in Java 8+)**: In Java 7, concurrent rehashing used head-insertion which created circular linked lists (`Node.next = Node`), causing infinite loops during `get()`. In Java 8+, while tail-insertion fixed the circular linked list bug, concurrent restructuring during Red-Black Tree treeification (`treeifyBin`) can corrupt the tree structure (`TreeNode` parent/left/right pointers), throwing `ClassCastException` or spinning indefinitely.
> 
> In production, never synchronize around a raw `HashMap`. Use `ConcurrentHashMap`, which leverages lock-free CAS on empty bucket heads and fine-grained `synchronized` locks per bucket head, providing $O(1)$ lock striping without locking the entire map."

#### 🧠 Core Engineering Basics & Internals
- **Memory Visibility**: `HashMap` fields (`table`, `size`, `modCount`) are NOT marked `volatile`. Modifications in CPU L1/L2 cache on Core 0 are not guaranteed to be visible to Core 1 under the Java Memory Model (JMM) without a happens-before edge.
- **Resize Race Condition**: When `size > threshold` (`capacity * 0.75`), `resize()` allocates a new table and transfers nodes. Concurrent callers create duplicate tables or overwrite bucket pointers midway.

#### 💻 Production-Grade Solution
```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ThreadSafeCacheService {
    // ConcurrentHashMap uses CAS for new buckets and synchronized on bucket head for updates
    private final Map<String, Long> userRequestCounts = new ConcurrentHashMap<>();

    // ATOMIC compound operation: never do containsKey + put
    public void incrementRequest(String userId) {
        userRequestCounts.compute(userId, (key, currentVal) -> (currentVal == null) ? 1L : currentVal + 1L);
    }

    public long getCount(String userId) {
        return userRequestCounts.getOrDefault(userId, 0L);
    }
}
```

---

### 02. A race condition causes duplicate payments. How would you solve it?

#### 🎙️ 2-Minute Verbal Script
> "Duplicate payments occur due to a classic **Check-Then-Act (TOCTOU - Time of Check to Time of Use)** race condition: two concurrent HTTP requests arrive simultaneously (e.g., user double-clicks, network retry), both query `SELECT status FROM payment WHERE order_id = ?`, both see `PENDING`, and both trigger the payment gateway charge.
> 
> To solve this at senior engineering standards, we implement a **Defense-in-Depth Idempotency Architecture** spanning 3 tiers:
> 1. **Client/Gateway Tier (Idempotency Key)**: The client generates a unique UUID `Idempotency-Key` passed in the HTTP header. An API Gateway or filter checks a distributed Redis cache using an atomic `SETNX` (Set if Not Exists) with a 60-second TTL.
> 2. **Database Tier (Unique Constraint & State Machine)**: The database `payments` table has a `UNIQUE (idempotency_key)` or `UNIQUE (order_id)` constraint. The record starts in `PROCESSING` state. The second insert fails immediately with `DataIntegrityViolationException`.
> 3. **Payment Gateway Tier**: When calling Stripe/PayPal/Adyen, we forward the same idempotency key to the external gateway so the downstream processor also deduplicates the charge."

#### 🧠 Core Engineering Basics & Internals
```
[Client Request 1 & 2] 
       │ (Headers: Idempotency-Key: pay_abc123)
       ▼
[Redis SETNX pay_abc123 "IN_PROGRESS" EX 60]
 ├── Request 1: OK (Acquires Lock) ──► Insert DB PENDING ──► Call Stripe ──► DB SUCCESS
 └── Request 2: FAILS (Lock Held)  ──► Wait / Return HTTP 409 Conflict or Cached Result
```

#### 💻 Production-Grade Solution
```java
@Service
public class PaymentProcessingService {

    @Autowired private PaymentRepository paymentRepo;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private PaymentGatewayClient gatewayClient;

    @Transactional
    public PaymentResponse processPayment(String idempotencyKey, BigDecimal amount, String customerId) {
        // 1. Distributed Lock / In-Flight Deduplication via Redis
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent("lock:payment:" + idempotencyKey, "PROCESSING", Duration.ofSeconds(60));

        if (Boolean.FALSE.equals(acquired)) {
            // Check if already completed and return cached response
            return paymentRepo.findByIdempotencyKey(idempotencyKey)
                .map(p -> new PaymentResponse(p.getId(), p.getStatus(), "Existing transaction returned"))
                .orElseThrow(() -> new PaymentConflictException("Payment request currently in progress"));
        }

        try {
            // 2. DB Level Idempotency Record (Unique Index prevents concurrent inserts)
            PaymentEntity payment = new PaymentEntity(idempotencyKey, amount, customerId, PaymentStatus.PROCESSING);
            paymentRepo.saveAndFlush(payment);

            // 3. Call External Gateway with Idempotency Key
            GatewayResult result = gatewayClient.charge(idempotencyKey, amount, customerId);

            // 4. Update Status to COMPLETED
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayTxnId(result.transactionId());
            paymentRepo.save(payment);

            return new PaymentResponse(payment.getId(), PaymentStatus.SUCCESS, "Charged successfully");
        } catch (DataIntegrityViolationException ex) {
            throw new PaymentConflictException("Duplicate payment detected at DB boundary");
        } finally {
            redisTemplate.delete("lock:payment:" + idempotencyKey);
        }
    }
}
```

---

### 03. Thousands of threads are created and CPU usage spikes. What would you do?

#### 🎙️ 2-Minute Verbal Script
> "When thousands of platform threads are spawned, CPU spikes are primarily caused by **thread contention, massive OS context switching overhead, and JVM Garbage Collection pressure**—not useful business execution.
> 
> Each OS thread on Linux allocates a ~1MB thread stack (`-Xss1m`). Spawning 5,000 threads consumes 5GB of non-heap native memory alone. The Linux kernel scheduler spends all CPU time swapping registers and cache lines (`L1/L2/L3` thrashing) rather than executing instructions.
> 
> **Troubleshooting & Remediation Plan**:
> 1. **Immediate Triage**: Run `top -H -p <pid>` to identify high-CPU threads. Run `jstack <pid>` or `jcmd <pid> Thread.print` to convert thread IDs (hex) and inspect thread names and stack traces (e.g., look for unbounded `new Thread()` or `Executors.newCachedThreadPool()`).
> 2. **Architectural Fix**: Replace unbounded thread pools with a strictly bounded `ThreadPoolExecutor` using an explicit `ArrayBlockingQueue` and a sensible `RejectedExecutionHandler` (`CallerRunsPolicy` or `AbortPolicy`).
> 3. **Modern Java 21 Migration**: If the workload is I/O-bound (REST/DB calls), switch to **Virtual Threads (Project Loom)** via `Executors.newVirtualThreadPerTaskExecutor()`. Virtual threads run on a small pool of carrier threads (equal to CPU cores) and yield execution via `Continuation.yield()` when blocking on socket I/O, eliminating OS context switching."

#### 🧠 Core Engineering Sizing Formula
$$\text{Max Threads} = \text{CPU Cores} \times \text{Target CPU Utilization} \times \left(1 + \frac{\text{Wait Time (I/O)}}{\text{Compute Time (CPU)}}\right)$$

#### 💻 Production-Grade Solution
```java
@Configuration
public class ConcurrencyConfig {

    // For CPU-bound or bounded legacy workloads
    @Bean(name = "boundedTaskExecutor")
    public Executor boundedTaskExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
            cores,                      // Core pool size
            cores * 2,                  // Max pool size
            60L, TimeUnit.SECONDS,      // Keep alive time
            new ArrayBlockingQueue<>(500), // STRICT BOUNDED QUEUE
            new CustomThreadFactory("app-worker-"),
            new ThreadPoolExecutor.CallerRunsPolicy() // Backpressure: calling thread executes task
        );
    }

    // Java 21+ Virtual Threads for high-concurrency I/O workloads
    @Bean(name = "virtualThreadExecutor")
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

---

### 04. A REST API takes 30 seconds because of 3 downstream calls. How would you optimize it?

#### 🎙️ 2-Minute Verbal Script
> "A 30-second response time with 3 downstream calls indicates **sequential blocking execution** (10s + 10s + 10s) with excessive socket timeouts and zero caching or circuit breaking.
> 
> To reduce latency from 30s to under 500ms:
> 1. **Parallel Async Execution**: Execute independent downstream calls concurrently using `CompletableFuture.allOf()` backed by a dedicated custom thread pool or Java 21 Virtual Threads. Total response time drops from the sum of latencies ($T_1 + T_2 + T_3$) to $\max(T_1, T_2, T_3)$.
> 2. **Strict Timeout Budgets**: Downstream clients (e.g., Spring `RestClient`, `WebClient`, or `HttpClient`) must enforce strict connect (1s) and read timeouts (2s).
> 3. **Resilience & Fallbacks**: Wrap downstream calls in Resilience4j Circuit Breakers. If a non-critical downstream service (e.g., user recommendations) times out, return a cached or degraded default response instead of failing the parent request.
> 4. **Caching**: Store static or slow-changing downstream data in Redis or an L1 Caffeine cache."

#### 💻 Production-Grade Solution
```java
@Service
public class AggregatorService {

    private final RestClient restClient;
    private final ExecutorService downstreamPool;

    public AggregatorService(RestClient.Builder builder) {
        this.restClient = builder
            .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .build()))
            .build();
        this.downstreamPool = Executors.newVirtualThreadPerTaskExecutor(); // Java 21+
    }

    public DashboardResponse aggregateDashboard(String userId) {
        CompletableFuture<UserProfile> profileFuture = CompletableFuture.supplyAsync(
            () -> fetchWithFallback("https://user-svc/users/" + userId, UserProfile.class, UserProfile.DEFAULT),
            downstreamPool
        );

        CompletableFuture<List<Order>> ordersFuture = CompletableFuture.supplyAsync(
            () -> fetchWithFallback("https://order-svc/orders/" + userId, List.class, Collections.emptyList()),
            downstreamPool
        );

        CompletableFuture<AccountBalance> balanceFuture = CompletableFuture.supplyAsync(
            () -> fetchWithFallback("https://bank-svc/balance/" + userId, AccountBalance.class, AccountBalance.ZERO),
            downstreamPool
        );

        // Wait with global deadline (e.g., max 3 seconds total)
        try {
            CompletableFuture.allOf(profileFuture, ordersFuture, balanceFuture)
                .get(3, TimeUnit.SECONDS);

            return new DashboardResponse(profileFuture.join(), ordersFuture.join(), balanceFuture.join());
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            // Partial degradation
            return new DashboardResponse(
                profileFuture.getNow(UserProfile.DEFAULT),
                ordersFuture.getNow(Collections.emptyList()),
                balanceFuture.getNow(AccountBalance.ZERO)
            );
        }
    }

    private <T> T fetchWithFallback(String uri, Class<T> clazz, T fallback) {
        try {
            return restClient.get().uri(uri).retrieve().body(clazz);
        } catch (Exception ex) {
            // Log warning & return fallback
            return fallback;
        }
    }
}
```

---

### 05. Multiple threads update the same record. How would you maintain consistency?

#### 🎙️ 2-Minute Verbal Script
> "When multiple threads concurrently modify the same record, we face the **Lost Update** anomaly. There are three industry-standard strategies depending on write contention:
> 1. **Optimistic Locking (Low-to-Medium Contention)**: Add a `@Version` integer column to the JPA entity. When updating, JPA generates `UPDATE account SET balance = ?, version = version + 1 WHERE id = ? AND version = ?`. If another thread committed first, the update affects 0 rows, triggering `OptimisticLockException`. We catch this and retry with exponential backoff.
> 2. **Pessimistic Locking (High Contention & Financial Records)**: Use `SELECT ... FOR UPDATE` via `@Lock(LockModeType.PESSIMISTIC_WRITE)`. The database acquires an exclusive row lock at the engine level, serializing concurrent transactions.
> 3. **Atomic DB Expression (High-Throughput Simple Arithmetic)**: Avoid in-memory read-modify-write entirely. Issue atomic SQL: `UPDATE account SET balance = balance + :amount WHERE id = :id AND (balance + :amount) >= 0`."

#### 💻 Production-Grade Solution
```java
// Option 1: Optimistic Locking with JPA
@Entity
public class AccountEntity {
    @Id private Long id;
    private BigDecimal balance;

    @Version
    private Long version; // Managed automatically by Hibernate
}

// Option 2: Atomic In-Database Update (Highest Throughput)
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE AccountEntity a SET a.balance = a.balance - :debitAmount " +
           "WHERE a.id = :accountId AND a.balance >= :debitAmount")
    int atomicDebit(@Param("accountId") Long accountId, @Param("debitAmount") BigDecimal debitAmount);

    // Option 3: Pessimistic Write Lock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a WHERE a.id = :id")
    Optional<AccountEntity> findByIdForUpdate(@Param("id") Long id);
}
```

---

# 🍃 Part 2: Spring Boot & JPA/Hibernate

---

### 06. Startup time increases from 15 seconds to 2 minutes. How would you investigate?

#### 🎙️ 2-Minute Verbal Script
> "A dramatic jump in Spring Boot startup time is typically caused by blocking network operations in bean initialization, heavy database migrations, eager bean instantiation, or classpath scanning bloat.
> 
> **Step-by-Step Investigation**:
> 1. **Spring Boot Startup Flight Recorder**: Enable `BufferingApplicationStartup` in `SpringApplication.setApplicationStartup()` and query the Actuator endpoint `/actuator/startup`. It outputs the exact millisecond duration of every single bean creation and `BeanPostProcessor`.
> 2. **Check Blocking `@PostConstruct` and `InitializingBean`**: Developers often execute blocking HTTP calls or large cache warmups inside `@PostConstruct` or `@EventListener(ApplicationReadyEvent.class)`. Move these to asynchronous background threads.
> 3. **Database Migration Bottlenecks (Flyway/Liquibase)**: Inspect whether a new migration script added an unindexed column to a table with 50M rows, locking the table on startup.
> 4. **Auto-Configuration & Classpath Scan**: Check if `@ComponentScan` is scanning root packages (`com.*`), loading hundreds of unintended configurations."

#### 💻 Production-Grade Debugging Setup
```java
public class Application {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);
        // Record startup metrics (queryable via /actuator/startup)
        app.setApplicationStartup(new BufferingApplicationStartup(2048));
        app.run(args);
    }
}
```
```properties
# application.properties optimization levers
# 1. Enable lazy initialization in local/dev to isolate slow beans
spring.main.lazy-initialization=false

# 2. Expose startup endpoint
management.endpoints.web.exposure.include=health,info,startup
```

---

### 07. Circular dependency appears after deployment. How would you resolve it?

#### 🎙️ 2-Minute Verbal Script
> "A circular dependency occurs when `ServiceA` requires `ServiceB` in its constructor, and `ServiceB` requires `ServiceA` (or through a cycle $A \to B \to C \to A$). During context startup, Spring detects a cycle and throws `BeanCurrentlyInCreationException`.
> 
> Starting in Spring Boot 2.6+, circular references are disabled by default. Setting `spring.main.allow-circular-references=true` is an anti-pattern that hides severe architectural coupling.
> 
> **Proper Resolution Strategies**:
> 1. **Refactor & Extract (Single Responsibility Principle - Recommended)**: The circularity proves that both services share a third common responsibility. Extract that shared logic into a new `ServiceC` or `EventPublisher`, breaking the cycle ($A \to C$ and $B \to C$).
> 2. **Event-Driven Decoupling**: Replace the direct synchronous bean call with Spring's `ApplicationEventPublisher`. `ServiceA` publishes `OrderCompletedEvent`, and `ServiceB` listens via `@EventListener`.
> 3. **Field/Setter Injection with `@Lazy` (Tactical Hotfix)**: Annotate one dependency injection point with `@Lazy`. Spring injects a CGLIB dynamic proxy instead of the real bean, delaying resolution until the first method invocation."

#### 💻 Production-Grade Solution (Event Decoupling)
```java
// BEFORE: Circular Dependency
// OrderService -> PaymentService -> OrderService

// AFTER: Decoupled via ApplicationEvents
@Service
public class OrderService {
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void completeOrder(String orderId) {
        // Business logic...
        eventPublisher.publishEvent(new OrderCompletedEvent(orderId));
    }
}

@Service
public class NotificationService {
    @EventListener
    public void onOrderCompleted(OrderCompletedEvent event) {
        // Process notification without holding a reference to OrderService
    }
}
```

---

### 08. API works locally but fails in production with LazyInitializationException.

#### 🎙️ 2-Minute Verbal Script
> "`LazyInitializationException: could not initialize proxy - no Session` occurs when code attempts to access a lazily-fetched Hibernate association (`fetch = FetchType.LAZY`) after the underlying Hibernate `Session` / database transaction has already closed.
> 
> **Why it works locally but fails in production**:
> In local development, Spring Boot enables **Open Session in View (OSIV)** by default (`spring.jpa.open-in-view=true`). OSIV keeps the database connection and Hibernate session open throughout the entire HTTP request lifecycle—even during JSON serialization in the controller. In production, OSIV is usually disabled (or should be) because holding DB connections during slow HTTP responses starves the HikariCP connection pool.
> 
> **How to fix it properly**:
> 1. Keep OSIV disabled (`spring.jpa.open-in-view=false`).
> 2. Use **JPQL `JOIN FETCH`** or **Spring Data `@EntityGraph`** in the repository layer to eagerly load the exact relations required for the use case in a single SQL query.
> 3. Use **DTO Projections** to select only the required fields directly from the database into record classes."

#### 💻 Production-Grade Solution
```java
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    // Solution 1: JOIN FETCH in JPQL
    @Query("SELECT o FROM OrderEntity o JOIN FETCH o.orderItems WHERE o.id = :id")
    Optional<OrderEntity> findByIdWithItems(@Param("id") Long id);

    // Solution 2: @EntityGraph (dynamic fetch plan)
    @EntityGraph(attributePaths = {"orderItems", "customer"})
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id")
    Optional<OrderEntity> findByIdEagerly(@Param("id") Long id);

    // Solution 3: Direct DTO Projection (Best Performance)
    @Query("SELECT new com.app.dto.OrderSummaryDTO(o.id, o.total, c.email) " +
           "FROM OrderEntity o JOIN o.customer c WHERE o.id = :id")
    Optional<OrderSummaryDTO> findSummaryById(@Param("id") Long id);
}
```

---

### 09. Transaction partially updates data even though rollback was expected. Why?

#### 🎙️ 2-Minute Verbal Script
> "When a `@Transactional` method partially commits data despite an exception, it is almost always due to one of four Spring AOP and transaction manager mechanics:
> 1. **Self-Invocation (Proxy Bypass)**: Calling `@Transactional public void methodB()` from `methodA()` inside the same class bypasses the Spring CGLIB dynamic proxy. The transactional interceptor is never invoked, so `methodB()` runs without a transaction.
> 2. **Checked Exceptions vs Unchecked Exceptions**: By default, Spring transactions roll back **only on `RuntimeException` and `Error`**, NOT on checked exceptions (`java.lang.Exception`, `IOException`, `SQLException`). If a checked exception is thrown, the transaction commits! You must explicitly specify `@Transactional(rollbackFor = Exception.class)`.
> 3. **Swallowed Exceptions (Catch Block)**: Catching an exception inside the method with `try-catch` without rethrowing or invoking `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()` signals to Spring that the error was handled, allowing the transaction to commit.
> 4. **`Propagation.REQUIRES_NEW`**: A nested helper method annotated with `REQUIRES_NEW` creates an independent physical transaction that commits immediately, regardless of the outer transaction rolling back."

#### 💻 Production-Grade Solution
```java
@Service
public class OrderService {

    @Autowired private OrderRepository orderRepo;
    @Autowired private AuditRepository auditRepo;

    // RULE 1: Always specify rollbackFor = Exception.class
    @Transactional(rollbackFor = Exception.class)
    public void processOrder(OrderDTO dto) throws BusinessException {
        orderRepo.save(new OrderEntity(dto));

        try {
            callThirdPartyService(dto);
        } catch (Exception ex) {
            // RULE 2: If swallowing exception, explicitly mark transaction as ROLLBACK ONLY
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new BusinessException("Order processing failed", ex);
        }
    }
}
```

---

### 10. Application suddenly throws OutOfMemoryError. How would you debug it?

#### 🎙️ 2-Minute Verbal Script
> "When an `OutOfMemoryError` strikes, the first step is identifying the exact OOM sub-type from the stack trace:
> - `java.lang.OutOfMemoryError: Java heap space`: Object allocation exceeded `-Xmx` due to memory leaks, unbounded cache growth, or huge batch queries.
> - `java.lang.OutOfMemoryError: Metaspace`: Class metadata exceeded `-XX:MaxMetaspaceSize` due to dynamic proxy generation (CGLIB/ByteBuddy/reflection leaks).
> - `java.lang.OutOfMemoryError: GC Overhead limit exceeded`: JVM spent >98% of CPU time in GC and freed <2% of heap.
> 
> **Production Debugging Protocol**:
> 1. Ensure JVM flags `-XX:+HeapDumpOnOutOfMemoryError` and `-XX:HeapDumpPath=/var/log/dumps/heap.hprof` are enabled in production.
> 2. Open the `.hprof` file in **Eclipse Memory Analyzer Tool (MAT)** or **IntelliJ Profiler**.
> 3. Run the **Dominator Tree** and **Leak Suspects Report**. Check for retained heap sizes held by `ThreadLocal` variables (Tomcat worker thread pools), unbounded static HashMaps, unpaged database queries (`findAll()` returning 10M rows), or unclosed I/O streams."

#### 💻 JVM Production Diagnostic Configuration
```bash
# Recommended Production JVM Flags for Troubleshooting
java -XX:+UseG1GC \
     -Xms4g -Xmx4g \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/app/heap_dump.hprof \
     -XX:+ExitOnOutOfMemoryError \
     -jar app.jar
```

---

# 🌐 Part 3: Microservices & Distributed Architecture

---

### 11. Service A → B → C, and C is down. How do you prevent cascading failure?

#### 🎙️ 2-Minute Verbal Script
> "In a distributed call chain ($A \to B \to C$), when downstream Service C fails or hangs, Service B's worker threads block waiting for socket reads. Service B rapidly exhausts its thread pool, causing Service B to fail, which in turn exhausts Service A's thread pool—bringing down the entire ecosystem.
> 
> To prevent cascading failures, we implement a **Four-Layer Resilience Strategy**:
> 1. **Timeouts (Connect & Read)**: Set aggressive timeouts on Service B's HTTP client (e.g., 500ms connect, 1500ms read). Never leave default infinite timeouts.
> 2. **Circuit Breaker (Resilience4j)**: Wrap calls to Service C in a Circuit Breaker. If the error rate exceeds 50% over a 10-call sliding window, the circuit trips to **OPEN** state. All subsequent calls fail-fast immediately without creating network sockets.
> 3. **Bulkhead Isolation**: Allocate a separate, dedicated thread pool or semaphore for calls to Service C so that a hang in C cannot consume threads needed for other services.
> 4. **Graceful Fallback**: Return cached data, a degraded empty response, or enqueue the request to a dead-letter queue."

#### 💻 Production-Grade Resilience4j Configuration
```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      serviceC:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 20
        minimumNumberOfCalls: 10
        failureRateThreshold: 50.0
        slowCallRateThreshold: 50.0
        slowCallDurationThreshold: 2000ms
        waitDurationInOpenState: 10000ms
        permittedNumberOfCallsInHalfOpenState: 5
  bulkhead:
    instances:
      serviceC:
        maxConcurrentCalls: 20
        maxWaitDuration: 20ms
```
```java
@Service
public class ServiceBClient {

    @Autowired private RestClient restClient;

    @CircuitBreaker(name = "serviceC", fallbackMethod = "fallbackForServiceC")
    @Bulkhead(name = "serviceC")
    public ServiceCData callServiceC(String param) {
        return restClient.get()
            .uri("https://service-c/api/data?param=" + param)
            .retrieve()
            .body(ServiceCData.class);
    }

    public ServiceCData fallbackForServiceC(String param, Throwable ex) {
        // Fallback returns stale cache or safe default
        return ServiceCData.defaultDegraded();
    }
}
```

---

### 12. Same payment gets processed twice. How do you prevent duplicate processing?

#### 🎙️ 2-Minute Verbal Script
> "Duplicate payment processing occurs due to network retries, client double-submissions, or message replay in event-driven systems.
> 
> To guarantee **Exactly-Once Processing Semantics at the Application Layer**:
> 1. **Idempotent Token Verification**: Every payment mutation requires a unique `idempotency_key` generated by the initiator.
> 2. **Two-Phase Idempotency Table with Row-Level Atomic Insert**:
>    - Step 1: Execute `INSERT INTO idempotency_records (key, status, response_payload) VALUES ('key123', 'IN_PROGRESS', NULL) ON CONFLICT (key) DO NOTHING`.
>    - Step 2: If rows inserted is 0, query the record. If status is `IN_PROGRESS`, return HTTP 409 Conflict. If `COMPLETED`, return the stored `response_payload`.
>    - Step 3: If rows inserted is 1, process the payment with the gateway.
>    - Step 4: Update status to `COMPLETED` and persist the JSON response payload atomically within the same transaction."

#### 💻 Production-Grade Solution
```java
@Repository
public class IdempotencyRepository {
    @Autowired private JdbcTemplate jdbcTemplate;

    public boolean startProcessing(String key) {
        String sql = "INSERT INTO idempotency_keys (idempotency_key, status, created_at) " +
                     "VALUES (?, 'IN_PROGRESS', NOW()) ON CONFLICT (idempotency_key) DO NOTHING";
        return jdbcTemplate.update(sql, key) > 0;
    }

    public void complete(String key, String responseJson) {
        String sql = "UPDATE idempotency_keys SET status = 'COMPLETED', response = ? WHERE idempotency_key = ?";
        jdbcTemplate.update(sql, responseJson, key);
    }
}
```

---

### 13. One microservice becomes slow and affects the entire system. What patterns would you use?

#### 🎙️ 2-Minute Verbal Script
> "When one microservice slows down (e.g., due to slow DB queries or GC pauses), upstream services back up as threads wait on blocked I/O sockets.
> 
> We apply five architectural patterns to isolate and contain the slowness:
> 1. **Bulkhead Pattern**: Isolate thread pools per downstream service. If Service C is slow, only its 10 allocated threads in Service B are exhausted; Service B's remaining 190 threads continue serving other clients.
> 2. **Circuit Breaker with Slow Call Rate Thresholding**: Resilience4j flags calls taking >2s as slow. If >50% of calls are slow, it trips the circuit to OPEN, failing fast.
> 3. **Reactive Backpressure / Rate Limiting**: Use Token Bucket rate limiters at the API Gateway to shed excess load (HTTP 429).
> 4. **Asynchronous Decoupling**: Transition synchronous REST calls to Kafka or AWS SQS event queues.
> 5. **Client-Side Load Balancing with Outlier Detection**: Using Envoy or Spring Cloud LoadBalancer, automatically evict slow instance pods from the routing pool."

---

### 14. How would you trace a request across 15 microservices?

#### 🎙️ 2-Minute Verbal Script
> "To trace a transaction traversing 15 microservices, we implement **Distributed Tracing using the W3C TraceContext standard and OpenTelemetry (OTel)**.
> 
> **How it works behind the scenes**:
> 1. **Trace ID & Span ID Generation**: When a request enters the API Gateway, the gateway generates a global 128-bit `TraceId` and an initial `SpanId`.
> 2. **Context Propagation via HTTP Headers**: When making downstream REST/gRPC/Kafka calls, the OpenTelemetry instrumentation injects standard W3C HTTP headers: `traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01` (Version, TraceId, ParentSpanId, TraceFlags).
> 3. **MDC Logging Injection**: Micrometer Tracing automatically binds the `TraceId` to Logback/Log4j2's `MDC` (Mapped Diagnostic Context), prefixing every log line with `[service-name,traceId,spanId]`.
> 4. **Telemetry Collector**: The OpenTelemetry agent exports spans asynchronously over gRPC/Protobuf to an observability backend like **Grafana Tempo, Jaeger, or AWS X-Ray** for timeline visualization."

#### 💻 Production Logback Configuration with MDC Tracing
```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId:-},%X{spanId:-}] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
</configuration>
```

---

### 15. REST, Kafka or gRPC — which would you choose and why?

#### 🎙️ 2-Minute Verbal Script
> "The choice depends on communication synchronicity, payload format, latency constraints, and decoupling needs:
> 
> | Dimension | REST (HTTP/1.1 or HTTP/2 JSON) | gRPC (HTTP/2 Protocol Buffers) | Apache Kafka (Binary Event Streaming) |
> |---|---|---|---|
> | **Primary Use Case** | Public APIs, Web/Mobile frontends, CRUD | Internal Microservice-to-Microservice RPC | Asynchronous Event-Driven Architecture |
> | **Communication** | Synchronous Request/Response | Synchronous / Bidirectional Streaming | Asynchronous Publish/Subscribe |
> | **Payload** | Human-readable Text/JSON (Higher overhead) | Compact Binary Protobuf (5-10x faster) | Schema-backed Binary (Avro / Protobuf) |
> | **Coupling** | Temporal Coupling (Both services must be UP) | Temporal Coupling | Fully Decoupled (Durable storage on disk) |
> 
> **Decision Rule**:
> - Use **REST** for public customer-facing APIs and third-party integrations.
> - Use **gRPC** for internal, low-latency, high-throughput synchronous service-to-service RPC calls where both services must participate in real-time.
> - Use **Kafka** for domain events (e.g., `OrderPlaced`), state synchronization, audit logging, and asynchronous eventual consistency."

---

# 📬 Part 4: Apache Kafka & Event-Driven Systems

---

### 16. Kafka consumer processes the same message twice. How would you handle it?

#### 🎙️ 2-Minute Verbal Script
> "Kafka guarantees **at-least-once delivery** by default. Duplicate processing happens when:
> 1. A consumer processes a message successfully, but crashes before committing the offset.
> 2. The consumer takes longer to process a batch than `max.poll.interval.ms`. Kafka marks the consumer dead, triggers a **rebalance**, and assigns the partition to another consumer who re-processes the uncommitted offset.
> 
> **Solution: Idempotent Consumer Design**:
> 1. **Idempotency Key & Deduplication Store**: Extract a business unique key (e.g., `orderId`, `eventId`) from the Kafka message header or payload.
> 2. **Transactional Outbox / State Guard**: Check a Redis or PostgreSQL deduplication table before executing business logic.
> 3. **Kafka Configuration Tuning**: Disable auto-commit (`enable.auto.commit=false`), use manual immediate acknowledgment (`AckMode.RECORD` or `AckMode.MANUAL_IMMEDIATE`), and tune `max.poll.interval.ms` to accommodate processing time."

#### 💻 Production-Grade Spring Kafka Idempotent Listener
```java
@Component
public class OrderEventConsumer {

    @Autowired private ProcessedEventRepository processedEventRepo;
    @Autowired private OrderService orderService;

    @KafkaListener(topics = "order-events", groupId = "order-processor-group")
    @Transactional
    public void consume(ConsumerRecord<String, OrderEvent> record, Acknowledgment ack) {
        String eventId = record.value().getEventId();

        // 1. Atomic Deduplication Check (DB Unique constraint or insert check)
        boolean isNew = processedEventRepo.insertIfAbsent(eventId, record.partition(), record.offset());
        if (!isNew) {
            ack.acknowledge(); // Skip already-processed message
            return;
        }

        // 2. Business Execution
        orderService.processOrder(record.value());

        // 3. Manual Acknowledgment
        ack.acknowledge();
    }
}
```

---

### 17. One partition has much higher traffic than others. How would you fix it?

#### 🎙️ 2-Minute Verbal Script
> "A single partition experiencing abnormally high traffic is called **Partition Skew (Hot Partition)**. In Kafka, ordering and partition routing are determined by `murmur2(key) % total_partitions`. A hot partition occurs when:
> - A default key has high cardinality imbalance (e.g., a massive merchant like Amazon generates 80% of all orders with `merchantId` as key).
> - Messages are published with `null` keys in older Kafka clients, or the same constant key is passed by mistake.
> 
> **How to fix it**:
> 1. **Key Salting (Compound Key)**: Append a bounded random salt to the key: `key = merchantId + "_" + (random.nextInt(5))`. This distributes the merchant's traffic evenly across 5 partitions.
> 2. **Separate Dedicated Topics for Whales**: Route mega-merchants to a dedicated high-capacity topic while smaller tenants share standard topics.
> 3. **Custom Partitioner**: Implement Kafka's `Partitioner` interface to apply custom routing algorithms.
> 4. **No Key for Non-Ordered Data**: If strict ordering is not required, pass `key = null` so the Kafka producer uses the **Sticky Partitioner** (round-robin per batch), ensuring perfectly uniform distribution."

#### 💻 Production-Grade Custom Partitioner
```java
public class SaltedKeyPartitioner implements Partitioner {

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();

        String rawKey = (String) key;
        if (isHeavyTenant(rawKey)) {
            // Salt heavy tenants across a sub-range of partitions
            int salt = ThreadLocalRandom.current().nextInt(5);
            return Math.abs((rawKey + "_" + salt).hashCode()) % numPartitions;
        }

        // Standard Murmur2 hash for regular keys
        return Math.abs(Utils.murmur2(keyBytes)) % numPartitions;
    }

    private boolean isHeavyTenant(String key) {
        return "MERCHANT_VIP_001".equals(key);
    }

    @Override public void close() {}
    @Override public void configure(Map<String, ?> configs) {}
}
```

---

### 18/19. A message fails repeatedly during processing. What should happen next?

#### 🎙️ 2-Minute Verbal Script
> "A message that fails repeatedly is a **Poison Pill**. If a consumer retries synchronously in-line without bounds, it blocks the partition, halts offset progression, causes consumer lag to explode, and may trigger rebalance storms.
> 
> **Production Dead Letter Queue (DLQ) Architecture**:
> 1. **Non-Blocking Retry Topics with Exponential Backoff**:
>    - `order-topic` (Main topic - immediate processing)
>    - `order-topic-retry-10s` (First retry after 10s)
>    - `order-topic-retry-60s` (Second retry after 60s)
>    - `order-topic-dlq` (Dead Letter Queue after max attempts)
> 2. **Header Enrichment**: When routing to the DLQ, inject error metadata into Kafka Record Headers: `X-Exception-Message`, `X-Exception-Stacktrace`, `X-Original-Topic`, `X-Retry-Count`.
> 3. **Alerting & Replay Tooling**: Trigger PagerDuty/Datadog alerts on DLQ messages and provide an administrative CLI/UI to fix the root cause and replay DLQ records back to the main topic."

#### 💻 Production-Grade Spring Kafka DLQ Configuration
```java
@Configuration
public class KafkaRetryConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
        // DeadLetterPublishingRecoverer routes failed messages to <topic>.DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
            (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        // Exponential backoff: 1s initial, 2.0x multiplier, max 3 retries
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        // Do not retry fatal deserialization or validation errors
        handler.addNotRetryableExceptions(IllegalArgumentException.class, DeserializationException.class);
        return handler;
    }
}
```

---

### 20. How would you guarantee message ordering for a customer?

#### 🎙️ 2-Minute Verbal Script
> "Kafka guarantees strict message ordering **only within a single partition, never across multiple partitions**.
> 
> To guarantee end-to-end message ordering for a customer:
> 1. **Consistent Partition Key**: Always set the Kafka record key to `customerId`. All events for that customer will hash to the exact same partition.
> 2. **Producer Configuration (In-Flight Requests)**:
>    - Set `enable.idempotence=true`.
>    - Set `max.in.flight.requests.per.connection=5` (with idempotence enabled) or `1` (without idempotence) to prevent out-of-order writes when retrying batch failures over the network.
> 3. **Consumer Single-Threaded Processing**: Ensure that the consumer processes events sequentially for that partition. Never dispatch partition records to an asynchronous uncoordinated thread pool."

---

# 🗄️ Part 5: Database Engineering & SQL Optimization

---

### 21. A query that took 50 ms now takes 10 seconds. How would you troubleshoot it?

#### 🎙️ 2-Minute Verbal Script
> "When a fast query degrades from 50ms to 10s, it is rarely random—it points to query plan regression, index degradation, lock contention, or table bloat.
> 
> **Systematic Troubleshooting Protocol**:
> 1. **Run `EXPLAIN (ANALYZE, BUFFERS)` in PostgreSQL or `EXPLAIN FORMAT=JSON` in MySQL**: Check if the optimizer shifted from an **Index Scan** to a **Sequential Scan** (Full Table Scan).
> 2. **Check for Parameter Sniffing / Data Volume Crossing**: As tables cross thresholds, the query optimizer may decide an index scan is more expensive than a sequential scan if statistics are stale. Run `ANALYZE <table>` to refresh cost estimates.
> 3. **Inspect Active Lock Contention**: Query `pg_stat_activity` / `information_schema.innodb_locks` to see if the query is blocked on exclusive row locks (`EXCLUSIVE` / `RowShareLock`) held by an uncommitted long-running transaction.
> 4. **Check Table Bloat & Dead Tuples**: In PostgreSQL, heavy updates/deletes create dead tuples. If autovacuum cannot keep up, an index scan traverses millions of dead pages."

#### 💻 Diagnostic SQL Commands
```sql
-- 1. Inspect execution plan with memory and buffer usage
EXPLAIN (ANALYZE, BUFFERS, SETTINGS)
SELECT * FROM orders WHERE customer_id = 45281 AND created_at >= '2026-01-01';

-- 2. Check for blocking locks and running queries (PostgreSQL)
SELECT 
    pid, now() - pg_stat_activity.query_start AS duration, 
    query, state, wait_event_type, wait_event
FROM pg_stat_activity
WHERE state != 'idle' AND (now() - pg_stat_activity.query_start) > interval '5 seconds';
```

---

### 22. Database CPU reaches 100% during peak hours. What steps would you take?

#### 🎙️ 2-Minute Verbal Script
> "When database CPU reaches 100%, the database is on the verge of connection timeouts and cascading application crashes.
> 
> **Emergency Incident Mitigation Protocol**:
> 1. **Immediate Kill of Runaway Queries**: Query `pg_stat_activity` / `SHOW FULL PROCESSLIST`, locate long-running analytical queries or unindexed scans, and terminate them using `pg_terminate_backend(pid)` or `KILL <id>`.
> 2. **Enable Read Replicas (CQRS Split)**: Direct all read-heavy traffic (`@Transactional(readOnly = true)`) to read replicas via Spring Routing DataSource / AWS Aurora Reader Endpoints.
> 3. **Implement Redis Caching for Hot Keys**: Cache the top 5 most frequently executed query results with short TTLs (30s) to absorb peak read spikes.
> 4. **Connection Pool Clamping**: If 500 app instances open 50 connections each, 25,000 DB connections cause severe CPU context thrashing. Put **AWS RDS Proxy** or **PgBouncer** in front to multiplex connections to a size matching `CPU Cores * 2`."

---

### 23. Two transactions update the same row simultaneously. How would you handle concurrency?

#### 🎙️ 2-Minute Verbal Script
> "When two transactions modify the same row, database transaction isolation levels determine behavior:
> - Under **Read Committed**, Transaction B blocks on the row lock until Transaction A commits, then overwrites Transaction A's change (Lost Update).
> - Under **Repeatable Read / Serializable**, Transaction B throws a serialization failure (`could not serialize access due to concurrent update`).
> 
> **Production Solutions**:
> 1. **Optimistic Locking**: Best for high-read, low-write contention. Check `@Version` column.
> 2. **Pessimistic Locking (`SELECT ... FOR UPDATE`)**: Best for financial balance deductions where conflicts must queue sequentially.
> 3. **Atomic Delta SQL Updates**: Execute `UPDATE wallet SET balance = balance - 50 WHERE id = 1 AND balance >= 50`. The database engine applies row locks internally for microseconds, achieving maximum throughput."

---

### 24. A table has hundreds of millions of records. How would you improve performance?

#### 🎙️ 2-Minute Verbal Script
> "When a table grows to hundreds of millions of rows, standard B-Trees exceed memory (RAM) and queries become disk I/O bound.
> 
> **Database Scaling Playbook**:
> 1. **Horizontal Table Partitioning**: Partition the table by range (e.g., `PARTITION BY RANGE (created_at)`) into monthly or yearly tables. Query pruning skips all non-relevant partitions.
> 2. **Keyset (Cursor-Based) Pagination**: Replace `OFFSET 1000000 LIMIT 20` (which scans and discards 1M rows) with `WHERE id > :lastSeenId ORDER BY id ASC LIMIT 20` ($O(1)$ index seek).
> 3. **Covering Indexes**: Create composite indexes containing all queried columns (`INCLUDE` clause) to achieve Index-Only Scans without table page lookups.
> 4. **Cold Data Archiving**: Move records older than 1 year to AWS S3 Parquet / Apache Iceberg for BigQuery/Athena querying."

#### 💻 Partitioning & Keyset SQL
```sql
-- 1. Table Partitioning by Range
CREATE TABLE orders (
    id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    total_amount DECIMAL(10,2)
) PARTITION BY RANGE (created_at);

CREATE TABLE orders_2026_q1 PARTITION OF orders
    FOR VALUES FROM ('2026-01-01') TO ('2026-04-01');

-- 2. Fast Keyset Pagination (Avoids OFFSET)
SELECT id, customer_id, total_amount 
FROM orders 
WHERE customer_id = 101 AND id > 54829100 
ORDER BY id ASC 
LIMIT 20;
```

---

### 25. Optimistic or pessimistic locking for an inventory system — which one and why?

#### 🎙️ 2-Minute Verbal Script
> "In a high-contention flash sale (e.g., 10,000 users attempting to buy 50 iPhone units in 2 seconds), **Optimistic Locking fails catastrophically**.
> 
> **Why Optimistic Locking Fails Here**:
> With 10,000 concurrent updates on 50 units, 1 request succeeds and 9,999 fail with `OptimisticLockException`. If clients retry, it creates an exponential **Retry Storm**, saturating CPU and DB connections while accomplishing almost zero progress.
> 
> **The Optimal Solution**:
> 1. **Pessimistic Locking or Atomic SQL Decrement**: Use atomic database updates: `UPDATE inventory SET stock = stock - :qty WHERE item_id = :id AND stock >= :qty`.
> 2. **Redis In-Memory Pre-Allocation (High Scale)**: Decrement inventory in Redis using an atomic Lua script (`DECRBY`). Only forward successful allocations to Kafka for asynchronous DB persistence."

#### 💻 High-Throughput Redis Lua Script for Flash Sales
```lua
-- Atomically checks stock and decrements if available
local stock = tonumber(redis.call('get', KEYS[1]))
local requested = tonumber(ARGV[1])

if stock and stock >= requested then
    redis.call('decrby', KEYS[1], requested)
    return 1 -- SUCCESS
else
    return 0 -- OUT OF STOCK
end
```

---

# ☁️ Part 6: System Design, AWS & Production Reliability

---

### 26. API receives 100,000 requests/minute. How would you scale it?

#### 🎙️ 2-Minute Verbal Script
> "100,000 requests per minute is approximately **1,667 requests per second (RPS)** average, with peak spikes around **5,000 RPS**.
> 
> **End-to-End Architectural Blueprint**:
> 1. **Edge Tier**: Route traffic through **AWS CloudFront CDN** (for caching static responses) and **AWS WAF** (DDoS mitigation).
> 2. **Load Balancing & Compute Tier**: Route traffic via **Application Load Balancer (ALB)** across an **AWS ECS Fargate / EKS cluster** with Target Tracking Auto Scaling based on CPU (60%) and ALB RequestCountPerTarget.
> 3. **Application Tier**: Stateless Spring Boot instances with Java 21 Virtual Threads and embedded Caffeine L1 cache.
> 4. **Caching Tier**: Multi-node **Redis Cluster (AWS ElastiCache)** for distributed session and hot data caching.
> 5. **Database Tier**: **AWS Aurora PostgreSQL** with 1 Primary Writer and 2 Reader instances with Auto-scaling, sitting behind **RDS Proxy** for connection pooling."

---

### 27. Users are abusing your API. How would you implement rate limiting?

#### 🎙️ 2-Minute Verbal Script
> "To prevent API abuse and DDoS attacks, we implement a **Distributed Token Bucket / Sliding Window Counter algorithm** at both the API Gateway and service layers.
> 
> **Implementation Architecture**:
> 1. **Key Identifier**: Rate limit by `API-Key` (authenticated users), `JWT Subject`, or Client IP (unauthenticated).
> 2. **Distributed Rate Limiting via Redis**: Use **Bucket4j with Redis** or a Redis Lua script executing an atomic sliding window counter.
> 3. **HTTP 429 Response Headers**: When a client exceeds limits, return `HTTP 429 Too Many Requests` with RFC standard headers:
>    - `X-RateLimit-Limit`: Maximum requests allowed per window.
>    - `X-RateLimit-Remaining`: Remaining request quota.
>    - `Retry-After`: Milliseconds before client can retry."

#### 💻 Production-Grade Spring Cloud Gateway Rate Limiter Config
```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: payment_route
          uri: lb://payment-service
          predicates:
            - Path=/api/v1/payments/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100   # 100 tokens per second
                redis-rate-limiter.burstCapacity: 200   # Max 200 tokens burst
                key-resolver: "#{@userKeyResolver}"
```

---

### 28. Redis crashes unexpectedly. How should your application behave?

#### 🎙️ 2-Minute Verbal Script
> "When Redis crashes, the application must **never crash or hard-fail customer traffic**. It must execute a **Resilient Fail-Open Strategy with Thundering Herd Protection**:
> 
> 1. **Fail-Open Policy**: Wrap Redis calls in a `try-catch` and Resilience4j Circuit Breaker. If Redis times out or throws `RedisConnectionException`, catch it, log a metric, and fall back to the primary database.
> 2. **Thundering Herd / Cache Stampede Protection**: If 10,000 concurrent threads bypass the dead cache and hit the database simultaneously, the DB will collapse. Protect the database using:
>    - **In-Memory Local L1 Cache (Caffeine)** with a 60-second TTL.
>    - **Semaphore / Singleflight Locks**: Allow only 1 thread per entity key to query the DB while other threads wait for the result."

#### 💻 Production-Grade Fallback with Cache Stampede Guard
```java
@Service
public class ProductService {

    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private ProductRepository productRepo;
    private final Cache<Long, ProductDTO> l1Cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(60))
        .maximumSize(10_000)
        .build();

    public ProductDTO getProduct(Long id) {
        // 1. Check L1 Memory Cache first (Survives Redis crash)
        ProductDTO l1Val = l1Cache.getIfPresent(id);
        if (l1Val != null) return l1Val;

        // 2. Try Redis with Fail-Open try-catch
        try {
            String redisVal = redisTemplate.opsForValue().get("product:" + id);
            if (redisVal != null) {
                ProductDTO dto = deserialize(redisVal);
                l1Cache.put(id, dto);
                return dto;
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable, falling back to database for product {}", id);
        }

        // 3. Fallback to Database and populate L1
        ProductDTO dbVal = productRepo.findById(id).map(ProductDTO::new).orElse(null);
        if (dbVal != null) l1Cache.put(id, dbVal);
        return dbVal;
    }
}
```

---

### 29. EC2 needs secure access to S3. How would you configure it without storing credentials?

#### 🎙️ 2-Minute Verbal Script
> "Never store hardcoded `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` in properties files, environment variables, or Git repositories.
> 
> **Zero-Credential IAM Configuration**:
> 1. **AWS IAM Role & Instance Profile**: Create an IAM Role (e.g., `AppS3ReaderRole`) with a least-privilege policy allowing `s3:GetObject` on the specific bucket ARN.
> 2. **Attach to EC2**: Attach the IAM Role to the EC2 instance via an **Instance Profile**.
> 3. **AWS Security Token Service (STS) & IMDSv2**: The AWS SDK for Java uses the `DefaultCredentialsProvider` chain. It automatically queries the **EC2 Instance Metadata Service v2 (IMDSv2)** at `http://169.254.169.254/latest/meta-data/iam/security-credentials/`, fetching short-lived, auto-rotating temporary credentials.
> 4. **Application Code**: Simply instantiate `S3Client.create()`. The SDK handles authentication transparently."

#### 💻 Production-Grade AWS SDK v2 Code & IAM Policy
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:PutObject"],
      "Resource": "arn:aws:s3:::my-production-bucket/*"
    }
  ]
}
```
```java
@Configuration
public class AwsS3Config {

    @Bean
    public S3Client s3Client() {
        // Automatically resolves credentials from EC2 Instance Profile IMDSv2
        return S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
}
```

---

### 30. A deployment causes increased latency. How would you find the root cause and roll back safely?

#### 🎙️ 2-Minute Verbal Script
> "When a production deployment triggers a latency spike, the primary objective is **Mean Time to Recovery (MTTR)**—mitigate first, diagnose deeply second.
> 
> **Deployment Latency Incident Protocol**:
> 1. **Automated Canary Analysis & Immediate Rollback**: If using Blue/Green or Canary deployments (via Argo Rollouts / AWS CodeDeploy), check the automated health metrics. If p99 latency exceeds 500ms or error rates exceed 1%, immediately roll back traffic 100% to the previous stable revision.
> 2. **Differential APM Trace Comparison**: In Datadog/Dynatrace/Grafana, compare the service latency flamegraph of the new deployment against the previous build:
>    - Did database query duration spike? (Missing index in migration, N+1 query regression).
>    - Did JVM GC pause time spike? (Memory leak or high allocation rate in new code).
>    - Did lock contention increase? (Synchronized block or database row locks).
> 3. **Thread Dump & Async-Profiler Analysis**: On a canary pod, run `async-profiler` to identify CPU flamegraph hotspots or lock bottlenecks before destroying the container."

---

## 🏆 Summary Checklist for Scenario Interviews

| # | Scenario Domain | Core Principle / Silver Bullet |
|---|---|---|
| **1-5** | **Java & Concurrency** | Use `ConcurrentHashMap`, Idempotency keys, Bounded ThreadPools/Virtual Threads, and Atomic DB expressions. |
| **6-10** | **Spring Boot & JPA** | Profile via `/actuator/startup`, Event-driven cycle breaking, `JOIN FETCH`/DTO projections, and `-XX:+HeapDumpOnOutOfMemoryError`. |
| **11-15** | **Microservices** | Circuit Breakers, Bulkheads, 2-Phase Idempotency tables, W3C Distributed Tracing, and Protocol optimization. |
| **16-20** | **Apache Kafka** | Idempotent consumers, Salted partition keys, Non-blocking retry topics/DLQ, and Partition-keyed ordering. |
| **21-25** | **Databases & SQL** | `EXPLAIN (ANALYZE, BUFFERS)`, Connection pooling via PgBouncer/RDS Proxy, Range Partitioning, and Redis Lua pre-allocation. |
| **26-30** | **System Design & AWS** | Auto-scaled ECS/Fargate, Redis sliding window rate limiters, Fail-open caching, and IAM EC2 Instance Profiles. |
