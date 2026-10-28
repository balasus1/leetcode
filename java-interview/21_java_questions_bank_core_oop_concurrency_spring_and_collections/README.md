# 🎯 Module 21: Java Full-Stack & Backend Master Questions Bank

> **Complete 56-Question Technical Interview Playbook**  
> Covers Java Core, OOP, Exceptions, Modern Language Features, Concurrency (5-thread sequence, Volatile vs Atomic), Design Patterns, Spring & Spring Boot, Collections Framework, Streams API, and Functional Programming.  
> 
> 🔗 **Direct Full Playbook Link**:  
> [👉 View the Complete 56-Question Master Guide (`ALL_JAVA_QUESTIONS_BANK_MASTER.md`)](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/ALL_JAVA_QUESTIONS_BANK_MASTER.md)

---

## 📑 Summary of Covered Domains & 56 Questions

### ☕ 1. Java Core
1. **Platform Independence (JVM vs JRE vs JDK)**: Bytecode WORA mechanics, Tiered Compilation (C1/C2 JIT).
2. **ClassLoader Delegation**: Bootstrap, Platform, Application, Custom ClassLoaders & delegation bypass (Tomcat).
3. **ClassNotFoundException vs NoClassDefFoundError**: Dynamic reflection failure vs runtime missing class or `<clinit>` failure.
4. **String Pool, intern(), == vs equals()**: SCP memory layout in Heap, `.intern()` canonical pointers, reference vs value equality.

### 🧱 2. OOP Concepts
5. **Encapsulation**: State-behavior bundling, access modifiers, invariant protection.
6. **Serializable Interface**: Marker interface, `serialVersionUID` evolution, `transient` sensitive field masking.
7. **Static, Final, or Private Classes**: Outer class rules (public/package-private, final) vs static nested & private inner classes.
8. **OOP vs Scripting Languages**: Static compile-time verification vs dynamic runtime duck typing, concurrency scaling.

### 🛑 3. Exceptions & Error Handling
9. **Throwable Hierarchy**: `java.lang.Throwable` root, Error vs Exception branches.
10. **Error vs Exception**: Unrecoverable JVM faults (`OutOfMemoryError`) vs recoverable application errors (`IOException`).
11. **Checked vs Unchecked Exceptions**: Compile-time check (`Exception`) vs runtime bugs (`RuntimeException`), framework conventions.
12. **Try-with-Resources**: `AutoCloseable`, automatic deterministic cleanup, suppressed exception tracking (`getSuppressed()`).
13. **Exception Propagation**: Call stack frame popping, uncaught exception handlers.

### ⚙️ 4. Java Language Features
14. **Autoboxing & Unboxing**: `Integer.valueOf()` / `intValue()`, the Integer Cache trap (`-128` to `127`), unboxing NPEs.
15. **Key Java 8 Features**: Lambdas, Streams, Functional Interfaces, Default/Static methods, `Optional`, `java.time`.
16. **Lambda Functions & invokedynamic**: Anonymous functions, JSR 292 `invokedynamic` (Indy) eliminating classloader bloat.
17. **Why Interfaces Cannot Have Constructors**: Behavioral contracts, absence of instance state, multiple inheritance diamond prevention.
18. **For-Loop vs Enhanced For-Each Performance**: Array JIT bounds elimination, `ArrayList` Iterator allocation, `LinkedList` $O(n^2)$ trap.

### 🧵 5. Concurrency & Multithreading
19. **5 Threads Printing in Sequence (1 to N)**: `ReentrantLock` + array of 5 `Condition` variables, fair state-machine handoff.
20. **Volatile vs Atomic**: JMM visibility and memory barriers vs CPU hardware Compare-And-Swap (CAS) lock-free atomicity.

### 🎨 6. Design Patterns
21. **Builder Pattern**: Telescoping constructor elimination, parameter validation, thread-safe immutable objects.
22. **Singleton Pattern**: Bill Pugh (static holder), Double-Checked Locking with `volatile`, Enum Singleton (attack-immune).
23. **Factory Pattern**: Loose coupling, Open/Closed principle, dynamic runtime instantiation.
24. **Why Design Patterns Are Needed**: Shared vocabulary, SOLID adherence, proven architectural resilience.
25. **Known Patterns & How to Invent a Pattern**: Categorization (GoF + Distributed) and empirical pattern extraction methodology.

