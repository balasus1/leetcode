# Volume 2: High-Volume Distributed Ticketing, Sagas & Race Conditions (Q11 - Q20)
*Targeting Staff / Principal Full-Stack Interviews (Mercor, micro1, FAANG)*
*Candidate Context: Balasubramanian Shanmugham (Bahwan CyberTek 1M Users/Day Train Ticketing Architecture, Kafka Event-Driven Sagas, Redis)*

---

## Question 11: Provisional Seat Reservation Under 50,000-User Burst
**Interviewer:** *"When a high-demand train schedule opens, 50,000 users click 'Book Seat 42A' within the same second. If you use standard relational transactions with `SELECT ... WHERE status = 'AVAILABLE'`, the database locks up and double bookings happen. How did you architect the seat reservation system to guarantee zero double bookings and sub-10ms response times?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Relying on traditional database transactions with optimistic locking (`version` column) under a 50,000-user rush causes massive retry storms where 49,999 transactions fail and abort, overwhelming the relational database with deadlocks and rollback overhead.
>
> To solve this for our 1M daily user platform, I separated the **Provisional Seat Hold** from the **Final Financial Booking** using a fast, in-memory distributed reservation layer in **Redis** before touching PostgreSQL.
>
> When a user requests a seat, the request hits Redis with an atomic operation:
> `SET seat:{train_id}:{seat_no} {user_id} NX EX 600`
> Because Redis executes single-threaded on its event loop, exactly one user succeeds in setting the key with the `NX` (Not Exists) flag and gets a 10-minute TTL (`EX 600`). The remaining 49,999 requests fail immediately in under 2 milliseconds and receive a clean 'Seat already reserved' response without generating any database load.
>
> Once the Redis lock is acquired, an event is published to Kafka to initiate the **Saga checkout orchestration**. The user has a strict 10-minute window to complete payment. If payment succeeds, an asynchronous consumer persists the finalized ticket into PostgreSQL and clears the temporary hold. If payment fails or times out, a compensating action publishes a `SeatHoldReleasedEvent`, and a background worker deletes the Redis hold, immediately releasing the seat back to the inventory pool.
>
> The primary edge case is Redis node failover: if Redis crashes before master-replica replication syncs the key, a split-second window could allow a duplicate hold. We defended against this by adding a database-level uniqueness constraint on `(train_id, seat_no, travel_date)` in PostgreSQL during the final ticket issuance."

### Working Java Code
```java
package com.bala.ticketing.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Service
public class SeatReservationService {
    private final StringRedisTemplate redisTemplate;

    public SeatReservationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String holdSeat(String trainId, String seatId, String userId) {
        String lockKey = String.format("seat:%s:%s", trainId, seatId);
        String reservationToken = UUID.randomUUID() + ":" + userId;

        // Atomic SET if Not Exists with 10-minute TTL
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, reservationToken, Duration.ofMinutes(10));

        return Boolean.TRUE.equals(acquired) ? reservationToken : null;
    }

    public boolean releaseSeat(String trainId, String seatId, String reservationToken) {
        String lockKey = String.format("seat:%s:%s", trainId, seatId);
        String luaScript = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), reservationToken);
        return result != null && result > 0;
    }
}
```

---

