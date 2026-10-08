# 13. What Are Records and Sealed Classes in Java? Why Were They Introduced?

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "**Records** (introduced in Java 14/16) and **Sealed Classes** (introduced in Java 15/17) are revolutionary features that bring **Data-Oriented Programming, immutability, and algebraic data types** natively to Java.
>
> 1. **Records**: Eliminate boilerplate for transparent, immutable data carriers. In one line, a Record generates `final` fields, a canonical constructor, getters (e.g. `name()`), `equals()`, `hashCode()`, and `toString()`. Unlike Lombok `@Value`, Records are deeply integrated into the JVM bytecode and reflection API, making them ideal for DTOs, API payloads, and Kafka events.
> 2. **Sealed Classes**: Provide **strict control over inheritance hierarchies** by explicitly declaring which classes are permitted to extend or implement them using `sealed`, `permits`, `final`, and `non-sealed` modifiers.
>
> When combined with **Pattern Matching for `switch`** (Java 21), Sealed Classes allow the compiler to perform **exhaustiveness checking**, completely eliminating the need for a fallback `default` case and catching missing business logic branches at compile-time."

---

## 🧠 Comparison: Records vs Standard POJO / Lombok

| Feature | Java Record | Standard POJO | Lombok `@Data`/`@Value` |
|---|---|---|---|
| **Boilerplate** | 1 line of code | 50+ lines (getters, setters, equals, hash) | 1 annotation |
| **Immutability** | Guaranteed immutable by JVM | Optional (mutable by default) | Enforced via `@Value` |
| **Inheritance** | Cannot extend other classes (extends `java.lang.Record`) | Can extend any class | Can extend any class |
| **Reflection & Serialization** | Safe, constructor cannot be bypassed | Prone to bypass via reflection | Standard reflection |

---

## 💻 Working Java Code: Records + Sealed Classes + Pattern Matching

```java
package round1;

import java.math.BigDecimal;

public class RecordsAndSealedDemo {

    // 1. Sealed Interface hierarchy modeling Payment States
    public sealed interface PaymentStatus
            permits PaymentStatus.Pending, PaymentStatus.Success, PaymentStatus.Failed {

        // Records implementing the sealed interface
        record Pending(String transactionId, long timestamp) implements PaymentStatus {}
        
        record Success(String transactionId, BigDecimal amount, String receiptUrl) implements PaymentStatus {}
        
        record Failed(String transactionId, String errorCode, String errorMessage) implements PaymentStatus {}
    }

    // 2. Pattern Matching with Exhaustive Switch (No 'default' needed!)
    public static String handlePaymentStatus(PaymentStatus status) {
        return switch (status) {
            case PaymentStatus.Pending p -> 
                "Payment " + p.transactionId() + " is pending. Initiated at: " + p.timestamp();
            case PaymentStatus.Success s -> 
                "Success! Charged $" + s.amount() + ". Receipt: " + s.receiptUrl();
            case PaymentStatus.Failed f -> 
                "Payment Failed [" + f.errorCode() + "]: " + f.errorMessage();
        };
    }

    public static void main(String[] args) {
        PaymentStatus status1 = new PaymentStatus.Success("TXN-101", new BigDecimal("99.99"), "https://pay.io/r/101");
        PaymentStatus status2 = new PaymentStatus.Failed("TXN-102", "INSUFFICIENT_FUNDS", "Declined by issuing bank");

        System.out.println(handlePaymentStatus(status1));
        System.out.println(handlePaymentStatus(status2));
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "Can a Record have custom constructors or validation?"
**Answer:**
> "Yes! Records support **Compact Constructors** where parameters are validated without reassigning `this.x = x`:
> ```java
> public record Money(BigDecimal amount, String currency) {
>     public Money {
>         if (amount.compareTo(BigDecimal.ZERO) < 0) {
>             throw new IllegalArgumentException("Amount cannot be negative");
>         }
>         currency = Objects.requireNonNull(currency).toUpperCase();
>     }
> }
> ```"

### 2. "Why can't a Record extend another class?"
**Answer:**
> "Because all Records implicitly extend `java.lang.Record`. Since Java does not support multiple class inheritance, a Record cannot extend another class, although it can implement multiple interfaces."
