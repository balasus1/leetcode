# Graph Systems, Distributed Graph Algorithms & Search Engines
### Master Architecture Guide for Senior & Principal Interviews

---

## 1. Graph Databases vs Relational JOINs: Index-Free Adjacency

When traversing relationship networks (e.g., Social Friend-of-Friends, Fraud Ring Detection, Knowledge Graphs), traditional relational databases degrade exponentially due to recursive `JOIN` table scans.

```
+---------------------------------------------------------------------------------------------------+
| RELATIONAL RECURSIVE JOINS vs GRAPH INDEX-FREE ADJACENCY                                          |
|                                                                                                   |
| RELATIONAL 3-HOP JOIN (O(N * log M) per hop):                                                     |
| [ Users Table ] ──(Index Lookup)──► [ Follows Table ] ──(Index Lookup)──► [ Follows Table ] ...    |
|  * Each hop performs an expensive index search across millions of rows.                           |
|  * 4-hop query across 10M records takes SECONDS or MINUTES.                                       |
|                                                                                                   |
| GRAPH DATABASE (INDEX-FREE ADJACENCY - O(1) per pointer hop):                                     |
| [ Node: Alice ] ──(Memory Pointer)──► [ Node: Bob ] ──(Memory Pointer)──► [ Node: Charlie ]       |
|  * Each node directly holds physical memory/disk pointers to its connected adjacent neighbor edges|
|  * Traversal time depends ONLY on the size of the subgraph, INDEPENDENT of total global graph size|
+---------------------------------------------------------------------------------------------------+
```

---

## 2. Graph Algorithms in System Design

```
+---------------------------------------------------------------------------------------------------+
| CORE GRAPH ALGORITHMS IN SYSTEM ARCHITECTURE                                                      |
|                                                                                                   |
| Algorithm                 | Time Complexity   | System Design Application                         |
| ------------------------- | ----------------- | ------------------------------------------------- |
| Bidirectional Dijkstra    | O((V + E) log V)  | Google Maps / Uber routing (Road network shortest) |
| A* (A-Star) with Heuristic| O(E)              | Real-time pathfinding with Euclidean distance      |
| Multi-Source BFS          | O(V + E)          | LinkedIn 2nd/3rd degree connections, Friend Recs  |
| PageRank / Random Walk    | O(k * (V + E))    | Web search ranking, Twitter "Who to Follow"       |
| Topological Sort          | O(V + E)          | CI/CD Pipeline DAGs, Airflow Workflow Scheduler    |
| Min-Cut / METIS           | Heuristic NP-Hard | Distributed graph sharding (Minimizing edge cuts)  |
+---------------------------------------------------------------------------------------------------+
```

### Social Network 2nd-Degree Friend Traversal (Multi-Source BFS)

```typescript
export class SocialGraphEngine {
  private adjacencyList: Map<string, Set<string>> = new Map();

  public addFollow(userA: string, userB: string): void {
    if (!this.adjacencyList.has(userA)) this.adjacencyList.set(userA, new Set());
    this.adjacencyList.get(userA)!.add(userB);
  }

  // Find 2nd-degree friends (Friends of Friends who are not already directly followed)
  public getSecondDegreeRecommendations(userId: string, limit: number = 20): string[] {
    const directFriends = this.adjacencyList.get(userId) || new Set();
    const secondDegreeFrequency = new Map<string, number>();

    for (const friendId of directFriends) {
      const friendsOfFriend = this.adjacencyList.get(friendId) || new Set();
      for (const candidate of friendsOfFriend) {
        // Exclude self and direct friends
        if (candidate !== userId && !directFriends.has(candidate)) {
          secondDegreeFrequency.set(candidate, (secondDegreeFrequency.get(candidate) || 0) + 1);
        }
      }
    }

    // Sort by mutual friend count descending
    return Array.from(secondDegreeFrequency.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, limit)
      .map(([candidateId]) => candidateId);
  }
}
```

---

## 3. Search Engine Architecture: Inverted Index & Elasticsearch Internals

Full-text search engines (Elasticsearch, Apache Lucene, OpenSearch) provide sub-50ms search and fuzzy matching across billions of unstructured documents.

```
+---------------------------------------------------------------------------------------------------+
| INVERTED INDEX DATA STRUCTURE (TERMS -> POSTING LISTS)                                            |
|                                                                                                   |
| Document 1: "Distributed systems scale with Kafka"                                                |
| Document 2: "Kafka streams process distributed events"                                            |
| Document 3: "Distributed databases require consensus"                                             |
|                                                                                                   |
| INVERTED INDEX (Lexicographically Sorted Terms in Finite State Transducer FST):                   |
| ┌────────────────┬─────────────────────────────────────────────────────────────┐                  |
| │ Term           │ Posting List (Doc IDs + Term Frequencies + Positions)       │                  |
| ├────────────────┼─────────────────────────────────────────────────────────────┤                  |
| │ consensus      │ [Doc 3 (pos: 4)]                                            │                  |
| │ databases      │ [Doc 3 (pos: 2)]                                            │                  |
| │ distributed    │ [Doc 1 (pos: 1)], [Doc 2 (pos: 4)], [Doc 3 (pos: 1)]        │                  |
| │ events         │ [Doc 2 (pos: 5)]                                            │                  |
| │ kafka          │ [Doc 1 (pos: 5)], [Doc 2 (pos: 1)]                         │                  |
| │ scale          │ [Doc 1 (pos: 3)]                                            │                  |
| └────────────────┴─────────────────────────────────────────────────────────────┘                  |
|                                                                                                   |
| Query: "distributed AND kafka"                                                                    |
| -> Intersect Posting Lists for "distributed" [1, 2, 3] and "kafka" [1, 2]                          |
| -> Result: [Doc 1, Doc 2] in O(Len1 + Len2) time via Bitset / Roaring Bitmap intersection!         |
+---------------------------------------------------------------------------------------------------+
```

### Lucene Relevance Ranking: The BM25 Algorithm

BM25 (Best Matching 25) calculates the relevance score of a document $D$ for a search query $Q = \{q_1, q_2, \dots\}$:

$$\text{Score}(D, Q) = \sum_{i=1}^{n} \text{IDF}(q_i) \times \frac{f(q_i, D) \times (k_1 + 1)}{f(q_i, D) + k_1 \times \left(1 - b + b \times \frac{|D|}{\text{avgdl}}\right)}$$

Where:
- $\text{IDF}(q_i) = \ln\left( \frac{N - n(q_i) + 0.5}{n(q_i) + 0.5} + 1 \right)$: Inverse Document Frequency (Rare words like `"paxos"` receive massive weight; common words like `"the"` receive zero weight).
- $f(q_i, D)$: Term frequency of word $q_i$ in document $D$.
- $|D| / \text{avgdl}$: Normalizes for document length (prevents long articles from dominating search results purely through verbosity).
- $k_1 \approx 1.2, b \approx 0.75$: Tuning hyperparameters.
