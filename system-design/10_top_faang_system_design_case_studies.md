# Top FAANG System Design Case Studies & Production Architectures
### Master Case Study Blueprints for Senior & Principal Interviews

---

## 1. Design a Global Real-Time Chat System (WhatsApp / Slack)

### Capacity & Scale Estimation
- **Users**: $2\text{ Billion Total}$, $500\text{M Daily Active Users (DAU)}$.
- **Traffic**: $500\text{M DAU} \times 40\text{ msgs/day} = 20\text{ Billion msgs/day} \approx 230,000\text{ msgs/sec (Average)}$, Peak $= 700,000\text{ msgs/sec}$.
- **Storage**: $20\text{B msgs} \times 200\text{ Bytes} = 4\text{TB/day} \times 365 = 1.46\text{PB/year}$.

```
+---------------------------------------------------------------------------------------------------+
| REAL-TIME CHAT SERVICE ARCHITECTURE (WHATSAPP / SLACK)                                            |
|                                                                                                   |
|  [ User A (Sender) ]                                             [ User B (Receiver) ]            |
|         │                                                                  ▲                      |
|         ▼ Persistent WebSocket (TLS)                                       │ Persistent WebSocket |
|  ┌──────────────────────────────┐                       ┌──────────────────────────────┐          |
|  │  WebSocket Gateway Node 1    │                       │  WebSocket Gateway Node 4    │          |
|  └──────────────┬───────────────┘                       └──────────────▲───────────────┘          |
|                 │                                                      │                          |
|                 │ 1. Forward Message                                   │ 5. Push Message          |
|                 ▼                                                      │    over open socket      |
|  ┌──────────────────────────────┐                       ┌──────────────┴───────────────┐          |
|  │      Chat Routing Service    │ ──── 4. Fanout ─────► │  Message Delivery Worker     │          |
|  └──────────────┬───────────────┘                       └──────────────────────────────┘          |
|                 │                                                      ▲                          |
|                 │ 2. Query Recipient Gateway Node                      │                          |
|                 ▼                                                      │                          |
|  ┌──────────────────────────────┐                       ┌──────────────┴───────────────┐          |
|  │  Redis User Session Registry │                       │ Push Notification Svc (APNs) │          |
|  │  (User_B -> "gateway_node_4")│                       │ (If User B is OFFLINE)       │          |
|  └──────────────────────────────┘                       └──────────────────────────────┘          |
|                 │                                                                                 |
|                 │ 3. Asynchronous Persist Stream                                                  |
|                 ▼                                                                                 |
|  ┌──────────────────────────────┐                                                                 |
|  │   Apache Kafka Cluster       │ ──► [ Cassandra / ScyllaDB Message Store ]                      |
|  │   (Partition by Chat_ID)     │     (Partition: chat_id, Cluster Key: message_id DESC)          |
|  └──────────────────────────────┘                                                                 |
+---------------------------------------------------------------------------------------------------+
```

### Data Model (Apache Cassandra / ScyllaDB)
```sql
CREATE TABLE chat_messages (
    chat_id uuid,
    message_id bigint, -- Snowflake ID (Time-sortable)
    sender_id uuid,
    content text,
    media_url text,
    status text, -- SENT, DELIVERED, READ
    created_at timestamp,
    PRIMARY KEY ((chat_id), message_id)
) WITH CLUSTERING ORDER BY (message_id DESC);
```

---

## 2. Design Social Newsfeed Architecture (Twitter / Instagram)

### The Core Architectural Dilemma: Fan-out on Write vs Fan-out on Read

```
+---------------------------------------------------------------------------------------------------+
| HYBRID FAN-OUT NEWSFEED ARCHITECTURE                                                              |
|                                                                                                   |
| [ Regular User Posts Tweet ] ────────► [ Fan-out on WRITE Worker ]                                |
|                                                    │                                              |
|                                                    ▼ Pushes Tweet ID to In-Memory Timelines       |
|                                        [ Redis Timeline Cache of all 200 Followers ]              |
|                                        (O(1) Instant Home Feed Read for Followers)                |
|                                                                                                   |
| [ Celebrity (Elon Musk, 150M Followers) Posts Tweet ]:                                            |
| * Fan-out on write would require 150 MILLION Redis writes, crashing the cache cluster!           |
|                                                                                                   |
| SOLUTION: Fan-out on READ (Pull Model for Celebrities):                                           |
| 1. Celebrity tweet saved ONLY in celebrity's own user timeline.                                   |
| 2. When a user opens feed: Fetch user's pre-computed Redis timeline + Merge & Sort live tweets    |
|    from all followed celebrities on-the-fly.                                                      |
+---------------------------------------------------------------------------------------------------+
```

