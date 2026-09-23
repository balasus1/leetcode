# Section 1: Complexity Analysis & Big-O Mastery

Understanding computational complexity is the foundational skill tested in every FAANG/Tier-1 technical interview. Interviewers expect you to proactively state Time and Space complexity for both brute-force and optimal solutions, derive them using mathematical invariants, and discuss trade-offs (e.g., CPU cache locality, auxiliary memory vs. in-place modification).

---

## 1. Introduction to Asymptotic Analysis & Big-O

### The Big-Three Asymptotic Notations
1. **Big O ($O$) - Asymptotic Upper Bound**: Represents the worst-case scenario (upper limit on growth rate). E.g., $f(n) = O(g(n))$ if $\exists c > 0, n_0 \ge 0$ such that $f(n) \le c \cdot g(n)$ for all $n \ge n_0$.
2. **Big Omega ($\Omega$) - Asymptotic Lower Bound**: Represents the best-case scenario.
3. **Big Theta ($\Theta$) - Asymptotic Tight Bound**: When an algorithm's upper and lower bounds grow at the same rate ($f(n) = \Theta(g(n)) \iff f(n) = O(g(n)) \text{ and } f(n) = \Omega(g(n))$).

### Common Complexity Classes (Ordered from Fastest to Slowest)
| Notation | Name | Typical Example |
| :--- | :--- | :--- |
| $O(1)$ | Constant | Array index lookup, Hash table lookup (average), push/pop on stack |
| $O(\log n)$ | Logarithmic | Binary search, balanced BST search, GCD Euclidean algorithm |
| $O(\sqrt{n})$ | Square Root | Primality test, integer factorization |
| $O(n)$ | Linear | Single pass scan, string reversal, array search |
| $O(n \log n)$ | Linearithmic | Merge Sort, QuickSort (average), Heap Sort |
| $O(n^2)$ | Quadratic | Nested loops, Bubble/Selection/Insertion sort, naive pairs comparison |
| $O(n^3)$ | Cubic | Matrix multiplication (naive), all triplets search |
| $O(2^n)$ | Exponential | Recursive Fibonacci, generating all subsets (power set) |
| $O(n!)$ | Factorial | Generating all string permutations, Traveling Salesperson (brute-force) |

---

## 2. Deep Dive: Nested Loop Challenges (Mathematical Derivations)

### Challenge 1: Nested Loop with Addition
```java
for (int i = 0; i < n; i++) {
    for (int j = 0; j < n; j++) {
        // O(1) statement
    }
}
```
- **Derivation**: Outer loop executes $n$ times. For every iteration of $i$, inner loop executes $n$ times.
$$\text{Total operations} = \sum_{i=0}^{n-1} n = n \times n = n^2 \implies \mathbf{O(n^2)}$$

---

### Challenge 2: Nested Loop with Subtraction (Dependent Bound)
```java
for (int i = 0; i < n; i++) {
    for (int j = i; j < n; j++) {
        // O(1) statement
    }
}
```
- **Derivation**: When $i = 0$, inner loop runs $n$ times; when $i = 1$, $n-1$ times $\dots$ when $i = n-1$, $1$ time.
$$\text{Total operations} = n + (n-1) + (n-2) + \dots + 1 = \frac{n(n+1)}{2} = \frac{n^2}{2} + \frac{n}{2} \implies \mathbf{O(n^2)}$$

---

### Challenge 3: Nested Loop with Multiplication (Logarithmic Inner Loop)
```java
for (int i = 1; i <= n; i++) {
    for (int j = 1; j <= n; j *= 2) {
        // O(1) statement
    }
}
```
- **Derivation**: Outer loop runs $n$ times. In the inner loop, $j$ takes values $1, 2, 4, 8, \dots, 2^k \le n$. Thus $k = \lfloor \log_2 n \rfloor + 1$ iterations.
$$\text{Total operations} = n \times (\log_2 n + 1) \implies \mathbf{O(n \log n)}$$

---

### Challenge 4: Nested Loop with Multiplication - Basic (Logarithmic Outer & Inner)
```java
for (int i = 1; i <= n; i *= 2) {
    for (int j = 1; j <= n; j *= 2) {
        // O(1) statement
    }
}
```
- **Derivation**: Outer loop runs $\log_2 n$ times. Inner loop runs $\log_2 n$ times for each outer step.
$$\text{Total operations} = (\log_2 n) \times (\log_2 n) = (\log_2 n)^2 \implies \mathbf{O(\log^2 n)}$$

