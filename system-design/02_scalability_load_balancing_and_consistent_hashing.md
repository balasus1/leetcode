# Scalability, Load Balancing & Consistent Hashing
### Master Architecture Guide for Senior & Principal Interviews

---

## 1. Scalability Fundamentals: Scaling Dimensions & Elasticity

Scalability is the ability of a system to handle increasing load without degrading performance (latency, throughput, error rate) by adding hardware or compute resources.

```
+---------------------------------------------------------------------------------------------------+
| THE 3 DIMENSIONS OF SCALING (THE SCALE CUBE - AKF PARTNERS)                                       |
|                                                                                                   |
|              Y-Axis: Functional Decomposition & Microservices                                     |
|                     ▲ (Split by verb/noun: Auth, Billing, Orders, Feed)                           |
|                     │                                                                             |
|                     │         / Z-Axis: Data Partitioning / Customer Sharding                     |
|                     │        /  (Split by Tenant ID, Geo Region, Hash Ring)                       |
|                     │       /                                                                     |
|                     │      /                                                                      |
|                     │     /                                                                       |
|                     │    /                                                                        |
|                     │   /                                                                         |
|                     │  /                                                                          |
|                     └─/────────────────────────► X-Axis: Horizontal Duplication                  |
|                                                  (Stateless Nginx/Pod Replicas behind L4/L7 LB)  |
+---------------------------------------------------------------------------------------------------+
```

### Vertical Scaling (Scale-Up) vs Horizontal Scaling (Scale-Out)

| Dimension | Vertical Scaling (Scale-Up) | Horizontal Scaling (Scale-Out) |
| :--- | :--- | :--- |
| **Mechanism** | Upgrading CPU cores, RAM, NVMe IOPS on a single instance (e.g., `r6i.32xlarge` with 128 vCPUs, 1TB RAM). | Adding more commodity compute instances/pods across availability zones. |
| **Upper Ceiling** | Hard hardware limit; extreme exponential cost at the high end. | Virtually unlimited theoretical capacity. |
| **Fault Tolerance** | Single Point of Failure (SPOF). Machine crash = total outage. | High redundancy. Dead nodes are automatically replaced by orchestrator. |
| **Data Consistency** | Simple ACID transactions on single-node shared memory/disk. | Requires distributed consensus, eventual consistency, or 2PC/Sagas. |
| **When to Use** | Small datasets, early MVPs, initial relational database engines. | High-traffic distributed SaaS, petabyte storage, high-availability tiers. |

---

## 2. Load Balancing Architectures: L4 vs L7 & Anycast

A Load Balancer distributes incoming network traffic across multiple backend servers to prevent overload, maximize throughput, and ensure zero-downtime failovers.

```
                                      [ Internet Clients ]
                                                │
                                                ▼ Anycast BGP Routing
                                   [ Global Edge Anycast IP ]
                                                │
                      ┌─────────────────────────┴─────────────────────────┐
                      ▼ Region: US-East                                   ▼ Region: EU-West
          [ Layer 4 LB: Maglev / IPVS ]                       [ Layer 4 LB: Maglev / IPVS ]
          (Direct Server Return / TCP Flow)                   (Direct Server Return / TCP Flow)
                      │                                                   │
         ┌────────────┴────────────┐                         ┌────────────┴────────────┐
         ▼                         ▼                         ▼                         ▼
   [ Layer 7 Envoy ]         [ Layer 7 Envoy ]         [ Layer 7 Envoy ]         [ Layer 7 Envoy ]
   (TLS Termination,         (TLS Termination,         (TLS Termination,         (TLS Termination,
    Path/Header Routing,      Path/Header Routing,      Path/Header Routing,      Path/Header Routing,
    Rate Limit, gRPC)         Rate Limit, gRPC)         Rate Limit, gRPC)         Rate Limit, gRPC)
         │                         │                         │                         │
   ┌─────┴─────┐             ┌─────┴─────┐             ┌─────┴─────┐             ┌─────┴─────┐
   ▼           ▼             ▼           ▼             ▼           ▼             ▼           ▼
[App Pod]  [App Pod]      [App Pod]  [App Pod]      [App Pod]  [App Pod]      [App Pod]  [App Pod]
```

### Layer 4 (Transport Layer) vs Layer 7 (Application Layer) Load Balancers