---

## 3. Design Ride-Sharing Geospatial Dispatch (Uber / Lyft)

### Spatial Indexing: Geohash vs Google S2 vs Uber H3 Hexagonal Grid

```
+---------------------------------------------------------------------------------------------------+
| UBER H3 HEXAGONAL SPATIAL INDEXING                                                                |
|                                                                                                   |
| Why Hexagons (H3) beat Squares (Geohash):                                                         |
|  - In a square grid, diagonal neighbors are dist = sqrt(2) * side, while adjacent are dist = side.|
|  - In a HEXAGONAL grid (H3), all 6 neighboring cells are AT THE EXACT SAME EQUIDISTANT DISTANCE!  |
|                                                                                                   |
|                                  ┌───────┐                                                        |
|                                 /         \                                                       |
|                         ┌───────   Hex 2   ───────┐                                               |
|                        /         \       /         \                                              |
|                       │   Hex 1   │─────│   Hex 3   │                                             |
|                        \         /       \         /                                              |
|                         └───────   Hex 0   ───────┘                                               |
|                                 \ (Driver)/                                                       |
|                                  └───────┘                                                        |
|                                                                                                   |
| Driver Location Ingestion:                                                                        |
|  - Drivers emit GPS coords every 4 seconds.                                                       |
|  - Coords mapped to H3 Index Integer (e.g. Resolution 8, ~460m radius).                           |
|  - Stored in Redis Sorted Set: Key = "h3:8828308281fffff", Score = Timestamp, Member = Driver_ID  |
|                                                                                                   |
| Matchmaking Query (k-Ring Search):                                                                |
|  - Rider requests pickup at H3 cell H0.                                                           |
|  - Query H0 + ring of 6 immediate adjacent hexagon cells (O(1) in-memory lookup).                 |
+---------------------------------------------------------------------------------------------------+
```

---

## 4. Design Video Transcoding & Streaming (YouTube / Netflix)

```
+---------------------------------------------------------------------------------------------------+
| YOUTUBE VIDEO INGESTION & ADAPTIVE BITRATE (HLS/DASH) STREAMING                                   |
|                                                                                                   |
| 1. Direct-to-S3 Multipart Upload (Raw 4K MP4 File)                                                |
|       │                                                                                           |
|       ▼ S3 ObjectCreated Notification                                                             |
| 2. [ AWS SQS Queue ] ──► [ Transcoding Orchestrator (DAG Engine) ]                                 |
|                                      │ Splits video into 5-second GOP chunks                      |
|                                      ▼                                                            |
|                      ┌───────────────┴───────────────┐                                            |
|                      ▼                               ▼                                            |
|          [ GPU Transcoder Worker 1 ]     [ GPU Transcoder Worker 2 ]                              |
|          - Encodes 1080p (4000 Kbps)     - Encodes 720p (2000 Kbps)                               |
|          - Encodes 480p  (1000 Kbps)     - Encodes 360p (500 Kbps)                                |
|                      │                               │                                            |
|                      └───────────────┬───────────────┘                                            |
|                                      ▼                                                            |
| 3. Output S3 Bucket:                                                                              |
|    - `master.m3u8` (Manifest listing available resolutions & bitrates)                            |
|    - `/1080p/chunk_001.ts`, `/720p/chunk_001.ts`, `/480p/chunk_001.ts`                            |
|                                      │                                                            |
|                                      ▼ Origin Pull                                                |
| 4. [ Global Anycast CDN (Cloudflare / Fastly) ]                                                   |
|                                      │                                                            |
|                                      ▼ HLS Video Player (ExoPlayer / HLS.js)                      |
| 5. Client Video Player continuously adapts video resolution based on current client bandwidth!   |
+---------------------------------------------------------------------------------------------------+
```

---

## 5. Design a Distributed Unique ID Generator (Twitter Snowflake)
*Covered in detail with complete TypeScript code in [Module 05: Distributed Systems, Consensus & Transactions](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/05_distributed_systems_cap_pacelc_consensus_and_transactions.md).*

---

## 6. Design a Distributed Rate Limiter
*Covered in detail with complete Redis Lua scripts in [Module 03: Reliability, Fault Tolerance & Resilience Patterns](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/03_reliability_fault_tolerance_and_resilience_patterns.md).*

---

## 7. Design a Global URL Shortener Service (TinyURL)
*Covered in detail with complete base62 encoding and capacity planning in [Module 01: URL Shortener Service](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/01_url_shortener_service.md).*