---

### Challenge 5: Nested Loop with Multiplication - Intermediate (Dependent Multiplication)
```java
for (int i = 1; i <= n; i *= 2) {
    for (int j = 1; j <= i; j++) {
        // O(1) statement
    }
}
```
- **Derivation**: $i$ takes values $1, 2, 4, 8, \dots, 2^k$ where $2^k \le n$. The inner loop runs $i$ times.
$$\text{Total operations} = 1 + 2 + 4 + 8 + \dots + 2^k = 2^{k+1} - 1 < 2 \cdot n \implies \mathbf{O(n)}$$
*Key Interview Insight*: Even though there is a nested loop, the geometric series sum converges to $2n - 1$, making it linear $O(n)$, NOT $O(n \log n)$.

---

### Challenge 6: Nested Loop with Multiplication - Advanced (Inverse Step)
```java
for (int i = n; i > 0; i /= 2) {
    for (int j = 0; j < i; j++) {
        // O(1) statement
    }
}
```
- **Derivation**: $i$ starts at $n$, then $n/2, n/4, \dots, 1$.
$$\text{Total operations} = n + \frac{n}{2} + \frac{n}{4} + \dots + 1 = n \left(1 + \frac{1}{2} + \frac{1}{4} + \dots\right) \le 2n \implies \mathbf{O(n)}$$

---

### Challenge 7: Nested Loop with Multiplication - Pro (Square Step)
```java
for (int i = 2; i <= n; i = i * i) {
    for (int j = 1; j <= n; j++) {
        // O(1) statement
    }
}
```
- **Derivation**: $i$ sequences as $2, 2^2, 2^4, 2^8, \dots, 2^{2^k} \le n$. Taking logarithm twice: $2^k \le \log_2 n \implies k \le \log_2(\log_2 n)$.
- Outer loop runs $O(\log \log n)$ times. Inner loop runs $n$ times.
$$\text{Total operations} = n \cdot \log(\log n) \implies \mathbf{O(n \log \log n)}$$

---

## 3. Complexity Interview Cheat Sheet

### Common Algorithm Complexities
- **Array Access / Modification**: $O(1)$ Time, $O(1)$ Space
- **Binary Search**: $O(\log n)$ Time, $O(1)$ Iterative / $O(\log n)$ Recursive Space
- **Merge Sort**: $O(n \log n)$ Time (Best/Avg/Worst), $O(n)$ Auxiliary Space
- **QuickSort**: $O(n \log n)$ Avg, $O(n^2)$ Worst (pivot imbalance), $O(\log n)$ call stack space
- **Tree Traversals (DFS/BFS)**: $O(V + E)$ or $O(N)$ Time, $O(H)$ or $O(W)$ Space
- **Graph BFS / DFS (Adj List)**: $O(V + E)$ Time, $O(V)$ Space
- **Dijkstra with Min-Heap**: $O((V + E) \log V)$ Time, $O(V)$ Space
- **Topological Sort**: $O(V + E)$ Time, $O(V)$ Space
- **Subsets / Combinations ($2^n$)**: $O(n \cdot 2^n)$ Time & Space
- **Permutations ($n!$)**: $O(n \cdot n!)$ Time & Space

---

## 4. Complexity Interview Questions & Probing Scenarios

1. **Question**: *"What is the time and space complexity of String concatenation in a loop `s += c` vs `StringBuilder`?"*
   - **Answer**: `s += c` creates a new String of length $k$ each iteration $\sum_{k=1}^n k = O(n^2)$ time. `StringBuilder` uses amortized doubling array resize, giving $O(n)$ total time.
2. **Question**: *"Can recursive algorithms have $O(1)$ auxiliary space?"*
   - **Answer**: In Java, no (unless transformed into iteration or tail-call optimized in specific runtimes), because each activation frame consumes stack memory proportional to recursion depth $O(d)$.
3. **Question**: *"How does Amortized $O(1)$ work in `ArrayList` resizing?"*
   - **Answer**: When the array fills up at capacity $C$, a new array of size $2C$ is allocated and elements copied. Resizing costs $1 + 2 + 4 + \dots + n = 2n = O(n)$ total across $n$ insertions, yielding $\frac{O(n)}{n} = O(1)$ amortized cost per insertion.
