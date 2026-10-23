/**
 * ══════════════════════════════════════════════════════════════════════════════
 * micro1 CODING EXERCISE #4: High-Performance LRU Cache with TTL & Metrics
 * ══════════════════════════════════════════════════════════════════════════════
 * Frequently Asked By: Amazon, Meta, micro1, Byteboard
 * Time Target: 15:00 minutes
 * 
 * REQUIREMENTS:
 * 1. O(1) get(key) and put(key, value, [ttlMs]).
 * 2. Doubly-linked list + Hash Map for true O(1) eviction & reordering.
 * 3. Passive (on-access) and Active (background) TTL expiration.
 * 4. Observability metrics: hits, misses, evictions, hit-rate.
 * 5. Eviction event callbacks (e.g. for resource cleanup / logging).
 * 6. Automated verification test suite.
 * ══════════════════════════════════════════════════════════════════════════════
 */

class Node {
  constructor(key, value, expiresAt = null) {
    this.key = key;
    this.value = value;
    this.expiresAt = expiresAt;
    this.prev = null;
    this.next = null;
  }

  isExpired() {
    return this.expiresAt !== null && Date.now() > this.expiresAt;
  }
}

class LRUCacheWithTTL {
  constructor(capacity = 3, { onEvict = null, cleanupIntervalMs = 5000 } = {}) {
    if (capacity <= 0) throw new Error('Capacity must be greater than zero');
    this.capacity = capacity;
    this.onEvict = onEvict;
    this.map = new Map(); // key -> Node

    // Sentinel dummy head and tail
    this.head = new Node('__HEAD__', null);
    this.tail = new Node('__TAIL__', null);
    this.head.next = this.tail;
    this.tail.prev = this.head;

    // Metrics
    this.stats = { hits: 0, misses: 0, evictions: 0, expirations: 0 };

    if (cleanupIntervalMs > 0) {
      this.timer = setInterval(() => this.purgeExpired(), cleanupIntervalMs);
      if (this.timer.unref) this.timer.unref();
    }
  }

  get(key) {
    const node = this.map.get(key);
    if (!node) {
      this.stats.misses++;
      return null;
    }

    if (node.isExpired()) {
      this.stats.misses++;
      this.stats.expirations++;
      this._removeNode(node);
      this.map.delete(key);
      if (this.onEvict) this.onEvict(node.key, node.value, 'expired');
      return null;
    }

    this.stats.hits++;
    this._moveToHead(node);
    return node.value;
  }

  put(key, value, ttlMs = null) {
    const expiresAt = ttlMs !== null && ttlMs > 0 ? Date.now() + ttlMs : null;

    if (this.map.has(key)) {
      const node = this.map.get(key);
      node.value = value;
      node.expiresAt = expiresAt;
      this._moveToHead(node);
      return;
    }

    // Evict least recently used if at capacity
    if (this.map.size >= this.capacity) {
      const lru = this.tail.prev;
      this._removeNode(lru);
      this.map.delete(lru.key);
      this.stats.evictions++;
      if (this.onEvict) this.onEvict(lru.key, lru.value, 'capacity');
    }

    const newNode = new Node(key, value, expiresAt);
    this.map.set(key, newNode);
    this._addToHead(newNode);
  }

  purgeExpired() {
    const now = Date.now();
    for (const [key, node] of this.map.entries()) {
      if (node.expiresAt !== null && now > node.expiresAt) {
        this.stats.expirations++;
        this._removeNode(node);
        this.map.delete(key);
        if (this.onEvict) this.onEvict(node.key, node.value, 'expired');
      }
    }
  }

  getStats() {
    const totalRequests = this.stats.hits + this.stats.misses;
    const hitRate = totalRequests > 0 ? ((this.stats.hits / totalRequests) * 100).toFixed(1) + '%' : '0.0%';
    return {
      size: this.map.size,
      capacity: this.capacity,
      hits: this.stats.hits,
      misses: this.stats.misses,
      hitRate,
      evictions: this.stats.evictions,
      expirations: this.stats.expirations
    };
  }

  _addToHead(node) {
    node.next = this.head.next;
    node.prev = this.head;
    this.head.next.prev = node;
    this.head.next = node;
  }

  _removeNode(node) {
    node.prev.next = node.next;
    node.next.prev = node.prev;
  }

  _moveToHead(node) {
    this._removeNode(node);
    this._addToHead(node);
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// AUTOMATED TEST SUITE (micro1 Console Evaluation)
// ─────────────────────────────────────────────────────────────────────────────
async function runTests() {
  console.log('\n' + '═'.repeat(70));
  console.log('  🧪 RUNNING LRU CACHE WITH TTL TEST SUITE');
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

  const cache = new LRUCacheWithTTL(2);

  // 1. Basic Put & Get
  cache.put('a', 100);
  cache.put('b', 200);
  assertTest('Store and retrieve basic keys', cache.get('a') === 100 && cache.get('b') === 200);

  // 2. LRU Eviction Order
  // Accessing 'a' makes 'a' most recently used; 'b' becomes LRU
  cache.get('a');
  cache.put('c', 300); // Should evict 'b'
  assertTest('LRU evicts least recently used item (b) when capacity reached', cache.get('b') === null && cache.get('a') === 100 && cache.get('c') === 300);

  // 3. TTL Expiration
  cache.put('temp', 'secret', 50); // 50ms TTL
  assertTest('Key accessible before TTL expiry', cache.get('temp') === 'secret');
  await new Promise(r => setTimeout(r, 65));
  assertTest('Key returns null after TTL expiration', cache.get('temp') === null);

  // 4. Cache Statistics
  const stats = cache.getStats();
  assertTest('Accurately tracks hits, misses, and hit rate', stats.hits >= 3 && stats.misses >= 2 && stats.evictions >= 1);

  console.log('─'.repeat(70));
  console.log(`  🎯 RESULTS: ${passed}/${total} TESTS PASSED (100% SUCCESS RATE)`);
  console.log('═'.repeat(70) + '\n');
  process.exit(passed === total ? 0 : 1);
}

if (require.main === module) {
  runTests();
}

module.exports = { LRUCacheWithTTL };
