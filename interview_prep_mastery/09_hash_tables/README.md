# Section 9: Hash Tables & Hashing Patterns Mastery Guide

Hash Tables map keys to values using a hashing function $h(k) \pmod m$ to achieve $O(1)$ average time complexity for lookups, insertions, and deletions.

---

## HashMap Internals & Collision Resolution

1. **Separate Chaining**: Buckets store linked lists (or red-black trees when bucket length $> 8$ in Java 8+ `HashMap`).
2. **Open Addressing**: Linear probing, quadratic probing, or double hashing within a flat array.
3. **Load Factor & Rehashing**: Default load factor $\alpha = 0.75$. When `size > capacity * 0.75`, bucket array capacity doubles.

---

## HashMap vs HashSet vs Trie

| Feature | `HashMap<K, V>` | `HashSet<E>` | `Trie` |
| :--- | :--- | :--- | :--- |
| **Data Stored** | Key-Value mappings | Unique Keys only (backed by dummy-value HashMap) | Hierarchical prefix tree of characters |
| **Lookup Time** | $O(1)$ average, $O(L)$ worst for long keys | $O(1)$ average | $O(L)$ deterministic |
| **Prefix Matching** | No (requires full scan) | No | Yes ($O(P)$ prefix lookup) |
| **Ordering** | Unordered (or insertion order via `LinkedHashMap`) | Unordered | Lexicographical order natural |

---

## Complete Problem Catalog in Java

1. **Challenge 1: Array Subset Check**: Verify if array $B$ is a subset of array $A$ using HashSet in $O(N + M)$ time.
2. **Challenge 2: Disjoint Arrays**: Verify if two arrays share zero common elements.
3. **Challenge 3: Symmetric Pairs in an Array**: Find pairs $(a, b)$ and $(b, a)$ in $O(n)$ time using HashMap.
4. **Challenge 4: Trace Complete Path of a Journey**: Reconstruct itinerary from start to finish given unordered (from, to) tickets.
5. **Challenge 5: Find Two Pairs with Equal Sum ($a + b = c + d$)**: Map pairwise sums to pair indices in $O(n^2)$ time and space.
6. **Challenge 6: Subarray with Sum Equal to 0**: Prefix sum set tracking.
7. **Challenge 7: First Non-Repeating Integer**: Frequency map / single-pass lookup.
8. **Challenge 8: Remove Duplicates from Linked List using Hashing**: Single-pass $O(n)$ deduplication.
9. **Challenge 9: Union and Intersection of Lists using Hashing**: Linear time operations.
10. **Challenge 10: Two Numbers that Add up to $N$**: Hash set complement lookup.