## Question 12: Event-Driven Kafka Saga Choreography on Payment Failure
**Interviewer:** *"In your ticketing system, after a seat is held, the payment service times out or the bank declines the transaction. Walk me through your Kafka event-driven Saga choreography: what events are emitted, how do you handle out-of-order Kafka messages, and how do you prevent ghost tickets?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "When payments fail in a distributed microservices environment, Two-Phase Commit is unacceptable because holding database locks across external payment gateways like Stripe or Visa causes connection starvation. Instead, we use an **Event-Driven Choreography Saga** with idempotency gates.
>
> The flow executes as follows:
> 1. When the user submits payment, the Booking Service emits `PaymentInitiatedEvent`.
> 2. The Payment Service captures the intent and calls the payment gateway. If the payment fails (insufficient funds, gateway timeout, or decline), the service immediately publishes a `PaymentFailedEvent` to Kafka containing the `bookingId`, `seatId`, and a correlation ID.
> 3. The Inventory Service subscribes to `PaymentFailedEvent`. Upon consumption, it executes the compensating transaction: it evicts the provisional hold from Redis and marks the booking state as `CANCELLED_PAYMENT_FAILED` in the database.
>
> However, Kafka guarantees at-least-once delivery, which introduces two major failure modes: duplicate messages and out-of-order arrival. For instance, a `PaymentFailedEvent` could conceivably arrive before the corresponding `BookingCreatedEvent` due to consumer group rebalancing.
>
> We solve this using two architectural guards:
> First, **Idempotent Consumers with an Outbox Table**: Each consumer checks an `idempotent_events_processed` table within a local database transaction. If the `event_id` has already been processed, the message is acknowledged and skipped.
> Second, **State Machine Ordering Guards**: The booking entity follows a strict state transition matrix (`PENDING -> RESERVED -> PAID` or `CANCELLED`). If a `PaymentFailedEvent` arrives while the booking is still `PENDING`, the service rejects invalid forward state mutations or stores the compensation marker, guaranteeing that a delayed payment success can never mint a ghost ticket."

### Working TypeScript / Node.js Code
```typescript
import { EachMessagePayload } from 'kafkajs';
import { Pool } from 'pg';

interface PaymentFailedEvent {
  eventId: string;
  bookingId: string;
  trainId: string;
  seatId: string;
  reason: string;
}

export class BookingSagaCompensationConsumer {
  constructor(private pgPool: Pool) {}

  async handlePaymentFailure(payload: EachMessagePayload) {
    const raw = payload.message.value?.toString();
    if (!raw) return;
    const event: PaymentFailedEvent = JSON.parse(raw);
    const client = await this.pgPool.connect();

    try {
      await client.query('BEGIN');

      // Idempotency verification
      const check = await client.query('SELECT 1 FROM processed_events WHERE event_id = $1', [event.eventId]);
      if (check.rowCount && check.rowCount > 0) {
        await client.query('ROLLBACK');
        return; // Already processed
      }

      // Safe state machine update
      await client.query(
        `UPDATE bookings 
         SET status = 'CANCELLED', cancellation_reason = $1, updated_at = NOW() 
         WHERE booking_id = $2 AND status != 'CONFIRMED'`,
        [event.reason, event.bookingId]
      );

      await client.query(
        'INSERT INTO processed_events (event_id, event_type, processed_at) VALUES ($1, $2, NOW())',
        [event.eventId, 'PAYMENT_FAILED']
      );

      await client.query('COMMIT');
    } catch (err) {
      await client.query('ROLLBACK');
      throw err; // Trigger Kafka redelivery
    } finally {
      client.release();
    }
  }
}
```

---

## Question 13: Out-of-Order Event Arrival in Distributed Booking
**Interviewer:** *"In a distributed Kafka event-driven system with multiple partitions, network retries cause an `InventoryReleasedEvent` to arrive BEFORE the `BookingCreatedEvent`. How do you design state machines to handle reverse out-of-order arrival without corrupting business state?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In distributed event-driven systems, assuming that events will arrive in chronological order across distinct topics or partitions is a critical architectural flaw. Network retries, consumer restarts, or multi-partition topologies will inevitably cause events to arrive out of order.
>
> We solve this using **Causal State Versioning with Tombstone Pre-commit markers**:
>
> 1. Every event carries a monotonically increasing entity version number or logical sequence counter (`version: 2`).
> 2. When the consumer receives an `InventoryReleasedEvent` (version 2) for an entity that does not yet exist locally because `BookingCreatedEvent` (version 1) was delayed, the database does **not** fail or create an invalid orphan record.
> 3. Instead, the consumer writes a **Pending Pre-Cancellation Tombstone** in PostgreSQL:
>    `INSERT INTO bookings (booking_id, status, highest_seen_version) VALUES (:id, 'CANCELLED_PRE_RELEASE', 2);`
> 4. When the delayed `BookingCreatedEvent` (version 1) finally arrives 3 seconds later, the insertion logic inspects the highest seen version:
>    $$\text{incoming\_version } (1) < \text{current\_state\_version } (2)$$
>    The system immediately recognizes that this entity has already been superseded and canceled. It writes the metadata but keeps the status locked at `CANCELLED`, completely preventing a delayed create event from reviving a dead booking.
>
> This guarantees that our state transitions form a monotonic lattice: once an entity reaches a terminal failure state, earlier delayed lifecycle events can never regress the system."

