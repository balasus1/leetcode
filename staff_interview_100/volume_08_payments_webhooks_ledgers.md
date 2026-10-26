# Volume 8: Financial Payments, Webhook Security & Ledgers (Q71 - Q80)
*Targeting Staff / Principal Full-Stack Interviews (Mercor, micro1, FAANG)*
*Candidate Context: Balasubramanian Shanmugham (Payment Gateway Sagas, Detica AML Platform, Factiva Sanctions Screening)*

---

## Question 71: Webhook Cryptographic Verification & Replay Attack Defense
**Interviewer:** *"When handling Stripe or Razorpay payment webhooks, networks drop and gateways retry webhooks up to 72 hours later. How do you design a payment webhook handler that handles out-of-order retries, defends against replay attacks, and ensures account balances are never credited twice?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Payment webhooks must be treated with zero trust because they originate over the public internet and are subject to network retries, replay attacks, and clock drift.
>
> We secure our webhook processing using three strict security and architectural layers:
>
> 1. **Cryptographic Signature Verification**: We inspect the webhook header (e.g. `Stripe-Signature`). We extract the timestamp $t$ and signature $v1$. We compute an HMAC-SHA256 signature using the raw request body and our shared webhook secret. If the signature does not match, the request is rejected with 400 Bad Request.
> 2. **Replay Attack Defense**: We compare the signature timestamp $t$ against server time. If $|t_{server} - t| > 300\text{ seconds}$ (5 minutes), we drop the request to prevent malicious actors from capturing and replaying old payment packets.
> 3. **Transactional Idempotency**: Before mutating financial records, we check a `payment_transactions` table using a unique `idempotency_key` (the gateway's `event_id`):
>    - If the event exists with status `PROCESSED`, we immediately return HTTP 200 OK without re-running business logic.
>    - If it's new, we insert an entry with status `PROCESSING` and execute the balance update inside a single database transaction.
>
> If an unexpected error occurs during credit allocation, the transaction rolls back, allowing subsequent gateway retries to succeed cleanly."

### Working Java / Spring Boot Payment Webhook Controller
```java
package com.bala.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/v1/webhooks")
public class PaymentWebhookController {

    private static final String WEBHOOK_SECRET = "whsec_prod_secret_token";

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String rawPayload,
            @RequestHeader("Stripe-Signature") String signatureHeader) {

        if (!verifySignature(rawPayload, signatureHeader, WEBHOOK_SECRET)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Signature");
        }

        // Process idempotently
        return ResponseEntity.ok("PROCESSED");
    }

    private boolean verifySignature(String payload, String sigHeader, String secret) {
        try {
            String[] parts = sigHeader.split(",");
            String timestamp = parts[0].split("=")[1];
            String expectedHash = parts[1].split("=")[1];

            long eventTime = Long.parseLong(timestamp);
            if (Math.abs(System.currentTimeMillis() / 1000 - eventTime) > 300) {
                return false; // Replay window exceeded
            }

            String signedPayload = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String computedHash = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));

            return MessageDigest.isEqual(computedHash.getBytes(), expectedHash.getBytes());
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## Question 72: Double-Entry Bookkeeping Ledger Database Schema
**Interviewer:** *"Why is storing user money as a single mutable column (`balance INT`) in a `users` table a catastrophic anti-pattern in financial engineering? How do you design an immutable double-entry bookkeeping ledger in PostgreSQL?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Storing user money as a single mutable number in a database—such as `UPDATE users SET balance = balance + 50 WHERE id = 101`—is a fatal flaw in financial systems. If a transaction crashes midway, an un-indexed query updates the wrong row, or a race condition occurs, you have no historical trail of *where* the money came from, *who* authorized it, or *why* the balance changed. You cannot pass a financial audit.
>
> In professional financial engineering, we use **Double-Entry Bookkeeping**:
>
> 1. **Money is Never Created or Destroyed**: Every financial transaction must consist of at least two entries: an equal **Debit** and **Credit**. The fundamental invariant is:
>    $$\sum \text{Debits} = \sum \text{Credits} \quad (\text{Net Balance Change} = 0)$$
> 2. **Immutable Append-Only Records**: Once an entry is written to the ledger, it is **never updated or deleted**. If an error was made, a compensating reversal transaction is appended.
> 3. **The Schema**:
>    - `accounts`: Represents system accounts (Cash, Accounts Receivable, User Wallets).
>    - `transactions`: Represents the business event (e.g. Order #9021).
>    - `journal_entries`: The individual debit and credit rows linked to an account and transaction.
>
> A user's current balance is derived as the sum of their historical debits and credits:
> `SELECT SUM(amount) FROM journal_entries WHERE account_id = :id;`
> For high-speed lookups, we maintain snapshot checkpoints, but the immutable journal entries remain the sole source of financial truth."

### Working PostgreSQL Double-Entry Schema
```sql
CREATE TABLE accounts (
    account_id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    account_type VARCHAR(32) NOT NULL, -- ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE transactions (
    transaction_id UUID PRIMARY KEY,
    idempotency_key VARCHAR(128) UNIQUE NOT NULL,
    description TEXT NOT NULL,
    posted_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE journal_entries (
    entry_id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(transaction_id),
    account_id UUID NOT NULL REFERENCES accounts(account_id),
    amount NUMERIC(18, 4) NOT NULL, -- Positive for Credit, Negative for Debit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Constraint: Verify transaction debits equal credits before commit
CREATE OR REPLACE FUNCTION verify_transaction_balance() RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT SUM(amount) FROM journal_entries WHERE transaction_id = NEW.transaction_id) != 0 THEN
        RAISE EXCEPTION 'Transaction unbalanced: sum of debits and credits must equal zero!';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

---

## Question 73: Idempotency Key Architecture: Preventing Double Charges
**Interviewer:** *"A customer clicks 'Pay $100'. Their browser establishes an HTTP connection, the payment gateway charges their credit card, but the return network packet drops. The customer's browser shows a timeout, and they click 'Pay $100' again. How does an Idempotency Key architecture prevent them from being billed twice?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "This is the classic **Network Uncertainty Problem** in distributed payments. The client knows the request was sent, but cannot know whether the server executed the charge or crashed before returning the HTTP response.
>
> We prevent double charges using **Client-Generated Idempotency Keys backed by Redis and PostgreSQL**:
>
> 1. **Key Generation**: When the user opens the checkout modal, the frontend generates a unique UUIDv4 `Idempotency-Key` and attaches it to the HTTP POST header.
> 2. **Atomic Locking in Redis**:
>    When the API Gateway receives the request, it executes:
>    `SET lock:idempotency:{key} 'PROCESSING' NX EX 120`
>    - If the key already exists and has status `PROCESSING`, another thread or retry is currently in flight. The gateway returns HTTP 409 Conflict ('Payment in progress').
> 3. **Database Check & Execution**:
>    The server checks if a payment with this idempotency key has already committed in PostgreSQL.
>    - If it has already completed, the server immediately returns the **cached historical response** (HTTP 200 with receipt) without contacting the bank.
>    - If it's a new payment, it charges the gateway, writes the transaction to PostgreSQL, and updates the Redis idempotency key with status `COMPLETED` and the serialized response payload.
>
> When the user clicks 'Pay' the second time, the system recognizes the idempotency key, skips the credit card charge, and returns the existing receipt, guaranteeing zero duplicate charges."

---

## Question 74: Asynchronous Webhooks Arriving Before Frontend Redirects
**Interviewer:** *"After completing 3D-Secure credit card verification, the customer's browser redirects back to `/checkout/success`. Concurrently, the payment gateway emits an asynchronous server-to-server webhook. Because mobile redirects are slow, the webhook arrives first; or conversely, the redirect arrives first. How do you reconcile these two asynchronous notification paths?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In modern payment processing (like Stripe Checkout or 3D-Secure), the return flow consists of two independent paths:
> 1. The **Synchronous User Redirect**: The customer's browser is redirected to `/checkout/success?session_id=cs_123`.
> 2. The **Asynchronous Webhook**: The payment gateway fires an HTTP POST `checkout.session.completed` directly to your backend servers.
>
> Because these two events traverse different network routes, either can arrive first.
>
> We architect this using a **State Machine with Optimistic Polling**:
>
> - **The Webhook is Authoritative**: The webhook is the cryptographic, server-to-server source of truth. When the webhook arrives, it transitions the database order status from `PENDING` to `PAID` and provisions the purchased tickets or entitlements.
> - **The Frontend Never Trusts the URL Query Parameter**: The redirect URL (`/checkout/success?session_id=...`) is merely a visual navigation hint. An attacker could type that URL directly into their browser.
> - **Client Hydration via Polling/WebSocket**: When the user lands on the success page, the frontend queries: `GET /api/orders/{sessionId}`.
>   - If the webhook has already arrived, the order is `PAID`, and the receipt displays immediately.
>   - If the webhook is still in flight (taking 1 to 2 seconds across the gateway), the order status is `PENDING`. The frontend displays a smooth 'Confirming your payment...' skeleton and polls every 1 second (up to 5 attempts) using TanStack Query, transitioning to confirmed the instant the webhook commits."

---

## Question 75: Real-Time Anti-Money Laundering (AML) Rules Engines (Detica Heritage)
**Interviewer:** *"On your resume at Digital Harbor / Detica, you built Watchlist and Scenario Manager modules for an anti-money laundering platform integrating Factiva screening. How do you detect transaction velocity spikes (e.g. 5 transactions under $9,999 within 30 minutes to avoid the $10,000 CTR reporting threshold) in real time?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In anti-money laundering (AML), criminals frequently engage in **Structuring (Smurfing)**: breaking down a large cash deposit into multiple smaller transactions just below the $10,000 Currency Transaction Report (CTR) regulatory threshold (e.g. four $9,500 transfers within 30 minutes) to avoid detection.
>
> At Detica, I designed real-time scenario rules engines to detect these behavioral patterns using **Stream Processing with Sliding Time Windows**:
>
> 1. **Real-Time Sliding Window State in Redis**:
>    When a transaction occurs, the system does not run slow SQL aggregations across millions of historical rows. Instead, it appends the transaction amount and timestamp to a Redis Sorted Set keyed by customer:
>    `ZADD aml:velocity:{customerId} {timestamp} "{txId}:{amount}"`
> 2. **Scenario Evaluation**:
>    A sliding window query runs in under 1 millisecond:
>    - Remove entries older than 30 minutes: `ZREMRANGEBYSCORE ... 0 (now - 1800000)`.
>    - Read remaining entries within the window: `ZRANGE aml:velocity:{customerId} 0 -1`.
> 3. **Rule Trigger**:
>    The rule engine evaluates:
>    $$\text{Count}(tx) \ge 3 \quad \text{AND} \quad \text{All}(amount \ge 9000 \text{ AND } amount < 10000) \quad \text{AND} \quad \sum amount > 25000$$
>    If matched, an AML Suspicious Activity Report (SAR) case is automatically created in the Scenario Manager, the customer account is temporarily flagged for manual review, and the transaction is placed on hold."

---

## Question 76: Handling Partial Refunds & Automated Dispute State Machines
**Interviewer:** *"A customer places a $300 order consisting of 3 items ($100 each). They return item 1 (partial refund), item 2 is charged back by the credit card issuer as fraud, and item 3 is kept. How do you design an order and payment state machine that models partial refunds and chargeback disputes without balance corruption?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Modeling orders with a single binary status (`PAID`, `REFUNDED`) breaks down when partial refunds, chargebacks, and returns occur. 
>
> We decouple the order into an **Item-Level State Machine paired with a Multi-Entry Ledger**:
>
> 1. **Order Item States**: Each line item maintains its own lifecycle status:
>    - Item 1: `RETURNED_REFUNDED`
>    - Item 2: `DISPUTED_CHARGEBACK`
>    - Item 3: `DELIVERED_SETTLED`
> 2. **Financial Ledger Accounting**:
>    - When the $100 partial refund for Item 1 is issued: The gateway emits `charge.refunded`. The ledger creates a credit entry to the User Account ($100) and debits the Merchant Settlement Account ($100). The total order refunded amount is updated: `refunded_amount = 100.00`.
> 3. **Chargeback Dispute Handling**:
>    When a chargeback dispute occurs for Item 2, the payment network forcefully claws back the $100 plus an additional $15 dispute fee.
>    The webhook emits `charge.dispute.created`.
>    - The system transitions Item 2 to `DISPUTED`.
>    - The ledger creates two entries: $100 debited from Merchant Escrow to Network, and a $15 Dispute Expense debited to the Merchant.
>    - An automated dispute case is created with an evidence upload deadline.
>
> If the merchant wins the dispute, the $100 is credited back; if lost, it transitions to `CHARGEBACK_LOST`. The parent order status is derived dynamically: `PARTIALLY_REFUNDED_UNDER_DISPUTE`."

---

## Question 77: Two-Phase Commit (2PC) vs Saga: The Distributed Locking Trap
**Interviewer:** *"Why does using Two-Phase Commit (2PC / XA Transactions) across microservices in a banking application create catastrophic latency and thread exhaustion during network partitions, and how does the Saga pattern solve this?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Two-Phase Commit (2PC) was designed to provide atomic distributed transactions across multiple database nodes by having a central Coordinator execute a **Prepare Phase** followed by a **Commit Phase**.
>
> Why is 2PC an anti-pattern for modern microservices?
> Because 2PC is a **blocking protocol**.
> During Phase 1 (Prepare), every participating database cohort executes local SQL statements and **acquires exclusive database locks on affected account rows**. The cohorts must hold these exclusive locks until the Coordinator broadcasts Phase 2 (Commit).
>
> If a network partition occurs, or if the Coordinator crashes between Phase 1 and Phase 2, the cohorts are left in a state of limbo: they cannot unilaterally commit or abort.
>
> While waiting, **those row locks remain held**. Any other customer attempting to transfer funds or query balances on those accounts blocks. Within seconds, database connection pools across all microservices saturate, threads back up, and the entire banking platform crashes in a domino cascade.
>
> The **Saga Pattern** solves this by abandoning distributed locks. Each microservice executes its local transaction immediately, commits, and releases its database locks in under 5ms. If a downstream step fails, the Saga emits compensating events that execute reverse business logic (e.g. issuing a refund), exchanging immediate consistency for **eventual consistency** and 100x higher availability."

---

## Question 78: Database Deadlock Detection in Concurrent Balance Transfers
**Interviewer:** *"Thread 1 transfers $50 from Account A to Account B. Thread 2 transfers $30 from Account B to Account A simultaneously. Both execute `SELECT ... FOR UPDATE`. The database detects a deadlock and aborts one transaction. How do you prevent deadlocks in financial transfers?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "This is the textbook **Cross-Resource Deadlock** in concurrent database transactions.
>
> - Thread 1 executes `SELECT balance FROM accounts WHERE id = 'A' FOR UPDATE` (acquires exclusive lock on Account A).
> - Thread 2 concurrently executes `SELECT balance FROM accounts WHERE id = 'B' FOR UPDATE` (acquires exclusive lock on Account B).
> - Next, Thread 1 attempts to lock Account B, but blocks waiting for Thread 2.
> - Simultaneously, Thread 2 attempts to lock Account A, but blocks waiting for Thread 1.
> - Neither thread can proceed. The PostgreSQL deadlock detector wakes up after `deadlock_timeout` (typically 1 second) and forcibly terminates one transaction with an SQL error: `deadlock detected`.
>
> We prevent deadlocks completely by enforcing an invariant: **Global Lock Ordering by Primary Key**:
>
> Regardless of which account is the sender and which is the receiver, application code **always acquires locks in ascending alphanumeric order of Account IDs**:
>
> ```java
> String firstLock = accountA.compareTo(accountB) < 0 ? accountA : accountB;
> String secondLock = accountA.compareTo(accountB) < 0 ? accountB : accountA;
>
> lockAccount(firstLock);
> lockAccount(secondLock);
> ```
>
> Because both Thread 1 and Thread 2 always attempt to lock Account A first before attempting to lock Account B, Thread 2 blocks on the very first statement before acquiring any locks. Circular wait is mathematically impossible, eliminating deadlocks entirely."

### Working Java Lock-Ordering Transfer Service
```java
package com.bala.payment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class DeadlockFreeTransferService {
    private final JdbcTemplate jdbcTemplate;

    public DeadlockFreeTransferService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void transferFunds(String fromAcc, String toAcc, BigDecimal amount) {
        // Enforce deterministic global lock order
        String first = fromAcc.compareTo(toAcc) < 0 ? fromAcc : toAcc;
        String second = fromAcc.compareTo(toAcc) < 0 ? toAcc : fromAcc;

        // 1. Lock in sorted order (prevents circular wait deadlocks)
        jdbcTemplate.queryForObject("SELECT account_id FROM accounts WHERE account_id = ? FOR UPDATE", String.class, first);
        jdbcTemplate.queryForObject("SELECT account_id FROM accounts WHERE account_id = ? FOR UPDATE", String.class, second);

        // 2. Execute atomic balance transfer
        jdbcTemplate.update("UPDATE accounts SET balance = balance - ? WHERE account_id = ?", amount, fromAcc);
        jdbcTemplate.update("UPDATE accounts SET balance = balance + ? WHERE account_id = ?", amount, toAcc);
    }
}
```

---

## Question 79: Automated Daily Financial Reconciliation Cron Jobs
**Interviewer:** *"Every night at 02:00 UTC, your financial platform runs an automated reconciliation job comparing internal database transaction ledgers against Stripe/bank settlement CSV reports. How do you design an audit reconciliation job that isolates discrepancies down to the cent across millions of transactions?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In financial platforms processing millions of transactions, discrepancies inevitably occur: bank network drops can cause a charge to settle on Stripe that never confirmed in your database, or currency conversion rounding can introduce 1-cent drift.
>
> We design our **Automated Financial Reconciliation Engine** using a three-way matching pipeline:
>
> 1. **Data Ingestion**: At 02:00 UTC, the scheduler downloads the bank settlement report (Stripe Balance Transaction CSV / MT940 bank statement) for the prior day via SFTP/API and loads it into a temporary staging table: `external_settlement_staging`.
> 2. **Three-Way Join Reconciliation**:
>    A SQL batch job performs a Full Outer Join between internal `journal_entries` and `external_settlement_staging` matched on `payment_gateway_ref`:
>    ```sql
>    SELECT 
>      COALESCE(i.tx_id, e.external_id) AS tx_ref,
>      i.amount AS internal_amt,
>      e.amount AS external_amt,
>      (COALESCE(i.amount, 0) - COALESCE(e.amount, 0)) AS variance
>    FROM internal_entries i
>    FULL OUTER JOIN external_settlement_staging e 
>      ON i.gateway_ref = e.external_id
>    WHERE i.amount != e.amount OR i.tx_id IS NULL OR e.external_id IS NULL;
>    ```
> 3. **Discrepancy Categorization**:
>    - *Type 1 (Missing in Internal)*: Bank collected money, but database shows pending. Automatically re-queries gateway and updates ledger.
>    - *Type 2 (Missing in Bank)*: Internal ledger shows success, but bank has no record. Flags immediate fraud/failure alert.
>    - *Type 3 (Variance != 0)*: Rounding or fee discrepancy. Route to Finance Reconciliation Dashboard.
>
> An audit summary report is signed cryptographically and emitted to compliance archives."

---

## Question 80: High-Concurrency Wallet Updates: OCC vs Pessimistic Locks
**Interviewer:** *"A viral flash-sale wallet service processes 10,000 balance deductions per second. Compare Optimistic Concurrency Control (OCC via `@Version`) against Pessimistic Locking (`SELECT FOR UPDATE`). At what contention threshold does OCC degrade, and how do you achieve peak throughput?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Choosing between Optimistic Concurrency Control (OCC) and Pessimistic Locking depends entirely on the **contention density** of your write workload:
>
> 1. **Optimistic Concurrency Control (OCC)**:
>    Transactions read the row without acquiring locks and write back using:
>    `UPDATE wallets SET balance = :newBal, version = version + 1 WHERE id = :id AND version = :oldVersion;`
>    - *When it excels*: Under low-contention workloads (e.g. 10,000 requests spread across 10,000 *different* user wallets), OCC has zero lock overhead, zero lock wait time, and maximum throughput.
>    - *The Failure Mode*: Under high-contention hotspots (e.g. 5,000 users attempting to deduct from a *single shared merchant wallet*), OCC collapses: 1 transaction succeeds and 4,999 abort with `OptimisticLockException`. Retrying 4,999 transactions causes exponential retry storms that melt the CPU.
>
> 2. **Pessimistic Locking (`SELECT FOR UPDATE`)**:
>    Acquires an exclusive database lock immediately upon read.
>    - *When it excels*: Under high contention, pessimistic locking queues requests deterministically without wasted CPU retry aborts.
>    - *The Failure Mode*: Under high traffic, long queue lengths cause connection pool exhaustion and thread starvation.
>
> **The Architect's Solution for 10,000 updates/sec**:
> For ultra-high-contention wallets, we use neither raw OCC nor pessimistic database locks. We move wallet deductions into an **in-memory Redis queue executing atomic Lua scripts** (`redis.call('decrby', key, amt)`), and stream balance changes asynchronously to PostgreSQL in batched background intervals."
