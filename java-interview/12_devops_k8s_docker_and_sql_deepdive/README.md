# 12. Kubernetes, Docker, DevOps & Advanced SQL Deep-Dive

Deep-dive explanations, 60-second verbal scripts, Kubernetes yaml manifests, storage mechanics, and complex SQL query execution orders.

---

## 📑 Topics Index
1. [Kubernetes StatefulSets vs Deployments](#1-kubernetes-statefulsets-vs-deployments)
2. [Kubernetes Rolling Updates & Zero-Downtime Configuration](#2-kubernetes-rolling-updates--zero-downtime-configuration)
3. [Docker Volumes vs Bind Mounts vs tmpfs](#3-docker-volumes-vs-bind-mounts-vs-tmpfs)
4. [SQL Query: 2nd Highest Salary in Each Department](#4-sql-query-2nd-highest-salary-in-each-department)
5. [Complex SQL Query with WHERE, GROUP BY, HAVING, ORDER BY & Execution Order](#5-complex-sql-query-with-where-group-by-having-order-by--execution-order)

---

### 1. Kubernetes StatefulSets vs Deployments
#### 🎙️ 60-Second Verbal Script
> "| Feature | `Deployment` | `StatefulSet` |
> |---|---|---|
> | **Primary Workload** | **Stateless Apps** (REST APIs, Microservices) | **Stateful Distributed Systems** (Kafka, Postgres, Redis) |
> | **Pod Identity** | Random hashes (e.g. `order-7b8f9-x1y2`) | **Stable, deterministic ordinal names** (`kafka-0`, `kafka-1`) |
> | **Storage Binding** | Shared PersistentVolume | Dedicated **VolumeClaimTemplate** per Pod instance |
> | **Startup / Scaling** | Replicas launch and terminate in parallel | **Strict ordered launch (0 $\to$ 1 $\to$ 2) and reverse termination** |
> | **Network Identity** | Single cluster IP via Service | **Headless Service** providing unique DNS records for each Pod |"

---

### 2. Kubernetes Rolling Updates & Zero-Downtime Configuration
#### 🎙️ 60-Second Verbal Script
> "To achieve true **Zero-Downtime Rolling Updates** in Kubernetes:
>
> 1. Set Deployment strategy to `RollingUpdate` with `maxSurge: 25%` (can spin up 25% extra new pods) and `maxUnavailable: 0` (guarantees zero pods taken down before new ones are healthy).
> 2. Configure a **`readinessProbe`** targeting `/actuator/health/readiness`. Kubernetes routes traffic to the new pod ONLY after it returns HTTP 200.
> 3. Implement **Graceful Shutdown** in Spring Boot (`server.shutdown=graceful`) with `terminationGracePeriodSeconds: 30` to let active in-flight requests finish processing before SIGKILL is sent."

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

---

### 3. Docker Volumes vs Bind Mounts vs tmpfs
#### 🎙️ 60-Second Verbal Script
> "- **Docker Volumes (`/var/lib/docker/volumes/`)**: Managed completely by Docker daemon. Completely isolated from host OS filesystem structure. Production standard for database persistence and backups.
> - **Bind Mounts**: Maps an arbitrary file or directory on the host machine (`-v /Users/dev/code:/app`) into the container. Excellent for local live-reload development.
> - **tmpfs Mounts**: Stored strictly in host memory (RAM), never written to disk. Used for ephemeral sensitive secrets or high-speed temporary caching."

---

### 4. SQL Query: 2nd Highest Salary in Each Department
#### 🎙️ 60-Second Verbal Script
> "To find the 2nd highest salary in each department, we use the `DENSE_RANK()` window function partitioned by department and ordered by salary descending. `DENSE_RANK()` handles salary ties gracefully without skipping rank numbers."

```sql
WITH RankedSalaries AS (
    SELECT 
        dept_id,
        emp_id,
        emp_name,
        salary,
        DENSE_RANK() OVER (
            PARTITION BY dept_id 
            ORDER BY salary DESC
        ) AS salary_rank
    FROM employees
)
SELECT dept_id, emp_id, emp_name, salary
FROM RankedSalaries
WHERE salary_rank = 2;
```

---

### 5. Complex SQL Query with WHERE, GROUP BY, HAVING, ORDER BY & Execution Order

#### 📝 Scenario:
Find all **Departments** with at least **3 Active Employees** where the **Average Department Salary exceeds $75,000**, sorted by the highest average salary.

```sql
SELECT 
    d.dept_name,
    COUNT(e.emp_id) AS total_active_employees,
    ROUND(AVG(e.salary), 2) AS avg_department_salary
FROM departments d
JOIN employees e ON d.dept_id = e.dept_id
WHERE e.status = 'ACTIVE'                -- 1. Filter rows before grouping
GROUP BY d.dept_id, d.dept_name          -- 2. Group into buckets
HAVING COUNT(e.emp_id) >= 3              -- 3. Filter groups (aggregate condition 1)
   AND AVG(e.salary) > 75000.00          -- 4. Filter groups (aggregate condition 2)
ORDER BY avg_department_salary DESC;     -- 5. Sort final results
```

#### 🔍 Step-by-Step SQL Execution Order (The Interviewer Checklist):
> "SQL does NOT execute top-to-bottom in the order it is written (`SELECT` comes last!). The internal database execution order is:
>
> 1. **`FROM` & `JOIN`**: Tables are joined to form the Cartesian dataset.
> 2. **`WHERE`**: Filters individual rows *before* aggregation (filters out inactive employees).
> 3. **`GROUP BY`**: Aggregates the remaining rows into distinct department groups.
> 4. **`HAVING`**: Filters the aggregated groups based on aggregate functions (`COUNT >= 3` and `AVG > 75000`).
> 5. **`SELECT`**: Evaluates column expressions, aliases, and `ROUND()`.
> 6. **`ORDER BY`**: Sorts the finalized projected rows.
> 7. **`LIMIT / OFFSET`**: Restricts the returned page size."
