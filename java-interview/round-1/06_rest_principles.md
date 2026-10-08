# 06. Explain REST Principles & Richardson Maturity Model

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "**REST** (Representational State Transfer) is an architectural style defined by Roy Fielding for designing scalable, networked web services. It is governed by **6 core architectural constraints**:
>
> 1. **Client-Server Separation**: Separation of user interface concerns from data storage concerns, allowing independent evolution.
> 2. **Statelessness**: Every request from client to server must contain all necessary context (authentication tokens, session state). The server stores no client session context between requests.
> 3. **Cacheability**: Responses must implicitly or explicitly define themselves as cacheable or non-cacheable (via `Cache-Control`, `ETag`, `Last-Modified` headers) to prevent stale data and reduce network latency.
> 4. **Uniform Interface**: The cornerstone of REST: identification of resources via URIs, resource manipulation through representations (JSON/XML), self-descriptive messages (HTTP headers/content types), and **HATEOAS** (Hypermedia As The Engine Of Application State).
> 5. **Layered System**: The client cannot tell whether it is connected directly to the end server or an intermediary like API gateways, load balancers, or CDNs.
> 6. **Code on Demand (Optional)**: Servers temporarily extending client functionality by transferring executable code (e.g., JavaScript applets).
>
> In practice, we evaluate RESTful services against the **Richardson Maturity Model** from Level 0 (The Swamp of POX/RPC) up to Level 3 (Hypermedia / HATEOAS)."

---

## 🧠 Richardson Maturity Model (Levels 0 to 3)

```
┌─────────────────────────────────────────────────────────────┐
│ Level 3: HATEOAS (Hypermedia controls & discoverable links) │
├─────────────────────────────────────────────────────────────┤
│ Level 2: HTTP Verbs (GET, POST, PUT, DELETE) + Status Codes │
├─────────────────────────────────────────────────────────────┤
│ Level 1: Resources (Individual URIs like /users, /orders)   │
├─────────────────────────────────────────────────────────────┤
│ Level 0: The Swamp of POX (Single URI e.g. /api via POST)   │
└─────────────────────────────────────────────────────────────┘
```

---

## 💻 Spring HATEOAS Example (Level 3 REST)

```java
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public EntityModel<AccountDto> getAccount(@PathVariable String id) {
        AccountDto account = accountService.findById(id);

        // Attaching dynamic hypermedia links based on business state
        EntityModel<AccountDto> resource = EntityModel.of(account);
        
        // Self link
        resource.add(linkTo(methodOn(AccountController.class).getAccount(id)).withSelfRel());
        
        // Conditional link: if account has balance, allow withdraw/transfer
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            resource.add(linkTo(methodOn(AccountController.class).withdraw(id, null)).withRel("withdraw"));
            resource.add(linkTo(methodOn(AccountController.class).transfer(id, null)).withRel("transfer"));
        }
        // Link to transaction history
        resource.add(linkTo(methodOn(AccountController.class).getTransactions(id)).withRel("transactions"));

        return resource;
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "Why is statelessness so critical for cloud-native microservices?"
**Answer:**
> "Statelessness enables **effortless horizontal scaling (Auto-scaling)**. Any instance of the microservice can handle any incoming request behind a Round-Robin Load Balancer without requiring sticky sessions or distributed session synchronization across instances."

### 2. "What are HTTP Idempotent methods vs Safe methods?"
**Answer:**
> - **Safe Methods** (do not alter server state): `GET`, `HEAD`, `OPTIONS`.
> - **Idempotent Methods** (multiple identical requests have the exact same effect as a single request): `GET`, `HEAD`, `PUT`, `DELETE`, `OPTIONS`.
> - **Non-Idempotent / Unsafe**: `POST`, `PATCH` (by RFC spec).