---

## Question 14: High-Concurrency Ticket Issuance via `SKIP LOCKED`
**Interviewer:** *"When a background worker fleet processes thousands of confirmed bookings to generate official PDF tickets and QR codes, multiple worker pods poll the database table. Using simple `SELECT ... FOR UPDATE` causes workers to block each other. How does `FOR UPDATE SKIP LOCKED` eliminate worker lock contention?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "When scaling background worker pools to process pending jobs from a relational database, standard pessimistic locking using `SELECT ... FOR UPDATE` is a severe bottleneck. 
>
> If 20 worker threads execute `SELECT * FROM bookings WHERE status = 'PENDING_TICKET' LIMIT 10 FOR UPDATE`, worker 1 acquires locks on the first 10 rows. Workers 2 through 20 do not move to the next available rows; instead, they **block and queue up** waiting for worker 1 to commit and release its locks on those exact same 10 rows. This serializes background processing, resulting in high lock wait times and idle worker threads.
>
> To achieve linear, lock-free parallel worker scaling, we use PostgreSQL's `SELECT ... FOR UPDATE SKIP LOCKED`.
>
> When a worker executes:
> ```sql
> SELECT booking_id 
> FROM bookings 
> WHERE status = 'PENDING_TICKET' 
> ORDER BY created_at ASC 
> LIMIT 10 
> FOR UPDATE SKIP LOCKED;
> ```
> The database query engine scans the index and locks the first 10 matching rows. When the next worker executes the exact same query concurrently, the engine immediately **skips past** all currently locked rows and locks the *next* 10 unreserved rows without waiting or blocking.
>
> This converts a relational database table into an ultra-high-throughput, atomic work queue. Workers operate in complete parallel isolation with zero lock contention, maintaining linear throughput across dozens of pods."

### Working SQL & Spring Data Batch Worker
```java
package com.bala.ticketing.worker;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class TicketGenerationWorker {
    private final JdbcTemplate jdbcTemplate;

    public TicketGenerationWorker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelay = 200)
    @Transactional
    public void processPendingTickets() {
        // Atomic acquisition skipping locked rows
        String selectSql = 
            "SELECT booking_id FROM bookings " +
            "WHERE status = 'PAID' AND ticket_generated = false " +
            "ORDER BY created_at ASC " +
            "LIMIT 50 " +
            "FOR UPDATE SKIP LOCKED";

        List<String> bookingIds = jdbcTemplate.queryForList(selectSql, String.class);
        if (bookingIds.isEmpty()) return;

        for (String id : bookingIds) {
            generatePdfAndQr(id);
            jdbcTemplate.update("UPDATE bookings SET ticket_generated = true WHERE booking_id = ?", id);
        }
    }

    private void generatePdfAndQr(String bookingId) {
        // Fast QR code generation and PDF upload
    }
}
```

---

