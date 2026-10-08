# 04. GoF Design Patterns & Enterprise Microservices Architecture Patterns

Deep-dive explanations, 60-second verbal scripts, thread-safe implementations, and architectural breakdowns.

---

## 📑 Topics Index
1. [Factory Pattern vs Abstract Factory Pattern](#1-factory-pattern-vs-abstract-factory-pattern)
2. [Builder Pattern & Immutability](#2-builder-pattern--immutability)
3. [Thread-Safe Singleton Implementations & Pitfalls](#3-thread-safe-singleton-implementations--pitfalls)
4. [Strategy Pattern vs Template Method Pattern](#4-strategy-pattern-vs-template-method-pattern)
5. [Observer Pattern in Event-Driven Systems](#5-observer-pattern-in-event-driven-systems)
6. [Adapter Pattern vs Decorator Pattern vs Proxy Pattern](#6-adapter-pattern-vs-decorator-pattern-vs-proxy-pattern)
7. [Dependency Injection & IoC Pattern Mechanics](#7-dependency-injection--ioc-pattern-mechanics)
8. [Design Patterns Heavily Used in Spring Framework](#8-design-patterns-heavily-used-in-spring-framework)
9. [Crucial Design Patterns in Microservices Architecture (Saga, Outbox, CQRS)](#9-crucial-design-patterns-in-microservices-architecture)

---

### 1. Factory Pattern vs Abstract Factory Pattern
#### 🎙️ 60-Second Verbal Script
> "Both are Creational patterns that decouple object creation from client code:
> - **Factory Method Pattern**: Defines an interface for creating a **single product**, allowing subclasses to decide which concrete class to instantiate (e.g. `NotificationFactory` returning `EmailNotification` or `SmsNotification`).
> - **Abstract Factory Pattern**: Provides an interface for creating **families of related or dependent products** without specifying their concrete classes (e.g., `MacUIFactory` creating `MacButton` and `MacCheckbox`, versus `WindowsUIFactory` creating `WindowsButton` and `WindowsCheckbox`)."

---

### 2. Builder Pattern & Immutability
#### 🎙️ 60-Second Verbal Script
> "The **Builder Pattern** separates the step-by-step construction of a complex object from its representation, eliminating the **Telescoping Constructor Anti-Pattern** (constructors with 10+ parameters where many are optional or null).
>
> It is the premier pattern for creating **immutable objects** because:
> 1. Parameters are gathered and validated in a mutable Builder object first.
> 2. The final `.build()` method invokes a private constructor on the target class, populating strictly `final` fields without ever exposing public setters, guaranteeing **thread safety and immutability**."

---

### 3. Thread-Safe Singleton Implementations & Pitfalls
#### 🎙️ 60-Second Verbal Script
> "A **Singleton** guarantees a class has only one instance and provides a global access point.
>
> In Java, there are two production-grade thread-safe approaches:
> 1. **Bill Pugh Static Inner Helper Class**: Leverages the JVM class loader mechanism. The instance is created only when the inner class is referenced, providing lazy initialization with zero synchronization overhead.
> 2. **Double-Checked Locking with `volatile`**: Checks for null twice, synchronizing only on the first instance creation. The `volatile` keyword is mandatory to prevent JVM CPU instruction reordering.
> 3. **Enum Singleton (Joshua Bloch)**: The most bulletproof approach because it handles serialization and reflection attacks automatically.
>
> **Drawbacks of Singleton**: Global mutable state, tight coupling, difficult to mock in unit tests, and violates the Single Responsibility Principle."

```java
// 1. Bill Pugh Singleton (Recommended for Classes)
public class BillPughSingleton {
    private BillPughSingleton() {}

    private static class SingletonHelper {
        private static final BillPughSingleton INSTANCE = new BillPughSingleton();
    }

    public static BillPughSingleton getInstance() {
        return SingletonHelper.INSTANCE;
    }
}

// 2. Double-Checked Locking with volatile
public class DoubleCheckedSingleton {
    private static volatile DoubleCheckedSingleton instance;

    private DoubleCheckedSingleton() {}

    public static DoubleCheckedSingleton getInstance() {
        if (instance == null) {
            synchronized (DoubleCheckedSingleton.class) {
                if (instance == null) {
                    instance = new DoubleCheckedSingleton();
                }
            }
        }
        return instance;
    }
}
```

---

### 4. Strategy Pattern vs Template Method Pattern
#### 🎙️ 60-Second Verbal Script
> "| Feature | Strategy Pattern | Template Method Pattern |
> |---|---|---|
> | **Paradigm** | **Composition** (Has-A) | **Inheritance** (Is-A) |
> | **Mechanism** | Object encapsulates an algorithm behind an interface | Abstract base class defines skeleton algorithm, sub-classes override steps |
> | **Flexibility** | Can swap algorithms dynamically at runtime | Fixed skeleton compiled at build-time |
> | **Spring Example** | `Map<PaymentType, PaymentStrategy>` | `JdbcTemplate`, `TransactionTemplate` |"

---

### 5. Observer Pattern in Event-Driven Systems
#### 🎙️ 60-Second Verbal Script
> "The **Observer Pattern** defines a one-to-many dependency where a subject notifies all registered observers automatically upon state change.
>
> In modern event-driven architectures:
> 1. **In-Memory**: Spring's `ApplicationEventPublisher` publishes events, and `@EventListener` / `@TransactionalEventListener` methods react asynchronously.
> 2. **Distributed**: **Apache Kafka / AWS SNS-SQS** act as the distributed event broker (Subject), and microservices consumer groups act as Observers processing domain events asynchronously."

---

### 6. Adapter Pattern vs Decorator Pattern vs Proxy Pattern
#### 🎙️ 60-Second Verbal Script
> "- **Adapter Pattern**: Converts an **incompatible interface** into one that the client expects (e.g. wrapping a legacy 3rd-party XML payment gateway to fit your modern JSON `PaymentGateway` interface).
> - **Decorator Pattern**: Dynamically **adds new behavior/responsibilities** to an object without altering its interface (e.g. `BufferedInputStream` wrapping `FileInputStream`).
> - **Proxy Pattern**: **Controls and manages access** to the original object without altering its interface (e.g., Spring AOP `@Transactional` proxy managing database transactions, lazy-loading Hibernate proxies, or security auth checks)."

---

### 7. Dependency Injection & IoC Pattern Mechanics
#### 🎙️ 60-Second Verbal Script
> "**Inversion of Control (IoC)** is a broad architectural principle where control of object creation, configuration, and lifecycle is inverted from the program code to an external container.
>
> **Dependency Injection (DI)** is the specific design pattern used to implement IoC. Instead of a class calling `new OrderRepository()`, dependencies are provided ('injected') from the outside (via Constructor, Setter, or Field injection). This decouples components and makes unit testing trivial with mock frameworks."

---

### 8. Design Patterns Heavily Used in Spring Framework
#### 🎙️ 60-Second Verbal Script
> "Spring Framework is a masterclass in GoF design patterns:
> 1. **Singleton Pattern**: Default Spring bean scope (`@Component`).
> 2. **Factory Pattern**: `BeanFactory` and `FactoryBean`.
> 3. **Proxy Pattern**: Spring AOP, `@Transactional`, and `@Cacheable` proxies (JDK dynamic / CGLIB).
> 4. **Template Method Pattern**: `JdbcTemplate`, `RestTemplate`, `JmsTemplate`.
> 5. **Front Controller Pattern**: Spring MVC's `DispatcherServlet`.
> 6. **Observer Pattern**: `ApplicationEventPublisher` and `@EventListener`.
> 7. **Strategy Pattern**: `ResourceLoader` resolving `classpath:`, `file:`, or `http:` resources."

---

### 9. Crucial Design Patterns in Microservices Architecture
#### 🎙️ 60-Second Verbal Script
> "Enterprise microservices rely on battle-tested distributed system patterns:
>
> 1. **Transactional Outbox Pattern**: Solves the dual-write problem. A service saves domain data AND an outbox event in the same local database ACID transaction. A Debezium CDC (Change Data Capture) connector or Polling Publisher reads the outbox table and publishes to Kafka, guaranteeing **at-least-once message delivery**.
> 2. **Saga Pattern**: Manages distributed multi-service transactions without 2-Phase Commit (2PC) using either:
>    - **Choreography**: Event-driven where each service listens to Kafka events and executes compensating actions on failure.
>    - **Orchestration**: A central Saga Orchestrator (e.g. Temporal or Camunda) sends commands to participants.
> 3. **Circuit Breaker Pattern (Resilience4j)**: Prevents cascading failure across microservices when a downstream dependency is failing.
> 4. **CQRS (Command Query Responsibility Segregation)**: Separates write models (Commands) from read models (Queries) for high read scalability."
