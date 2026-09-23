package interview_prep_mastery._11_concurrency_and_distributed;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * ============================================================================
 * MODULE 11: DISTRIBUTED SYSTEMS CORE BUILDING BLOCKS
 * Topics:
 * 1. Consistent Hashing Ring with Virtual Nodes (VNodes) & Dispersion Test
 * 2. Smooth Weighted Round Robin Load Balancer (Nginx SWRR Algorithm)
 * 3. Leader Election with Heartbeat Lease Simulator
 * 4. Vector Clocks for Distributed Causality Tracking
 * ============================================================================
 */
public class DistributedSystemsCore {

    // ------------------------------------------------------------------------
    // 1. Consistent Hashing Ring with Virtual Nodes (VNodes)
    // ------------------------------------------------------------------------
    /**
     * Interview Probing Questions & Invariants:
     * - Why use Virtual Nodes (VNodes)?
     *   -> Without VNodes, a few physical nodes can own massive chunks of the ring.
     *      VNodes uniformly interleave slices of each server across the 2^32 hash ring.
     * - How does key lookup work?
     *   -> hash(key) -> find first node on ring with hash >= hash(key) via TreeMap.ceilingEntry().
     *      If none, wrap around to treeMap.firstEntry().
     */
    public static class ConsistentHashRing {
        private final int numberOfVirtualNodes;
        private final SortedMap<Long, String> ring = new TreeMap<>();
        private final Set<String> physicalNodes = new HashSet<>();

        public ConsistentHashRing(int numberOfVirtualNodes, Collection<String> nodes) {
            this.numberOfVirtualNodes = numberOfVirtualNodes;
            if (nodes != null) {
                for (String node : nodes) {
                    addNode(node);
                }
            }
        }

        public synchronized void addNode(String node) {
            physicalNodes.add(node);
            for (int i = 0; i < numberOfVirtualNodes; i++) {
                long hash = hash(node + "#VN#" + i);
                ring.put(hash, node);
            }
        }

        public synchronized void removeNode(String node) {
            physicalNodes.remove(node);
            for (int i = 0; i < numberOfVirtualNodes; i++) {
                long hash = hash(node + "#VN#" + i);
                ring.remove(hash);
            }
        }

        public synchronized String getNode(String key) {
            if (ring.isEmpty()) return null;
            long hash = hash(key);
            SortedMap<Long, String> tailMap = ring.tailMap(hash);
            long targetHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
            return ring.get(targetHash);
        }

        public int getTotalVirtualNodes() {
            return ring.size();
        }

