# 01. Explain Different Types of Class Loaders in Java

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "In Java, **Class Loaders** are components of the Java Virtual Machine responsible for dynamically loading `.class` bytecode files into memory during runtime.
>
> Java follows a hierarchical **Delegation-Parent Model** with three primary built-in Class Loaders:
>
> 1. **Bootstrap Class Loader**: Written in native C/C++, it is the root loader that loads core Java runtime classes (like `java.lang.*`, `java.util.*` from `java.base` module or `rt.jar`). In Java code, its reference returns `null`.
> 2. **Platform / Extension Class Loader**: Loads platform/extension classes (e.g. `java.sql.*`, XML parsers) from the Java modular runtime.
> 3. **Application / System Class Loader**: Loads application-specific classes located on the application `CLASSPATH` or `--module-path` (your project code and Maven/Gradle dependencies).
>
> In addition, enterprise frameworks use **Custom Class Loaders** (e.g. Tomcat's `WebappClassLoader` for hot-swapping and servlet isolation, or Spring Boot's `LaunchedURLClassLoader` for loading nested JARs in a fat JAR).
>
> The Class Loader mechanism adheres to three principles: **Delegation** (always ask parent first), **Visibility** (child sees parent's classes, but parent cannot see child's), and **Uniqueness** (a class is loaded exactly once per class loader hierarchy)."

---

## 🧠 Class Loader Hierarchy & Delegation Model

```
       ┌────────────────────────────────────────────────────────┐
       │   Bootstrap Class Loader (Native C/C++, rt.jar / base) │
       └───────────────────────────▲────────────────────────────┘
                                   │ (Delegates Parent First)
       ┌───────────────────────────┴────────────────────────────┐
       │   Platform / Extension Class Loader (ext / modular)   │
       └───────────────────────────▲────────────────────────────┘
                                   │ (Delegates Parent First)
       ┌───────────────────────────┴────────────────────────────┐
       │   Application / System Class Loader (App CLASSPATH)    │
       └───────────────────────────▲────────────────────────────┘
                                   │ (Delegates Parent First)
       ┌───────────────────────────┴────────────────────────────┐
       │   Custom / WebApp Class Loader (Tomcat / Spring Boot)  │
       └────────────────────────────────────────────────────────┘
```

---

## 💻 Inspecting Class Loaders in Java

```java
package clientround;

import java.util.ArrayList;

public class ClassLoaderInspectionDemo {
    public static void main(String[] args) {
        // 1. Core Java Class (Loaded by Bootstrap Loader)
        Class<?> stringClass = String.class;
        System.out.println("String ClassLoader (Bootstrap -> null): " + stringClass.getClassLoader());

        // 2. Platform Class (Platform Class Loader)
        Class<?> sqlDriverClass = java.sql.Driver.class;
        System.out.println("SQL Driver ClassLoader: " + sqlDriverClass.getClassLoader());

        // 3. User Defined Application Class (Application Class Loader)
        Class<?> appClass = ClassLoaderInspectionDemo.class;
        System.out.println("Application ClassLoader: " + appClass.getClassLoader());

        // 4. Hierarchy Traversing
        ClassLoader current = appClass.getClassLoader();
        System.out.println("\n--- Traversing Class Loader Hierarchy ---");
        while (current != null) {
            System.out.println("Loader: " + current);
            current = current.getParent();
        }
        System.out.println("Top-level parent reached: Bootstrap (Native)");
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "Can you load two different versions of the exact same class in a single JVM?"
**Answer:**
> "Yes! By using **two distinct Custom Class Loaders**. In Java, the uniqueness of a class type is defined by the tuple `(ClassLoader Instance, Fully Qualified Class Name)`. This is how OSGi containers and application servers like Tomcat isolate different deployed web apps with conflicting library versions."

### 2. "What is `ClassNotFoundException` vs `NoClassDefFoundError`?"
**Answer:**
> - **`ClassNotFoundException`**: A checked exception thrown when an application tries to explicitly load a class at runtime using `Class.forName()` or `ClassLoader.loadClass()`, but the `.class` file is not found on the classpath.
> - **`NoClassDefFoundError`**: An unrecoverable `Error` thrown when a class was present at **compile time**, but is missing at **runtime** (or static initialization of that class failed previously)."
