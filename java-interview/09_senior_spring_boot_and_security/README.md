# 09. Spring Boot Architecture, Security (JWT/RBAC) & Cloud Integrations

Deep-dive explanations, 60-second verbal scripts, Spring Security filter chain mechanics, and complete architecture walkthroughs for Senior Backend Engineers.

---

## 📑 Topics Index
1. [Spring Boot Clean Layered Architecture](#1-spring-boot-clean-layered-architecture)
2. [End-to-End JWT Authentication Flow in Spring Security 6 / Spring Boot 3](#2-end-to-end-jwt-authentication-flow-in-spring-security-6--spring-boot-3)
3. [Authorization with RBAC & `@PreAuthorize`](#3-authorization-with-rbac--preauthorize)
4. [Multi-Environment Configuration (`application.yml` Profiles)](#4-multi-environment-configuration-applicationyml-profiles)
5. [Role of `@Configuration` vs `@Bean` & `proxyBeanMethods`](#5-role-of-configuration-vs-bean--proxybeanmethods)
6. [How Spring Boot Manages Embedded Servers (Tomcat / Jetty)](#6-how-spring-boot-manages-embedded-servers-tomcat--jetty)
7. [`@ControllerAdvice` vs `@ExceptionHandler`](#7-controlleradvice-vs-exceptionhandler)
8. [Integrating Spring Boot with Cloud Services (AWS S3, RDS Aurora)](#8-integrating-spring-boot-with-cloud-services-aws-s3-rds-aurora)

---

### 1. Spring Boot Clean Layered Architecture
#### 🎙️ 60-Second Verbal Script
> "Our enterprise Spring Boot microservices adhere strictly to a **4-Tier Clean Layered Architecture**:
>
> 1. **Controller Layer (`@RestController`)**: Handles HTTP requests, path/query validation (`@Valid`), HTTP status codes, and delegates directly to the Service layer without holding business logic.
> 2. **Service Layer (`@Service`)**: Encapsulates core business rules, transactional boundaries (`@Transactional`), domain validations, and external third-party API orchestrations.
> 3. **Repository / Data Access Layer (`@Repository`)**: Interacts with the database via Spring Data JPA or Spring Data JDBC, executing optimized JPQL/native SQL queries.
> 4. **Domain & DTO Layer**: Pure domain entities mapped to database tables, decoupled from client-facing Request/Response DTOs using MapStruct."

```
[HTTP Request] ──► [Filter Chain (Security/Tracing)]
                          │
                          ▼
                  [@RestController] (Validation & Statuses)
                          │
                          ▼
                    [@Service] (Business Rules & @Transactional)
                          │
                          ▼
                   [@Repository] (Spring Data JPA / HikariCP)
                          │
                          ▼
                 [PostgreSQL / AWS RDS]
```

---

### 2. End-to-End JWT Authentication Flow in Spring Security 6 / Spring Boot 3
#### 🎙️ 60-Second Verbal Script
> "The complete **JWT Authentication Flow** follows 5 structured steps:
>
> 1. **Client Login Request**: User sends credentials (`POST /api/v1/auth/login`) with username and password.
> 2. **AuthenticationManager Validation**: Spring Security's `AuthenticationManager` verifies credentials against the database using `BCryptPasswordEncoder`.
> 3. **Token Generation**: On success, the `JwtTokenProvider` generates a cryptographically signed **Access Token (JWT)** (short-lived, e.g. 15 mins) and a **Refresh Token** (long-lived, e.g. 7 days stored in Redis or DB).
> 4. **Filter Interception on Subsequent Requests**: Every subsequent API request passes through a custom `OncePerRequestFilter` (`JwtAuthenticationFilter`). It extracts the `Authorization: Bearer <token>` header, validates the HMAC-SHA256 or RSA signature, extracts user claims, and builds an `Authentication` token.
> 5. **SecurityContext Registration**: The filter populates `SecurityContextHolder.getContext().setAuthentication(auth)`, allowing downstream controllers to access authenticated user identity."

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserDetailsService userDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenService.validateToken(token)) {
                String username = jwtTokenService.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

---

### 3. Authorization with RBAC & `@PreAuthorize`
#### 🎙️ 60-Second Verbal Script
> "Authorization determines whether an authenticated user has permission to access a specific resource.
>
> In Spring Boot 3, we enable method security with `@EnableMethodSecurity`. We implement **Role-Based Access Control (RBAC)** by decorating service/controller methods with `@PreAuthorize`:
>
> ```java
> @PreAuthorize("hasRole('ADMIN') or hasAuthority('ORDER_WRITE')")
> @DeleteMapping("/orders/{id}")
> public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
>     orderService.cancelOrder(id);
>     return ResponseEntity.noContent().build();
> }
> ```
> Under the hood, Spring AOP evaluates the SpEL (Spring Expression Language) expression against the user's `GrantedAuthority` collection in the `SecurityContext` before allowing method execution."

---

### 4. Multi-Environment Configuration (`application.yml` Profiles)
#### 🎙️ 60-Second Verbal Script
> "In cloud-native Spring Boot applications, we maintain environment-specific properties using Spring Profiles (`dev`, `staging`, `prod`):
>
> 1. `application.yml` stores shared defaults and active profile declarations (`spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}`).
> 2. `application-dev.yml` contains local H2/PostgreSQL database URLs and debug loggers.
> 3. `application-prod.yml` pulls production connection strings and secrets dynamically from environment variables or AWS Secrets Manager.
> 4. In Kubernetes, we inject `SPRING_PROFILES_ACTIVE=prod` via ConfigMaps."

---

### 5. Role of `@Configuration` vs `@Bean` & `proxyBeanMethods`
#### 🎙️ 60-Second Verbal Script
> "- **`@Configuration`**: Tags a class as a full Spring configuration class. By default, Spring wraps it in a CGLIB dynamic proxy (`proxyBeanMethods = true`) to enforce singleton semantics: calling an internal `@Bean` method directly from another `@Bean` method returns the existing container singleton instance rather than creating a duplicate object.
> - **`@Bean`**: Placed on methods to register the returned object instance as a bean in the `ApplicationContext`."

---

### 6. How Spring Boot Manages Embedded Servers (Tomcat / Jetty)
#### 🎙️ 60-Second Verbal Script
> "Spring Boot uses **Embedded Servlet Containers** to make applications self-contained executable JARs:
>
> 1. When `spring-boot-starter-web` is on the classpath, auto-configuration registers a `TomcatServletWebServerFactory`.
> 2. During `context.refresh()`, the `ServletWebServerApplicationContext` calls `createWebServer()`, programmatically instantiating Apache Tomcat, configuring connection pools, binding to port 8080, and mounting the `DispatcherServlet`.
> 3. To switch to Jetty or Undertow, we simply exclude `spring-boot-starter-tomcat` and add `spring-boot-starter-undertow` in `pom.xml` or `build.gradle`."

---

### 7. `@ControllerAdvice` vs `@ExceptionHandler`
#### 🎙️ 60-Second Verbal Script
> "- **`@ExceptionHandler`**: An annotation placed on methods to catch and handle specific exception classes (e.g. `@ExceptionHandler(OrderNotFoundException.class)`). When used alone inside a specific Controller, it **only handles exceptions thrown by that individual controller**.
> - **`@ControllerAdvice` / `@RestControllerAdvice`**: An interceptor component that applies `@ExceptionHandler` methods **globally across all controllers** in the entire application, serving as the central hub for standardized API error handling."

---

### 8. Integrating Spring Boot with Cloud Services (AWS S3, RDS Aurora)
#### 🎙️ 60-Second Verbal Script
> "We integrate Spring Boot with AWS using **Spring Cloud AWS** and **AWS SDK v2**:
>
> 1. **AWS RDS Aurora PostgreSQL**: Configured via standard JDBC URL and HikariCP. In Kubernetes (EKS), we authenticate using IAM Database Authentication or AWS Secrets Manager rotation.
> 2. **Amazon S3**: Injected via `S3Template` or `S3Client`. We use S3 for generating pre-signed URLs, allowing mobile/web clients to upload large media files directly to S3 without saturating backend API memory."