        private long hash(String key) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(key.getBytes());
                // Use first 8 bytes for 64-bit positive hash
                long h = 0;
                for (int i = 0; i < 8; i++) {
                    h = (h << 8) | (digest[i] & 0xFF);
                }
                return h & 0x7FFFFFFFFFFFFFFFL;
            } catch (NoSuchAlgorithmException e) {
                return key.hashCode() & 0x7FFFFFFFL;
            }
        }
    }

    // ------------------------------------------------------------------------
    // 2. Smooth Weighted Round Robin Load Balancer (Nginx Algorithm)
    // ------------------------------------------------------------------------
    /**
     * Problem with naive Weighted Round Robin:
     * With weights {A:5, B:1, C:1}, naive WRR sends 5 straight requests to A (A,A,A,A,A,B,C),
     * causing temporary load spikes on A.
     *
     * Smooth Weighted Round Robin (SWRR) interleaves requests smoothly: (A,A,B,A,C,A,A).
     */
    public static class SmoothWeightedRoundRobin {
        public static class Server {
            public final String name;
            public final int weight;
            public int currentWeight;

            public Server(String name, int weight) {
                this.name = name;
                this.weight = weight;
                this.currentWeight = 0;
            }
        }

        private final List<Server> servers = new ArrayList<>();
        private int totalWeight = 0;

        public synchronized void addServer(String name, int weight) {
            servers.add(new Server(name, weight));
            totalWeight += weight;
        }

        public synchronized String selectServer() {
            if (servers.isEmpty()) return null;

            Server best = null;
            for (Server s : servers) {
                s.currentWeight += s.weight;
                if (best == null || s.currentWeight > best.currentWeight) {
                    best = s;
                }
            }

            if (best != null) {
                best.currentWeight -= totalWeight;
                return best.name;
            }
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // 3. Leader Election with Heartbeat & Lease Simulator
    // ------------------------------------------------------------------------
    public static class LeaderElectionCluster {
        public static class Node {
            public final String id;
            public volatile boolean isLeader = false;
            public volatile boolean isAlive = true;

            public Node(String id) {
                this.id = id;
            }
        }

        private final List<Node> nodes = new ArrayList<>();
        private final AtomicReference<String> currentLeader = new AtomicReference<>(null);
        private final AtomicLong leaseExpiryTime = new AtomicLong(0);
        private final long LEASE_DURATION_MS = 300;

        public void addNode(Node node) {
            nodes.add(node);
        }

        public synchronized void electLeader() {
            long now = System.currentTimeMillis();
            String leaderId = currentLeader.get();

            // Check if current leader is dead or lease expired
            boolean leaderValid = false;
            if (leaderId != null) {
                for (Node n : nodes) {
                    if (n.id.equals(leaderId) && n.isAlive && now < leaseExpiryTime.get()) {
                        leaderValid = true;
                        break;
                    }
                }
            }

            if (!leaderValid) {
                // Elect lowest ID alive node as new leader (Deterministic Coordinator election)
                Node newLeader = null;
                for (Node n : nodes) {
                    if (n.isAlive) {
                        if (newLeader == null || n.id.compareTo(newLeader.id) < 0) {
                            newLeader = n;
                        }
                    }
                }

                for (Node n : nodes) {
                    n.isLeader = (newLeader != null && n.id.equals(newLeader.id));
                }

                if (newLeader != null) {
                    currentLeader.set(newLeader.id);
                    leaseExpiryTime.set(now + LEASE_DURATION_MS);
                } else {
                    currentLeader.set(null);
                }
            }
        }

        public String getLeaderId() {
            return currentLeader.get();
        }
    }

    // ------------------------------------------------------------------------
    // 4. Vector Clocks for Distributed Causality Tracking
    // ------------------------------------------------------------------------
    /**
     * Vector Clock Comparison:
     * - VC1 happens-before VC2 if for all nodes i: VC1[i] <= VC2[i] and exists j: VC1[j] < VC2[j].
     * - Otherwise, if neither happens-before the other, the events are CONCURRENT (Conflict/Split-Brain).
     */
    public static class VectorClock {
        private final Map<String, Integer> clock = new HashMap<>();

        public VectorClock() {}

        public VectorClock(VectorClock other) {
            this.clock.putAll(other.clock);
        }

        public synchronized void increment(String nodeId) {
            clock.put(nodeId, clock.getOrDefault(nodeId, 0) + 1);
        }

        public synchronized void merge(VectorClock other) {
            for (Map.Entry<String, Integer> entry : other.clock.entrySet()) {
                clock.put(entry.getKey(), Math.max(clock.getOrDefault(entry.getKey(), 0), entry.getValue()));
            }
        }

        public static int compare(VectorClock vc1, VectorClock vc2) {
            boolean vc1Greater = false;
            boolean vc2Greater = false;

            Set<String> allNodes = new HashSet<>(vc1.clock.keySet());
            allNodes.addAll(vc2.clock.keySet());

            for (String node : allNodes) {
                int c1 = vc1.clock.getOrDefault(node, 0);
                int c2 = vc2.clock.getOrDefault(node, 0);

                if (c1 > c2) vc1Greater = true;
                if (c2 > c1) vc2Greater = true;
            }

            if (vc1Greater && !vc2Greater) return 1;   // vc1 is strictly after vc2
            if (vc2Greater && !vc1Greater) return -1;  // vc1 is strictly before vc2
            if (!vc1Greater && !vc2Greater) return 0;  // identical
            return 2;                                  // Concurrent (Conflict!)
        }
    }

    // ------------------------------------------------------------------------
    // Test Suite for Distributed Systems Building Blocks
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING DISTRIBUTED SYSTEMS CORE TEST SUITE ");
        System.out.println("=================================================");

        // 1. Consistent Hashing with VNodes
        List<String> nodes = Arrays.asList("Server-A", "Server-B", "Server-C");
        ConsistentHashRing ring = new ConsistentHashRing(100, nodes);
        assert ring.getTotalVirtualNodes() == 300;

        // Distribute 1000 keys and verify balanced distribution
        Map<String, Integer> distribution = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            String server = ring.getNode("user_key_" + i);
            distribution.put(server, distribution.getOrDefault(server, 0) + 1);
        }

        // Each server should get roughly 1/3 (between 250 and 420 keys)
        for (String node : nodes) {
            int count = distribution.getOrDefault(node, 0);
            assert count > 200 && count < 450 : "Uneven distribution on " + node + ": " + count;
        }

        // Add a server and verify minimal repartitioning
        Map<String, String> beforeMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            beforeMap.put("k_" + i, ring.getNode("k_" + i));
        }

        ring.addNode("Server-D");
        int migratedKeys = 0;
        for (int i = 0; i < 1000; i++) {
            String newServer = ring.getNode("k_" + i);
            if (!newServer.equals(beforeMap.get("k_" + i))) {
                migratedKeys++;
            }
        }
        // When going from 3 to 4 servers, roughly 1/4 (200-320) keys should migrate
        assert migratedKeys > 150 && migratedKeys < 350 : "Expected ~25% key migration, got: " + migratedKeys;

        // 2. Smooth Weighted Round Robin (SWRR)
        SmoothWeightedRoundRobin swrr = new SmoothWeightedRoundRobin();
        swrr.addServer("A", 5);
        swrr.addServer("B", 1);
        swrr.addServer("C", 1);

        List<String> sequence = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            sequence.add(swrr.selectServer());
        }
        // Nginx standard smooth sequence for {A:5, B:1, C:1} -> [A, A, B, A, C, A, A]
        assert sequence.equals(Arrays.asList("A", "A", "B", "A", "C", "A", "A")) : "SWRR sequence mismatch: " + sequence;

        // 3. Leader Election with Failover
        LeaderElectionCluster cluster = new LeaderElectionCluster();
        LeaderElectionCluster.Node n1 = new LeaderElectionCluster.Node("Node-1");
        LeaderElectionCluster.Node n2 = new LeaderElectionCluster.Node("Node-2");
        cluster.addNode(n1);
        cluster.addNode(n2);

        cluster.electLeader();
        assert cluster.getLeaderId().equals("Node-1") : "Lowest ID Node-1 should be leader";

        // Kill leader and re-elect
        n1.isAlive = false;
        cluster.electLeader();
        assert cluster.getLeaderId().equals("Node-2") : "Node-2 should take over as new leader";

        // 4. Vector Clock Causality
        VectorClock vcA = new VectorClock();
        VectorClock vcB = new VectorClock();

        vcA.increment("NodeA"); // Event on A: {NodeA: 1}
        vcB.merge(vcA);
        vcB.increment("NodeB"); // Event on B after receiving from A: {NodeA: 1, NodeB: 1}

        assert VectorClock.compare(vcA, vcB) == -1 : "vcA happened before vcB";

        // Create concurrent conflict
        VectorClock vcC1 = new VectorClock(vcA);
        VectorClock vcC2 = new VectorClock(vcA);
        vcC1.increment("NodeA"); // {NodeA: 2}
        vcC2.increment("NodeB"); // {NodeA: 1, NodeB: 1}
        assert VectorClock.compare(vcC1, vcC2) == 2 : "Concurrent edits detected";

        System.out.println(" DISTRIBUTED SYSTEMS CORE SUITE ALL PASSED!");
        System.out.println("=================================================");
    }
}
