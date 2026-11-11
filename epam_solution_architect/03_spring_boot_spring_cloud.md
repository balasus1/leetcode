# 03: Spring Boot, Spring Cloud, Spring Data, AOP & Testing (Basic → Advanced)

> JD: *"Expert-level Java, Spring Boot, Spring Cloud, microservices… write core framework components."*
> Spring Security is covered in [07_api_integration_security.md](07_api_integration_security.md). Spring Kafka is covered in [06_data_messaging_kafka_rabbitmq.md](06_data_messaging_kafka_rabbitmq.md).

| Level | Questions |
|---|---|
| Basic | B1–B10 |
| Medium (Boot / Data / AOP / Test) | M1–M12 |
| Spring Cloud | SC1–SC8 |
| Advanced | A1–A7 |

---

## Basic

### B1. What are IoC and DI in Spring?
🎤 "With Inversion of Control, the container creates and wires objects instead of the objects creating their own dependencies. Dependency Injection is how it supplies them. I always use **constructor injection**: dependencies are explicit, fields can be `final`, it's easy to unit-test without Spring, and circular dependencies fail fast at startup instead of hiding."

### B2. What are the bean scopes?
🎤 "**singleton** (default, one per context), **prototype** (new instance per lookup), plus the web scopes **request**, **session** and **application**. A common trap: injecting a prototype or request-scoped bean into a singleton gives you one instance forever. Use `ObjectProvider<T>`, a scoped proxy, or `@Lookup`. Singletons must be stateless or thread-safe."

### B3. `@Component` vs `@Bean` vs `@Configuration`?
🎤 "`@Component` (and `@Service`, `@Repository`, `@Controller`) marks your own classes for component scanning. `@Bean` methods inside a `@Configuration` class create beans for **third-party classes** or when construction needs logic. In a full `@Configuration` class, the methods are CGLIB-proxied so calls between `@Bean` methods return the same singleton. `proxyBeanMethods=false` (lite mode) skips that for faster startup when you don't need it."

### B4. How does Spring Boot auto-configuration work?
🎤 "Boot reads auto-configuration classes listed in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. Each is guarded by **conditions**: `@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty`. So it configures, for example, a DataSource only if a driver is on the classpath and you haven't defined one yourself. Your beans always win. To debug, use `--debug` or the actuator `conditions` endpoint to see what matched and why."

### B5. Profiles and externalized configuration: what's the precedence?
🎤 "Configuration comes from many sources: `application.yml`, profile-specific files, environment variables, command-line args, and config servers or Kubernetes ConfigMaps. Later sources override earlier ones; command-line and env vars beat files. I bind config into **type-safe `@ConfigurationProperties` records** with `@Validated`, so a bad value fails startup. Secrets never go in YAML. They come from Vault or AWS Secrets Manager via a config import."

### B6. How do you handle errors globally in a REST API?
🎤 "With `@RestControllerAdvice` and `@ExceptionHandler`, mapping domain exceptions to **RFC 9457 `ProblemDetail`** responses, which Spring 6 supports natively. That gives a consistent error contract: type, title, status, detail, plus extensions like `traceId`. I enable `spring.mvc.problemdetails.enabled=true` and never leak stack traces to clients."

```java
@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(SeatUnavailableException.class)
    ProblemDetail seat(SeatUnavailableException e) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        pd.setType(URI.create("https://api.example.com/problems/seat-unavailable"));
        pd.setProperty("seatId", e.seatId());
        return pd;
    }
}
```

### B7. What is Spring Boot Actuator, and what do you expose?
🎤 "Production endpoints: `health` (with liveness and readiness groups for Kubernetes), `metrics` and `prometheus` via Micrometer, `info`, `loggers` (change log levels at runtime), and `threaddump`/`heapdump`, secured. I expose only what's needed, on a **separate management port**, never publicly, and behind authentication."

### B8. `@RestController` vs `@Controller`; how are requests mapped?
🎤 "`@RestController` = `@Controller` + `@ResponseBody`, so return values are serialized (JSON via Jackson). The `DispatcherServlet` routes requests to handler methods via `@GetMapping` and similar, binds `@PathVariable`, `@RequestParam` and `@RequestBody`, validates with `@Valid`, and handles content negotiation."

### B9. How does bean validation work?
🎤 "Jakarta Bean Validation annotations (`@NotBlank`, `@Size`, `@Pattern`, custom constraints) go on DTOs. Add `@Valid` on the controller parameter, or `@Validated` on a class for method-level validation. Validation failures map to 400 problem details. Business rules like 'seat already held' belong in the domain layer, not annotations."

