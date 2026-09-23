package interview_prep_mastery._10_graphs;

import java.util.*;

/**
 * ============================================================================
 * MODULE 10: GRAPHS - COMPREHENSIVE CHALLENGES 1 TO 8
 * ============================================================================
 */
public class GraphChallenges {

    // Graph Adjacency List Representation
    public static class Graph {
        public int vertices;
        public List<List<Integer>> adjList;

        public Graph(int vertices) {
            this.vertices = vertices;
            this.adjList = new ArrayList<>(vertices);
            for (int i = 0; i < vertices; i++) {
                this.adjList.add(new ArrayList<>());
            }
        }

        public void addDirectedEdge(int source, int destination) {
            if (source < vertices && destination < vertices) {
                this.adjList.get(source).add(destination);
            }
        }

        public void addUndirectedEdge(int source, int destination) {
            if (source < vertices && destination < vertices) {
                this.adjList.get(source).add(destination);
                this.adjList.get(destination).add(source);
            }
        }
    }

    // ------------------------------------------------------------------------
    // 1. What is a Bipartite Graph? (2-Coloring Check)
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(V + E)
     * - Space: O(V)
     */
    public static boolean isBipartite(Graph g) {
        if (g == null || g.vertices == 0) return true;
        int[] colors = new int[g.vertices]; // 0: uncolored, 1: blue, -1: red
        Queue<Integer> queue = new LinkedList<>();

        for (int i = 0; i < g.vertices; i++) {
            if (colors[i] == 0) {
                colors[i] = 1;
                queue.offer(i);

                while (!queue.isEmpty()) {
                    int u = queue.poll();
                    for (int v : g.adjList.get(u)) {
                        if (colors[v] == 0) {
                            colors[v] = -colors[u];
                            queue.offer(v);
                        } else if (colors[v] == colors[u]) {
                            return false; // Same color adjacent!
                        }
                    }
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------
    // Challenge 1: Implement Breadth First Search (BFS)
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(V + E)
     * - Space: O(V)
     */
    public static List<Integer> bfs(Graph g) {
        List<Integer> result = new ArrayList<>();
        if (g == null || g.vertices == 0) return result;
        boolean[] visited = new boolean[g.vertices];
        Queue<Integer> queue = new LinkedList<>();

        for (int i = 0; i < g.vertices; i++) {
            if (!visited[i]) {
                visited[i] = true;
                queue.offer(i);
                while (!queue.isEmpty()) {
                    int u = queue.poll();
                    result.add(u);
                    for (int v : g.adjList.get(u)) {
                        if (!visited[v]) {
                            visited[v] = true;
                            queue.offer(v);
                        }
                    }
                }
            }
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // Challenge 2: Implement Depth First Search (DFS)
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(V + E)
     * - Space: O(V)
     */
    public static List<Integer> dfs(Graph g) {
        List<Integer> result = new ArrayList<>();
        if (g == null || g.vertices == 0) return result;
        boolean[] visited = new boolean[g.vertices];

        for (int i = 0; i < g.vertices; i++) {
            if (!visited[i]) {
                dfsHelper(g, i, visited, result);
            }
        }
        return result;
    }

    private static void dfsHelper(Graph g, int u, boolean[] visited, List<Integer> result) {
        visited[u] = true;
        result.add(u);
        for (int v : g.adjList.get(u)) {
            if (!visited[v]) {
                dfsHelper(g, v, visited, result);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Challenge 3: Cycle Detection in a Directed Graph
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(V + E)
     * - Space: O(V)
     */
    public static boolean detectCycleInDirectedGraph(Graph g) {
        if (g == null || g.vertices == 0) return false;
        boolean[] visited = new boolean[g.vertices];
        boolean[] recStack = new boolean[g.vertices];

        for (int i = 0; i < g.vertices; i++) {
            if (!visited[i]) {
                if (cycleDfs(g, i, visited, recStack)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean cycleDfs(Graph g, int u, boolean[] visited, boolean[] recStack) {
        visited[u] = true;
        recStack[u] = true;

        for (int v : g.adjList.get(u)) {
            if (!visited[v] && cycleDfs(g, v, visited, recStack)) {
                return true;
            } else if (recStack[v]) {
                return true; // Back-edge detected
            }
        }
        recStack[u] = false;
        return false;
    }

    // ------------------------------------------------------------------------
    // Challenge 4: Find "Mother Vertex" in a Directed Graph
    // ------------------------------------------------------------------------
    /**
     * Mother vertex can reach all vertices in graph.
     * Kosaraju DFS observation: Last finished vertex in DFS is candidate.
     *
     * Complexity:
     * - Time: O(V + E)
     * - Space: O(V)
     */
    public static int findMotherVertex(Graph g) {
        if (g == null || g.vertices == 0) return -1;
        boolean[] visited = new boolean[g.vertices];
        int lastFinished = -1;

        for (int i = 0; i < g.vertices; i++) {
            if (!visited[i]) {
                dfsHelper(g, i, visited, new ArrayList<>());
                lastFinished = i;
            }
        }

        // Verify candidate
        Arrays.fill(visited, false);
        List<Integer> reach = new ArrayList<>();
        dfsHelper(g, lastFinished, visited, reach);
        return reach.size() == g.vertices ? lastFinished : -1;
    }

    // ------------------------------------------------------------------------
    // Challenge 5: Count the Number of Edges in an Undirected Graph
    // ------------------------------------------------------------------------
    /**
     * Handshaking Lemma: Total edges = Sum(degrees) / 2
     *
     * Complexity:
     * - Time: O(V)
     * - Space: O(1)
     */
    public static int countEdges(Graph g) {
        if (g == null) return 0;
        int sum = 0;
        for (int i = 0; i < g.vertices; i++) {
            sum += g.adjList.get(i).size();
        }
        return sum / 2;
    }

    // ------------------------------------------------------------------------
    // Challenge 6: Check if a Path Exists Between Two Vertices
    // ------------------------------------------------------------------------
    public static boolean checkPathExists(Graph g, int source, int destination) {
        if (g == null || source >= g.vertices || destination >= g.vertices) return false;
        if (source == destination) return true;
        boolean[] visited = new boolean[g.vertices];
        Queue<Integer> queue = new LinkedList<>();

        visited[source] = true;
        queue.offer(source);

        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : g.adjList.get(u)) {
                if (v == destination) return true;
                if (!visited[v]) {
                    visited[v] = true;
                    queue.offer(v);
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // Challenge 7: Check if a Directed Graph is a Tree or Not
    // ------------------------------------------------------------------------
    /**
     * A directed graph is a tree (rooted branching tree) if:
     * 1. Exactly one root with in-degree 0.
     * 2. All other vertices have in-degree exactly 1.
     * 3. No cycles (all vertices reachable from root).
     */
    public static boolean isTree(Graph g) {
        if (g == null || g.vertices == 0) return true;
        int[] inDegree = new int[g.vertices];

        for (int i = 0; i < g.vertices; i++) {
            for (int v : g.adjList.get(i)) {
                inDegree[v]++;
            }
        }

        int root = -1;
        for (int i = 0; i < g.vertices; i++) {
            if (inDegree[i] == 0) {
                if (root != -1) return false; // More than 1 root
                root = i;
            } else if (inDegree[i] > 1) {
                return false; // More than 1 parent
            }
        }

        if (root == -1) return false; // No root (cycle)

        // Check reachability from root
        boolean[] visited = new boolean[g.vertices];
        List<Integer> reached = new ArrayList<>();
        dfsHelper(g, root, visited, reached);

        return reached.size() == g.vertices;
    }

    // ------------------------------------------------------------------------
    // Challenge 8: Find Length of Shortest Path between Two Vertices
    // ------------------------------------------------------------------------
    /**
     * BFS Level Distance tracking.
     *
     * Complexity:
     * - Time: O(V + E)
     * - Space: O(V)
     */
    public static int findShortestPathLength(Graph g, int source, int destination) {
        if (g == null || source >= g.vertices || destination >= g.vertices) return -1;
        if (source == destination) return 0;
        int[] distance = new int[g.vertices];
        Arrays.fill(distance, -1);
        Queue<Integer> queue = new LinkedList<>();

        distance[source] = 0;
        queue.offer(source);

        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : g.adjList.get(u)) {
                if (distance[v] == -1) {
                    distance[v] = distance[u] + 1;
                    if (v == destination) return distance[v];
                    queue.offer(v);
                }
            }
        }
        return -1; // Unreachable
    }

    // ------------------------------------------------------------------------
    // Test Suite for Graph Challenges
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING GRAPH CHALLENGES TEST SUITE ");
        System.out.println("=================================================");

        // 1. BFS & DFS
        Graph g = new Graph(5);
        g.addDirectedEdge(0, 1);
        g.addDirectedEdge(0, 2);
        g.addDirectedEdge(1, 3);
        g.addDirectedEdge(2, 4);

        assert bfs(g).equals(Arrays.asList(0, 1, 2, 3, 4));
        assert dfs(g).equals(Arrays.asList(0, 1, 3, 2, 4));

        // 2. Bipartite
        Graph bipGraph = new Graph(4);
        bipGraph.addUndirectedEdge(0, 1);
        bipGraph.addUndirectedEdge(1, 2);
        bipGraph.addUndirectedEdge(2, 3);
        bipGraph.addUndirectedEdge(3, 0);
        assert isBipartite(bipGraph);

        // 3. Cycle Detection
        assert !detectCycleInDirectedGraph(g);
        g.addDirectedEdge(3, 0); // Adds cycle 0 -> 1 -> 3 -> 0
        assert detectCycleInDirectedGraph(g);

        // 4. Mother Vertex
        Graph gMother = new Graph(4);
        gMother.addDirectedEdge(0, 1);
        gMother.addDirectedEdge(0, 2);
        gMother.addDirectedEdge(1, 3);
        assert findMotherVertex(gMother) == 0;

        // 5. Count Edges
        Graph gUndirected = new Graph(3);
        gUndirected.addUndirectedEdge(0, 1);
        gUndirected.addUndirectedEdge(1, 2);
        gUndirected.addUndirectedEdge(2, 0);
        assert countEdges(gUndirected) == 3;

        // 6. Path Exists
        assert checkPathExists(gMother, 0, 3);
        assert !checkPathExists(gMother, 3, 0);

        // 7. Tree Check
        Graph gTree = new Graph(4);
        gTree.addDirectedEdge(0, 1);
        gTree.addDirectedEdge(0, 2);
        gTree.addDirectedEdge(1, 3);
        assert isTree(gTree);

        // 8. Shortest Path Length
        assert findShortestPathLength(gMother, 0, 3) == 2;

        System.out.println(" GRAPH CHALLENGES ALL PASSED!");
        System.out.println("=================================================");
    }
}
