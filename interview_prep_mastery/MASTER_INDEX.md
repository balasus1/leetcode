# 🚀 Master Coding Interview Preparation Curriculum & DSA Playbook

A structured, FAANG/Tier-1 interview preparation curriculum. Every single topic includes deep conceptual foundations, time & space complexity proofs, brute-force vs. optimal trade-offs, probing interview questions, and fully executable, zero-dependency Java code with built-in automated test suites.

---

## 🧭 Master Quick Navigation

| # | Section | Guide | Java Source & Test Suites | Key Highlights |
|---|---|---|---|---|
| **1** | **Complexity Analysis & Big-O** | [01 Complexity Guide](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/01_complexity_analysis/README.md) | [ComplexityChallenges.java](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/01_complexity_analysis/ComplexityChallenges.java) | Big-O rules, 7 nested loop step proofs ($O(n \log \log n)$, $O(\log^2 n)$, geometric series), Cheat sheet |
| **2** | **Arrays** | [02 Arrays Guide](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/02_arrays/README.md) | [Part 1 (1–11)](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/02_arrays/ArrayChallengesPart1.java)<br>[Part 2 (Search/Sort)](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/02_arrays/ArrayChallengesPart2.java)<br>[Part 3 (Patterns)](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/02_arrays/ArrayChallengesPart3.java) | 2D Arrays, Kadane's, Modulo Max/Min encoding, Sliding Window Max Deque, Cyclic sort, Bitonic peak, QuickSort |
| **3** | **Linked Lists** | [03 Linked Lists Guide](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/03_linked_lists/README.md) | [Singly LL (1–10)](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/03_linked_lists/SinglyLinkedListChallenges.java)<br>[Doubly LL (11)](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/03_linked_lists/DoublyLinkedListChallenges.java)<br>[Advanced LL](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/03_linked_lists/AdvancedLinkedListChallenges.java) | Cycle detection, In-place reversal, DLL Palindrome, Intersection, K-group reversal, Merge K sorted |
| **4** | **Strings** | [04 Strings Guide](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/04_strings/README.md) | [Part 1 (Palindromes)](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/04_strings/StringCoreAndPalindromes.java)<br>[Part 2 (DP/Advanced)](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/04_strings/StringAdvancedAndDP.java) | Longest Palindromic Substring/Subsequence, XML to Tree, Regex `.*` DP, LCS, Word Break, Boggle |
| **5** | **Stacks & Queues** | [05 Stacks/Queues Guide](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/05_stacks_and_queues/README.md) | [Implementations](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/05_stacks_and_queues/StackQueueImplementations.java)<br>[Challenges (1–9)](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/05_stacks_and_queues/StackQueueChallenges.java) | MinStack in $O(1)$ space, 2 Stacks in 1 Array, Next Greater Element, Celebrity Problem, Postfix eval |
| **6** | **Trees & BST** | [06 Trees Guide](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/06_trees/README.md) | [BinaryTreeChallenges.java](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/06_trees/BinaryTreeChallenges.java) | BST Iterator, Inorder Successor (with/without parent), Sibling next pointers, Boundary Perimeter, N-ary to Binary |
| **7** | **Trie (Prefix Tree)** | [07 Trie Guide](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/07_trie/README.md) | [TrieChallenges.java](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/07_trie/TrieChallenges.java) | Node design, Recursive deletion, Word count, Autocomplete/All words, Lexicographical array sort |
| **8** | **Heaps & Priority Queues** | [08 Heaps Guide](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/08_heaps/README.md) | [HeapChallenges.java](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/08_heaps/HeapChallenges.java) | Min/Max Heap array invariants, Max-to-Min conversion in $O(n)$, Top K Smallest/Largest |
| **9** | **Hash Tables** | [09 Hash Tables Guide](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/09_hash_tables/README.md) | [HashTableChallenges.java](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/09_hash_tables/HashTableChallenges.java) | Disjoint arrays, Symmetric pairs, Journey path tracing, Equal sum pairs ($a+b=c+d$), Zero-sum subarray |
| **10** | **Graphs** | [10 Graphs Guide](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/10_graphs/README.md) | [GraphChallenges.java](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/10_graphs/GraphChallenges.java) | BFS/DFS, 2-Coloring Bipartite, Directed Cycle Detection, Mother Vertex, Directed Tree check, Shortest Path |
| **11** | **Concurrency & Distributed Systems** | [11 Concurrency & Distributed Guide](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/11_concurrency_and_distributed/README.md) | [ConcurrencyPrimitives.java](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/11_concurrency_and_distributed/ConcurrencyPrimitivesAndPatterns.java)<br>[DistributedSystemsCore.java](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/11_concurrency_and_distributed/DistributedSystemsCore.java) | Bounded Blocking Queue (2 conditions), Thread-Safe LRU Cache, Token Bucket, Custom ThreadPool, Consistent Hashing Ring (VNodes), Nginx SWRR Load Balancer, Leader Election, Vector Clocks |
| **12** | **JVM Internals, Spring @Transactional & Design Patterns (45 Q&A)** | [Module 12 Master Guide](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/12_jvm_spring_patterns/README.md)<br>• [01 JVM Internals](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/12_jvm_spring_patterns/01_JVM_INTERNALS.md)<br>• [02 Spring @Transactional](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/12_jvm_spring_patterns/02_SPRING_TRANSACTIONAL.md)<br>• [03 Design Patterns](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/12_jvm_spring_patterns/03_JAVA_DESIGN_PATTERNS.md) | [JvmInternalsDemos.java](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/_12_jvm_spring_patterns/JvmInternalsDemos.java)<br>[SpringTransactionalSimulator.java](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/_12_jvm_spring_patterns/SpringTransactionalSimulator.java)<br>[DesignPatternsMasterDemos.java](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/_12_jvm_spring_patterns/DesignPatternsMasterDemos.java) | **45 Comprehensive Questions**: Memory layout, ClassLoader delegation, G1 vs ZGC, Escape Analysis, Metaspace vs PermGen, OOM/CPU Triage, AOP Proxies, Self-Invocation, 7 Propagations, 4 Isolations, Immutable Builder, 3 Thread-Safe Singletons, Saga & Outbox |
| **⭐** | **15-Year Experience Drill (50 Q&A)** | [50 Senior Experience Drills](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/experience_drills/drill-my-experience.md) | [drill-my-experience.md](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/experience_drills/drill-my-experience.md) | 50 Verbal Scenarios (1–2 min answers): Spring Boot lifecycles, `@Transactional` proxy traps, Hibernate N+1 & dirty checking, Resilience4j, TLS/mTLS, Kafka outbox CDC, Little's Law, Incident RCA, Team Leadership |