### 🍃 7. Spring & Spring Boot
26. **What is Spring Boot**: Starters, Auto-Configuration, Embedded Tomcat, Actuator production readiness.
27. **Core Annotations**: `@Component`, `@Bean`, `@Qualifier`, `@Value` configuration mechanics.
28. **@Controller vs @RestController**: HTML view resolution vs `@ResponseBody` Jackson JSON serialization.
29. **@Mock vs @InjectMocks**: Mockito synthetic dummy mocks vs real instance dependency injection.
30. **OAuth 2.0 & RBAC**: Auth Code flow + PKCE, JWT claims (`roles`), Spring Security `@PreAuthorize`.
31. **Spring Data JPA Interfaces**: `Repository` $\rightarrow$ `CrudRepository` $\rightarrow$ `PagingAndSortingRepository` $\rightarrow$ `JpaRepository`.
32. **Controller Exception Handling**: `@RestControllerAdvice`, `@ExceptionHandler`, RFC 7807 `ProblemDetail`.
33. **Transaction Propagation**: `REQUIRED` (default) vs `REQUIRES_NEW` (independent) vs `NESTED` (JDBC savepoints).
34. **application.properties**: External configuration, profiles, Spring Boot precedence order.
35. **SQL Generation**: Query method AST $\rightarrow$ JPQL $\rightarrow$ Hibernate Dialect $\rightarrow$ JDBC PreparedStatement.
36. **MVC Architecture**: Strict separation of Controller (transport/validation), Service (business/transactions), Repository (persistence).
37. **Pagination & Constraints**: `Page<T>` (with `COUNT(*)`) vs `Slice<T>` (infinite scroll), `DataIntegrityViolationException` mapping.

### 📦 8. Java Collections Framework
38. **HashMap Internals**: Bucket array, bitwise hash distribution `(h ^ (h >>> 16))`, linked list $\rightarrow$ Red-Black treeification (threshold 8).
39. **HashMap vs Hashtable vs ConcurrentHashMap**: Unsynchronized vs method-level lock bottleneck vs CAS + bucket-level `synchronized`.
40. **List vs Set vs Map**: Ordered indexed sequences vs unique membership vs key-value indexing.
41. **LinkedList**: Doubly-linked node overhead (40 bytes/node), CPU cache miss penalties, $O(n)$ random access.
42. **Comparable vs Comparator**: Natural internal ordering (`compareTo`) vs external dynamic strategies (`compare`).
43. **hashCode and equals Contract**: Consistency, hash collision mitigation, Joshua Bloch 31 multiplier.
44. **Iterable vs Iterator**: Collection traversal eligibility (`for-each`) vs stateful iteration cursor.

### 🌊 9. Streams API
45. **What Streams API is**: Lazy, declarative data processing pipeline; non-storing, single-consumption.
46. **Ways to Create Streams**: Collections, arrays, static values, infinite generators, primitive ranges, I/O lines.
47. **Streams vs Collections**: Eager storage & external iteration vs lazy computation & internal iteration.
48. **Intermediate vs Terminal Operations**: Lazy pipeline builders (`filter`, `map`) vs pipeline executors (`collect`, `count`).
49. **map vs flatMap**: 1-to-1 transformation vs 1-to-many sub-stream flattening.
50. **collect, findFirst, findAny**: Reduction to collections, deterministic vs non-deterministic short-circuiting.
51. **Checked Exceptions in Streams**: Functional interface limitations, generic unchecked wrappers.
52. **Converting Streams to Collections**: `toList()`, `toSet()`, `toMap()` with collision merge rules.
53. **Sequential vs Parallel Streams**: $N \times Q > 10,000$ rule, CPU-bound workloads, avoiding blocking I/O thread pool starvation.

### 🎯 10. Functional Programming
54. **Functional Interface**: Single Abstract Method (SAM) contract, `@FunctionalInterface` compiler check.
55. **Default & Static Methods in Functional Interfaces**: Why default, static, and Object overrides are permitted.
56. **Standard Functional Interfaces**: `Runnable`, `Callable`, `Comparator`, `Function`, `Consumer`, `Supplier`, `Predicate`.

---

🔗 **Open the complete playbook with full code snippets and visual diagrams here**:  
👉 **[ALL_JAVA_QUESTIONS_BANK_MASTER.md](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/ALL_JAVA_QUESTIONS_BANK_MASTER.md)**
