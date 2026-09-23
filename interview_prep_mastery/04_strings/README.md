# Section 4: String Algorithms & Dynamic Programming Mastery

Strings in Java are immutable sequence objects backed by `byte[]` / `char[]`. String interview questions evaluate two pointers, sliding window frequency maps, trie lookups, and 2D dynamic programming matrices.

---

## String Patterns & Interview Cheat Sheet

| Pattern | Key Intuition | Time / Space | Representative Problems |
| :--- | :--- | :--- | :--- |
| **Two Pointers (Expanding from Center)** | Palindrome detection, odd vs even length centers | $O(n^2)$ / $O(1)$ | Longest Palindromic Substring, Count Palindromic Substrings |
| **Sliding Window (Frequency Map)** | Tracking unique characters, character replacement count | $O(n)$ / $O(1)$ to $O(K)$ | Fruits into Baskets, Longest K Distinct Chars, Same Letters after Replacement |
| **Dynamic Programming (2D Table)** | Matching subsequences, edits, transformations | $O(m \cdot n)$ / $O(m \cdot n)$ | Longest Common Subsequence, Longest Common Substring, Regex Matching |
| **Backtracking / DFS** | Generating combinations, abbreviations, word break, boggle | $O(2^n)$ or $O(4^L)$ | Generalized Abbreviations, Generate Balanced Parens, Boggle |
| **Parser / Stack** | XML / JSON / Nested Tag parsing | $O(n)$ / $O(n)$ | XML to Tree, Balanced Parentheses |

---

## Complete Problem Catalog

### Part 1: String Fundamentals & Palindromes
1. **Common String Methods**: In-place reversals, StringBuilder vs String, ASCII lookup.
2. **Reverse Words in a Sentence**: In-place word token reversal.
3. **Remove Duplicates from a String**: LinkedHashSet / Bitmask for $O(n)$ deduplication.
4. **Remove White Spaces**: In-place fast/slow pointer compaction.
5. **Find All Palindrome Substrings**: Center expansion collection.
6. **Longest Palindromic Substring**: Optimal $O(n^2)$ time and $O(1)$ auxiliary space.
7. **Longest Palindromic Subsequence**: 2D DP matrix $O(n^2)$.
8. **Count of Palindromic Substrings**: Center expansion count.
9. **Minimum Deletions to Make String Palindrome**: Length minus LPS ($n - \text{LPS}(s)$).
10. **Palindromic Partitioning (Min Cuts)**: Dynamic programming cut optimization.

### Part 2: Advanced DP, Sliding Window & Combinatorics
11. **Word Break Problem**: 1D DP memoization ($O(n^2)$ time).
12. **XML to Tree Parser**: Tag tokenizer using Stack and N-ary Tree Node.
13. **Regular Expression Matching (`.` and `*`)**: 2D Boolean DP.
14. **Longest Common Substring**: Continuous diagonal match DP.
15. **Longest Common Subsequence (LCS)**: 2D DP matching.
16. **Min Deletions & Insertions to Transform String A to B**: $O(m + n - 2 \cdot \text{LCS})$.
17. **Fruits into Baskets (Max 2 Distinct Characters)**: Sliding window with hash map.
18. **Longest Substring with max K Distinct Characters**: Dynamic sliding window.
19. **String Permutations by Changing Case**: BFS / Backtracking recursion.
20. **Balanced Parentheses Validator**: Stack matching.
21. **Unique Generalized Abbreviations**: Bit manipulation / Backtracking recursion.
22. **Longest Substring with Distinct Characters (No Duplicates)**: Sliding window with last seen indices.
23. **Longest Substring with Same Letters After Replacement**: Sliding window with max repeating frequency.
24. **Boggle (Word Search in 2D Character Board)**: Backtracking DFS + Visited matrix.
25. **Generate All Combinations of Balanced Parentheses**: Catalan number recursive backtracking.
