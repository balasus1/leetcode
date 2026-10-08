# 03. Explain Strategy Design Pattern with Real-World Project Example

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "The **Strategy Design Pattern** is a behavioral design pattern that defines a family of interchangeable algorithms, encapsulates each one into a separate class, and allows the algorithm to be selected dynamically at runtime without modifying the client code.
>
> In our e-commerce platform, we used the Strategy Pattern for our **Payment Processing Engine** supporting Stripe, PayPal, and Apple Pay.
>
> Instead of a messy `switch-case` or long `if-else` ladder inside the checkout service, we defined a `PaymentStrategy` interface with a `processPayment(PaymentRequest req)` method and a `supports(PaymentType type)` identifier. We implemented concrete classes for each gateway annotated with Spring's `@Component`.
>
> Spring automatically injects all strategies into a `List<PaymentStrategy>` or `Map<String, PaymentStrategy>` inside our `PaymentService`. At runtime, when an order arrives with `paymentType = 'STRIPE'`, the service queries the map in $O(1)$ time and delegates execution. If tomorrow we add Crypto payment, we just create a new strategy class with zero edits to existing classes, adhering strictly to the **Open/Closed Principle**."

---

## 🧠 Key Technical Bullets
- **Pattern Type**: Behavioral.
- **Problem it solves**: Eliminates conditional spaghetti code (`if-else` / `switch`) when selecting algorithms.
- **Spring Integration**: Leverages Spring's ability to inject collections (`List<Strategy>` or `Map<String, Strategy>`).
- **Benefits**: High testability (each strategy unit tested in isolation), high extensibility, zero regression risk when onboarding new payment channels.

---

## 💻 Production-Grade Spring Boot Implementation

```java
package com.example.payment.strategy;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 1. Common Strategy Interface
public interface PaymentStrategy {
    PaymentMethod getPaymentMethod();
    PaymentResponse processPayment(BigDecimal amount, String currency, String accountDetails);
}

public enum PaymentMethod {
    CREDIT_CARD, PAYPAL, APPLE_PAY
}

public record PaymentResponse(String transactionId, boolean success, String message) {}

// 2. Concrete Strategy 1: Stripe Credit Card
@Component
public class CreditCardPaymentStrategy implements PaymentStrategy {
    @Override
    public PaymentMethod getPaymentMethod() { return PaymentMethod.CREDIT_CARD; }

    @Override
    public PaymentResponse processPayment(BigDecimal amount, String currency, String accountDetails) {
        // Integrate with Stripe SDK
        return new PaymentResponse("TXN_CC_98765", true, "Charged $" + amount + " via CreditCard");
    }
}

// 3. Concrete Strategy 2: PayPal
@Component
public class PayPalPaymentStrategy implements PaymentStrategy {
    @Override
    public PaymentMethod getPaymentMethod() { return PaymentMethod.PAYPAL; }

    @Override
    public PaymentResponse processPayment(BigDecimal amount, String currency, String accountDetails) {
        // Integrate with PayPal REST API
        return new PaymentResponse("TXN_PP_12345", true, "Charged $" + amount + " via PayPal");
    }
}

// 4. Context Service: Spring Boot Auto-Wiring Strategy Registry
@Service
public class PaymentContextService {

    private final Map<PaymentMethod, PaymentStrategy> strategyRegistry;

    // Spring automatically injects all beans implementing PaymentStrategy
    public PaymentContextService(List<PaymentStrategy> strategies) {
        this.strategyRegistry = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        PaymentStrategy::getPaymentMethod,
                        Function.identity()
                ));
    }

    public PaymentResponse executePayment(PaymentMethod method, BigDecimal amount, String currency, String details) {
        PaymentStrategy strategy = strategyRegistry.get(method);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + method);
        }
        return strategy.processPayment(amount, currency, details);
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "How is Strategy Pattern different from Factory Pattern?"
**Answer:**
> "The **Factory Pattern** is a *Creational* pattern focused on *how objects are instantiated* without exposing creation logic. The **Strategy Pattern** is a *Behavioral* pattern focused on *how objects execute different algorithms or behaviors* interchangeably at runtime. Often, a Factory is used under the hood to return a Strategy instance."

### 2. "How would you handle fallback strategies if a primary payment provider goes down?"
**Answer:**
> "We combine the Strategy pattern with the **Chain of Responsibility** or **Circuit Breaker (Resilience4j)**. If `CreditCardPaymentStrategy` throws a `ProviderUnavailableException`, the context can automatically invoke a secondary fallback strategy."
