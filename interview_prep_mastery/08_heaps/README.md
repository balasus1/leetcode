# Section 8: Heaps & Priority Queues Mastery Guide

A **Heap** is a complete binary tree serialized in a 1D array where every parent node satisfies the **Heap-Order Property**:
- **Min-Heap**: $\text{Parent} \le \text{Children}$ (Root is global minimum).
- **Max-Heap**: $\text{Parent} \ge \text{Children}$ (Root is global maximum).

---

## Array Representation Index Invariants (0-Indexed)

For any node at index $i$:
- **Parent Index**: $\lfloor (i - 1) / 2 \rfloor$
- **Left Child Index**: $2i + 1$
- **Right Child Index**: $2i + 2$
- **Last Non-Leaf Node**: $\lfloor n/2 \rfloor - 1$

---

## Heap Operation Complexities & Cheat Sheet

| Operation | Description | Time Complexity | Auxiliary Space |
| :--- | :--- | :--- | :--- |
| **`peek()` / `findMin()` / `findMax()`** | Inspect root element | $O(1)$ | $O(1)$ |
| **`insert(val)` / `heapifyUp()`** | Sift up to restore heap invariant | $O(\log n)$ | $O(1)$ |
| **`poll()` / `extractMin()` / `heapifyDown()`** | Replace root with last element and sift down | $O(\log n)$ | $O(1)$ |
| **`buildHeap()` / Array Heapify** | Bottom-up heapify from last non-leaf node down to 0 | $O(n)$ ! | $O(1)$ in-place |
| **Convert Max-Heap to Min-Heap** | In-place min-heapify starting from $\lfloor n/2 \rfloor - 1$ down to 0 | $O(n)$ | $O(1)$ in-place |
| **Top K Largest Elements** | Size-$K$ Min-Heap | $O(n \log k)$ | $O(k)$ |
| **Top K Smallest Elements** | Size-$K$ Max-Heap | $O(n \log k)$ | $O(k)$ |

---

## Complete Problem Catalog in Java

1. **Max-Heap Implementation**: `insert`, `poll`, `peek`, `heapifyUp`, `heapifyDown`.
2. **Min-Heap Implementation**: Array-based complete min-heap with dynamic resizing.
3. **Challenge 1: Convert a Max-Heap to a Min-Heap**: In-place $O(n)$ min-heapify algorithm.
4. **Challenge 2: Find the K Smallest Elements in an Array**: $O(n \log k)$ using Max-Heap of size $K$.
5. **Challenge 3: Find the K Largest Elements in an Array**: $O(n \log k)$ using Min-Heap of size $K$.
6. **Heap Interview Questions & Tradeoffs**: Heap Sort vs QuickSort vs MergeSort, Median of a Data Stream (Two Heaps).
