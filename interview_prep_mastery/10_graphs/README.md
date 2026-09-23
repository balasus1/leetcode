# Section 10: Graph Theory & Algorithms Mastery Guide

A **Graph** $G = (V, E)$ consists of a set of vertices $V$ and edges $E$. Graphs can be directed/undirected, weighted/unweighted, cyclic/acyclic (DAG).

---

## Graph Representations: Adjacency List vs Matrix

| Feature | Adjacency List `List<List<Integer>>` | Adjacency Matrix `int[V][V]` |
| :--- | :--- | :--- |
| **Space Complexity** | $O(V + E)$ (Optimal for sparse graphs) | $O(V^2)$ (Better for dense graphs) |
| **Add Edge** | $O(1)$ | $O(1)$ |
| **Check Edge $(u, v)$** | $O(\text{deg}(u))$ | $O(1)$ |
| **Traverse All Neighbors** | $O(\text{deg}(u))$ | $O(V)$ |

---

## Graph Algorithm Complexities & Cheat Sheet

| Algorithm | Primary Purpose | Time Complexity | Space Complexity |
| :--- | :--- | :--- | :--- |
| **Breadth-First Search (BFS)** | Shortest path in unweighted graphs, level-order | $O(V + E)$ | $O(V)$ |
| **Depth-First Search (DFS)** | Reachability, connected components, mother vertex | $O(V + E)$ | $O(V)$ |
| **Bipartite Graph (2-Coloring)**| 2-Coloring graph using BFS/DFS | $O(V + E)$ | $O(V)$ |
| **Cycle in Directed Graph** | 3-Coloring DFS / Recursion Stack tracking | $O(V + E)$ | $O(V)$ |
| **Cycle in Undirected Graph** | Union-Find / DFS with parent pointer | $O(V + E)$ | $O(V)$ |
| **Mother Vertex Discovery** | Kosaraju-inspired last finished DFS vertex | $O(V + E)$ | $O(V)$ |
| **Is Directed Graph a Tree?** | Exactly 1 root (in-degree 0), all others in-degree 1, no cycles | $O(V + E)$ | $O(V)$ |

---

## Complete Problem Catalog in Java

1. **Graph Implementation**: Doubly linked adjacency list with directed/undirected edge insertion.
2. **What is a Bipartite Graph?**: 2-Coloring BFS check.
3. **Challenge 1: Implement Breadth First Search (BFS)**: Full graph traversal handling disconnected components.
4. **Challenge 2: Implement Depth First Search (DFS)**: Recursive DFS handling disconnected components.
5. **Challenge 3: Cycle Detection in a Directed Graph**: DFS with active recursion call stack `recStack[]`.
6. **Challenge 4: Find "Mother Vertex"**: Find last finished vertex in DFS, then verify full reachability.
7. **Challenge 5: Count Edges in Undirected Graph**: Sum of all degrees divided by 2 ($\sum \text{deg}(v) / 2$).
8. **Challenge 6: Check If Path Exists Between Two Vertices**: BFS / DFS reachability query.
9. **Challenge 7: Check If Directed Graph is a Tree**: In-degree validation + single root + cycle check.
10. **Challenge 8: Shortest Path Length between Two Vertices**: BFS distance level tracking.