## Question 15: Mitigating Read-Replica Lag on Booking Confirmation
**Interviewer:** *"After a passenger completes payment, the application immediately redirects them to `/booking/confirmation/{id}`. Due to asynchronous PostgreSQL replication lag (e.g. 200ms lag), the read replica returns HTTP 404 'Booking Not Found', causing user panic. How do you eliminate read-replica lag issues in full-stack architectures?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "This is the classic **Read-Your-Own-Writes** consistency challenge in primary-replica database topologies. Writes are processed on the primary database, while read queries are load-balanced across read replicas. Because PostgreSQL streaming replication is asynchronous, there is a physical lag—typically 50ms to 500ms—before changes appear on replicas.
>
> When the user redirects immediately to the confirmation page, the read replica hasn't received the write yet, returning an erroneous 404 or an outdated 'Pending' status.
>
> We solve this using a multi-tiered routing strategy:
>
> 1. **Session-Level Replication Sticky Routing (Causal Consistency)**: When a mutation commits on the primary database, the backend sets a secure cookie or response header: `X-Write-Timestamp: <timestamp>`. During subsequent read requests, the API gateway checks this timestamp. If the user performed a write within the last 5 seconds, all read queries for that user session are forcefully routed directly to the **Primary Database**, bypassing read replicas entirely.
> 2. **Client-Side Cache Hydration via TanStack Query**: Instead of making an immediate GET request to the replica, the Next.js frontend uses TanStack Query's `setQueryData` to seed the confirmation cache directly from the mutation response payload. The user sees their confirmed ticket instantly with zero network round-trip.
> 3. **LSN-Based Replica Lag Verification**: For critical services, the gateway checks the PostgreSQL Log Sequence Number (LSN) of the transaction. If a read replica's replay LSN lags behind the write LSN, the query waits up to 100ms for the replica to catch up or falls back to primary."

---

## Question 16: Distributed Clock Drift in Provisional Seat Expiries
**Interviewer:** *"Your system uses a 10-minute provisional seat hold. Server A's clock drifts forward by 2 minutes, while Server B's clock drifts backward by 1 minute. Server A prematurely releases the seat while Server B believes the user still has 3 minutes to pay. How do you defend distributed workflows against NTP clock drift?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In distributed systems, relying on raw physical wall-clock timestamps (`System.currentTimeMillis()`) across disparate server nodes for critical business logic is fundamentally unsafe due to NTP clock skew, leap seconds, and asymmetric virtualized hypervisor pauses.
>
> We eliminate dependency on distributed wall clocks through two architectural controls:
>
> First, **Relative Monotonic Expirations in Redis**: Instead of calculating an absolute expiration timestamp on the application server (e.g. `now + 600000ms`) and storing that timestamp, we pass a **relative TTL duration** directly to Redis: `SET key val EX 600`.
> Redis manages expiration internally on a single server node using its internal monotonic clock. Regardless of whether Node A or Node B's operating system clock has drifted by 5 minutes, Redis expires the key exactly 600 monotonic seconds after creation.
>
> Second, **Server-Authoritative Validation During Checkout**: When the Payment Service attempts to finalize the booking, it does not trust the client's timestamp or its own local clock. It executes a Lua script on the Redis cluster:
> `redis.call('get', KEYS[1])`
> If the key has expired in Redis, the payment is rejected before contacting the credit card gateway.
>
> In AWS/OpenShift environments, we synchronize all underlying host nodes using **Amazon Time Sync Service** (which utilizes atomic clocks and satellite links with Chrony) to keep physical drift bounded under 1 millisecond across all data center zones."

---

