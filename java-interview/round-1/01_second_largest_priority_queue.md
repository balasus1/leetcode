# 01. Find the Second-Largest Element using a Priority Queue

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "To find the second-largest element in an array using a Priority Queue, we have two primary approaches: a Max-Heap and a bounded Min-Heap.
>
> The most optimal approach is using a **Min-Heap of fixed size 2** (or size $K$ for the $K$-th largest). As we iterate through the array, we insert unique elements into the Min-Heap. If the heap size exceeds 2, we poll the smallest element. After one pass, the top of the Min-Heap is guaranteed to be the second-largest element.
>
> This gives us an optimal time complexity of **$O(N \log K)$** — which simplifies to linear **$O(N)$** when $K=2$ — and a tiny space complexity of **$O(1)$** auxiliary space because the heap holds at most 2 elements.
>
> In production code, we must explicitly handle edge cases such as duplicate maximum elements (e.g., `[10, 10, 9]`) by using a `Set` or skipping duplicates, as well as validating arrays with fewer than 2 unique elements."

---

## 🧠 Key Technical Bullets
- **Heap Type Choice**: Min-Heap (`PriorityQueue<Integer>`) keeps the largest elements, discarding smaller ones at the root.
- **Handling Duplicates**: If distinct second-largest is needed (e.g. in `[10, 10, 9]`, second largest is `9`), track visited elements in a `HashSet` or filter before offering to the heap.
- **Time Complexity**: $O(N \log 2) = O(N)$ time.
- **Space Complexity**: $O(1)$ auxiliary space for $K=2$.

---

## 💻 Production-Grade Working Java Code

```java
package round1;

import java.util.*;

public class SecondLargestPriorityQueue {

    /**
     * Finds the 2nd distinct largest element using a bounded Min-Heap of size 2.
     * Time Complexity: O(N)
     * Space Complexity: O(1) auxiliary heap space
     */
    public static Optional<Integer> findSecondLargestDistinct(int[] nums) {
        if (nums == null || nums.length < 2) {
            return Optional.empty();
        }

        // Min-Heap (natural ordering keeps smallest at top)
        PriorityQueue<Integer> minHeap = new PriorityQueue<>(2);
        Set<Integer> seen = new HashSet<>();

        for (int num : nums) {
            if (seen.add(num)) { // Only process distinct elements
                minHeap.offer(num);
                if (minHeap.size() > 2) {
                    minHeap.poll(); // Evict smallest element
                }
            }
        }

        return minHeap.size() == 2 ? Optional.of(minHeap.peek()) : Optional.empty();
    }

    public static void main(String[] args) {
        int[] test1 = {12, 35, 1, 10, 34, 1};
        int[] test2 = {10, 10, 10};
        int[] test3 = {5, 20, 20, 17, 8};

        System.out.println("Test 1 Result: " + findSecondLargestDistinct(test1).orElse(-1)); // Expected: 34
        System.out.println("Test 2 Result: " + findSecondLargestDistinct(test2).orElse(-1)); // Expected: -1 (no 2nd distinct)
        System.out.println("Test 3 Result: " + findSecondLargestDistinct(test3).orElse(-1)); // Expected: 17
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "Can you do this in $O(N)$ time without any extra heap or set overhead?"
**Answer:**
> "Yes! By maintaining two primitive variables: `firstMax` and `secondMax` initialized to `Integer.MIN_VALUE`. We scan the array once. If `num > firstMax`, `secondMax = firstMax` and `firstMax = num`. Else if `num > secondMax && num < firstMax`, `secondMax = num`. This runs in $O(N)$ time with strictly $O(1)$ space and zero object allocation overhead."

### 2. "Why use PriorityQueue over simple variables if $K=2$?"
**Answer:**
> "While two variables are optimal for $K=2$, the PriorityQueue solution is **extensible to finding the $K$-th largest element** in a streaming data scenario where $K$ is dynamic or large, maintaining a clean $O(N \log K)$ bound."