```
+---------------------------------------------------------------------------------------------------+
| L4 vs L7 PACKET INSPECTION                                                                        |
|                                                                                                   |
| Layer 4 (TCP/UDP):                                                                                |
| [ IP Header | TCP Header (Src Port, Dest Port) | Encrypted Payload ...                          ] |
|  --> Inspects ONLY IP & Port. Does NOT decrypt TLS. Blazing fast (millions of pkts/sec).         |
|                                                                                                   |
| Layer 7 (HTTP/gRPC/WebSocket):                                                                    |
| [ IP Header | TCP Header | TLS Decrypted | HTTP Header (Host, Path, Cookies, JWT) | JSON Body ]  |
|  --> Terminates TLS, inspects URL path (/api/v1/checkout vs /static), parses headers, gRPC.       |
+---------------------------------------------------------------------------------------------------+
```

| Metric / Capability | Layer 4 (L4) Load Balancing | Layer 7 (L7) Load Balancing |
| :--- | :--- | :--- |
| **OSI Layer** | Layer 4 (TCP, UDP, SCTP). | Layer 7 (HTTP, HTTPS, HTTP/2, HTTP/3, gRPC, WebSockets). |
| **TLS / SSL Handling** | Passthrough; backend servers or downstream L7 proxies handle decryption. | Terminates TLS at the LB boundary using hardware acceleration / modern ciphers. |
| **Routing Granularity** | IP address + Port tuple ($5\text{-tuple}$ flow hash). | URL paths (`/orders` vs `/catalog`), HTTP headers, Cookies, JWT claims, gRPC methods. |
| **Throughput & Latency** | Extreme throughput ($10\text{M}+$ PPS), sub-millisecond latency. | Higher CPU overhead due to TLS crypto and HTTP header buffer parsing. |
| **Key Technologies** | Google Maglev, Linux IPVS, AWS NLB, DPDK, HAProxy (TCP mode). | Envoy Proxy, Nginx, AWS ALB, Traefik, Cloudflare / Fastly reverse proxy. |
| **Direct Server Return (DSR)** | **Supported**. Request goes via LB, response bypasses LB directly to client. | **Not Supported**. Both request and response must flow through L7 proxy. |

---

## 3. Direct Server Return (DSR) & Kernel Bypass (Maglev / DPDK)

In traditional reverse proxy architectures, asymmetric web traffic (small requests $\sim 1\text{KB}$, huge responses $\sim 1\text{MB}$) causes the load balancer's egress network interface to bottleneck.

```
+---------------------------------------------------------------------------------------------------+
| DIRECT SERVER RETURN (DSR) FLOW                                                                   |
|                                                                                                   |
|    1. Client (IP: C) sends Request (Dst: VIP)                                                     |
|       │                                                                                           |
|       ▼                                                                                           |
|    [ L4 Maglev LB ] ─── 2. Encapsulates Generic UDP/GRE (Dst: Backend Real IP) ───► [ Backend Node ] |
|                                                                                         │         |
|                                3. Response sent directly to Client IP                    │         |
|                                   (Src: VIP, Dst: Client IP)                            │         |
|    Client ◄─────────────────────────────────────────────────────────────────────────────┘         |
+---------------------------------------------------------------------------------------------------+
```

1. **Client Request**: Client sends TCP packet with destination = Virtual IP (`VIP`).
2. **L4 Balancer Forwarding**: L4 Balancer chooses backend node via consistent hash and encapsulates the packet in Generic Routing Encapsulation (GRE) or Geneve tunnel **without modifying the original IP header**.
3. **Loopback Interface on Backend**: Backend node has `VIP` configured on its `lo` (loopback) interface (configured with `arp_ignore`). It decapsulates the packet and processes it locally as if it received it directly.
4. **Direct Response**: Backend node transmits response directly to client's public IP using `Src IP = VIP`. The load balancer never sees egress traffic, scaling egress bandwidth to hundreds of gigabits.

---

## 4. Load Balancing Algorithms: Deep Dive & Trade-offs

```
+---------------------------------------------------------------------------------------------------+
| LOAD BALANCING ALGORITHMS                                                                         |
|                                                                                                   |
| [ Round Robin ]         ──► Rotates sequentially: S1 -> S2 -> S3 -> S1.                           |
| [ Weighted Round Robin] ──► Accounts for machine capacity: S1(w=3), S2(w=1) -> S1,S1,S1,S2.       |
| [ Least Connections ]   ──► Routes to server with fewest active TCP sockets (ideal for websockets)|
| [ Weighted Response ]   ──► Dynamically routes to server with lowest rolling p99 latency (EMA).   |
| [ IP Hash / 5-Tuple ]   ──► Hash(SrcIP, DstIP, SrcPort, DstPort, Protocol) % N.                   |
| [ Consistent Hashing ]  ──► Virtual node ring; minimizing key reshuffling when servers scale.     |
| [ Power of Two Choices] ──► Picks 2 random servers, selects the one with lower active load.       |
+---------------------------------------------------------------------------------------------------+
```

