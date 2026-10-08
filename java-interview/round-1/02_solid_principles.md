# 02. Explain SOLID Principles with Examples

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "SOLID represents five fundamental object-oriented design principles that create maintainable, decoupled, and testable enterprise architectures:
>
> 1. **S - Single Responsibility Principle**: A class should have only one reason to change. For example, separating an `OrderService` (business logic) from an `InvoiceGenerator` (PDF rendering) and `OrderNotificationSender` (email/SMS).
> 2. **O - Open/Closed Principle**: Software entities should be open for extension but closed for modification. In Spring, we achieve this using interfaces and polymorphism — for instance, adding a new `CryptoPaymentProcessor` without modifying the core `PaymentEngine`.
> 3. **L - Liskov Substitution Principle**: Subtypes must be substitutable for their base types without breaking application behavior. Subclasses shouldn't throw `UnsupportedOperationException` for inherited methods (the classic Rectangle-Square violation).
> 4. **I - Interface Segregation Principle**: Clients shouldn't be forced to depend on methods they don't use. Prefer small, role-specific interfaces (like `ReadableStream`, `WritableStream`) over bloated 'God' interfaces.
> 5. **D - Dependency Inversion Principle**: High-level modules should not depend on low-level modules; both should depend on abstractions. This is the bedrock of Spring's **Inversion of Control (IoC) and Dependency Injection (DI)**, where controllers inject `UserService` interfaces rather than instantiating concrete database DAOs."

---

## 🧠 Key Technical Bullets
| Principle | Core Meaning | Anti-Pattern | Spring / Java Best Practice |
|---|---|---|---|
| **SRP** | Single reason to change | God Service doing validation, DB, email, PDF | Separate `@Service` for domain, `@Repository` for DB, notification client |
| **OCP** | Open for extension, closed for modification | Giant `switch-case` / `if-else` checking payment types | Strategy Pattern with Spring autowired `Map<String, PaymentStrategy>` |
| **LSP** | Behavioral subtyping | Child class throwing `UnsupportedOperationException` | Composition over Inheritance; contract fulfillment |
| **ISP** | Lean interfaces | Giant interface with 20 methods where implementations stub 15 | Fine-grained interfaces (e.g. `UserReader`, `UserWriter`) |
| **DIP** | Abstraction over Concretion | `new MySQLUserRepository()` inside business service | Inject `UserRepository` interface via constructor injection |

---

## 💻 Clean Production-Grade Code Examples

### 1. SRP & DIP in Action:
```java
// DIP: High-level service depends on abstraction (UserRepository, NotificationService)
public interface UserRepository {
    User findById(String id);
}

public interface NotificationService {
    void sendNotification(User user, String message);
}

// SRP: OrderService handles ONLY order domain logic
@Service
public class OrderService {
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // Constructor Injection (Spring DIP best practice)
    public OrderService(UserRepository userRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public void processOrder(String userId, double amount) {
        User user = userRepository.findById(userId);
        // Domain logic...
        notificationService.sendNotification(user, "Your order of $" + amount + " is confirmed.");
    }
}
```

### 2. OCP & ISP in Action (Extensible Discount Engine):
```java
// ISP: Lean, dedicated contract
public interface DiscountStrategy {
    boolean isApplicable(Customer customer);
    BigDecimal applyDiscount(BigDecimal totalAmount);
}

// OCP: Adding new VIP discount requires ZERO changes to existing classes
@Component
public class VipDiscountStrategy implements DiscountStrategy {
    public boolean isApplicable(Customer customer) { return customer.isVip(); }
    public BigDecimal applyDiscount(BigDecimal totalAmount) {
        return totalAmount.multiply(BigDecimal.valueOf(0.80)); // 20% off
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "How does Spring Framework specifically enable the Dependency Inversion Principle?"
**Answer:**
> "Spring's IoC container reverses control: instead of an object instantiating its dependencies via `new`, the container manages the lifecycle and injects them via constructor injection. This decouples classes from concrete implementations and allows hot-swapping mocks in unit tests."

### 2. "How do you detect a violation of Liskov Substitution Principle during code review?"
**Answer:**
> "Look for:
> 1. Derived classes overriding parent methods with empty bodies or throwing `UnsupportedOperationException`.
> 2. Code containing `if (object instanceof DerivedClass)` type-checking ladders before calling a method."