### B10. What is Spring Boot DevTools / Testcontainers for local dev?
🎤 "DevTools gives fast restarts and LiveReload in development only. Spring Boot 3.1+ has **Testcontainers at dev time** with `@ServiceConnection`, so running the app locally spins up real Postgres, Kafka and Redis containers automatically. That's the same infrastructure as the tests, with no shared dev database."

---

## Medium

### M1. `@Transactional`: propagation, isolation, and the classic pitfalls? ✅
🎤 (~1.5 min) "Spring wraps the bean in a **proxy** that starts and commits the transaction. The pitfalls I check for in code reviews:

1. **Self-invocation**: calling a `@Transactional` method from another method in the same class bypasses the proxy, so there's no transaction. Move it to another bean.
2. **Rollback rules**: by default only **unchecked** exceptions roll back. Checked exceptions commit unless you add `rollbackFor`.
3. **Non-public methods** aren't proxied by default.
4. **Long transactions with remote calls inside**: holding DB connections and locks while calling an HTTP service. That was exactly the scheduler's lock-contention issue. Keep transactions short and do I/O outside.

**Propagation**: `REQUIRED` (join or create), `REQUIRES_NEW` (suspend and create, which I use for audit logging that must persist even if the business transaction rolls back), `NESTED` (savepoints), `MANDATORY`, `NOT_SUPPORTED`. **Isolation**: the default is the database's (READ COMMITTED on Postgres). Raise it only for specific invariants, and prefer explicit locking (`SELECT … FOR UPDATE`) or optimistic `@Version` over SERIALIZABLE everywhere. And use `readOnly = true` on queries, which enables flush-mode and routing optimizations."

```java
@Transactional                                   // short: DB only
public JobRun claimAndMarkRunning(long jobId) {
    var job = repo.findByIdForUpdate(jobId);     // SELECT ... FOR UPDATE (narrow lock scope)
    job.transitionTo(RUNNING);
    outbox.save(JobEvent.started(job));          // event in the same transaction (outbox)
    return job.toRun();
}                                                 // commit → lock released; downstream call happens AFTER
```

### M2. How does Spring AOP work? JDK proxy vs CGLIB, and its limits?
🎤 (~1 min) "Spring AOP is **proxy-based**. A JDK dynamic proxy is used if the bean implements an interface and proxying is interface-based. Otherwise, and by default in Boot, a **CGLIB subclass proxy** is used. Advice runs when calls go **through the proxy**. Limits: no interception of self-invocation, of `private` or `final` methods, or of objects not managed by Spring. For those you need AspectJ weaving. I use aspects for cross-cutting concerns: tenant binding, audit, metrics, and Resilience4j annotations, plus `@Transactional` and `@Cacheable`, which are themselves AOP. Control ordering with `@Order`, for example tenant binding before audit."

```java
@Aspect @Component @Order(1)
class AuditAspect {
    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    void audit(JoinPoint jp, Audited audited, Object result) {
        auditLog.record(audited.action(), TenantContext.current(), jp.getArgs(), result);
    }
}
```

### M3. JPA/Hibernate: the N+1 problem and how you fix it?
🎤 (~1 min) "Loading 100 bookings, then lazily touching `booking.getPassengers()` for each, fires 1 + 100 queries. I detect it with SQL logging or Hibernate statistics in tests, which can assert query counts. Fixes: **fetch join** or **`@EntityGraph`** for that use case, **batch fetching** (`hibernate.default_batch_fetch_size=50`), or **DTO projections** for read screens, which is often the best option because you select only the columns you need. Never switch to `EAGER` globally; it moves the problem everywhere."

### M4. Optimistic vs pessimistic locking in Spring Data? ✅
🎤 (~1.5 min) "**Optimistic**: a `@Version` column. The update includes `WHERE version = ?`, and if another transaction changed the row you get `OptimisticLockException`, then retry or tell the user. Great for **low-contention edits** like admin changes to a job definition. **Pessimistic**: `@Lock(PESSIMISTIC_WRITE)` gives `SELECT … FOR UPDATE`, blocking others. Use it for **high-contention, must-not-conflict transitions**. In the scheduler, state transitions (pause, resume, stop, delete) used pessimistic locks, because two operators or two pods acting on the same job must serialize. The lesson was to **lock briefly**: acquire, transition, commit. That plus metadata caching took P99 from 680 to 210 ms. Add lock timeouts (`jakarta.persistence.lock.timeout`) so waits fail fast."

