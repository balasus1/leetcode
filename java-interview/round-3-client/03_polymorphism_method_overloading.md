# 03. Explain Polymorphism and Method Overloading with a Real Project Example

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "**Polymorphism** means 'many forms' and allows objects to behave differently based on their context. In Java, it manifests in two forms:
>
> 1. **Compile-Time Polymorphism (Static Binding / Method Overloading)**: Multiple methods in the same class share the same name but have **different parameter lists** (number, type, or order of arguments). The compiler decides which method to invoke at build time.
> 2. **Runtime Polymorphism (Dynamic Binding / Method Overriding)**: A subclass provides a specific implementation of a method defined in its parent interface/class. The JVM resolves the method at runtime based on the actual heap object instance.
>
> In our real-world **Enterprise Notification Engine**:
> - We use **Method Overloading** in our `NotificationService` to provide convenient developer APIs: `sendNotification(userId, message)`, `sendNotification(userId, message, priority)`, and `sendNotification(userId, message, attachmentFile, priority)`.
> - We use **Method Overriding / Dynamic Polymorphism** across our channel providers (`EmailNotificationSender`, `SmsNotificationSender`, `PushNotificationSender`), each implementing the `NotificationSender` interface. At runtime, the routing engine dynamically delegates to the appropriate sender without hardcoding channel-specific logic."

---

## 🧠 Compile-Time vs Runtime Polymorphism

| Feature | Method Overloading (Compile-Time) | Method Overriding (Runtime) |
|---|---|---|
| **Mechanism** | Multiple methods with same name, different signatures | Subclass provides specific implementation |
| **Location** | Within the **same class** | Across **Parent and Child classes** / Interfaces |
| **Binding** | Early / Static binding by compiler | Late / Dynamic binding by JVM (`vtable`) |
| **Return Type** | Can be different (signature must differ) | Must be identical or **Covariant return type** |
| **Performance** | Zero runtime overhead | Tiny dynamic dispatch overhead |

---

## 💻 Clean Production Project Example: Notification Engine

```java
package clientround;

import java.io.File;

// 1. Runtime Polymorphism: Common Contract
public interface NotificationSender {
    void send(String recipient, String message);
}

// Concrete Implementations overriding send()
class EmailNotificationSender implements NotificationSender {
    @Override
    public void send(String recipient, String message) {
        System.out.println("📧 [Email Sent to " + recipient + "]: " + message);
    }
}

class SmsNotificationSender implements NotificationSender {
    @Override
    public void send(String recipient, String message) {
        System.out.println("📱 [SMS Sent to " + recipient + "]: " + message);
    }
}

// 2. Compile-Time Polymorphism: Method Overloading
public class NotificationManager {

    private final NotificationSender primarySender;

    public NotificationManager(NotificationSender primarySender) {
        this.primarySender = primarySender;
    }

    // Overload 1: Basic text message
    public void sendNotification(String recipient, String message) {
        primarySender.send(recipient, message);
    }

    // Overload 2: Text message with urgency flag
    public void sendNotification(String recipient, String message, boolean isUrgent) {
        String formattedMessage = isUrgent ? "[URGENT] " + message : message;
        primarySender.send(recipient, formattedMessage);
    }

    // Overload 3: Rich message with attachment
    public void sendNotification(String recipient, String message, File attachment, boolean isUrgent) {
        String fullPayload = String.format("%s (Attachment: %s)", message, attachment.getName());
        sendNotification(recipient, fullPayload, isUrgent); // Delegates to Overload 2
    }

    public static void main(String[] args) {
        // Runtime polymorphism in action: swapping email for SMS seamlessly
        NotificationSender emailSender = new EmailNotificationSender();
        NotificationSender smsSender = new SmsNotificationSender();

        NotificationManager emailManager = new NotificationManager(emailSender);
        emailManager.sendNotification("john@acme.com", "Your invoice is ready");
        emailManager.sendNotification("john@acme.com", "Server Outage Alert", true);

        NotificationManager smsManager = new NotificationManager(smsSender);
        smsManager.sendNotification("+1555123456", "Your OTP is 849201", true);
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "Can you overload a method by changing ONLY the return type?"
**Answer:**
> "No! In Java, the method signature consists of the **method name and parameter list**. Changing only the return type causes a **Compile-Time Error** because the compiler cannot determine which method to invoke during an expression like `obj.process()` where the return value is discarded."

### 2. "What is Covariant Return Type in method overriding?"
**Answer:**
> "In Java 5+, an overriding method in a subclass is allowed to return a **more specific subtype** of the return type declared in the parent method. For example, if parent returns `Number`, child can override and return `Integer` or `Double`."
