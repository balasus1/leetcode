# 08. Modern Java 17/21, Concurrency, Virtual Threads & Immutability

Deep-dive answers, 60-second verbal scripts, concurrency mechanics, and working Java snippets for Senior Backend Engineer interviews.

---

## 📑 Topics Index
1. [Benefits of Sealed Classes in Java 17](#1-benefits-of-sealed-classes-in-java-17)
2. [Virtual Threads (Java 21 Project Loom) & High-Load Request Handling](#2-virtual-threads-java-21-project-loom--high-load-request-handling)
3. [`Optional.map()` vs `Optional.flatMap()`](#3-optionalmap-vs-optionalflatmap)
4. [Garbage Collection Differences: Serial vs Parallel vs G1 vs ZGC](#4-garbage-collection-differences-serial-vs-parallel-vs-g1-vs-zgc)
5. [Local Variable Type Inference (`var`) Best Practices](#5-local-variable-type-inference-var-best-practices)
6. [`CopyOnWriteArrayList` vs `ArrayList`](#6-copyonwritearraylist-vs-arraylist)
7. [`CompletableFuture` for Non-Blocking Asynchronous Programming](#7-completablefuture-for-non-blocking-asynchronous-programming)
8. [Pattern Matching for `switch` & Exhaustiveness in Java 17/21](#8-pattern-matching-for-switch--exhaustiveness-in-java-1721)
9. [Text Blocks (Multi-line Strings) for JSON / XML / SQL](#9-text-blocks-multi-line-strings-for-json--xml--sql)
10. [Immutability in Java & How to Enforce It](#10-immutability-in-java--how-to-enforce-it)

---

### 1. Benefits of Sealed Classes in Java 17
#### 🎙️ 60-Second Verbal Script
> "Introduced as a final feature in Java 17, **Sealed Classes and Interfaces** allow domain designers to restrict which other classes or interfaces may extend or implement them using `sealed`, `permits`, `final`, `non-sealed`, and `sealed` modifiers.
>
> The key benefits are:
> 1. **Domain Modeling Control**: Prevents unwanted third-party subclasses from extending critical core abstractions (e.g. restricting `PaymentMethod` strictly to `CreditCard`, `PayPal`, and `Crypto`).
> 2. **Compiler Exhaustiveness Checking**: When paired with **Pattern Matching for `switch`**, the Java compiler knows every possible subtype. If all permitted subtypes are handled, the compiler does NOT require a fallback `default` branch and alerts you immediately if a new subtype is added without updating the switch logic.
> 3. **Secure API Design**: Enforces closed algebraic data types (ADTs) at the bytecode level without having to declare classes `package-private` or rely on private constructors."

---

### 2. Virtual Threads (Java 21 Project Loom) & High-Load Request Handling
#### 🎙️ 60-Second Verbal Script
> "Prior to Java 21, the JVM used a 1:1 mapping between Java threads and OS kernel threads. Operating system threads are expensive (~1MB stack memory, heavy kernel context-switching cost), limiting a single server to around 2,000–5,000 concurrent active threads before thread exhaustion.
>
> **Virtual Threads (Project Loom)** are lightweight, user-mode threads managed directly by the JVM with an $M:N$ scheduler:
> - Millions of virtual threads can be spawned concurrently, consuming only a few kilobytes of heap memory.
> - **Non-blocking Blocking I/O**: When a virtual thread executes a blocking operation (e.g., waiting for a Postgres SQL query, Kafka publish, or external REST API call), the JVM automatically **unmounts** the virtual thread from its underlying OS **Carrier Thread** (backed by `ForkJoinPool`), freeing that carrier thread to execute other virtual threads. Once the I/O completes, the virtual thread is seamlessly remounted.
> - In Spring Boot 3.2+, enabling `spring.threads.virtual.enabled=true` allows embedded Tomcat to process tens of thousands of concurrent blocking requests with near-zero latency penalty."

---

### 3. `Optional.map()` vs `Optional.flatMap()`
#### 🎙️ 60-Second Verbal Script
> "Both methods transform the value inside an `Optional` if present, but differ in how they handle mapping functions that themselves return an `Optional`:
>
> - **`Optional.map(Function<T, R>)`**: Used when the mapper function returns a plain value `R`. The `map()` method automatically wraps the returned value into `Optional<R>`. If the mapper itself returns an `Optional<R>`, `map()` results in a nested `Optional<Optional<R>>`.
> - **`Optional.flatMap(Function<T, Optional<R>>)`**: Used when the mapper function already returns an `Optional<R>`. It flattens the result, preventing double-wrapped optionals and returning clean `Optional<R>`."

```java
public record Address(String city) {}
public record User(Optional<Address> address) {}

User user = new User(Optional.of(new Address("New York")));

// map() produces Optional<Optional<Address>>
Optional<Optional<Address>> nested = Optional.of(user).map(User::address);

// flatMap() flattens it directly to Optional<Address>
Optional<String> city = Optional.of(user)
        .flatMap(User::address)
        .map(Address::city);
```

---

### 4. Garbage Collection Differences: Serial vs Parallel vs G1 vs ZGC
#### 🎙️ 60-Second Verbal Script
> "| Collector | Threading Model | Primary Goal | Typical STW Pause | Best Use Case |
> |---|---|---|---|---|
> | **Serial GC** | Single-threaded | Minimal memory footprint | 100ms - Seconds | Tiny CLI tools, single-core embedded VMs |
> | **Parallel GC** | Multi-threaded young & old | Maximum raw throughput | 200ms - Multiple seconds | Offline batch computing & data science |
> | **G1 GC** | Multi-threaded region-based | Balanced latency & throughput | 10ms - 200ms (Configurable) | General enterprise web apps (Default in Java 9-21) |
> | **ZGC (Java 21)**| Concurrent colored pointers | Ultra-low latency (< 1ms) | **< 1 millisecond** | High-frequency trading, massive heaps (up to 16TB) |"

---

### 5. Local Variable Type Inference (`var`) Best Practices
#### 🎙️ 60-Second Verbal Script
> "Introduced in Java 10, **`var`** enables local variable type inference, allowing the compiler to infer the static type from the right-hand initialization expression.
>
> **Best Practices**:
> 1. Use `var` to eliminate noisy boilerplate when the type is obvious:
>    `var users = new ArrayList<UserResponseDto>();` or `var connection = getDatabaseConnection();`.
> 2. Avoid `var` when the initializer does not provide clear type information: `var result = process();` (hurts code reviews).
> 3. `var` is strictly compile-time static typing; it is **NOT** dynamic typing like JavaScript.
> 4. `var` cannot be used for method parameters, return types, or class member fields."

---

### 6. `CopyOnWriteArrayList` vs `ArrayList`
#### 🎙️ 60-Second Verbal Script
> "- **`ArrayList`**: Non-synchronized, fast for single-threaded usage. Concurrent modifications during iteration throw `ConcurrentModificationException`.
> - **`CopyOnWriteArrayList`**: A thread-safe variant of `List` where all mutative operations (`add`, `set`, `remove`) create a **fresh cloned copy** of the underlying array.
> - **Performance Characteristics**: Iteration is completely lock-free, extremely fast, and never throws `ConcurrentModificationException`. However, write operations are very expensive ($O(N)$ allocation).
> - **Best Use Case**: Ideal for scenarios where **reads vastly outnumber writes** (e.g. keeping a list of event listeners or read-heavy system configuration caches)."

---

### 7. `CompletableFuture` for Non-Blocking Asynchronous Programming
#### 🎙️ 60-Second Verbal Script
> "**`CompletableFuture`** (Java 8+) implements `Future` and `CompletionStage`, enabling asynchronous, non-blocking pipeline orchestration without blocking caller threads:
>
> - **Creation**: `CompletableFuture.supplyAsync(supplier, customExecutor)`.
> - **Transformation & Chaining**: `.thenApply()` (sync transform), `.thenCompose()` (async flatMap).
> - **Combining Independent Tasks**: `CompletableFuture.allOf(f1, f2, f3).join()` executes multiple microservice calls in parallel and aggregates their results, slashing overall request latency."

```java
public CompletableFuture<UserProfileResponse> fetchUserProfileAsync(String userId) {
    CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() -> userService.getUser(userId), customPool);
    CompletableFuture<List<Order>> ordersFuture = CompletableFuture.supplyAsync(() -> orderService.getOrders(userId), customPool);

    return userFuture.thenCombine(ordersFuture, (user, orders) -> new UserProfileResponse(user, orders));
}
```

---

### 8. Pattern Matching for `switch` & Exhaustiveness in Java 17/21
#### 🎙️ 60-Second Verbal Script
> "Pattern Matching for `switch` (Java 21 LTS) enhances switch statements and expressions from testing simple constants to testing **type patterns, record patterns, and guarded conditions (`when`)**:
>
> ```java
> static String formatValue(Object obj) {
>     return switch (obj) {
>         case Integer i -> String.format("int %d", i);
>         case String s when s.length() > 5 -> "Long string: " + s.toUpperCase();
>         case String s -> "Short string: " + s;
>         case null -> "Null value";
>         default -> "Unknown type";
>     };
> }
> ```
> It eliminates verbose `instanceof` type casting ladders and allows handling `null` directly as a case label."

---

### 9. Text Blocks (Multi-line Strings) for JSON / XML / SQL
#### 🎙️ 60-Second Verbal Script
> "Text Blocks (Java 15+) use triple quotes `"""` to declare multi-line string literals without manual `\n` concatenations or messy backslash escape characters.
>
> It automatically strips incidental whitespace while preserving intentional formatting, making complex SQL queries, JSON mock payloads, and XML templates clean and readable in Java code."

```java
String jsonPayload = """
        {
            "orderId": "%s",
            "amount": %.2f,
            "status": "APPROVED"
        }
        """.formatted("ORD-987", 250.75);
```

---

### 10. Immutability in Java & How to Enforce It
#### 🎙️ 60-Second Verbal Script
> "An **immutable object** cannot have its internal state modified after creation, making it inherently thread-safe without synchronization locks.
>
> **Rules to enforce immutability in Java**:
> 1. Declare the class `final` (or use a `record`) so it cannot be subclassed.
> 2. Make all fields `private` and `final`.
> 3. Do not provide any setter methods.
> 4. Perform **Defensive Copying** in constructors and getters for any mutable fields (e.g. returning `Collections.unmodifiableList(new ArrayList<>(list))` or cloning `Date` instances)."
