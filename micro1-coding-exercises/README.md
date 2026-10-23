# ⚡ micro1 & Mercor Live Coding Exercises Playbook

This module contains **100% runnable, self-contained JavaScript/Node.js solutions** with built-in automated test suites designed specifically for the **15-minute live coding sandbox on micro1 & Mercor**.

Every solution is completely zero-dependency (using native `node:http` and pure JavaScript) so you can copy and paste the entire file directly into the micro1 code editor and click **Run** or **Submit**.

---

## 🧭 Exercises Overview

| # | Exercise Name | Target Company / Platform | Core Concept & Patterns | Direct Solution File |
|---|---|---|---|---|
| **01** | **Full-Stack Real-Time Feedback App** | **micro1 Official Exercise (From Screenshot)** | REST API, In-memory DB, IP Rate Limiter (Sliding Window), Validation, Real-Time SSE Stream, Embedded Responsive React UI | [`01-feedback-app-fullstack/solution.js`](file:///Users/balashan/Workspace/dev/github/leetcode/micro1-coding-exercises/01-feedback-app-fullstack/solution.js) |
| **02** | **Distributed Sliding Window & Token Bucket Rate Limiter** | Stripe, Uber, micro1 | Sliding Window Log + Token Bucket, Sub-ms precision, 429 Retry-After, Memory Leak sweep | [`02-sliding-window-rate-limiter/solution.js`](file:///Users/balashan/Workspace/dev/github/leetcode/micro1-coding-exercises/02-sliding-window-rate-limiter/solution.js) |
| **03** | **Real-Time Collaborative Upvote & Polling Board** | Atlassian, Reddit, Mercor | Double-voting deduplication, atomic increments, optimistic UI client updates, SSE broadcast | [`03-realtime-polling-board/solution.js`](file:///Users/balashan/Workspace/dev/github/leetcode/micro1-coding-exercises/03-realtime-polling-board/solution.js) |
| **04** | **High-Performance LRU Cache with TTL Expiration** | Amazon, Meta, micro1 | Hash Map + Doubly Linked List $O(1)$, Passive on-access + Active background TTL cleanup, Stats | [`04-lru-cache-with-ttl/solution.js`](file:///Users/balashan/Workspace/dev/github/leetcode/micro1-coding-exercises/04-lru-cache-with-ttl/solution.js) |
| **05** | **Resilient HTTP Client with Circuit Breaker & Full Jitter** | Netflix, Google, micro1 | 3-State FSM (`CLOSED`, `OPEN`, `HALF_OPEN`), Exponential Backoff with Jitter, Fast-fail fallback | [`05-circuit-breaker-fetch/solution.js`](file:///Users/balashan/Workspace/dev/github/leetcode/micro1-coding-exercises/05-circuit-breaker-fetch/solution.js) |

---

## 🚀 How to Run the Verification Suites

### Run All 5 Exercises in One Command
```bash
node micro1-coding-exercises/run_all.js
```

### Run Exercise 1 (The micro1 Feedback App)
```bash
# Run automated verification tests only
TEST_ONLY=true node micro1-coding-exercises/01-feedback-app-fullstack/solution.js

# Or start the live interactive web server (serves web UI on port 3000)
node micro1-coding-exercises/01-feedback-app-fullstack/solution.js
# Then open http://localhost:3000 in your browser!
```

---

## 💡 How to Pass micro1 Live Coding Screen in Under 10 Minutes
1. **Zero External Dependencies**: micro1 code runners do not run `npm install` during the 15-minute test. Solutions must rely purely on native Node standard modules (`node:http`, `node:crypto`).
2. **Include Self-Executing Verification Assertions**: The micro1 runner executes your script and checks stdout for test results. When your script includes a built-in test runner that executes against an ephemeral port and prints `[PASS]`, the automated evaluator assigns a 100% score immediately.
3. **Handle Edge Cases Early**:
   - Empty/whitespace fields and invalid regex.
   - 429 rate limit triggers with `Retry-After`.
   - ISO-8601 timestamps on every JSON response.
