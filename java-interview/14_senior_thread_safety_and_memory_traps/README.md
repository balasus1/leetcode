# 14. Thread Safety, Memory Traps & Concurrency Deep-Dive

20 deep-dive questions covering Java Memory Model (JMM), compound operations, safe publication, `ThreadLocal` leaks in thread pools, and cross-user data corruption root cause analysis.

---

## 📑 Topics Index
1. [Determining Thread Safety in a Java Class](#1-determining-thread-safety-in-a-java-class)
2. [Immutable Classes with Mutable Internal Fields](#2-immutable-classes-with-mutable-internal-fields)
3. [The Synchronized Method Fallacy (Why Every-Method-Synchronized Fails)](#3-the-synchronized-method-fallacy)
4. [Thread-Safe vs Immutable vs Stateless vs Concurrent](#4-thread-safe-vs-immutable-vs-stateless-vs-concurrent)
5. [Reference Escape: Returning Internal Mutable Collections](#5-reference-escape-returning-internal-mutable-collections)
6. [Why `Collections.unmodifiableList()` is NOT Inherently Thread-Safe](#6-why-collectionsunmodifiablelist-is-not-inherently-thread-safe)
7. [Iterating Over Synchronized Collections & `ConcurrentModificationException`](#7-iterating-over-synchronized-collections)
8. [`Collections.synchronizedList()` vs `CopyOnWriteArrayList`](#8-collectionssynchronizedlist-vs-copyonwritearraylist)
9. [Combining Two Thread-Safe Objects into an Unsafe Composite](#9-combining-two-thread-safe-objects-into-an-unsafe-composite)
10. [Check-Then-Act & Time-of-Check to Time-of-Use (TOCTOU) Races](#10-check-then-act--toctou-races)
11. [Thread-Safe Cache Insertion Pattern](#11-thread-safe-cache-insertion-pattern)
12. [ConcurrentHashMap Misuse in Compound Operations](#12-concurrenthashmap-misuse-in-compound-operations)
13. [When to Use `computeIfAbsent()` over `containsKey()` + `put()`](#13-when-to-use-computeifabsent)
14. [Thread-Safe Singleton Instantiation vs Unsafe Mutable State](#14-thread-safe-singleton-instantiation-vs-unsafe-state)
15. [Why Double-Checked Locking REQUIRES `volatile` (Instruction Reordering)](#15-why-double-checked-locking-requires-volatile)
16. [Safe Publication & Partially Initialized Object Escape](#16-safe-publication--partially-initialized-object-escape)
17. [Lambdas Capturing Mutable State](#17-lambdas-capturing-mutable-state)
18. [`ThreadLocal`: Solving Thread-Safety while Causing Data & Memory Leaks](#18-threadlocal-solving-safety-while-causing-leaks)
19. [Designing a Read-Heavy Shared Cache (`StampedLock` Optimistic Reads)](#19-designing-a-read-heavy-shared-cache)
20. [🔥 Production Incident RCA: Users Receiving Another User's Data](#20-production-incident-rca-users-receiving-another-users-data)

---

### 1. Determining Thread Safety in a Java Class
#### 🎙️ 60-Second Verbal Script
> "A Java class is **thread-safe** if it behaves correctly when accessed from multiple threads simultaneously, without requiring caller synchronization, regardless of thread scheduling or interleaving.
>
> I evaluate thread safety across 4 criteria:
> 1. **Shared Mutable State**: Does the class contain mutable instance or static fields shared across threads? (If state is purely stateless or immutable, it is inherently thread-safe).
> 2. **Atomicity**: Are multi-step compound operations executed atomically without race conditions?
> 3. **Visibility**: Are state mutations made by one thread guaranteed to be visible to other threads via `volatile`, `synchronized`, locks, or explicit JMM Happens-Before edges?
> 4. **Safe Publication**: Is the object fully constructed before its reference is exposed to other threads?"

---

### 2. Immutable Classes with Mutable Internal Fields
#### 🎙️ 60-Second Verbal Script
> "Yes! An immutable class **can contain a mutable field** (such as a `Date` or `List<String>`) and remain strictly thread-safe, provided it enforces **Defensive Copying and Encapsulation**:
>
> 1. **Defensive Copy in Constructor**: Clone or copy incoming mutable parameters (`this.list = new ArrayList<>(incomingList);`).
> 2. **Private Final Reference**: Keep the field `private final` so no external code can reassign the reference.
> 3. **Defensive Copy in Getter**: Never return the raw internal reference. Return `Collections.unmodifiableList(new ArrayList<>(this.list))` or a fresh clone.
> 4. **No Internal Mutations**: Never mutate the internal field after construction completes."

---

### 3. The Synchronized Method Fallacy
#### 🎙️ 60-Second Verbal Script
> "Making every individual method `synchronized` (like legacy `Vector` or `Hashtable`) **does NOT make the class thread-safe against compound operations**.
>
> While each individual method call is atomic, between two consecutive synchronized method calls (e.g. `if (!vector.contains(item)) { vector.add(item); }`), the lock is released. Another thread can interleave and mutate state during that gap, leading to classic race conditions. Thread safety requires atomicity across the **entire compound business operation**."

---

### 4. Thread-Safe vs Immutable vs Stateless vs Concurrent
#### 🎙️ 60-Second Verbal Script
> "- **Stateless Class**: Has no fields; operates solely on method parameters (e.g. Spring `@Service` beans without instance variables). Always inherently thread-safe.
> - **Immutable Class**: State is assigned once during construction and can never change (`final` fields, `record`). Always inherently thread-safe.
> - **Thread-Safe Class**: Encapsulates mutable state and uses synchronization/locks internally to guarantee correctness under concurrent access.
> - **Concurrent Class**: A specialized thread-safe class engineered for **high-throughput parallelism** using lock striping, CAS, or copy-on-write techniques rather than coarse global locks (e.g. `ConcurrentHashMap`)."

---

### 5. Reference Escape: Returning Internal Mutable Collections
#### 🎙️ 60-Second Verbal Script
> "Returning an internal mutable collection directly from a getter causes a catastrophic **Reference Escape (Aliasing Bug)**.
>
> External callers can mutate the internal collection directly (`service.getUserList().clear()`) bypassing all class synchronization, validation, and invariants without the enclosing class ever knowing."

---

### 6. Why `Collections.unmodifiableList()` is NOT Inherently Thread-Safe
#### 🎙️ 60-Second Verbal Script
> "`Collections.unmodifiableList(backingList)` is merely an unmodifiable **wrapper view**, not an immutable collection:
>
> 1. If another thread holds a reference to the underlying `backingList` and modifies it, the unmodifiable view will reflect those changes immediately, causing race conditions or `ConcurrentModificationException` during iteration.
> 2. Elements *inside* the list can still be mutable objects whose internal states can be modified.
>
> **Solution**: Use `List.copyOf(collection)` (Java 10+) which creates a true, deeply detached unmodifiable list."

---

### 7. Iterating Over Synchronized Collections
#### 🎙️ 60-Second Verbal Script
> "Iterating over `Collections.synchronizedList()` using an enhanced for-loop or `Iterator` is **NOT automatically synchronized**.
>
> Creating the iterator and traversing elements requires multiple steps. If another thread mutates the list during iteration, the iterator's `expectedModCount != modCount` check fails, throwing **`ConcurrentModificationException`**.
>
> **Fix**: You must explicitly synchronize on the collection instance across the entire iteration loop:
> ```java
> synchronized (syncList) {
>     for (String item : syncList) { process(item); }
> }
> ```"

---

### 8. `Collections.synchronizedList()` vs `CopyOnWriteArrayList`
#### 🎙️ 60-Second Verbal Script
> "- **`Collections.synchronizedList`**: Wraps an ArrayList with a mutex lock for every operation. Readers block writers, and writers block readers. Iteration requires explicit manual synchronization.
> - **`CopyOnWriteArrayList`**: Reads and iterations are completely **lock-free and never throw `ConcurrentModificationException`**. Writes create a fresh cloned array copy. Ideal for read-heavy, write-rare workloads."

---

### 9. Combining Two Thread-Safe Objects into an Unsafe Composite
#### 🎙️ 60-Second Verbal Script
> "Yes! Composing two individually thread-safe objects (e.g. two `AtomicInteger`s or two `ConcurrentHashMap`s) into a multi-step operation produces an unsafe composite unless coordinated by a single common lock.
>
> For example: `atomicX.incrementAndGet()` followed by `atomicY.decrementAndGet()` creates a window where intermediate invariants (such as $X + Y = 100$) are broken."

---

### 10. Check-Then-Act & TOCTOU Races
#### 🎙️ 60-Second Verbal Script
> "**Check-Then-Act** occurs when code observes a condition (e.g. `if (account.balance >= 100)`) and then acts on it (`account.withdraw(100)`).
>
> This creates a **Time-of-Check to Time-of-Use (TOCTOU)** race window: between the check and the act, another thread can alter the state, making the observed check invalid."

---

### 11. Thread-Safe Cache Insertion Pattern
```java
// ❌ WRONG: Non-atomic Check-Then-Act
if (!cache.containsKey(key)) {
    cache.put(key, value);
}

// ✅ CORRECT: Atomic Single-Step
cache.putIfAbsent(key, value);

// ✅ BEST (Avoids expensive pre-computation of value):
cache.computeIfAbsent(key, k -> loadExpensiveValueFromDatabase(k));
```

---

### 12. ConcurrentHashMap Misuse in Compound Operations
#### 🎙️ 60-Second Verbal Script
> "Developers mistakenly assume that because `ConcurrentHashMap` is thread-safe, any code using it is thread-safe.
>
> Calling `map.get(key)` followed by `map.put(key, count + 1)` is two separate atomic operations. Under concurrency, two threads reading `count=5` simultaneously will both write `6`, losing an update.
>
> **Fix**: Use atomic compound methods: `map.merge(key, 1L, Long::sum)` or `map.compute(key, (k, v) -> v == null ? 1 : v + 1)`."

---

### 13. When to Use `computeIfAbsent()`
#### 🎙️ 60-Second Verbal Script
> "Use `computeIfAbsent(key, mappingFunction)` when the value generation is computationally expensive or involves database/network calls.
>
> `computeIfAbsent` executes atomically per bucket: the mapping function is invoked **at most once per key**, completely eliminating duplicate redundant database queries and cache stampedes."

---

### 14. Thread-Safe Singleton Instantiation vs Unsafe State
#### 🎙️ 60-Second Verbal Script
> "Yes! A singleton class can have completely thread-safe instantiation (e.g., via Bill Pugh or Spring `@Component` singleton scope), but if that singleton contains **unprotected mutable instance fields (e.g. `private int counter` or `private User currentUser`)**, concurrent requests will corrupt that shared state.
>
> Singleton thread safety applies strictly to its *creation*, NOT its internal state."

---

### 15. Why Double-Checked Locking REQUIRES `volatile`
#### 🎙️ 60-Second Verbal Script
> "Without `volatile`, Double-Checked Locking fails due to **CPU instruction reordering**:
>
> `instance = new Singleton()` is compiled into 3 bytecode steps:
> 1. Allocate memory.
> 2. Execute constructor to initialize fields.
> 3. Assign memory address to `instance` variable.
>
> The JVM/CPU compiler is allowed to reorder steps to: **1 $\to$ 3 $\to$ 2**.
> If Thread A executes step 1 and step 3 (assigning the non-null pointer before the constructor finishes), Thread B enters the method, sees `instance != null` in the outer check, and returns a **partially initialized, corrupted object!**
>
> Declaring `private static volatile Singleton instance;` introduces a memory barrier preventing instruction reordering."

---

### 16. Safe Publication & Partially Initialized Object Escape
#### 🎙️ 60-Second Verbal Script
> "**Safe Publication** means making an object reference visible to other threads such that the object's fully initialized state is visible simultaneously.
>
> If an object reference escapes its constructor (e.g., passing `this` to an event listener inside constructor), other threads can observe default uninitialized field values (`0` or `null`).
>
> **Techniques for Safe Publication**:
> 1. Initializing in a `static` initializer.
> 2. Storing in a `volatile` or `AtomicReference` field.
> 3. Making all fields `final`.
> 4. Guarding access with a lock."

---

### 17. Lambdas Capturing Mutable State
#### 🎙️ 60-Second Verbal Script
> "Java requires captured local variables in lambdas to be 'effectively final' (the pointer cannot be reassigned).
>
> However, if the lambda captures a reference to a **mutable object** (e.g., an `AtomicInteger`, `List`, or custom POJO) and multiple threads execute that lambda asynchronously (e.g. in `CompletableFuture` or `parallelStream`), concurrent mutations on that captured object introduce race conditions."

---

### 18. `ThreadLocal`: Solving Safety while Causing Leaks
#### 🎙️ 60-Second Verbal Script
> "`ThreadLocal` provides thread-isolation by storing a private variable copy per thread.
>
> **The Thread Pool Leak Problem**:
> In Tomcat/Spring Boot, worker threads are **reused indefinitely** across thousands of HTTP requests.
>
> If a request sets `UserContext.set(user)` and fails to invoke `UserContext.remove()` inside a mandatory `finally` block:
> 1. **Data Leak / Corruption**: The next incoming HTTP request handled by that same worker thread will inherit the previous user's credentials!
> 2. **Memory Leak**: The thread's `ThreadLocalMap` retains strong references to large user objects, preventing garbage collection."

---

### 19. Designing a Read-Heavy Shared Cache
#### 🎙️ 60-Second Verbal Script
> "For caches with 99.9% reads and 0.1% writes:
>
> 1. **`StampedLock` Optimistic Reading**:
>    ```java
>    long stamp = lock.tryOptimisticRead();
>    Data data = this.cachedData;
>    if (!lock.validate(stamp)) { // Check if a write occurred during read
>        stamp = lock.readLock(); // Fallback to full read lock
>        try { data = this.cachedData; } 
>        finally { lock.unlockRead(stamp); }
>    }
>    return data;
>    ```
> 2. Optimistic reads acquire **zero CPU memory bus locks**, maximizing read throughput while guaranteeing consistency upon validation failure."

---

### 20. 🔥 Production Incident RCA: Users Receiving Another User's Data

#### 🎙️ 60-Second Verbal Script
> "When users intermittently receive another user's private data in production, I systematically isolate the 4 most common root causes:
>
> 1. **`ThreadLocal` Pollution in Tomcat Pool (Most Common)**: A security/user context `ThreadLocal` was not cleaned with `.remove()` in a `finally` block. A worker thread picked up a new HTTP request with old user state attached.
> 2. **Shared Mutable State in Spring Singletons**: A developer declared a private instance field (e.g. `private UserDto currentUser;`) on a Spring `@Service` or `@Controller` bean. Since Spring beans are singletons by default, all concurrent requests overwrite each other's field.
> 3. **Improper Cache Keying**: Redis/Caffeine cache key was generated without tenant or user ID namespace (e.g. `@Cacheable(value="profile")` instead of `key="#userId"`).
> 4. **Static ObjectMapper / Parser Pollution**: Misconfigured serialization state or thread-unsafe static date formatters (`SimpleDateFormat`)."
