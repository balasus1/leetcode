# Section 5: Stacks & Queues Mastery & Pattern Guide

Stacks (LIFO: Last-In-First-Out) and Queues (FIFO: First-In-First-Out) are fundamental abstract data types used extensively in parsing, graph traversals (DFS/BFS), monotonic sequences, buffering, and scheduling.

---

## Core Comparison: Stack vs Queue

| Feature | Stack (LIFO) | Queue (FIFO) |
| :--- | :--- | :--- |
| **Primary Operations** | `push(x)`, `pop()`, `peek()` | `offer(x)`, `poll()`, `peek()` |
| **Time Complexity** | $O(1)$ for all core operations | $O(1)$ for all core operations |
| **Implementation** | Resizable Array or Singly Linked List (Head) | Circular Array or Doubly Linked List / Tail pointer |
| **Primary Use Cases** | Function call stack, undo/redo, expression evaluation, DFS, monotonic stack | Task scheduling, breadth-first search (BFS), streaming buffers |

---

## Key Algorithmic Patterns & Cheat Sheet

1. **Monotonic Stack**:
   - Maintains elements in strictly increasing or decreasing order.
   - Finds the **Next Greater Element**, **Previous Greater Element**, **Largest Rectangle in Histogram** in $O(n)$ time.
2. **Min Stack / Max Stack in $O(1)$**:
   - **Approach 1 (Two Stacks)**: Main stack + auxiliary min stack.
   - **Approach 2 (Value Encoding)**: Store $2 \times val - minVal$ when a new minimum is encountered to achieve $O(1)$ extra space!
3. **Queue-Based BFS Generation**:
   - Generates binary representations, level orders, permutations in $O(n)$ time.
4. **Elimination / Reduction using Stack**:
   - **Celebrity Problem**: In $O(n)$ time, pop 2 candidates, test relation, discard one.
5. **Two Stacks in One Array**:
   - Grow Stack 1 from left ($0 \to \text{right}$) and Stack 2 from right ($N-1 \to \text{left}$) to optimize space utilization.