```java
public interface JobRepository extends JpaRepository<Job, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("select j from Job j where j.id = :id")
    Job findByIdForUpdate(@Param("id") Long id);
}
```

### M5. Caching with Spring: `@Cacheable`, L1/L2, invalidation? ✅
🎤 (~1.5 min) "I've implemented **L1 and L2 caching** in distributed systems. L1 is an in-process **Caffeine** cache (microseconds, per pod). L2 is **Redis** (shared across pods). The scheduler cached job definitions and cron metadata with about a 10-minute TTL, removing roughly 80% of repetitive DB reads. Invalidation: when an admin edits a job, we evict in Redis and publish an **invalidation message via Redis pub/sub**, so every pod drops its L1 entry. To prevent **stampedes**: Caffeine's `refreshAfterWrite`, single-flight loading, and **jittered TTLs**, so keys don't all expire together. Rules: cache only what tolerates staleness, put the tenant in the key, set size limits, and monitor hit ratio. Never cache seat availability for booking decisions; the hold always checks the source of truth."

### M6. How do you test Spring Boot apps (slices, Testcontainers, contracts)?
🎤 (~1.5 min) "I follow a pyramid. **Unit tests** with plain JUnit 5 and Mockito, no Spring context, for domain logic. **Slice tests**: `@WebMvcTest` for controllers and serialization, `@DataJpaTest` for repositories, `@JsonTest`. **Integration tests** with `@SpringBootTest` plus **Testcontainers** (real Postgres, Kafka, Redis), never H2, because SQL dialect and locking behaviour differ, and our locking logic needs a real Postgres. **Contract tests**: Spring Cloud Contract or Pact, and OpenAPI-based validation, which is the getConsenso philosophy. **Architecture tests** with ArchUnit. Coverage thresholds in CI with JaCoCo. Speed tricks: reuse contexts (avoid `@MockBean` sprawl, which forces new contexts), and reuse Testcontainers singletons across tests."

### M7. Spring MVC vs WebFlux, now that virtual threads exist?
🎤 (~1.5 min) "WebFlux is non-blocking and reactive. It handles huge concurrency with few threads, has backpressure built in, and suits streaming like SSE, gateways and proxies. The cost is a different programming model, harder debugging, and the need for reactive drivers end to end (R2DBC, reactive Kafka). One blocking call ruins it. **Spring MVC with virtual threads** now gives most of the scalability for typical I/O-bound microservices with simple imperative code and the JDBC ecosystem. My default today is MVC plus virtual threads. I choose WebFlux for gateways (Spring Cloud Gateway is reactive), streaming endpoints, or when the team already has reactive expertise."

### M8. `RestTemplate` vs `WebClient` vs `RestClient` vs OpenFeign vs HTTP interfaces?
🎤 (~1 min) "`RestTemplate` is in maintenance mode. **`RestClient`** (Spring 6.1) is the modern synchronous fluent client, ideal with virtual threads. **`WebClient`** is reactive and non-blocking. **HTTP interface clients** (`@HttpExchange`) are declarative like Feign but built into Spring. OpenFeign remains common in Spring Cloud estates. Whatever the client: **timeouts always** (connect and read), connection pooling, Resilience4j around it, and propagated trace and auth headers."

```java
@HttpExchange("/v1/jobs")
interface JobClient {
    @PostExchange("/{id}/trigger") TriggerResult trigger(@PathVariable String id, @RequestHeader("Idempotency-Key") String key);
}
@Bean JobClient jobClient(RestClient.Builder b) {
    var rc = b.baseUrl("http://billing").requestFactory(ClientHttpRequestFactories.get(
            ClientHttpRequestFactorySettings.DEFAULTS.withConnectTimeout(Duration.ofSeconds(1))
                                                    .withReadTimeout(Duration.ofSeconds(3)))).build();
    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(rc)).build().createClient(JobClient.class);
}
```

### M9. How do you make Spring Boot startup and memory lean?
🎤 (~1 min) "Remove unused starters and auto-configurations. Use lazy initialization selectively. Use **Class Data Sharing** (AppCDS), or **Spring AOT + GraalVM native image** for sidecars and serverless workloads (tens of milliseconds to start, far less memory, at the cost of build time and reflection configuration). **CRaC** (checkpoint/restore) on supported JDKs. Also tune `MaxRAMPercentage` and thread counts. Native image was the right call for the AI governance sidecar I designed, where per-pod memory mattered."

