# 15. Spring Bean Scopes, Java Return Mechanics & Custom File Handling

Deep-dive answers, 60-second verbal scripts, memory management, and code examples for Senior Backend Engineers.

---

## 📑 Topics Index
1. [All 6 Spring Bean Scopes](#1-all-6-spring-bean-scopes)
2. [Prototype Scope & The "Prototype-in-Singleton" Injection Trap](#2-prototype-scope--the-prototype-in-singleton-injection-trap)
3. [Java 8: Calculating Frequency of All Elements in a List](#3-java-8-calculating-frequency-of-all-elements-in-a-list)
4. [Mechanics of the `return` Statement in Java](#4-mechanics-of-the-return-statement-in-java)
5. [Custom File Handling vs Normal/Static File Serving](#5-custom-file-handling-vs-normalstatic-file-serving)

---

### 1. All 6 Spring Bean Scopes

#### 🎙️ 60-Second Verbal Script
> "Spring Framework provides 6 bean scopes (2 core and 4 web-aware):
>
> 1. **`singleton` (Default)**: Exactly one shared bean instance per Spring `ApplicationContext`. All autowired dependencies receive the same instance.
> 2. **`prototype`**: Creates a **brand-new bean instance every time** it is requested from the container via injection or `getBean()`.
> 3. **`request` (Web-aware)**: A new bean instance created for each individual HTTP request lifecycle.
> 4. **`session` (Web-aware)**: A single bean instance per HTTP HttpSession lifecycle.
> 5. **`application` (Web-aware)**: Scoped to the lifecycle of a `ServletContext` (shared across multiple servlet applications).
> 6. **`websocket` (Web-aware)**: Scoped to the lifecycle of a single WebSocket session."

---

### 2. Prototype Scope & The "Prototype-in-Singleton" Injection Trap

#### 🎙️ 60-Second Verbal Script
> "While a **Prototype** bean creates a new instance on every request, injecting a `@Scope("prototype")` bean directly into a `@Scope("singleton")` bean creates a major **injection trap**:
>
> - Because the Singleton bean is instantiated only once during startup, dependency injection occurs **only once**. The Singleton will hold onto that single prototype instance forever, effectively treating it as a Singleton!
>
> **How to Fix This Properly**:
> 1. **`ObjectProvider<T>` (Recommended)**: Inject `ObjectProvider<MyPrototypeBean>` and call `provider.getObject()` on demand.
> 2. **`@Lookup` Method Injection**: Annotate a getter method with `@Lookup`. Spring CGLIB overrides the method dynamically to fetch a fresh prototype instance from the container on each call."

```java
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReportTask {
    public void execute(String taskId) {
        System.out.println("Executing Task " + taskId + " on instance: " + this.hashCode());
    }
}

// 1. Solution via ObjectProvider
@Service
public class ReportSchedulerService {

    private final ObjectProvider<ReportTask> reportTaskProvider;

    public ReportSchedulerService(ObjectProvider<ReportTask> reportTaskProvider) {
        this.reportTaskProvider = reportTaskProvider;
    }

    public void triggerNewReport(String id) {
        ReportTask task = reportTaskProvider.getObject(); // Fresh prototype instance every time!
        task.execute(id);
    }
}

// 2. Solution via @Lookup Method Injection
@Service
public abstract class ReportLookupService {

    @Lookup
    public abstract ReportTask getReportTask(); // Spring CGLIB overrides this dynamically

    public void process(String id) {
        ReportTask task = getReportTask(); // Fresh instance fetched from container!
        task.execute(id);
    }
}
```

---

### 3. Java 8: Calculating Frequency of All Elements in a List

#### 🎙️ 60-Second Verbal Script
> "To calculate the frequency of all elements in a list using Java 8 Streams, we stream the list and collect it using `Collectors.groupingBy(Function.identity(), Collectors.counting())`.
>
> This traverses the list in a single $O(N)$ pass, producing a `Map<T, Long>` where the key is the element and the value is its occurrence count."

```java
package round1;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ElementFrequencyDemo {
    public static void main(String[] args) {
        List<String> items = List.of("apple", "banana", "apple", "orange", "banana", "apple");

        Map<String, Long> frequencyMap = items.stream()
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));

        frequencyMap.forEach((item, count) -> 
            System.out.println(item + " -> " + count)
        );
        // Output:
        // orange -> 1
        // banana -> 2
        // apple -> 3
    }
}
```

---

### 4. Mechanics of the `return` Statement in Java

#### 🎙️ 60-Second Verbal Script
> "In Java, the **`return` statement** has two fundamental purposes:
> 1. It immediately completes the execution of the current method and transfers control flow back to the caller.
> 2. In non-void methods, it supplies the evaluated value matching the declared method return type (`return value;`).
> 3. In `void` methods, `return;` is used for **Guard Clauses / Early Exits** to prevent deep nested `if-else` branching.
>
> **The `finally` Override Trap**:
> If a `try` or `catch` block executes a `return` statement, the value is evaluated and held. However, the `finally` block **always executes before returning**. If the `finally` block itself contains a `return` statement, it will silently **override and discard** the return value (or exception) from the `try` block."

---

### 5. Custom File Handling vs Normal/Static File Serving

#### 🎙️ 60-Second Verbal Script
> "In web applications, file handling is divided into two architectures:
>
> 1. **Normal / Static Files**: Pre-existing static assets (CSS, JS, images, PDF brochures). Handled by Spring Boot's default `ResourceHttpRequestHandler` or offloaded to an AWS CloudFront CDN / S3 bucket with HTTP caching (`ETag`, `Cache-Control`).
> 2. **Custom / Dynamic File Handling**: Involves reading, generating, transforming, or parsing dynamic files at runtime (e.g. streaming 500MB CSV export reports, parsing proprietary financial EDI files, or Excel data validation):
>    - **Never load entire large files into JVM Heap (avoids `OutOfMemoryError`)**.
>    - **Streaming Responses**: Use **`StreamingResponseBody`** or **`InputStreamResource`** to stream data directly from the database/disk to the client's HTTP socket in small memory chunks (e.g. 8KB buffers).
>    - **Cloud Uploads**: Use **AWS S3 Pre-Signed URLs** or S3 Multipart Streaming so file bytes bypass backend servers entirely."

```java
@RestController
@RequestMapping("/api/v1/reports")
public class DynamicReportController {

    private final ReportDataService reportService;

    public DynamicReportController(ReportDataService reportService) {
        this.reportService = reportService;
    }

    // Memory-efficient dynamic streaming of 1,000,000+ CSV rows without OOM!
    @GetMapping(value = "/transactions.csv", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> streamTransactionReport() {
        StreamingResponseBody responseBody = outputStream -> {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                writer.write("TransactionId,UserId,Amount,Timestamp\n");
                
                // Stream rows from database cursor directly to HTTP output stream
                reportService.streamRecords(record -> {
                    try {
                        writer.write(String.format("%s,%s,%.2f,%s\n",
                                record.id(), record.userId(), record.amount(), record.timestamp()));
                    } catch (IOException e) {
                        throw new RuntimeException("Stream write error", e);
                    }
                });
                writer.flush();
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.csv\"")
                .body(responseBody);
    }
}
```