## Question 17: Resilience4j Circuit Breakers for External Rail APIs
**Interviewer:** *"Your passenger booking flow integrates with an external government railway verification API. During peak hours, their API latency degrades from 200ms to 12 seconds, causing your Spring Boot connection threads to back up and crash your entire ticketing portal. How do you configure Resilience4j circuit breakers, timeouts, and fallback queues?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "This is a classic cascading failure: an unisolated, slow downstream third-party dependency exhausts the upstream caller's thread pool and socket connections, bringing down completely unrelated services.
>
> We isolate this using **Resilience4j Circuit Breaker, TimeLimiter, and Bulkhead patterns**:
>
> 1. **TimeLimiter**: We enforce a strict timeout budget of 1.5 seconds on every call to the external government API. Any call exceeding 1.5s is aborted immediately with a `TimeoutException`, releasing the calling thread.
> 2. **ThreadPoolBulkhead**: We assign the external API call its own isolated thread pool of 20 threads. Even if the external API completely freezes, at most 20 threads can ever be occupied, leaving our core ticketing thread pool untouched.
> 3. **Circuit Breaker State Machine**:
>    - Sliding window of 100 requests.
>    - If 50% of requests fail or time out, the circuit breaker trips from `CLOSED` to `OPEN`.
>    - In the `OPEN` state, calls are **fast-failed immediately in under 1 millisecond** without making any remote network call.
>    - After a `waitDurationInOpenState` of 10 seconds, it transitions to `HALF_OPEN` and permits 10 trial requests. If they succeed, it closes; if they fail, it re-opens.
>
> 4. **Graceful Fallback**: When the circuit is open, requests are diverted to an asynchronous queue or cached verification data, allowing passengers to proceed with tentative holds while background workers verify credentials once the upstream API recovers."

### Working Java / Spring Boot Configuration
```java
package com.bala.ticketing.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreaker externalRailCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f) // Trip if 50% fail
                .slowCallRateThreshold(50.0f) // Trip if 50% are slow
                .slowCallDurationThreshold(Duration.ofMillis(1500)) // > 1.5s is slow
                .slidingWindowSize(100)
                .minimumNumberOfCalls(20)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(10)
                .build();

        return CircuitBreaker.of("railGovApi", config);
    }

    @Bean
    public TimeLimiter externalRailTimeLimiter() {
        return TimeLimiter.of("railGovApi", TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(1500))
                .build());
    }
}
```

---

## Question 18: WebSocket Real-Time Train Tracking & Backpressure
**Interviewer:** *"You designed Java socket-based real-time communication for live train movement on your resume. When 100,000 active mobile apps connect to receive GPS coordinate updates every second, slow 3G mobile clients cause server buffer bloating and OOM. How do you handle reactive WebSocket backpressure and cluster broadcast scaling?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Broadcasting real-time GPS coordinates to 100,000 concurrent mobile clients over WebSockets presents two major scalability bottlenecks: memory buffer bloat from slow clients and multi-node broadcast fan-out.
>
> When a mobile client on a slow 3G cellular connection cannot read TCP packets as fast as the server generates train coordinates (e.g. 1 update per second), the server's outbound TCP socket send buffer and WebSocket session buffer rapidly fill up. If the server naively buffers messages for all 100,000 clients, the JVM heap exhausts within minutes.
>
> We resolve this using **Reactive Backpressure with Conflation (Latest-Value Eviction)**:
> For GPS coordinates, clients do not care about historical coordinates from 10 seconds ago; they only care about the **most recent coordinate**. In our Netty/Spring WebSocket pipeline, we configure an bounded ring buffer per client with a drop-oldest policy (`onBackpressureBuffer(1, BufferOverflowStrategy.DROP_OLDEST)`). If a client is slow, stale GPS updates are immediately dropped in memory, keeping outbound queues strictly capped at 1 message per client.
>
> To scale across 20 Spring Boot pods:
> Train GPS updates are published to a partitioned Kafka topic. Each pod subscribes to the stream and broadcasts only to its locally connected WebSocket sessions. To avoid cross-node broadcast storms, clients subscribe to specific train IDs (`/topic/train/{id}`), and pods register their interest in Redis Pub/Sub, routing GPS events only to pods that currently host listeners for that specific train."

---

