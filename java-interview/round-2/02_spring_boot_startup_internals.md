# 02. What Happens Internally When a Spring Boot Application Starts?

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "When `SpringApplication.run(Application.class, args)` is invoked, Spring Boot executes an orchestrated 8-step bootstrap sequence:
>
> 1. **Bootstrap Initialization**: It instantiates `SpringApplication`, detects the application type (Servlet vs Reactive WebFlux), and loads `BootstrapRegistryInitializers` and `ApplicationContextInitializers`.
> 2. **Environment Preparation**: Starts the `SpringApplicationRunListeners` and builds the `ConfigurableEnvironment`, resolving command-line arguments, system properties, and `application.yml/properties` profiles.
> 3. **Banner & Context Creation**: Prints the ASCII banner and instantiates the `AnnotationConfigServletWebServerApplicationContext`.
> 4. **Context Preparation**: Loads bean definitions, invokes `ApplicationContextInitializer` callbacks, and publishes the `ApplicationPreparedEvent`.
> 5. **Context Refresh (The Core Step)**: Calls `context.refresh()`. It registers `BeanFactoryPostProcessors`, scans the classpath via `@ComponentScan`, processes `@Configuration` classes, evaluates `@EnableAutoConfiguration` imports with condition evaluation, and instantiates the embedded web server (Tomcat/Jetty).
> 6. **Bean Post-Processing & Singleton Instantiation**: It initializes all eager singleton beans, running dependency injection, `@PostConstruct` lifecycle hooks, and AOP proxy generation via `BeanPostProcessor`s.
> 7. **Tomcat Start**: The embedded Tomcat binds to the configured port (e.g. 8080) and initializes the `DispatcherServlet`.
> 8. **Runners Execution**: Executes all beans implementing `CommandLineRunner` and `ApplicationRunner`, publishing the final `ApplicationReadyEvent`."

---

## 🧠 Step-by-Step Internal Sequence Diagram

```
1. SpringApplication.run()
       │
2. Determine Web Type (SERVLET / REACTIVE / NONE)
       │
3. Create & Configure Environment (Profiles, application.yml)
       │
4. Instantiate ApplicationContext (AnnotationConfigServletWebServerApplicationContext)
       │
5. Prepare Context (Post-processors, Listeners)
       │
6. Context.refresh() ◄───────────── [THE ENGINE OF SPRING]
       ├── Register BeanFactoryPostProcessors
       ├── Process @Configuration & Auto-Configurations
       ├── Start Embedded Tomcat Web Server
       └── Instantiate & Autowire All Singleton Beans (@PostConstruct, Proxies)
       │
7. DispatcherServlet Initialization & Port Binding
       │
8. Execute CommandLineRunners / ApplicationRunners ──► [ApplicationReadyEvent]
```

---

## 💻 Visualizing Bean Lifecycle Hooks in Context Refresh

```java
@Component
public class LifecycleDemoBean implements BeanNameAware, BeanFactoryAware, InitializingBean, DisposableBean {

    public LifecycleDemoBean() {
        System.out.println("1. Constructor: Bean Instantiated");
    }

    @Override
    public void setBeanName(String name) {
        System.out.println("2. Aware Interface: Bean Name set to -> " + name);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        System.out.println("3. Aware Interface: BeanFactory set");
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("4. JSR-250: @PostConstruct initialization method");
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("5. InitializingBean: afterPropertiesSet() called");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("6. JSR-250: @PreDestroy cleanup");
    }

    @Override
    public void destroy() {
        System.out.println("7. DisposableBean: destroy() called");
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "What is the difference between BeanFactoryPostProcessor and BeanPostProcessor?"
**Answer:**
> - **`BeanFactoryPostProcessor`**: Operates on **bean metadata/definitions** *before* any actual bean instances are created. (e.g., `PropertySourcesPlaceholderConfigurer` which resolves `${...}` property placeholders).
> - **`BeanPostProcessor`**: Operates on **actual bean instances** *after* they are instantiated by the IoC container. (e.g., intercepts beans to generate Spring AOP / `@Transactional` dynamic proxies or process `@Autowired`)."

### 2. "How does Spring Boot start an embedded Tomcat without `web.xml`?"
**Answer:**
> "During `context.refresh()`, the `ServletWebServerApplicationContext` calls `createWebServer()`. It discovers `TomcatServletWebServerFactory` on the classpath, instantiates embedded Tomcat programmatically, registers the `DispatcherServlet`, and binds to the connector port."
