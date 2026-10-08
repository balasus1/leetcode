# 16. Senior Live Problem Solving & Must-Know Algorithmic Patterns

The 6 highest-frequency live coding and problem-solving patterns asked in FAANG and Tier-1 Senior Java Backend interviews, featuring 60-second verbal scripts, optimal Big-O complexity, and copy-paste runnable code.

---

## 📑 Topics Index
1. [LRU Cache: $O(1)$ Doubly Linked List + HashMap](#1-lru-cache-o1-doubly-linked-list--hashmap)
2. [Sliding Window: Longest Substring Without Repeating Characters](#2-sliding-window-longest-substring-without-repeating-characters)
3. [Intervals Pattern: Merge Overlapping Intervals](#3-intervals-pattern-merge-overlapping-intervals)
4. [QuickSelect Pattern: $K$-th Largest Element in $O(N)$ Average Time](#4-quickselect-pattern-k-th-largest-element-in-on-average-time)
5. [Graph & Dependency Resolution: Microservice Circular Dependency Detection](#5-graph--dependency-resolution-microservice-circular-dependency-detection)
6. [Dynamic Programming: Coin Change (Minimum Coins)](#6-dynamic-programming-coin-change-minimum-coins)

---

### 1. LRU Cache: $O(1)$ Doubly Linked List + HashMap

#### 🎙️ 60-Second Verbal Script
> "To implement a Least Recently Used (LRU) Cache with **strictly $O(1)$ time complexity for both `get()` and `put()`**, we combine two data structures:
>
> 1. **`HashMap<K, Node>`**: Provides $O(1)$ key lookup to find the node memory reference in the heap.
> 2. **Custom Doubly Linked List with Pseudo Head and Tail**: Maintains access order. The most recently accessed node is always moved right behind `head`, while the least recently used node sits directly before `tail`.
> 3. **Operations**:
>    - **`get(key)`**: If key is present in map, detach node from current position, insert at head, and return value.
>    - **`put(key, value)`**: If key exists, update value and move to head. If new key and capacity is exceeded, evict the node right before `tail` (`tail.prev`), remove from HashMap, and insert the new node at head.
> 4. **Complexity**: $O(1)$ time for both `get` and `put`, $O(\text{capacity})$ auxiliary space."

```java
package coding;

import java.util.HashMap;
import java.util.Map;

public class LRUCache<K, V> {

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev, next;
        Node(K key, V value) { this.key = key; this.value = value; }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> map;
    private final Node<K, V> head, tail;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>();
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }

    public synchronized V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) return null;
        moveToHead(node);
        return node.value;
    }

    public synchronized void put(K key, V value) {
        Node<K, V> node = map.get(key);
        if (node != null) {
            node.value = value;
            moveToHead(node);
        } else {
            if (map.size() >= capacity) {
                Node<K, V> lru = tail.prev;
                removeNode(lru);
                map.remove(lru.key);
            }
            Node<K, V> newNode = new Node<>(key, value);
            map.put(key, newNode);
            addNode(newNode);
        }
    }

    private void addNode(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addNode(node);
    }

    public static void main(String[] args) {
        LRUCache<Integer, String> cache = new LRUCache<>(2);
        cache.put(1, "Service A");
        cache.put(2, "Service B");
        System.out.println("Get 1: " + cache.get(1)); // returns "Service A" (1 is now MRU)
        cache.put(3, "Service C");                   // evicts key 2 (LRU)
        System.out.println("Get 2 (evicted): " + cache.get(2)); // returns null
        System.out.println("Get 3: " + cache.get(3)); // returns "Service C"
    }
}
```

---

### 2. Sliding Window: Longest Substring Without Repeating Characters

#### 🎙️ 60-Second Verbal Script
> "To find the length of the longest substring without repeating characters:
>
> 1. We use a **Dynamic Sliding Window** with two pointers: `left` and `right`.
> 2. We maintain a `Map<Character, Integer>` (or an ASCII integer array of size 128) storing each character's **last seen index**.
> 3. As `right` iterates through the string, if the character was previously seen at `prevIndex >= left`, we immediately jump `left = prevIndex + 1` to skip the duplicate.
> 4. We calculate `maxLength = Math.max(maxLength, right - left + 1)` at each step and update the character's last seen index.
> 5. **Complexity**: Single pass **$O(N)$** time complexity and **$O(\min(N, M))$** space (where $M$ is charset size)."

```java
package coding;

import java.util.HashMap;
import java.util.Map;

public class LongestSubstringWithoutRepeating {

    public static int lengthOfLongestSubstring(String s) {
        if (s == null || s.isEmpty()) return 0;

        int maxLength = 0;
        int left = 0;
        Map<Character, Integer> lastSeen = new HashMap<>();

        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);

            if (lastSeen.containsKey(c)) {
                // Move left pointer past previous duplicate occurence
                left = Math.max(left, lastSeen.get(c) + 1);
            }

            lastSeen.put(c, right);
            maxLength = Math.max(maxLength, right - left + 1);
        }

        return maxLength;
    }

    public static void main(String[] args) {
        System.out.println("Result for 'abcabcbb': " + lengthOfLongestSubstring("abcabcbb")); // 3 ("abc")
        System.out.println("Result for 'bbbbb': " + lengthOfLongestSubstring("bbbbb"));       // 1 ("b")
        System.out.println("Result for 'pwwkew': " + lengthOfLongestSubstring("pwwkew"));     // 3 ("wke")
    }
}
```

---

### 3. Intervals Pattern: Merge Overlapping Intervals

#### 🎙️ 60-Second Verbal Script
> "To merge all overlapping intervals:
>
> 1. Sort the intervals by their **start time** in ascending order.
> 2. Initialize a merged list. Insert the first interval.
> 3. For each subsequent interval `curr`:
>    - Let `last` be the last interval in our merged list.
>    - If `curr.start <= last.end`, they overlap! Merge them by setting `last.end = Math.max(last.end, curr.end)`.
>    - Otherwise, they do not overlap; add `curr` to the merged list.
> 4. **Complexity**: Sorting takes **$O(N \log N)$** time; the single-pass merge takes **$O(N)$** time and **$O(N)$** space."

```java
package coding;

import java.util.*;

public class MergeIntervals {

    public static int[][] merge(int[][] intervals) {
        if (intervals == null || intervals.length <= 1) return intervals;

        // 1. Sort intervals by start time
        Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));

        List<int[]> merged = new ArrayList<>();
        int[] currentInterval = intervals[0];
        merged.add(currentInterval);

        for (int i = 1; i < intervals.length; i++) {
            int[] nextInterval = intervals[i];

            if (nextInterval[0] <= currentInterval[1]) {
                // Overlap detected: extend end time
                currentInterval[1] = Math.max(currentInterval[1], nextInterval[1]);
            } else {
                // Disjoint interval: start new tracking interval
                currentInterval = nextInterval;
                merged.add(currentInterval);
            }
        }

        return merged.toArray(new int[merged.size()][]);
    }

    public static void main(String[] args) {
        int[][] intervals = {{1, 3}, {2, 6}, {8, 10}, {15, 18}};
        int[][] result = merge(intervals);
        System.out.println("Merged Intervals: " + Arrays.deepToString(result));
        // Output: [[1, 6], [8, 10], [15, 18]]
    }
}
```

---

### 4. QuickSelect Pattern: $K$-th Largest Element in $O(N)$ Average Time

#### 🎙️ 60-Second Verbal Script
> "To find the $K$-th largest element in an unsorted array:
>
> - While a Min-Heap gives $O(N \log K)$ time, **QuickSelect** (Hoare's Selection Algorithm) achieves **$O(N)$ average time complexity and $O(1)$ auxiliary space**.
> - It utilizes the Partition algorithm from QuickSort:
>   1. We target the index `targetIndex = nums.length - K` (which represents the $K$-th largest in sorted order).
>   2. Pick a pivot and partition the array such that all elements smaller than pivot are on left, and greater on right.
>   3. If pivot index equals `targetIndex`, we found our element!
>   4. If pivot index $< \text{targetIndex}$, recurse strictly on the right subarray; otherwise recurse on left."

```java
package coding;

import java.util.Random;

public class QuickSelectKthLargest {

    private static final Random random = new Random();

    public static int findKthLargest(int[] nums, int k) {
        int targetIndex = nums.length - k;
        return quickSelect(nums, 0, nums.length - 1, targetIndex);
    }

    private static int quickSelect(int[] nums, int left, int right, int targetIndex) {
        if (left == right) return nums[left];

        // Randomize pivot to guarantee O(N) average runtime
        int pivotIdx = left + random.nextInt(right - left + 1);
        int finalPivotIdx = partition(nums, left, right, pivotIdx);

        if (finalPivotIdx == targetIndex) {
            return nums[finalPivotIdx];
        } else if (finalPivotIdx < targetIndex) {
            return quickSelect(nums, finalPivotIdx + 1, right, targetIndex);
        } else {
            return quickSelect(nums, left, finalPivotIdx - 1, targetIndex);
        }
    }

    private static int partition(int[] nums, int left, int right, int pivotIdx) {
        int pivotValue = nums[pivotIdx];
        swap(nums, pivotIdx, right);
        int storeIdx = left;

        for (int i = left; i < right; i++) {
            if (nums[i] < pivotValue) {
                swap(nums, storeIdx, i);
                storeIdx++;
            }
        }
        swap(nums, storeIdx, right);
        return storeIdx;
    }

    private static void swap(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }

    public static void main(String[] args) {
        int[] nums = {3, 2, 1, 5, 6, 4};
        System.out.println("2nd Largest Element: " + findKthLargest(nums, 2)); // Output: 5
    }
}
```

---

### 5. Graph & Dependency Resolution: Microservice Circular Dependency Detection

#### 🎙️ 60-Second Verbal Script
> "In microservice architectures and build systems, circular dependencies cause system startup deadlocks.
>
> We detect circular dependency cycles using **Topological Sorting (Kahn's Algorithm using In-Degrees / BFS)** or **DFS with 3-color cycle detection (WHITE=unvisited, GRAY=visiting, BLACK=processed)**:
>
> 1. Build an Adjacency List graph representing service calls ($A \to B$) and compute each service's **in-degree count** (number of incoming dependencies).
> 2. Enqueue all services with `in-degree == 0` (leaf dependencies with no incoming callers).
> 3. While queue is not empty, dequeue service $u$, decrement in-degrees of all downstream services $v$. If any $v$ reaches in-degree 0, enqueue it.
> 4. If the count of visited services equals total services $V$, the graph is a valid **Directed Acyclic Graph (DAG)**. If visited $< V$, a **Circular Dependency Cycle** exists!
> 5. **Complexity**: Time Complexity is **$O(V + E)$** and Space is **$O(V + E)$**."

```java
package coding;

import java.util.*;

public class MicroserviceCycleDetector {

    public static boolean hasCircularDependency(int numServices, int[][] dependencies) {
        List<List<Integer>> adj = new ArrayList<>();
        int[] inDegree = new int[numServices];

        for (int i = 0; i < numServices; i++) {
            adj.add(new ArrayList<>());
        }

        // dependencies[i] = [ServiceA, ServiceB] (Service A calls Service B: A -> B)
        for (int[] dep : dependencies) {
            int from = dep[0];
            int to = dep[1];
            adj.get(from).add(to);
            inDegree[to]++;
        }

        // Enqueue nodes with 0 in-degree
        Queue<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < numServices; i++) {
            if (inDegree[i] == 0) queue.offer(i);
        }

        int processedNodes = 0;
        while (!queue.isEmpty()) {
            int curr = queue.poll();
            processedNodes++;

            for (int neighbor : adj.get(curr)) {
                inDegree[neighbor]--;
                if (inDegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // If processed nodes < total nodes, a circular cycle was trapped!
        return processedNodes != numServices;
    }

    public static void main(String[] args) {
        // Example 1: Valid DAG (0 -> 1 -> 2)
        int[][] validDeps = {{0, 1}, {1, 2}};
        System.out.println("Cycle in validDeps? " + hasCircularDependency(3, validDeps)); // false

        // Example 2: Circular Dependency (0 -> 1 -> 2 -> 0)
        int[][] cycleDeps = {{0, 1}, {1, 2}, {2, 0}};
        System.out.println("Cycle in cycleDeps? " + hasCircularDependency(3, cycleDeps)); // true
    }
}
```

---

### 6. Dynamic Programming: Coin Change (Minimum Coins)

#### 🎙️ 60-Second Verbal Script
> "The **Coin Change Problem** asks for the fewest number of coins needed to make up a given amount.
>
> 1. We solve this using **Bottom-Up Dynamic Programming**:
> 2. Define `dp[i]` as the minimum coins needed to make amount `i`. Initialize array with `amount + 1` (acting as infinity), with `dp[0] = 0`.
> 3. For each sub-amount $i$ from 1 to `amount`, and for each coin in our array:
>    $$\text{if } (i - \text{coin} \ge 0) \implies dp[i] = \min(dp[i], dp[i - \text{coin}] + 1)$$
> 4. If `dp[amount] > amount`, return `-1` (impossible); otherwise return `dp[amount]`.
> 5. **Complexity**: Time Complexity is **$O(\text{amount} \times N)$** where $N$ is coin denominations count, Space Complexity is **$O(\text{amount})$**."

```java
package coding;

import java.util.Arrays;

public class CoinChangeMin {

    public static int coinChange(int[] coins, int amount) {
        if (amount < 0) return -1;
        if (amount == 0) return 0;

        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1); // Sentinel for infinity
        dp[0] = 0;

        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (i - coin >= 0) {
                    dp[i] = Math.min(dp[i], dp[i - coin] + 1);
                }
            }
        }

        return dp[amount] > amount ? -1 : dp[amount];
    }

    public static void main(String[] args) {
        int[] coins = {1, 2, 5};
        System.out.println("Min coins for amount 11: " + coinChange(coins, 11)); // Output: 3 (5 + 5 + 1)
        System.out.println("Min coins for amount 3 (coins=[2]): " + coinChange(new int[]{2}, 3)); // Output: -1
    }
}
```
