# 03. final vs finally vs finalize

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "Despite their similar names, **`final`**, **`finally`**, and **`finalize`** serve completely distinct purposes in Java:
>
> 1. **`final` is a keyword/modifier**:
>    - On a **variable**: creates a constant reference whose value or memory pointer cannot be reassigned.
>    - On a **method**: prevents method overriding by subclasses.
>    - On a **class**: prevents inheritance (e.g., `java.lang.String` or `Integer`).
> 2. **`finally` is a block in exception handling**: It guarantees execution of cleanup code (like closing streams or releasing locks) regardless of whether an exception was thrown or caught in the `try-catch` block. The only scenarios where `finally` will not execute are if `System.exit(0)` is invoked or the JVM crashes with an unrecoverable `Error` (e.g. `OutOfMemoryError` or power cut).
> 3. **`finalize()` is a deprecated method of `java.lang.Object`**: Historically invoked by the Garbage Collector before reclaiming an object's memory. It was deprecated in Java 9 and marked for removal in Java 18 because it causes unpredictable latency, resurrection bugs, and memory leaks. In modern Java, we replace it with **`AutoCloseable` + Try-With-Resources** or the `java.lang.ref.Cleaner` API."

---

## 🧠 Comparison Matrix

| Feature | `final` | `finally` | `finalize()` |
|---|---|---|---|
| **Type** | Access Modifier / Keyword | Control Flow Block | Method in `java.lang.Object` |
| **Applies to** | Variables, Methods, Classes | `try-catch` blocks | Objects / Garbage Collection |
| **Purpose** | Immutability & inheritance control | Resource cleanup & guaranteed execution | Pre-garbage collection cleanup hook |
| **Status in Modern Java**| Actively used everywhere | Actively used (along with Try-with-resources) | **Deprecated in Java 9, for removal** |
| **Alternative** | Sealed Classes / Records | Try-with-resources (`AutoCloseable`) | `java.lang.ref.Cleaner` API |

---

## 💻 Code Demonstration & Edge Cases

```java
package round2;

public class FinalFinallyFinalizeDemo {

    public static final int CONSTANT_MAX_CONNECTIONS = 100;

    public static int testFinallyBehavior() {
        try {
            System.out.println("1. Entering try block");
            throw new RuntimeException("Simulated error");
        } catch (Exception e) {
            System.out.println("2. Entering catch block");
            return 10; // Value is prepared for return
        } finally {
            System.out.println("3. Finally block ALWAYS executes!");
            // TIP: Never return from finally, it overrides the try/catch return value!
        }
    }

    // Modern Replacement for finalize(): AutoCloseable with Try-with-Resources
    public static class SafeDatabaseConnection implements AutoCloseable {
        public void executeQuery() {
            System.out.println("Executing SQL Query...");
        }

        @Override
        public void close() {
            System.out.println("Safely closed DB connection via AutoCloseable!");
        }
    }

    public static void main(String[] args) {
        int result = testFinallyBehavior();
        System.out.println("Returned result: " + result);

        System.out.println("\n--- Modern Resource Management ---");
        try (SafeDatabaseConnection conn = new SafeDatabaseConnection()) {
            conn.executeQuery();
        } // Auto-closes here automatically!
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "Does `finally` execute if `try` block has a `return` statement?"
**Answer:**
> "Yes! The `return` statement in the `try` or `catch` block evaluates its expression and holds the return value, then the `finally` block executes *before* the method actually returns to the caller."

### 2. "Why was `finalize()` deprecated?"
**Answer:**
> "Because:
> 1. There is no guarantee *when* or *if* `finalize()` will be executed by GC.
> 2. Slow finalizers block the JVM Finalizer thread, causing catastrophic `OutOfMemoryError`s.
> 3. An object can resurrect itself inside `finalize()` by assigning `this` to a static reference, confusing the GC."
