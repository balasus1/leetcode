# Section 7: Trie (Prefix Tree) Mastery Guide

A **Trie** (derived from re**trie**val) is an efficient tree-like data structure used for storing and searching dynamic sets of strings, prefixes, autocomplete suggestions, IP routing tables, and dictionary validations.

---

## Trie vs Hash Table Comparison

| Feature | Trie (Prefix Tree) | Hash Table / HashSet |
| :--- | :--- | :--- |
| **Search Time** | $O(L)$ where $L$ is word length | $O(L)$ average (computing hash), $O(L \cdot N)$ worst case collision |
| **Prefix Search / Autocomplete**| $O(P)$ prefix lookup + $O(\text{results})$ | $O(N \cdot L)$ scan over all keys |
| **Alphabetical Sorting** | Natural lexicographical traversal ($O(N \cdot L)$) | Requires full sort $O(N \log N \cdot L)$ |
| **Space Complexity** | $O(\text{ALPHABET\_SIZE} \times N \times L)$ (shared prefixes save space) | $O(N \times L)$ (independent entries) |

---

## Complete Problem Catalog in Java

1. **Trie Node Structure & Core Operations**: `insert`, `search`, `startsWith`, and recursive `delete`.
2. **Challenge 1: Total Number of Words in a Trie**: Recursive / iterative counting of marked terminal nodes.
3. **Challenge 2: Find All Words in a Trie**: DFS preorder traversal collecting all complete words.
4. **Challenge 3: Sort an Array of Strings using a Trie**: Lexicographical trie insertion followed by in-order collection.
5. **Challenge 4: Word Formation from a Dictionary**: Check if a word can be formed by concatenating two other dictionary words.
6. **Trie Interview Probing Questions**: Memory optimization with HashMaps vs Fixed Array `children[26]`, Compressed Tries (Radix Tree), Ternary Search Trees.