### M10. How do you schedule work in Spring: `@Scheduled` vs Quartz vs a workflow engine? ✅
🎤 (~1.5 min) "`@Scheduled` is in-process: simple cron or fixed-rate, but **every instance runs it**. Without ShedLock or leader election, you get duplicates across pods, and there's no persistence or misfire handling. **Quartz in clustered JDBC mode** persists triggers, ensures one node fires each trigger using DB row locks (`QRTZ_LOCKS`), and handles misfires and calendars. That's why I chose it for the centralized scheduler, adding fire-and-forget, recurring and chained jobs on top. **Workflow engines** (Temporal, Camunda, Step Functions) are for long-running, stateful, multi-step processes with retries, timers and human steps; they complement a scheduler rather than replace it. **Spring Batch** is for chunk-oriented bulk processing with restartability; Quartz can trigger it."

### M11. Spring Batch: when do you use it?
🎤 (~1 min) "For large, restartable bulk processing: reader → processor → writer in **chunks** with transaction per chunk, skip and retry policies, and a `JobRepository` that records progress, so a failed job **restarts from the last committed chunk**. Partitioning or remote chunking scales it out. Use cases: nightly reconciliations, data migrations, re-embedding a document corpus for RAG. Real-time streams belong on Kafka instead."

### M12. How do you implement idempotent REST endpoints in Spring?
🎤 (~1 min) "Clients send an `Idempotency-Key` header on POSTs. A filter or aspect stores key → (request hash, status, response) with a TTL, using a unique constraint in the DB or Redis `SET NX`. A retry with the same key returns the stored response. The same key with a different body gets a 422. Essential for payments and job triggers, where client retries after a timeout are normal."

---

## Spring Cloud

### SC1. What does Spring Cloud give you, and what do you still need it for on Kubernetes?
🎤 (~1.5 min) "Spring Cloud provides the distributed-systems plumbing: **Gateway**, **Config**, service discovery (**Eureka**, Consul or Kubernetes), **Circuit Breaker** (an abstraction over Resilience4j), **OpenFeign**, **Stream** (a binder abstraction over Kafka and RabbitMQ), **Bus**, and **Contract**. On Kubernetes or OpenShift, some of it overlaps with the platform. Service discovery comes from **Kubernetes DNS and Services**, config from **ConfigMaps and Secrets** (via Spring Cloud Kubernetes, or plain env vars), and traffic management, mTLS and retries can come from a **service mesh**. So on OpenShift I typically keep Spring Cloud Gateway (when we need Java-level routing logic), Circuit Breaker/Resilience4j, Stream or Spring Kafka, and Contract, and drop Eureka and Config Server. I document that split in an ADR so teams don't do retries in both the mesh *and* the app, which multiplies retries."

