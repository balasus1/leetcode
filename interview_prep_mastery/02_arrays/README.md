# Section 2: Array Mastery & Pattern Guide

Arrays are contiguous memory blocks offering $O(1)$ random access by index. Array questions test your mastery over two pointers, sliding window, prefix sums, binary search variants, in-place index manipulation, and cyclic sort.

---

## Key Array Patterns & Interview Cheat Sheet

| Pattern | When to Use | Typical Time / Space | Example Problems |
| :--- | :--- | :--- | :--- |
| **Two Pointers** (Opposite Ends) | Sorted array, searching pairs, palindromes, squaring sorted array | $O(n)$ / $O(1)$ | Two Sum II, 3Sum, Squaring Sorted Array, Container with Most Water |
| **Fast & Slow Pointers** | In-place removal, cycle detection, partitioning | $O(n)$ / $O(1)$ | Remove Even, Remove Duplicates, Move Zeros |
| **Sliding Window** (Fixed / Dynamic) | Contiguous subarrays, max/min sum of size $K$, distinct elements | $O(n)$ / $O(1)$ to $O(k)$ | Max in Sliding Window, Max Sum Subarray Size K, Smallest Subarray > S |
| **Prefix & Suffix Products / Sums** | Subarray sums, products without division | $O(n)$ / $O(n)$ or $O(1)$ | Product of Array Except Self, Subarray Sum Equals K |
| **Kadane's Algorithm** | Maximum contiguous subarray sum | $O(n)$ / $O(1)$ | Max Sum Subarray, Maximum Product Subarray |
| **Modified Binary Search** | Sorted or rotated arrays, boundary discovery | $O(\log n)$ / $O(1)$ | Search in Rotated Array, Low/High Index, Bitonic Maximum |
| **Cyclic Sort** | Numbers in range $[1 \dots n]$ or $[0 \dots n]$ | $O(n)$ / $O(1)$ | Find Missing Number, Find Duplicate, Cyclic Sort |
| **Interval Merging** | Overlapping ranges, scheduling, calendar conflicts | $O(n \log n)$ / $O(n)$ | Merge Overlapping Intervals, Insert Interval |

---

## Detailed Index of Challenges Covered in Java

### Part 1: Core Array Operations & Challenges 1–11
1. **2D Array Operations**: Traversal, Row/Col Sums, Transpose, Matrix Rotation.
2. **Challenge 1: Remove Even Integers**: In-place filtering and stream-based methods.
3. **Challenge 2: Merge Two Sorted Arrays**: In-place backwards merging and auxiliary array technique.
4. **Challenge 3: Find Two Numbers that Add up to $n$**: Hash set ($O(n)$ time) vs Two-pointer on sorted array.
5. **Challenge 4: Product of Array Except Self**: Two-pass prefix & suffix products without division in $O(n)$ time & $O(1)$ auxiliary space.
6. **Challenge 5: Find Minimum Value in Array**: Linear scan & boundary validation.
7. **Challenge 6: First Non-Repeating Integer**: Frequency map / single-pass array counting.
8. **Challenge 7: Find Second Maximum Value**: Single pass tracking $O(n)$ without full sorting.
9. **Challenge 8: Right Rotate Array by One Index / K Indices**: Reversal algorithm ($O(n)$ time, $O(1)$ space).
10. **Challenge 9: Re-arrange Positive & Negative Values**: Partitioning two-pointer strategy.
11. **Challenge 10: Rearrange Sorted Array in Max/Min Form**: Modulo arithmetic encoding trick in $O(1)$ extra space.
12. **Challenge 11: Maximum Sum Subarray (Kadane's Algorithm)**: Dynamic programming state machine $O(n)$ time.

### Part 2: Search, Windows, Intervals & Sorting
13. **Binary Search on Sorted Array**: Standard iterative & recursive implementations avoiding integer overflow.
14. **Find Maximum in Sliding Window**: Monotonic Double-Ended Queue (Deque) in $O(n)$ time.
15. **Search a Rotated Array**: Modified binary search identifying sorted half.
16. **Find Smallest Common Number in 3 Sorted Arrays**: 3-pointer simultaneous traversal in $O(n_1 + n_2 + n_3)$.
17. **Find Low/High Index of an Element**: Lower bound / Upper bound binary search.
18. **Move All Zeros to Beginning / End**: In-place two pointer swap.
19. **Stock Buy Sell to Maximize Profit**: One pass minimum price tracking (Valley-Peak).
20. **Merge Overlapping Intervals**: Sorting by start time + linear interval expansion.
21. **QuickSort Algorithm**: Lomuto and Hoare partitioning with $O(n \log n)$ average time.

### Part 3: Advanced Sliding Window, Cyclic Sort & Subsets
22. **Cyclic Sort**: Place elements at their correct index $A[i] = A[A[i]-1]$ in $O(n)$ time.
23. **Maximum Sum Subarray of Size K**: Fixed-size sliding window.
24. **Smallest Subarray With a Greater Sum**: Dynamic expandable & contractible sliding window.
25. **Squaring a Sorted Array**: Two pointers from both ends ($O(n)$ time).
26. **Subsets (Power Set)**: Cascading / Backtracking $O(n \cdot 2^n)$.
27. **Subsets With Duplicates**: Handling duplicate elements by skipping identical adjacent elements.
28. **Order-Agnostic Binary Search**: Ascending vs descending autodetection.
29. **Bitonic Array Maximum**: Peak finding in mountain arrays via binary search.
