# 06. @ControllerAdvice vs @RestControllerAdvice in Spring Boot

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "In Spring Boot, both **`@ControllerAdvice`** and **`@RestControllerAdvice`** are used to implement **Global Cross-Cutting Concerns** across all controllers — most notably **Global Exception Handling, `@InitBinder`, and `@ModelAttribute`**.
>
> The fundamental difference mirrors `@Controller` vs `@RestController`:
>
> 1. **`@ControllerAdvice`**: Intercepts exceptions and renders an HTML error view (e.g. returning an error page name like `"error/500"`). If you want an `@ExceptionHandler` method inside a `@ControllerAdvice` class to return a JSON error payload, you must manually annotate that method with `@ResponseBody`.
> 2. **`@RestControllerAdvice`**: A specialized convenience meta-annotation that **combines `@ControllerAdvice` and `@ResponseBody`**.
>
> In modern microservices, we use `@RestControllerAdvice` to intercept exceptions across all `@RestControllers` and return standardized, RFC-compliant error structures such as **RFC 7807 `ProblemDetail`** or custom Error DTOs with timestamps, error codes, and HTTP status codes."

---

## 🧠 Comparison Matrix

| Feature | `@ControllerAdvice` | `@RestControllerAdvice` |
|---|---|---|
| **Primary Target** | Traditional Spring MVC UI Web Apps | RESTful JSON/XML Microservices |
| **Combines** | `@Component` | `@ControllerAdvice` + `@ResponseBody` |
| **Default Return Type** | HTML View Name (`String` / `ModelAndView`) | Serialized Response Body (`JSON` / `XML`) |
| **Need for `@ResponseBody`?**| **Yes**, on individual handler methods for JSON | **No**, automatically applied to all methods |
| **Introduced In** | Spring 3.2 | Spring 4.3 |

---

## 💻 Production-Grade Global Exception Handler (`@RestControllerAdvice`)

```java
package com.example.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalRestExceptionHandler {

    // 1. Handle Custom Business Resource Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://api.example.com/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    // 2. Handle JSR-380 Validation Errors (@Valid @RequestBody failures)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> errorResponse = Map.of(
                "timestamp", Instant.now(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Bad Request - Validation Failed",
                "validationErrors", fieldErrors
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    // 3. Fallback General Exception Handler
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred.");
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "Can you target `@RestControllerAdvice` to only specific packages or annotations?"
**Answer:**
> "Yes! `@RestControllerAdvice` supports scoping attributes:
> - `basePackages = "com.example.orders"` (scopes to package)
> - `annotations = RestController.class` (scopes to specific controllers)
> - `assignableTypes = { OrderController.class }` (scopes to specific classes)"

### 2. "What is Spring 6 / Spring Boot 3's `ProblemDetail` (RFC 7807) standard?"
**Answer:**
> "`ProblemDetail` is the official specification for HTTP API problem details (RFC 7807). It standardizes JSON error responses across microservices with fields like `type`, `title`, `status`, `detail`, and `instance`, replacing disparate custom error formats."