### SC2. Spring Cloud Gateway: what do you use it for?
🎤 (~1 min) "It's a reactive API gateway with routes, predicates and filters. I use it for **JWT validation** at the edge, routing and versioning, **rate limiting** (Redis-backed `RequestRateLimiter`), request and response header manipulation, circuit breaker filters with fallbacks, and a **BFF**-style aggregation layer. On AWS, if we just need authentication, throttling and keys, a managed gateway (API Gateway) may be enough. I use SCG when routing logic is complex or needs custom Java filters."

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: booking
          uri: lb://booking-service
          predicates: [ Path=/api/v1/bookings/** ]
          filters:
            - name: RequestRateLimiter
              args: { redis-rate-limiter.replenishRate: 50, redis-rate-limiter.burstCapacity: 100, key-resolver: "#{@userKeyResolver}" }
            - name: CircuitBreaker
              args: { name: booking, fallbackUri: forward:/fallback/booking }
```

### SC3. Config Server vs Kubernetes ConfigMaps vs AWS AppConfig/Parameter Store?
🎤 (~1 min) "**Config Server** is Git-backed with versioning and refresh via Bus. Good for VM estates or many environments. **ConfigMaps/Secrets** are native to Kubernetes and GitOps-friendly via ArgoCD, but changes typically need a rolling restart unless you watch them. **AWS AppConfig / Parameter Store / Secrets Manager** are managed, with validation, gradual rollout and automatic rollback for feature flags. My rule: static config in ConfigMaps via GitOps, **secrets** in Vault or Secrets Manager (never ConfigMaps in plain text), and **runtime flags** in a flag service."

### SC4. Service discovery: Eureka vs Kubernetes DNS?
🎤 (~45 s) "Eureka is client-side discovery with a registry and client-side load balancing. Useful outside Kubernetes. On Kubernetes, Services give a stable DNS name with kube-proxy or mesh load balancing, so Eureka is redundant there. I avoid running both."

### SC5. Spring Cloud Circuit Breaker / Resilience4j configuration you'd defend? ✅
🎤 (~1.5 min) "Per dependency, not global. A **count-based sliding window** of around 50 calls with a minimum number of calls before it evaluates, so low traffic doesn't trip it. **Failure-rate threshold** around 50% and, importantly, a **slow-call threshold**: calls over X seconds count as failures, because slowness is what cascades. **Wait duration in open state** around 20–30 seconds, then half-open with a few permitted calls. Combine it with a **TimeLimiter** (the breaker needs calls to end), **bulkhead** per dependency, and **retry only for idempotent operations, with jitter**, ordered so retry sits inside the circuit breaker, otherwise retries hammer a failing service. Plus a fallback that degrades usefully, as in the scheduler's switch to the Kafka channel. Expose breaker states as metrics and alert on 'open'."

```yaml
resilience4j:
  circuitbreaker:
    instances:
      billing:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 50
        minimum-number-of-calls: 20
        failure-rate-threshold: 50
        slow-call-duration-threshold: 2s
        slow-call-rate-threshold: 60
        wait-duration-in-open-state: 20s
        permitted-number-of-calls-in-half-open-state: 5
  timelimiter:
    instances: { billing: { timeout-duration: 3s } }
  bulkhead:
    instances: { billing: { max-concurrent-calls: 40, max-wait-duration: 20ms } }
```

### SC6. Spring Cloud Stream vs Spring Kafka directly?
🎤 (~1 min) "**Stream** abstracts the broker behind functional `Supplier`/`Function`/`Consumer` beans and binders. It's portable between Kafka and RabbitMQ and quick to start. **Spring Kafka** gives full control over transactions, error handlers, DLT, seeking and batch listeners. For platform-critical flows like the Saga and outbox relays I use **Spring Kafka** for explicit control. For simple event consumers, Stream's brevity is fine."

### SC7. Distributed tracing in Spring Boot 3?
🎤 (~45 s) "Spring Cloud Sleuth is replaced by **Micrometer Tracing** with an OpenTelemetry or Brave bridge. Trace context propagates through RestClient, WebClient, Kafka and @Async (with context propagation), and log lines get `traceId`/`spanId` via MDC. Export via OTLP to Jaeger, Tempo or X-Ray. The Observation API gives you a metric and a span from one instrumentation point."

### SC8. Spring Cloud Contract / consumer-driven contracts: when?
🎤 (~1 min) "When many consumers depend on a provider and you want to catch breaking changes **before deployment**. Consumers define expectations, the provider build verifies them, and stubs are published for consumer tests. It complements OpenAPI-based governance (my getConsenso work): OpenAPI defines the agreed shape, and contracts verify the behaviour that real consumers use."

---

## Advanced

### A1. Build a custom Spring Boot starter as a platform component (e.g., tenant + audit + observability)
🎤 (~1.5 min) "As an architect I often encode standards as a **starter**, so every service gets them by adding one dependency. For example, a `platform-starter` that auto-configures: a servlet filter that binds the **tenant and correlation ID** from the JWT into MDC and context; an **audit aspect** publishing structured audit events; **ProblemDetail** error handling; Micrometer common tags; Resilience4j defaults; and Jackson settings. Each piece is guarded by `@ConditionalOnProperty` and `@ConditionalOnMissingBean`, so teams can opt out or override. It's versioned and released like a product, with a changelog. It's how standards scale across 20 teams without policing."

```java
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(PlatformProperties.class)
public class PlatformAutoConfiguration {

    @Bean @ConditionalOnMissingBean
    FilterRegistrationBean<TenantContextFilter> tenantFilter(PlatformProperties p) {
        var reg = new FilterRegistrationBean<>(new TenantContextFilter(p.tenantClaim()));
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return reg;
    }

    @Bean @ConditionalOnProperty(prefix = "platform.audit", name = "enabled", matchIfMissing = true)
    AuditAspect auditAspect(AuditPublisher publisher) { return new AuditAspect(publisher); }

    @Bean MeterRegistryCustomizer<MeterRegistry> commonTags(PlatformProperties p) {
        return r -> r.config().commonTags("service", p.serviceName(), "team", p.team());
    }
}
// register in META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### A2. Multi-tenancy in Spring/Hibernate: strategies and choice ✅
🎤 (~1.5 min) "Three strategies. **Database per tenant**: strongest isolation, easy per-tenant backup and restore, but expensive and operationally heavy at hundreds of tenants. **Schema per tenant**: Hibernate `MultiTenantConnectionProvider` plus a `CurrentTenantIdentifierResolver` switching schema. A good middle ground, though migrations multiply. **Shared schema with a `tenant_id` column**: cheapest, and scales to thousands of tenants, but isolation depends on discipline. So I enforce it in the database with **PostgreSQL row-level security** (`SET LOCAL app.tenant_id`), or Hibernate 6's `@TenantId`, not just in application `WHERE` clauses. My usual answer is **tiered**: shared schema with RLS for most tenants, dedicated schema or database for regulated or very large tenants. And always: tenant in cache keys, logs, metrics and audit (as in getConsenso's `tenant_id`/`api_key_id` audit), plus an automated cross-tenant leak test in CI."

### A3. Zero-downtime deployments and DB schema changes with Spring Boot
🎤 (~1.5 min) "The application side: graceful shutdown (`server.shutdown=graceful`, a preStop sleep so the load balancer drains first), readiness probes that only pass when warm, and rolling or blue-green or canary releases. The harder part is the **database**. I use **expand–contract** with Flyway or Liquibase. **Expand**: add the new column or table, nullable, backward compatible. Deploy code that **writes both** old and new and reads old. Backfill in batches. Switch reads to new. Stop writing old. **Contract**: drop the old column in a later release. Never rename or drop in the same release as the code change, because during a rolling deploy old and new versions run side by side. Large backfills run as throttled batch jobs, not inside the migration transaction."

### A4. Event publishing inside Spring transactions: the dual-write problem
🎤 (~1.5 min) "If you save to the DB and then `kafkaTemplate.send()`, a crash between them leaves the DB updated without an event, or the reverse if you send first. Options: `@TransactionalEventListener(phase = AFTER_COMMIT)`, which publishes after commit but can still lose the event if the process crashes right after. Kafka transactions synced with DB transactions: best effort, and not atomic across two resources. Or the **transactional outbox**: write the event to an `outbox` table in the **same DB transaction**, and a relay (Debezium CDC or a poller) publishes it to Kafka. Consumers are idempotent because delivery is at-least-once. The outbox is my default for anything that matters: booking events and scheduler job events."

```java
@Transactional
public void confirm(Booking b) {
    b.confirm();
    bookings.save(b);
    outbox.save(new OutboxEvent(UUID.randomUUID(), "booking", b.id(), "BookingConfirmed", json(b), Instant.now()));
}
```

### A5. Diagnosing a Spring Boot service slow under load ✅
🎤 (~1.5 min) "My checklist, from experience. **Connection pool**: Hikari `pending` greater than zero means threads wait for connections. Is the pool too small, or are transactions too long because of remote calls inside `@Transactional`? **Open Session in View**: `spring.jpa.open-in-view` defaults to true, holding a connection for the whole request including view rendering. I disable it. **N+1 queries**: count them in tests. **Tomcat threads** saturated by slow downstream calls with no timeouts. **Serialization**: huge object graphs or entities serialized directly. **Logging** synchronous at debug level. **GC** pauses. I check actuator metrics (`hikaricp.connections.pending`, `http.server.requests` percentiles, `jvm.gc.pause`), thread dumps, and SQL plans, then fix and re-run the JMeter profile to prove the change."

### A6. Spring decisions you made that could be better?
🎤 (~1 min) "**Open-in-view left at its default** on an early project caused connection starvation under load. Now it's off in our platform starter. **Feign with default timeouts** caused hung threads, so timeouts are mandatory in our templates. **Retries configured in both the Feign client and the service mesh** multiplied traffic during an outage, so there's now one retry owner per hop, documented in an ADR. And **H2 in tests** hid a Postgres locking bug that only surfaced in staging, so it's Testcontainers only now."

### A7. How would you structure a Spring Boot microservice codebase (hexagonal)?
🎤 (~1 min) "**Hexagonal / ports and adapters.** `domain` holds entities, value objects and domain services, with no Spring dependencies. `application` holds use cases (transaction boundaries) and the port interfaces. `adapters/in` holds REST controllers and Kafka listeners. `adapters/out` holds JPA repositories, REST clients and Kafka publishers. Dependencies point inward, which ArchUnit enforces. Benefits: the domain is unit-testable without Spring, adapters are swappable (REST vs Kafka channel, as in the scheduler), and modernization is easier because legacy integrations sit behind ports."
