# 03. Spring Boot Core, Cache, Async & @Transactional Mastery

A comprehensive guide covering Spring IoC mechanics, caching, concurrency, Spring AOP proxy architectures, transaction propagation, isolation levels, and common failure traps.

---

## 📑 Topics Index
1. [Where and How to Use `@Async`](#1-where-and-how-to-use-async)
2. [Spring Cache Architecture (`@Cacheable`, `@CacheEvict`, Redis)](#2-spring-cache-architecture-cacheable-cacheevict-redis)
3. [`@Autowired` vs `@Qualifier`](#3-autowired-vs-qualifier)
4. [Resolving Multiple Beans of Same Type (`@Primary`, `@Qualifier`)](#4-resolving-multiple-beans-of-same-type-primary-qualifier)
5. [Can Two Beans Have the Same Name? (Bean Definition Overriding)](#5-can-two-beans-have-the-same-name-bean-definition-overriding)
6. [BeanFactory vs ApplicationContext](#6-beanfactory-vs-applicationcontext)
7. [Spring Boot Actuator: What Exactly Does It Monitor?](#7-spring-boot-actuator-what-exactly-does-it-monitor)
8. [`ResponseEntity<T>` in REST APIs](#8-responseentityt-in-rest-apis)
9. [`@Transactional` Deep Dive: How It Works Internally via AOP Proxies](#9-transactional-deep-dive-how-it-works-internally-via-aop-proxies)
10. [The Self-Invocation Problem & Why `@Transactional` Fails](#10-the-self-invocation-problem--why-transactional-fails)
11. [Transaction Rollback Rules (`rollbackFor = Exception.class`)](#11-transaction-rollback-rules-rollbackfor--exceptionclass)
12. [Transaction Propagation Levels: `REQUIRED` vs `REQUIRES_NEW`](#12-transaction-propagation-levels-required-vs-requires_new)
13. [Transaction Isolation Levels & Anomalies (Dirty, Non-Repeatable & Phantom Reads)](#13-transaction-isolation-levels--anomalies-dirty-non-repeatable--phantom-reads)
14. [Why API Calls Should NEVER Be Placed Inside `@Transactional`](#14-why-api-calls-should-never-be-placed-inside-transactional)

---

### 1. Where and How to Use `@Async`
#### 🎙️ 60-Second Verbal Script
> "`@Async` marks a method for asynchronous execution on a separate background thread, immediately returning control to the caller.
>
> We use `@Async` for non-blocking side-effects that should not delay the HTTP response:
> 1. Sending confirmation emails or SMS alerts.
> 2. Publishing audit logs and analytics events.
> 3. Triggering heavy background document or PDF generation.
>
> In production, we enable `@EnableAsync` and **always configure a custom `ThreadPoolTaskExecutor` bean**. By default, Spring uses `SimpleAsyncTaskExecutor` which does not reuse threads and spawns a new thread per task, risking thread exhaustion under load. Methods can return `void` or `CompletableFuture<T>`."

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "appAsyncTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("AsyncWorker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

---

### 2. Spring Cache Architecture (`@Cacheable`, `@CacheEvict`, Redis)
#### 🎙️ 60-Second Verbal Script
> "Spring Cache is an abstraction layer that transparently caches method results using Spring AOP without coupling code to concrete cache providers (Caffeine, Redis, Hazelcast).
>
> - **`@Cacheable("products")`**: Checks if the key exists in cache. If found, returns the cached value directly; if not, executes the database method and puts the result in cache.
> - **`@CachePut("products", key = "#product.id")`**: Always executes the method and updates the cache with the new value.
> - **`@CacheEvict("products", key = "#id")`**: Removes entries from the cache upon deletion/invalidation (or `allEntries = true`).
>
> In distributed microservices, we back Spring Cache with **Redis CacheManager** with TTLs to prevent memory leaks and cache stampedes."

---

### 3. `@Autowired` vs `@Qualifier`
#### 🎙️ 60-Second Verbal Script
> "- **`@Autowired`**: Injects dependencies **by type** by querying the Spring `ApplicationContext`.
> - **`@Qualifier`**: Used alongside `@Autowired` to resolve ambiguity when **multiple beans of the same type** exist, injecting **by specific bean name**."

---

### 4. Resolving Multiple Beans of Same Type (`@Primary`, `@Qualifier`)
#### 🎙️ 60-Second Verbal Script
> "If two beans of the same interface exist without qualification, Spring throws **`NoUniqueBeanDefinitionException`**.
>
> We resolve this by:
> 1. **`@Primary`**: Designates one bean as the default fallback when multiple candidates exist.
> 2. **`@Qualifier("beanName")`**: Explicitly specifies the exact candidate at injection point.
> 3. **Injecting a Collection**: Spring can inject all implementations into a `List<Interface>` or `Map<String, Interface>` (as used in the Strategy Pattern)."

---

### 5. Can Two Beans Have the Same Name?
#### 🎙️ 60-Second Verbal Script
> "In a single Spring `ApplicationContext`, bean names must be unique. 
>
> If two beans are registered with the same name:
> - In older Spring versions, the second bean definition silently overwrote the first.
> - In **Spring Boot 2.1+ / 3.x**, bean overriding is disabled by default and throws **`BeanDefinitionOverrideException`**. It can only be re-enabled by setting `spring.main.allow-bean-definition-overriding=true` (which is generally discouraged in production)."

---

### 6. BeanFactory vs ApplicationContext
#### 🎙️ 60-Second Verbal Script
> "| Feature | `BeanFactory` | `ApplicationContext` |
> |---|---|---|
> | **Type** | Basic IoC container | Enterprise-grade advanced container |
> | **Bean Instantiation** | **Lazy** (on-demand when `getBean()` called) | **Eager** (all singletons created at startup) |
> | **Features** | Basic DI only | Full AOP, Event publishing (`ApplicationEvent`), i18n messages, Actuator, Environment properties |
> | **Usage** | Resource-constrained embedded devices | Modern enterprise web/cloud apps (Always preferred) |"

---

### 7. Spring Boot Actuator: What Exactly Does It Monitor?
#### 🎙️ 60-Second Verbal Script
> "**Spring Boot Actuator** provides production-ready monitoring and management endpoints out of the box:
>
> 1. **Health (`/actuator/health`)**: Reports liveness/readiness status and checks connectivity to DB, Redis, Kafka, and Disk.
> 2. **Metrics (`/actuator/metrics` & `/actuator/prometheus`)**: Exposes JVM memory, GC pauses, CPU, HTTP requests, and HikariCP connection pool metrics for Prometheus/Grafana.
> 3. **Environment & Config (`/actuator/env`)**: Inspects active profiles and environment properties.
> 4. **Thread Dumps (`/actuator/threaddump`)**: Generates live JVM thread stack traces.
> 5. **Loggers (`/actuator/loggers`)**: Dynamically updates logging levels (e.g. from `INFO` to `DEBUG`) at runtime without restarting the container."

---

### 8. `ResponseEntity<T>` in REST APIs
#### 🎙️ 60-Second Verbal Script
> "`ResponseEntity<T>` represents the entire HTTP response: **Status Code, HTTP Headers, and Response Body**.
>
> While returning a raw DTO always defaults to HTTP 200 OK, `ResponseEntity` provides complete programmatic control to return semantic HTTP status codes (`201 Created` with `Location` header, `204 No Content`, `404 Not Found`) and custom headers (e.g., pagination headers or caching `ETag` headers)."

---

### 9. `@Transactional` Deep Dive: How It Works Internally via AOP Proxies
#### 🎙️ 60-Second Verbal Script
> "Spring's `@Transactional` uses **Spring AOP Dynamic Proxies (CGLIB or JDK dynamic proxies)** to intercept method calls:
>
> 1. When a caller invokes a transactional method, it interacts with the **Proxy**, not the actual target bean.
> 2. The proxy delegates to **`TransactionInterceptor`**, which queries the **`PlatformTransactionManager`** (e.g., `JpaTransactionManager`).
> 3. The manager opens a database connection, binds it to the current thread via **`TransactionSynchronizationManager` (using `ThreadLocal`)**, sets `connection.setAutoCommit(false)`, and begins the transaction.
> 4. The target method executes within a `try-catch` block.
> 5. If the method completes successfully, the interceptor calls `connection.commit()`. If an unhandled `RuntimeException` or `Error` occurs, it calls `connection.rollback()`."

---

### 10. The Self-Invocation Problem & Why `@Transactional` Fails
#### 🎙️ 60-Second Verbal Script
> "The **Self-Invocation Problem** occurs when a method inside a Spring bean calls another `@Transactional` method within the **same class** using `this.internalMethod()`.
>
> Because the call is internal via `this`, it **completely bypasses the Spring AOP proxy**. The `TransactionInterceptor` is never invoked, meaning **no transaction is started or propagated**.
>
> **Fixes**:
> 1. Refactor the transactional method into a separate `@Service` class (Best practice).
> 2. Inject the service into itself using `@Lazy private MyService self;` and call `self.internalMethod()`.
> 3. Use AspectJ compile-time weaving instead of Spring AOP."

---

### 11. Transaction Rollback Rules (`rollbackFor = Exception.class`)
#### 🎙️ 60-Second Verbal Script
> "By default, Spring `@Transactional` rolls back **ONLY for Unchecked Exceptions** (`RuntimeException` and `Error`).
>
> If a method throws a **Checked Exception** (like `IOException`, `SQLException`, or custom checked exceptions), Spring will **NOT rollback the transaction by default**; it commits the transaction!
>
> To ensure rollback on any exception, always explicitly declare:
> `@Transactional(rollbackFor = Exception.class)`."

---

### 12. Transaction Propagation Levels: `REQUIRED` vs `REQUIRES_NEW`
#### 🎙️ 60-Second Verbal Script
> "- **`REQUIRED` (Default)**: Joins the existing transaction if one exists; creates a new one if none exists. If the child transaction fails, the entire outer transaction is marked rollback-only.
> - **`REQUIRES_NEW`**: Always suspends any existing outer transaction and starts an **independent, separate transaction**. The inner transaction commits or rolls back independently of the outer transaction (ideal for audit logging where audit logs must persist even if the main order transaction fails).
> - **Other Propagation Types**: `SUPPORTS`, `NOT_SUPPORTED`, `MANDATORY`, `NEVER`, and `NESTED` (uses JDBC Savepoints within the same transaction)."

---

### 13. Transaction Isolation Levels & Anomalies
#### 🎙️ 60-Second Verbal Script
> "| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read |
> |---|---|---|---|
> | **READ_UNCOMMITTED** | Possible | Possible | Possible |
> | **READ_COMMITTED (Postgres/Oracle Default)**| **Prevented** | Possible | Possible |
> | **REPEATABLE_READ (MySQL InnoDB Default)** | **Prevented** | **Prevented** | Possible |
> | **SERIALIZABLE** | **Prevented** | **Prevented** | **Prevented** |
>
> - **Dirty Read**: Reading uncommitted changes made by another transaction that might rollback.
> - **Non-Repeatable Read**: Re-reading the same row within a transaction returns different column values.
> - **Phantom Read**: Re-executing a range query returns newly inserted/deleted rows."

---

### 14. Why API Calls Should NEVER Be Placed Inside `@Transactional`
#### 🎙️ 60-Second Verbal Script
> "Placing external HTTP/REST API calls inside a `@Transactional` boundary is a **critical anti-pattern leading to catastrophic Connection Pool Exhaustion**:
>
> 1. When a transaction starts, it acquires a dedicated database connection from HikariCP.
> 2. If an external API call takes 3 seconds (due to network latency or retries), that database connection remains locked and idle for all 3 seconds.
> 3. Under high concurrency, the entire HikariCP connection pool (e.g. 10-30 connections) becomes completely starved in seconds, causing all other incoming database queries to timeout with `ConnectionTimeoutException`.
>
> **Best Practice**: Separate the workflow: (1) Fetch DB data, (2) Commit/Close transaction, (3) Perform external API call, (4) Start a new short transaction to persist the API response."