---

## ⚡ How to Compile and Execute All Test Suites

To execute the entire interview curriculum test suite in a single command:

```bash
# Compile all modules
javac -d out interview_prep_mastery/*.java \
             interview_prep_mastery/*/*.java

# Run Master Test Suite with Assertions Enabled (-ea)
java -ea -cp out interview_prep_mastery.MasterTestRunner
```

To run any individual module test suite:
```bash
# Example: Run Module 02 Arrays Part 1
java -ea -cp out interview_prep_mastery._02_arrays.ArrayChallengesPart1

# Example: Run Module 06 Trees
java -ea -cp out interview_prep_mastery._06_trees.BinaryTreeChallenges
```

---

## 🧠 Interview Framework: 5-Step Problem Solving Protocol

When presenting solutions during technical interview rounds:
1. **Clarify & Probe**: State assumptions on input size $N$, empty/null inputs, negative values, duplicates, and expected return types.
2. **Brute Force First**: State the naive solution ($O(N^2)$ or $O(2^N)$) along with its bottleneck.
3. **Optimize with Patterns**: Identify the applicable pattern (Two Pointers, Sliding Window, Monotonic Stack, DP, Trie, BFS/DFS).
4. **Code Cleanly**: Write modular code with clear variable names and loop bounds.
5. **Dry Run & Complexity Proof**: Trace with an edge case, state $O(\text{Time})$ and $O(\text{Space})$ confidently.