### The "Power of Two Random Choices" Algorithm (Mitigating Hotspots)

In large-scale distributed clusters ($1,000+$ pods), centralized least-connections tracking requires synchronized global state, creating high lock contention.

- **Naive Random Routing**: Leads to maximum queue load of $\Theta(\frac{\log n}{\log \log n})$.
- **Power of Two Choices (P2C)**: Pick two worker nodes completely at random. Query their queue depth/active connection count, and dispatch the request to the less loaded node.
- **Result**: Drastically drops maximum queue length to $\Theta(\log \log n)$, providing near-optimal load distribution with zero centralized lock contention. Used natively in **Envoy**, **Nginx Plus**, and **Finagle**.

---

## 5. Consistent Hashing: Virtual Nodes & Ring Partitions

When distributing cache keys or sharding database state across $N$ servers, naive modular hashing (`hash(key) % N`) is disastrous when $N$ changes:

$$\text{Rehashed Keys Percentage} = \frac{N - 1}{N} \approx 99.9\% \quad (\text{for large } N)$$

Every cache server misses simultaneously, resulting in a **Cache Avalanche** and database collapse.

```
+---------------------------------------------------------------------------------------------------+
| CONSISTENT HASHING RING WITH VIRTUAL NODES                                                        |
|                                                                                                   |
|                                  0 / 2^32 - 1                                                     |
|                                   Node A (VN1)                                                    |
|                                     [ 0x00 ]                                                      |
|                                ┌───────▲───────┐                                                  |
|                      Node C   /                 \   Node B                                        |
|                      (VN2)   │                   │  (VN1)                                         |
|                             │                     │                                               |
|                    Key_101  │      2^32 Space     │  Key_42                                       |
|                    ───────► │    Clockwise Search │ ◄──────                                       |
|                             │                     │                                               |
|                      Node B  \                   /   Node C                                       |
|                      (VN2)    \                 /    (VN1)                                        |
|                                └───────▼───────┘                                                  |
|                                   Node A (VN2)                                                    |
|                                                                                                   |
+---------------------------------------------------------------------------------------------------+
```

### Mathematical Principle of Consistent Hashing

1. **Map Ring Range**: Map the hash output space to an integer range $[0, 2^{32} - 1]$ arranged in a continuous circular ring.
2. **Hash Servers onto Ring**: Hash server identifiers (e.g., `hash("server-1-ip")`) to place servers at discrete points on the ring.
3. **Hash Keys onto Ring**: Hash object keys (e.g., `hash("user:98741")`) to points on the same ring.
4. **Locate Server (Clockwise Traversal)**: Move clockwise from the key's position until encountering the first server node. That node owns the key.
5. **Node Addition/Removal**: When a node is added or removed, **only $\frac{K}{N}$ keys are remapped** on average ($K = \text{total keys}$, $N = \text{number of nodes}$).

### Why Virtual Nodes (Vnodes) are Essential in Production

- **Problem without Vnodes (Non-Uniform Distribution)**: Hash functions do not distribute 5 physical servers evenly around a $2^{32}$ ring. One server may end up with $60\%$ of the ring arc, creating massive hotspots and OOM crashes.
- **Solution (Virtual Nodes)**: Map each physical machine to $V$ virtual positions on the ring (e.g., $V = 256$ virtual nodes: `server1#0`, `server1#1`, ..., `server1#255`).
- **Benefits**:
  1. **Standard Deviation Reduction**: Load distribution variance drops to $\sigma \approx \frac{1}{\sqrt{V}}$.
  2. **Heterogeneous Hardware Sizing**: A powerful server with 64 cores and 256GB RAM can be assigned 512 virtual nodes, while an 8-core server receives 64 virtual nodes.
  3. **Graceful Failover Rebalancing**: When a physical node dies, its $V$ virtual nodes disappear from $V$ distinct ring locations. Its traffic is evenly absorbed across **all remaining nodes**, rather than dumping 100% of its load onto a single immediate downstream neighbor.

