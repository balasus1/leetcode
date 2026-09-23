# Section 3: Linked Lists Mastery & Pattern Guide

Linked lists are linear data structures where elements are non-contiguous in memory and nodes are linked via pointers (`next`, and optionally `prev`).

---

## Linked Lists vs Arrays Comparison

| Property | Array | Singly Linked List | Doubly Linked List |
| :--- | :--- | :--- | :--- |
| **Random Access** | $O(1)$ by index | $O(n)$ traversal | $O(n)$ traversal |
| **Insertion / Deletion at Head** | $O(n)$ (elements must shift) | $O(1)$ | $O(1)$ |
| **Insertion / Deletion at Tail** | $O(1)$ amortized (ArrayList) | $O(1)$ with tail pointer | $O(1)$ with tail pointer |
| **Insertion / Deletion in Middle**| $O(n)$ shift | $O(1)$ given reference to node | $O(1)$ given reference to node |
| **Memory Overhead** | Low (contiguous memory) | Medium (1 pointer per node) | High (2 pointers per node) |
| **Cache Locality** | Excellent (CPU prefetching) | Poor (scattered heap memory) | Poor (scattered heap memory) |

---

## Core Linked List Algorithmic Patterns

1. **Dummy Head Technique (Sentinel Node)**: Eliminates edge cases when inserting or deleting the head node.
2. **Fast & Slow Pointers (Floyd's Tortoise & Hare)**:
   - Cycle detection ($O(n)$ time, $O(1)$ space)
   - Finding middle node ($2\times$ speed vs $1\times$ speed)
   - Finding cycle start node
3. **In-Place Pointer Reversal**:
   - Iterative 3-pointer (`prev`, `curr`, `next`) reversal
   - Sub-list reversal between indices $L$ and $R$
   - K-group reversal
4. **Two Pointers with Fixed Offset (Nth Node from End)**:
   - Fast pointer advances $N$ steps ahead, then both move in lockstep.
5. **Priority Queue / Min-Heap for Multi-List Merging**:
   - Merge $K$ sorted lists in $O(N \log K)$ time where $N$ is total nodes.

---

## Complete Problem Catalog

### Singly Linked List Core & Challenges 1–10
- Singly Linked List Node and Core Operations
- Challenge 1: Insert at Head & Insert at End
- Challenge 2: Search in Singly Linked List
- Challenge 3: Delete by Value
- Challenge 4: Find Length (Iterative & Recursive)
- Challenge 5: Reverse a Linked List (In-Place)
- Challenge 6: Detect Loop (Floyd's Cycle Finding)
- Challenge 7: Find Middle Node of Linked List
- Challenge 8: Remove Duplicates (Hash Set & In-Place)
- Challenge 9: Union and Intersection of Two Linked Lists
- Challenge 10: Return Nth Node from End

### Doubly Linked List & Challenge 11
- Doubly Linked List (Head & Tail pointers)
- Challenge 11: Palindrome Verification in Doubly Linked List ($O(n)$ time, $O(1)$ space)

### Advanced Linked List Patterns
- Intersection Point of Two Linked Lists (Two-pointer length alignment)
- Rotate a Linked List by $K$ places
- Reverse Alternative $K$ Nodes in a Singly Linked List
- Add Two Integers Represented by Linked Lists (Handling carry)
- Reverse a Sub-list between positions $L$ and $R$
- Reverse every $K$-element Sub-list
- Merge $K$ Sorted Lists (Min-Heap)
- $K$-th Smallest Number in $M$ Sorted Lists
