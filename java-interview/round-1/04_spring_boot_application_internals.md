# 04. Explain @SpringBootApplication and How It Works Internally

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "`@SpringBootApplication` is the core bootstrap meta-annotation in Spring Boot. It combines three critical annotations into one:
>
> 1. **`@SpringBootConfiguration`**: A specialized form of `@Configuration` that tags the class as a primary source of bean definitions.
> 2. **`@EnableAutoConfiguration`**: The magic engine of Spring Boot. It uses `SpringFactoriesLoader` (or in Spring Boot 3+, `AutoConfiguration.imports`) to scan the classpath, inspect dependencies (like `spring-boot-starter-web` or `h2`), and auto-configure beans conditionally using `@ConditionalOnClass`, `@ConditionalOnMissingBean`, and `@ConditionalOnProperty`.
> 3. **`@ComponentScan`**: Automatically discovers and registers all Spring components (`@Service`, `@Repository`, `@RestController`, `@Component`) in the current package and its sub-packages.
>
> When `SpringApplication.run(Application.class, args)` is invoked, it instantiates the `AnnotationConfigServletWebServerApplicationContext`, prepares the Environment, executes auto-configuration classes, embeds the web server (Tomcat/Netty), and initializes all singleton beans in dependency order."

---

## 🧠 Key Technical Bullets
- **Meta-Annotation Composition**:
  - `@SpringBootConfiguration` (Bean definitions)
  - `@EnableAutoConfiguration` (Opinionated convention-over-configuration)
  - `@ComponentScan` (Package-level bean discovery)
- **Auto-Configuration Discovery Mechanism**:
  - *Spring Boot 2.x*: Reads `META-INF/spring.factories` key `org.springframework.boot.autoconfigure.EnableAutoConfiguration`.
  - *Spring Boot 3.x*: Reads `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- **Conditional Annotations**:
  - `@ConditionalOnClass`: Configures bean only if a specific class is present on classpath (e.g. `DataSource.class`).
  - `@ConditionalOnMissingBean`: Configures default bean only if the developer hasn't declared a custom bean.
  - `@ConditionalOnProperty`: Activates bean based on `application.yml` properties.

---

## 💻 Visual Anatomy & Configuration Example

```
┌─────────────────────────────────────────────────────────────┐
│                 @SpringBootApplication                      │
└──────────────────────────────┬──────────────────────────────┘
                               │ (composes)
        ┌──────────────────────┼──────────────────────┐
        ▼                      ▼                      ▼
┌─────────────────┐  ┌───────────────────┐  ┌─────────────────┐
│ @SpringBoot     │  │ @EnableAuto       │  │  @ComponentScan │
│  Configuration  │  │   Configuration   │  │                 │
│                 │  │                   │  │ (Scans base-pkg │
│ (Declares Beans)│  │ (Loads from       │  │  & sub-packages)│
│                 │  │  imports file)    │  │                 │
└─────────────────┘  └───────────────────┘  └─────────────────┘
```

### Disabling Specific Auto-Configurations:
```java
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
public class CoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreApplication.class, args);
    }
}
```

### Custom Conditional Auto-Configuration Under the Hood:
```java
@AutoConfiguration
@ConditionalOnClass(DataSource.class)
@ConditionalOnMissingBean(DataSource.class)
@ConditionalOnProperty(name = "spring.datasource.enabled", havingValue = "true", matchIfMissing = true)
public class CustomDataSourceAutoConfiguration {

    @Bean
    public DataSource dataSource() {
        return new HikariDataSource();
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "How does Spring Boot resolve bean conflicts between custom beans and auto-configured beans?"
**Answer:**
> "User-defined beans take precedence over auto-configuration because auto-configuration classes are evaluated *after* user configuration. Auto-configuration classes typically use `@ConditionalOnMissingBean`, which checks if a user has already defined that bean; if present, the auto-configuration bean is skipped."

### 2. "What changed in Spring Boot 3 regarding auto-configuration file location?"
**Answer:**
> "In Spring Boot 3.0, auto-configuration registration moved away from `META-INF/spring.factories` to `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` for improved modularity and native AOT (GraalVM) compilation performance."
