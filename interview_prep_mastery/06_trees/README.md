# Section 6: Binary Trees & BST Mastery Guide

Trees are hierarchical, non-linear data structures. Binary Trees have at most 2 children per node, and Binary Search Trees (BST) maintain the invariant that for every node:
$$\text{All values in Left Subtree} < \text{Node Value} < \text{All values in Right Subtree}$$

---

## Tree Traversal Complexities & Cheat Sheet

| Traversal Type | Order | Primary Technique | Time / Space |
| :--- | :--- | :--- | :--- |
| **In-Order** | Left $\to$ Root $\to$ Right | Recursive / Stack (Iterative) / Iterator | $O(n)$ / $O(h)$ |
| **Pre-Order** | Root $\to$ Left $\to$ Right | Recursive / Stack / Serialization | $O(n)$ / $O(h)$ |
| **Post-Order** | Left $\to$ Right $\to$ Root | Bottom-up evaluation / Tree deletion / Zero sum | $O(n)$ / $O(h)$ |
| **Level-Order (BFS)**| Level by level (Top $\to$ Bottom) | Queue (FIFO) | $O(n)$ / $O(w)$ |
| **Boundary (Perimeter)**| Left boundary + Leaves + Right boundary | Directional DFS | $O(n)$ / $O(h)$ |
| **Morris Traversal** | Threaded Binary Tree | In-order pointer threading | $O(n)$ / $O(1)$ auxiliary! |

---

## Complete Problem Catalog in Java

1. **Binary Tree Implementation**: Core node representation with left/right/next pointers.
2. **Check if Two Binary Trees are Identical**: Structural and value equality via recursive DFS.
3. **Write an In-Order Iterator**: State-machine Iterator with $O(h)$ memory and $O(1)$ amortized `next()`.
4. **Iterative In-order Traversal**: Explicit stack traversal without recursion.
5. **In-order Successor in BST**: $O(h)$ search without parent pointers.
6. **In-order Successor with Parent Pointers**: Deepest leftmost child or first ancestor where current is in left subtree.
7. **Level Order Traversal**: BFS queue partitioning by level size.
8. **Validate Binary Search Tree**: Range boundary recursion $(min, max)$ avoiding integer bounds traps.
9. **Convert Binary Tree to Doubly Linked List**: In-order linking in $O(1)$ extra space.
10. **Print Tree Perimeter (Boundary Traversal)**: Root + Left Boundary + Leaf Nodes + Right Boundary (bottom-up).
11. **Connect Same Level Siblings**: `next` pointer level-by-level linking using BFS queue.
12. **Connect All Siblings**: Linking all tree nodes in sequential level-order traversal.
13. **Serialize & Deserialize Binary Tree**: Pre-order traversal with `#` null markers and queue reconstruction.
14. **Nth Highest Number in BST**: Reverse in-order traversal (Right $\to$ Root $\to$ Left).
15. **Mirror Binary Tree Nodes**: Swapping left and right child pointers at every level.
16. **Delete Zero Sum Sub-Trees**: Post-order subtree evaluation and detachment.
17. **Convert N-ary Tree to Binary Tree**: Left-Child Right-Sibling representation and inverse conversion.