---

## 6. Production TypeScript Implementation: Consistent Hash Ring

```typescript
import crypto from 'node:crypto';

export class ConsistentHashRing {
  private readonly virtualNodesPerServer: number;
  private readonly ring: Map<number, string> = new Map(); // Hash -> Server Node ID
  private sortedKeys: number[] = []; // Sorted array of ring hash positions

  constructor(virtualNodesPerServer: number = 150) {
    this.virtualNodesPerServer = virtualNodesPerServer;
  }

  // Murmur3 or 32-bit FNV-1a Hash for high speed and uniform dispersion
  private hashKey(key: string): number {
    const hash = crypto.createHash('md5').update(key).digest();
    return hash.readUInt32BE(0); // 32-bit unsigned integer (0 to 4,294,967,295)
  }

  public addServer(serverId: string): void {
    for (let i = 0; i < this.virtualNodesPerServer; i++) {
      const vNodeKey = `${serverId}#vn_${i}`;
      const hash = this.hashKey(vNodeKey);
      this.ring.set(hash, serverId);
      this.sortedKeys.push(hash);
    }
    this.sortedKeys.sort((a, b) => a - b);
  }

  public removeServer(serverId: string): void {
    for (let i = 0; i < this.virtualNodesPerServer; i++) {
      const vNodeKey = `${serverId}#vn_${i}`;
      const hash = this.hashKey(vNodeKey);
      this.ring.delete(hash);
    }
    this.sortedKeys = this.sortedKeys.filter(key => this.ring.has(key));
  }

  // O(log(N * V)) Binary Search for closest clockwise node
  public getNode(key: string): string | null {
    if (this.sortedKeys.length === 0) return null;

    const hash = this.hashKey(key);
    let low = 0;
    let high = this.sortedKeys.length - 1;

    // If key hash is greater than all nodes on ring, wrap around to first node (Index 0)
    if (hash > this.sortedKeys[high]) {
      return this.ring.get(this.sortedKeys[0]) ?? null;
    }

    // Binary search (lower_bound)
    let targetIndex = 0;
    while (low <= high) {
      const mid = Math.floor((low + high) / 2);
      if (this.sortedKeys[mid] >= hash) {
        targetIndex = mid;
        high = mid - 1; // Look for smaller candidate on left
      } else {
        low = mid + 1;
      }
    }

    const matchedHash = this.sortedKeys[targetIndex];
    return this.ring.get(matchedHash) ?? null;
  }
}
```

---

## 7. Global Traffic Management: Anycast BGP & GeoDNS

```
+---------------------------------------------------------------------------------------------------+
| ANYCAST ROUTING (SAME IP ANNOUNCED GLOBALLY ACROSS MULTIPLE DATA CENTERS)                         |
|                                                                                                   |
|                       Client in London ──► (Anycast IP: 198.51.100.1) ──► London Edge POP         |
|                       Client in Tokyo  ──► (Anycast IP: 198.51.100.1) ──► Tokyo Edge POP          |
|                       Client in NYC    ──► (Anycast IP: 198.51.100.1) ──► NYC Edge POP            |
|                                                                                                   |
|  * Routed at Layer 3/BGP via Autonomous Systems (AS).                                             |
|  * Shortest AS-Path routing automatically directs users to closest edge without DNS latency.     |
|  * Absorbs massive DDoS attacks locally at the edge without flooding origin data centers.         |
+---------------------------------------------------------------------------------------------------+
```

| Strategy | Anycast BGP Routing | GeoDNS (Latency-based DNS) |
| :--- | :--- | :--- |
| **Layer** | Layer 3 (Network Routing Protocol). | Layer 7 / Application (DNS Nameserver Resolution). |
| **Failover Speed** | **Sub-second**. BGP withdraws dead path routes instantly. | **Slow (Minutes/Hours)**. Bounded by DNS TTL and recursive resolver caches. |
| **DDoS Absorption** | Absorbs multi-terabit volumetric DDoS by dispersing traffic across 300+ global edge POPs. | Vulnerable to direct IP target attacks; DNS cache poisoning. |
| **TCP State Handling** | Route changes mid-TCP connection can cause TCP resets if edge servers do not synchronize connection tables. | Stable per resolved IP address during session life. |
| **Best-Practice Architecture** | Deploy Anycast at the Edge for L4/L7 DDoS & TLS Termination $\rightarrow$ Proxy to regional backends over dedicated private cloud backbone. |
