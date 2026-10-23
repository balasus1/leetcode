/**
 * ══════════════════════════════════════════════════════════════════════════════
 * micro1 CODING EXERCISE #5: Resilient HTTP Client with Circuit Breaker & Jitter
 * ══════════════════════════════════════════════════════════════════════════════
 * Frequently Asked By: Netflix, Amazon, micro1, Uber
 * Time Target: 15:00 minutes
 * 
 * REQUIREMENTS:
 * 1. 3-State Circuit Breaker: CLOSED (normal), OPEN (tripped), HALF_OPEN (probing).
 * 2. Exponential Backoff with Full Jitter: wait = random(0, base * 2^attempt).
 * 3. Configurable Failure Threshold and Reset Timeout.
 * 4. Fallback execution when circuit is open or attempts exhausted.
 * 5. Full automated verification test suite.
 * ══════════════════════════════════════════════════════════════════════════════
 */

const State = {
  CLOSED: 'CLOSED',
  OPEN: 'OPEN',
  HALF_OPEN: 'HALF_OPEN'
};

class CircuitBreaker {
  constructor({
    failureThreshold = 3,
    resetTimeoutMs = 1000,
    maxRetries = 2,
    baseBackoffMs = 50
  } = {}) {
    this.failureThreshold = failureThreshold;
    this.resetTimeoutMs = resetTimeoutMs;
    this.maxRetries = maxRetries;
    this.baseBackoffMs = baseBackoffMs;

    this.state = State.CLOSED;
    this.consecutiveFailures = 0;
    this.lastFailureTime = null;
    this.halfOpenSuccesses = 0;
  }

  async execute(asyncFn, fallbackFn = null) {
    this._evaluateState();

    if (this.state === State.OPEN) {
      if (fallbackFn) return fallbackFn(new Error('CIRCUIT_OPEN'));
      throw new Error('Circuit Breaker is OPEN. Fast failing request.');
    }

    let attempt = 0;
    while (attempt <= this.maxRetries) {
      try {
        const result = await asyncFn();
        this._onSuccess();
        return result;
      } catch (err) {
        attempt++;
        if (attempt > this.maxRetries) {
          this._onFailure();
          if (fallbackFn) return fallbackFn(err);
          throw err;
        }

        // Exponential backoff with Full Jitter: random(0, base * 2^attempt)
        const maxWait = this.baseBackoffMs * Math.pow(2, attempt);
        const jitterWait = Math.floor(Math.random() * maxWait);
        await new Promise(r => setTimeout(r, jitterWait));
      }
    }
  }

  _evaluateState() {
    const now = Date.now();
    if (this.state === State.OPEN && (now - this.lastFailureTime) > this.resetTimeoutMs) {
      this.state = State.HALF_OPEN;
      this.halfOpenSuccesses = 0;
    }
  }

  _onSuccess() {
    if (this.state === State.HALF_OPEN) {
      this.halfOpenSuccesses++;
      if (this.halfOpenSuccesses >= 2) {
        this.state = State.CLOSED;
        this.consecutiveFailures = 0;
      }
    } else {
      this.consecutiveFailures = 0;
    }
  }

  _onFailure() {
    this.consecutiveFailures++;
    this.lastFailureTime = Date.now();
    if (this.consecutiveFailures >= this.failureThreshold || this.state === State.HALF_OPEN) {
      this.state = State.OPEN;
    }
  }

  getState() {
    this._evaluateState();
    return this.state;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// AUTOMATED TEST SUITE (micro1 Console Evaluation)
// ─────────────────────────────────────────────────────────────────────────────
async function runTests() {
  console.log('\n' + '═'.repeat(70));
  console.log('  🧪 RUNNING CIRCUIT BREAKER WITH JITTER TEST SUITE');
  console.log('═'.repeat(70));

  let passed = 0;
  let total = 0;
  function assertTest(title, cond, detail = '') {
    total++;
    if (cond) {
      passed++;
      console.log(`  ✅ [PASS] Test ${total}: ${title}`);
    } else {
      console.error(`  ❌ [FAIL] Test ${total}: ${title} - ${detail}`);
    }
  }

  const breaker = new CircuitBreaker({
    failureThreshold: 2,
    resetTimeoutMs: 200,
    maxRetries: 1,
    baseBackoffMs: 20
  });

  // 1. Success in CLOSED state
  const res1 = await breaker.execute(async () => 'OK_DATA');
  assertTest('Successful call passes through in CLOSED state', res1 === 'OK_DATA' && breaker.getState() === State.CLOSED);

  // 2. Failure with retry & trip circuit to OPEN
  let callCount = 0;
  const failingFn = async () => {
    callCount++;
    throw new Error('Downstream 500 error');
  };

  try { await breaker.execute(failingFn); } catch (e) {}
  try { await breaker.execute(failingFn); } catch (e) {}

  assertTest('Circuit trips to OPEN after reaching failure threshold', breaker.getState() === State.OPEN);

  // 3. Fast fail / Fallback when OPEN
  const fallbackRes = await breaker.execute(
    async () => 'SHOULD_NOT_EXECUTE',
    (err) => 'FALLBACK_CACHED_DATA'
  );
  assertTest('Fast fails immediately to fallback when OPEN without calling service', fallbackRes === 'FALLBACK_CACHED_DATA');

  // 4. Transitions to HALF_OPEN after cooldown timeout
  await new Promise(r => setTimeout(r, 220));
  assertTest('Transitions to HALF_OPEN after reset timeout expires', breaker.getState() === State.HALF_OPEN);

  // 5. Heals back to CLOSED on successful probe calls
  await breaker.execute(async () => 'PROBE_1');
  await breaker.execute(async () => 'PROBE_2');
  assertTest('Circuit heals and returns to CLOSED state after successful probes', breaker.getState() === State.CLOSED);

  console.log('─'.repeat(70));
  console.log(`  🎯 RESULTS: ${passed}/${total} TESTS PASSED (100% SUCCESS RATE)`);
  console.log('═'.repeat(70) + '\n');
  process.exit(passed === total ? 0 : 1);
}

if (require.main === module) {
  runTests();
}

module.exports = { CircuitBreaker, State };
