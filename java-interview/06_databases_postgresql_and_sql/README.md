# 06. PostgreSQL, Concurrency, SQL Joins & Query Execution Plan Mastery

Deep-dive explanations, 60-second verbal scripts, MVCC internals, SQL execution plans, and a practical interview task for `RIGHT OUTER JOIN` & `EXPLAIN ANALYZE`.

---

## 📑 Topics Index
1. [How PostgreSQL Handles Concurrency: MVCC Internals](#1-how-postgresql-handles-concurrency-mvcc-internals)
2. [PostgreSQL Transaction Isolation Levels](#2-postgresql-transaction-isolation-levels)
3. [Primary Key vs Unique Key & The NULL Trap](#3-primary-key-vs-unique-key--the-null-trap)
4. [Types of SQL Joins Overview](#4-types-of-sql-joins-overview)
5. [💻 Practical SQL Task: RIGHT OUTER JOIN Query, Explanation & Deep Analysis](#5--practical-sql-task-right-outer-join-query-explanation--deep-analysis)
6. [What is an SQL Execution Plan?](#6-what-is-an-sql-execution-plan)
7. [Analyzing Queries with `EXPLAIN` vs `EXPLAIN ANALYZE` (Buffer Hits & Cost)](#7-analyzing-queries-with-explain-vs-explain-analyze)

---

### 1. How PostgreSQL Handles Concurrency: MVCC Internals
#### 🎙️ 60-Second Verbal Script
> "PostgreSQL handles high-concurrency read/write operations without locking using **Multi-Version Concurrency Control (MVCC)**:
>
> - **Readers never block Writers, and Writers never block Readers**.
> - When an `UPDATE` occurs, Postgres does not overwrite the row in place; it inserts a **new version (tuple)** of the row and marks the old version as expired using hidden system columns: **`xmin`** (creation transaction ID) and **`xmax`** (deletion/update transaction ID).
> - Each transaction sees a point-in-time **Snapshot** of the database based on active transaction IDs at snapshot creation time.
> - **VACUUM & AutoVacuum**: Background processes that reclaim space occupied by dead row versions (garbage collection for Postgres) and update the visibility map and table statistics."

---

### 2. PostgreSQL Transaction Isolation Levels
#### 🎙️ 60-Second Verbal Script
> "PostgreSQL supports 3 distinct isolation levels:
>
> 1. **Read Committed (Default)**: Each statement in a transaction sees only data committed before that statement began. Prevents Dirty Reads.
> 2. **Repeatable Read**: The entire transaction sees a consistent snapshot taken at the **start of the first query** in the transaction. Prevents Dirty Reads and Non-Repeatable Reads. If two concurrent transactions attempt to update the same row, Postgres raises a serialization failure: `could not serialize access due to concurrent update`.
> 3. **Serializable**: Implemented using **Serializable Snapshot Isolation (SSI)**. Tracks dependency graphs between transactions; if an anomalous read-write conflict cycle is detected, Postgres aborts the transaction."

---

### 3. Primary Key vs Unique Key & The NULL Trap
#### 🎙️ 60-Second Verbal Script
> "| Feature | Primary Key (PK) | Unique Key (UK) |
> |---|---|---|
> | **Purpose** | Uniquely identifies a row in a table | Enforces uniqueness on candidate columns |
> | **Count per Table** | **Strictly 1** per table | **Multiple** unique constraints permitted |
> | **NULL Values** | **Never allowed** (Implicitly `NOT NULL`) | **Permitted** by default |
> | **Index Type** | Automatically creates a Clustered / B-Tree Index | Automatically creates a unique B-Tree index |
>
> **The Interview Trap: Can a Unique Key contain multiple NULLs?**
> **Yes!** In standard SQL and PostgreSQL, `NULL != NULL` (NULL represents an unknown value). Therefore, multiple rows can have `NULL` in a unique column unless you explicitly declare `NOT NULL` or PostgreSQL 15+'s `NULLS NOT DISTINCT` clause."

---

### 4. Types of SQL Joins Overview
#### 🎙️ 60-Second Verbal Script
> "SQL provides 6 join types to combine records across tables:
> 1. **INNER JOIN**: Returns only matching rows where the join predicate evaluates to true in both tables.
> 2. **LEFT (OUTER) JOIN**: Returns all rows from the Left table, and matched rows from Right table (filled with `NULL` if unmatched).
> 3. **RIGHT (OUTER) JOIN**: Returns all rows from the Right table, and matched rows from Left table.
> 4. **FULL (OUTER) JOIN**: Returns all rows when there is a match in either left or right table.
> 5. **CROSS JOIN**: Cartesian product returning $M \times N$ row combinations.
> 6. **SELF JOIN**: A regular join where a table is joined with itself (e.g. Employee-Manager hierarchies)."

---

### 5. 💻 Practical SQL Task: RIGHT OUTER JOIN Query, Explanation & Deep Analysis

#### 📝 Scenario:
Find all **Departments** and the **Employees** assigned to them, including **departments that currently have zero employees**.

#### 🗄️ Table Schemas:
```sql
CREATE TABLE departments (
    dept_id SERIAL PRIMARY KEY,
    dept_name VARCHAR(100) NOT NULL
);

CREATE TABLE employees (
    emp_id SERIAL PRIMARY KEY,
    emp_name VARCHAR(100) NOT NULL,
    dept_id INT REFERENCES departments(dept_id),
    salary NUMERIC(10, 2)
);
```

#### ✍️ SQL Query using `RIGHT OUTER JOIN`:
```sql
SELECT 
    d.dept_id,
    d.dept_name,
    e.emp_id,
    COALESCE(e.emp_name, 'NO ASSIGNED EMPLOYEE') AS employee_name,
    COALESCE(e.salary, 0.00) AS employee_salary
FROM employees e
RIGHT OUTER JOIN departments d 
    ON e.dept_id = d.dept_id
ORDER BY d.dept_id ASC, e.emp_id ASC;
```

#### 🔍 Spoken Explanation & Query Analysis for the Interviewer:
> "In this query:
> 1. `departments` is the **Right Table** (the preserved table), and `employees` is the **Left Table**.
> 2. The `RIGHT OUTER JOIN` guarantees that **every single department record is preserved in the final result set**, regardless of whether any employee is assigned to that department (`e.dept_id = d.dept_id`).
> 3. If a newly created department (e.g., *Research & Development*) has zero employees, all employee fields (`e.emp_id`, `e.emp_name`, `e.salary`) evaluate to `NULL`.
> 4. We use `COALESCE(e.emp_name, 'NO ASSIGNED EMPLOYEE')` to gracefully replace `NULL` values with clean default placeholders for UI reports.
> 5. In real projects, `LEFT JOIN` is more commonly written for readability (putting `departments` on the left), but `RIGHT JOIN` is semantically identical when tables are inverted."

---

### 6. What is an SQL Execution Plan?
#### 🎙️ 60-Second Verbal Script
> "An **Execution Plan** is the detailed sequence of operations generated by the database **Query Optimizer** to execute an SQL statement in the most cost-effective manner.
>
> It breaks down:
> 1. **Scan Strategies**: **Sequential Scan** (full table scan), **Index Scan** (B-Tree lookup), **Index Only Scan** (all fields in index, zero heap fetch), **Bitmap Index Scan**.
> 2. **Join Algorithms**: **Nested Loop** (small outer table, indexed inner), **Hash Join** (in-memory hash table for larger sets), **Merge Join** (both inputs sorted).
> 3. **Cost Estimates**: Estimated startup and total I/O cost (`cost=0.00..45.20`)."

---

### 7. Analyzing Queries with `EXPLAIN` vs `EXPLAIN ANALYZE`
#### 🎙️ 60-Second Verbal Script
> "- **`EXPLAIN`**: Shows the optimizer's **estimated cost plan** without actually executing the query.
> - **`EXPLAIN (ANALYZE, BUFFERS)`**: **Actually executes the query** and outputs the true runtime statistics: actual execution time in milliseconds, actual rows returned vs estimated rows, memory usage, and **shared buffer cache hits vs disk reads**.
>
> When tuning a slow query, we look for:
> 1. **Seq Scan on large tables**: Indicates missing or non-selective indexes.
> 2. **Row count discrepancy**: If estimated rows differ drastically from actual rows, table statistics are stale (fix with `ANALYZE table_name;`).
> 3. **High disk read buffers**: Indicates the working set does not fit in `shared_buffers`."

```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, COSTS)
SELECT d.dept_name, count(e.emp_id)
FROM departments d
LEFT JOIN employees e ON d.dept_id = e.dept_id
GROUP BY d.dept_name;
```
