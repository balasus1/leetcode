# LeetCode patterns & daily practice

## Following the advice mentioned in # https://blog.algomaster.io/p/15-leetcode-patterns

### Before that I am understanding DSA from Bro code https://www.youtube.com/watch?v=CBYHwZcbD-s in 4 hrs.  Skipping and skimming few of the concepts which I already knew to complete them in 2 hrs.

- Big O notation:
  ![Common DS - Big-O notation](https://miro.medium.com/v2/resize:fit:1400/format:webp/1*cQ78W0R0qxaSgYLosfYMxg.png)
    - O(1) : constant time
      - map fetch, array element fetch, insert at start/end in linkedlist
    - O(log n): logarithmic time
      - binary search
    - O(n): linear time
      - array look-up, search in linkedlist
    - O(n log n): Quasilinear time
      - quicksort, mergesort, heapsort
    - O(n^2): quadratic time
      - insertion sort, selection sort, bubblesort 
    - O(n!): factorial time
      - find permutation of given list, travelling salesman problem
- Stack:
    - LIFO (Last In. First Out)
    - push(), pop(), peek(), isEmpty(), search()
    - push elements with millions will result in Java heap OOM error
    - Uses: undo/redo, backward/forward, backtracking, calling functions
- Queue:
    - FIFO (First-in, First-out)
    - add = enqueue, offer()
    - remove = dequeue, poll()
    - PriorityQueue: Ordered using Collections.reverseOrder() for desc in PriorityQueue constructor
    - Has properties of Collection as inherits Collection
    - Uses: Keyboard buffer, printer queue, priorityQueue, BF search
- Dynamic Array(ArrayList):
    - Linear data structure
    - Growth factor: 1.5(3/2)
    - Not efficient for insertion and deletion
    - Random access of elements O(1), constant time
    - Good locality of reference and data cache utilization
    - Easy to insert/delete at the end of the array
    - Disadvantages:wastes more memory, shifting and explanding/re-arranging elements
- LinkedList
    - Not efficient for searching, time complexity is O(n)
    - Efficient for insertion and deletion of elements, Time complexity O(1). Always constant
    - Has Deque properties. Acts as stack and queue
    - Operations: push(for stack), poll, peek(first/last), add, remove, offer(for queue)

## Plan: 

- Every day learn one or two problems and practice repeatedly using pen and paper or plain text editor
- End of 15th day, repeat the same for another 7 days
- On 23rd day, start and try solving leedCode problems
- Monitor your progress using below table.

|  #Day | #Date |  #Problem | #Practice (no.of times) | #FileName | #Feedback |
-------|-------| --- | --- |---|---|
| 1     | 07/22 |Prefix Sum|3|PrefixSum.java|Understood the approach and algorithm well |
| 2     | 07/24 |Prefix Sum|3|PrefixSum.java|Covered 3 different usecases of prefix sum |
| 3     | 12/31 |Majority Vote|3|MajorityElement.java|Covered 3 approaches. Boyer-Moore, HashMap, brute force |
