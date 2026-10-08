# 07. Java Stream Program to Find Maximum Salary in Each Department

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "To find the employee with the maximum salary in each department using Java Streams, we group the stream of employees by their department and apply a downstream collector.
>
> We use `Collectors.groupingBy(Employee::getDepartment, Collectors.collectingAndThen(Collectors.maxBy(Comparator.comparingDouble(Employee::getSalary)), Optional::get))`.
>
> Alternatively, if we just want a mapping of Department Name to Maximum Salary amount, we use `Collectors.toMap(Employee::getDepartment, Employee::getSalary, BinaryOperator.maxBy(Double::compare))`.
>
> This processes the collection in a single $O(N)$ pipeline with $O(D)$ auxiliary space, where $D$ is the number of distinct departments. It is thread-safe, declarative, avoids mutable state accumulation, and can be converted to `.parallelStream()` for large in-memory datasets."

---

## 🧠 Key Technical Bullets
- **Collector Combinations**:
  - `groupingBy(keyMapper, downstreamCollector)`
  - `maxBy(comparator)` returns `Optional<Employee>`
  - `collectingAndThen(collector, finisher)` unpacks `Optional<Employee>` safely
  - `toMap(keyMapper, valueMapper, mergeFunction)` for direct primitive values
- **Complexity**: Time: $O(N)$, Space: $O(D)$ where $D = \text{departments}$.

---

## 💻 Complete Runnable Java Code

```java
package round1;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

public class MaxSalaryByDepartment {

    public record Employee(int id, String name, String department, double salary) {}

    public static void main(String[] args) {
        List<Employee> employees = List.of(
            new Employee(1, "Alice", "Engineering", 125000),
            new Employee(2, "Bob", "Engineering", 145000),
            new Employee(3, "Charlie", "HR", 85000),
            new Employee(4, "Diana", "HR", 95000),
            new Employee(5, "Evan", "Marketing", 110000),
            new Employee(6, "Frank", "Marketing", 105000)
        );

        // Approach 1: Map<Department, EmployeeWithMaxSalary>
        Map<String, Employee> topEmployeeByDept = employees.stream()
                .collect(Collectors.groupingBy(
                        Employee::department,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingDouble(Employee::salary)),
                                opt -> opt.orElse(null)
                        )
                ));

        System.out.println("=== Top Employee in Each Department ===");
        topEmployeeByDept.forEach((dept, emp) -> 
            System.out.printf("Dept: %-12s | Top Earner: %-10s | Salary: $%,.2f%n", 
                    dept, emp.name(), emp.salary())
        );

        // Approach 2: Map<Department, Double (Max Salary Value)> using toMap with Merge Function
        Map<String, Double> maxSalaryByDept = employees.stream()
                .collect(Collectors.toMap(
                        Employee::department,
                        Employee::salary,
                        BinaryOperator.maxBy(Double::compare)
                ));

        System.out.println("\n=== Max Salary Amount by Department ===");
        maxSalaryByDept.forEach((dept, maxSal) -> 
            System.out.printf("Dept: %-12s | Max Salary: $%,.2f%n", dept, maxSal)
        );
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "What happens if two employees in the same department have the exact same maximum salary?"
**Answer:**
> "In `Collectors.maxBy()`, the first employee encountered in stream order will be retained because `Comparator` evaluates `compare(a, b) == 0` as not strictly greater. If we need to return *all* top earners sharing the max salary, we perform a 2-pass grouping or custom collector that collects a `List<Employee>` for the max salary."

### 2. "How would you do this in SQL for comparison?"
**Answer:**
> "Using the SQL window function `DENSE_RANK()` or `ROW_NUMBER()`:
> ```sql
> SELECT department, name, salary
> FROM (
>     SELECT department, name, salary,
>            DENSE_RANK() OVER (PARTITION BY department ORDER BY salary DESC) as rnk
>     FROM employees
> ) ranked
> WHERE rnk = 1;
> ```"
