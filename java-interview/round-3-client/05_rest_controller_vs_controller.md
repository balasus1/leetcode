# 05. @RestController vs @Controller in Spring MVC

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "In Spring MVC, **`@Controller`** and **`@RestController`** are both stereotypes used to define web endpoints, but they serve fundamentally different application architectures:
>
> 1. **`@Controller`** is designed for **traditional Server-Side Rendered (SSR) web applications** (using template engines like Thymeleaf, Freemarker, or JSP). When a method returns a `String` (e.g. `"home"`), Spring's `ViewResolver` looks up the corresponding HTML template file (e.g. `home.html`) and renders it in the browser. If you want a `@Controller` method to return raw JSON data, you must explicitly annotate the method with `@ResponseBody`.
> 2. **`@RestController`** is a convenience meta-annotation that **combines `@Controller` and `@ResponseBody`**. It is built specifically for **headless RESTful APIs**.
>
> In `@RestController`, every handler method automatically writes its return value directly into the HTTP response body, serialized into **JSON or XML** via Spring's `HttpMessageConverter` (Jackson by default), completely bypassing view resolution."

---

## 🧠 Architectural Comparison

```
                     ┌──────────────────────────────────────────────┐
                     │              Incoming HTTP Request           │
                     └──────────────────────┬───────────────────────┘
                                            │
                                            ▼
                                   [DispatcherServlet]
                                            │
                     ┌──────────────────────┴──────────────────────┐
                     ▼                                             ▼
          [@Controller Method]                          [@RestController Method]
          Returns: "user-profile"                       Returns: UserDto(id=1, name="Bala")
                     │                                             │
                     ▼                                             ▼
             [ViewResolver]                            [HttpMessageConverter (Jackson)]
                     │                                             │
                     ▼                                             ▼
          Renders HTML (Thymeleaf/JSP)                  Serializes to raw JSON / XML
                     │                                             │
                     └──────────────────────┬──────────────────────┘
                                            │
                                            ▼
                               [HTTP Response to Client]
```

---

## 💻 Code Demonstration

### 1. Traditional `@Controller` (Server-Side Rendering HTML):
```java
@Controller
@RequestMapping("/web")
public class WebPageController {

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("username", "Bala");
        // Returns view name -> Resolves to /templates/dashboard.html
        return "dashboard";
    }

    // Must add @ResponseBody if returning JSON from @Controller
    @GetMapping("/legacy-api")
    @ResponseBody
    public Map<String, String> getRawData() {
        return Map.of("status", "UP");
    }
}
```

### 2. Modern `@RestController` (REST API JSON Endpoint):
```java
@RestController
@RequestMapping("/api/v1/users")
public class UserRestController {

    private final UserService userService;

    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    // @ResponseBody is IMPLICIT on all methods!
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto user = userService.findUserById(id);
        // Automatically serialized into JSON: {"id":1, "name":"Bala", ...}
        return ResponseEntity.ok(user);
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "Can you define `@RestController` yourself if it didn't exist?"
**Answer:**
> "Yes, absolutely! `@RestController` is simply defined in the Spring Framework source code as:
> ```java
> @Target(ElementType.TYPE)
> @Retention(RetentionPolicy.RUNTIME)
> @Documented
> @Controller
> @ResponseBody
> public @interface RestController { ... }
> ```"

### 2. "How does Spring determine whether to serialize an object to JSON or XML in `@RestController`?"
**Answer:**
> "Through **HTTP Content Negotiation**. Spring inspects the incoming request's `Accept` header (e.g., `Accept: application/json` or `application/xml`) and delegates to the appropriate registered `HttpMessageConverter` (`MappingJackson2HttpMessageConverter` for JSON, `MappingJackson2XmlHttpMessageConverter` for XML)."