## Question 19: Distributed Tracing with OpenTelemetry & W3C TraceContext
**Interviewer:** *"A passenger's booking request spans 5 microservices (Gateway -> Passenger -> Seat Inventory -> Payment -> Notification) and fails with an intermittent timeout. How do you implement end-to-end distributed tracing using OpenTelemetry and propagate trace context across HTTP and Kafka?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Debugging distributed latency spikes and failures across microservices without distributed tracing is impossible because independent service logs have no correlation.
>
> We implement **OpenTelemetry (OTel)** using the standard **W3C TraceContext** specification, which revolves around two core identifiers:
> 1. `traceparent`: A 4-part header: `00-<32-hex-trace-id>-<16-hex-parent-span-id>-01`. The `trace-id` remains constant across the entire transaction lifecycle.
> 2. `tracestate`: Carries vendor-specific routing and filtering tags.
>
> The flow executes as follows:
> - At the API Gateway, an inbound request either arrives with an existing trace header or the gateway generates a new root `trace-id`.
> - For synchronous HTTP REST calls, OpenTelemetry client interceptors automatically inject the `traceparent` header into outgoing HTTP request headers.
> - For asynchronous Kafka messaging, standard HTTP headers don't exist. Our Kafka Producer interceptor serializes the OTel trace context into **Kafka Record Headers** (`record.headers().add('traceparent', ...)`).
> - When the downstream Payment or Notification service consumes the Kafka message, an OTel Consumer interceptor extracts the trace context from the record header and creates a child span linked to the originating producer span.
>
> All spans stream asynchronously to an OpenTelemetry Collector and are visualized in Jaeger or Datadog, providing a complete waterfall timeline highlighting exactly which database query or network hop caused the delay."

---

## Question 20: Handling Abrupt Client Disconnections Mid-Checkout
**Interviewer:** *"A passenger selects seat 24B (holding it via Redis for 10 minutes), enters their credit card, and suddenly closes their browser tab or loses cellular connectivity. How does your system release provisional holds gracefully without forcing other passengers to wait the full 10-minute timeout?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "If every passenger who closes their browser or abandons their cart ties up a seat for the entire 10-minute TTL, popular train departures quickly show zero available seats when in reality half the seats are orphaned holds.
>
> We address this using a hybrid **Client Heartbeat + Ephemeral Sub-lease Architecture**:
>
> 1. **Short Heartbeat Leases**: Instead of giving an unconditional 10-minute Redis TTL upfront, the initial provisional hold is set with an active lease of **60 seconds**:
>    `SET seat:{train}:{seat} {userId} NX EX 60`
> 2. **Client-Side Heartbeat**: While the checkout tab is open, a lightweight background JavaScript worker pings an endpoint every 20 seconds (`/api/v1/booking/keep-alive`). Each heartbeat renews the Redis TTL back to 60 seconds, up to a maximum hard cap of 10 minutes.
> 3. **Beacon API on Window Close**: When the user explicitly closes the tab, we invoke `navigator.sendBeacon('/api/v1/booking/release', payload)`. The browser guarantees transmission of this asynchronous POST beacon even as the page unloads, immediately triggering the Redis key deletion.
> 4. **Abrupt Disconnect Handling**: If the user's phone battery dies or connectivity drops completely, the heartbeats stop. After 60 seconds of silence, Redis automatically evicts the key via its TTL. A Redis Keyspace Notification (`__keyevent@0__:expired`) fires, prompting a background worker to publish a `SeatHoldExpiredEvent` to Kafka, returning the seat to the public search index in just 1 minute instead of 10."

### Working JavaScript Frontend Beacon & Heartbeat
```typescript
// React / Next.js Checkout Lifecycle Hook
import { useEffect } from 'react';

export function useSeatHoldKeepAlive(trainId: string, seatId: string, token: string) {
  useEffect(() => {
    // 1. Periodic heartbeat every 20 seconds
    const interval = setInterval(async () => {
      try {
        await fetch('/api/v1/booking/heartbeat', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ trainId, seatId, token })
        });
      } catch (err) {
        console.warn('Heartbeat failed, network down');
      }
    }, 20_000);

    // 2. Unload beacon for immediate tab closure release
    const handleUnload = () => {
      const payload = JSON.stringify({ trainId, seatId, token });
      navigator.sendBeacon('/api/v1/booking/release', payload);
    };

    window.addEventListener('beforeunload', handleUnload);

    return () => {
      clearInterval(interval);
      window.removeEventListener('beforeunload', handleUnload);
    };
  }, [trainId, seatId, token]);
}
```
