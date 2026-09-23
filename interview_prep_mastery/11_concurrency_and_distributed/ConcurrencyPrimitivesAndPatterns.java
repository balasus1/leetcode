package interview_prep_mastery._11_concurrency_and_distributed;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * ============================================================================
 * MODULE 11: CONCURRENCY & MULTITHREADING MASTER SUITE
 * Topics:
 * 1. Custom Bounded Blocking Queue (ReentrantLock + 2 Conditions)
 * 2. High-Throughput Thread-Safe LRU Cache
 * 3. Token Bucket Rate Limiter (Lock-Free / Atomic & Lock-based)
 * 4. Custom Thread Pool Executor (Worker Threads & Work Queue)
 * 5. Alternating Threads Coordination (FooBar using Semaphores)
 * ============================================================================
 */
public class ConcurrencyPrimitivesAndPatterns {

    // ------------------------------------------------------------------------
    // 1. Custom Bounded Blocking Queue (Producer-Consumer Engine)
    // ------------------------------------------------------------------------
    /**
     * Interview Probing Questions & Invariants:
     * - Why use two Conditions (notFull, notEmpty) instead of Object wait/notify?
     *   -> Two conditions prevent thread thrashing and false wakeups: producers only
     *      signal consumers (notEmpty), and consumers only signal producers (notFull).
     * - Why use while(count == capacity) instead of if?
     *   -> Guards against spurious wakeups per Java Concurrency in Practice guidelines.
     */
    public static class BoundedBlockingQueue<T> {
        private final Object[] items;
        private int head, tail, count;
        private final ReentrantLock lock;
        private final Condition notFull;
        private final Condition notEmpty;

        public BoundedBlockingQueue(int capacity) {
            if (capacity <= 0) throw new IllegalArgumentException("Capacity must be > 0");
            this.items = new Object[capacity];
            this.lock = new ReentrantLock();
            this.notFull = lock.newCondition();
            this.notEmpty = lock.newCondition();
            this.head = 0;
            this.tail = 0;
            this.count = 0;
        }

        public void put(T item) throws InterruptedException {
            lock.lockInterruptibly();
            try {
                while (count == items.length) {
                    notFull.await(); // Wait until space becomes available
                }
                items[tail] = item;
                tail = (tail + 1) % items.length;
                count++;
                notEmpty.signal(); // Wake up any waiting consumer
            } finally {
                lock.unlock();
            }
        }

        @SuppressWarnings("unchecked")
        public T take() throws InterruptedException {
            lock.lockInterruptibly();
            try {
                while (count == 0) {
                    notEmpty.await(); // Wait until an item is produced
                }
                T item = (T) items[head];
                items[head] = null; // Prevent memory leak
                head = (head + 1) % items.length;
                count--;
                notFull.signal(); // Wake up any waiting producer
                return item;
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            lock.lock();
            try {
                return count;
            } finally {
                lock.unlock();
            }
        }
    }

    // ------------------------------------------------------------------------
    // 2. High-Throughput Thread-Safe LRU Cache
    // ------------------------------------------------------------------------
    /**
     * Architecture: ConcurrentHashMap for O(1) lock-free read lookups,
     * coupled with a fine-grained locked Doubly Linked List for eviction ordering.
     */
    public static class ThreadSafeLRUCache<K, V> {
        private static class Node<K, V> {
            K key;
            V value;
            Node<K, V> prev, next;
            Node(K key, V value) {
                this.key = key;
                this.value = value;
            }
        }

        private final int capacity;
        private final ConcurrentHashMap<K, Node<K, V>> map;
        private final Node<K, V> head, tail;
        private final ReentrantLock listLock;

        public ThreadSafeLRUCache(int capacity) {
            this.capacity = capacity;
            this.map = new ConcurrentHashMap<>(capacity);
            this.head = new Node<>(null, null);
            this.tail = new Node<>(null, null);
            this.head.next = tail;
            this.tail.prev = head;
            this.listLock = new ReentrantLock();
        }

        public V get(K key) {
            Node<K, V> node = map.get(key);
            if (node == null) return null;
            moveToHead(node);
            return node.value;
        }

        public void put(K key, V value) {
            Node<K, V> existing = map.get(key);
            if (existing != null) {
                existing.value = value;
                moveToHead(existing);
                return;
            }

            Node<K, V> newNode = new Node<>(key, value);
            listLock.lock();
            try {
                if (map.size() >= capacity) {
                    Node<K, V> lru = removeTail();
                    if (lru != null) {
                        map.remove(lru.key);
                    }
                }
                addToHead(newNode);
                map.put(key, newNode);
            } finally {
                listLock.unlock();
            }
        }

        private void moveToHead(Node<K, V> node) {
            listLock.lock();
            try {
                removeNode(node);
                addToHead(node);
            } finally {
                listLock.unlock();
            }
        }

        private void addToHead(Node<K, V> node) {
            node.next = head.next;
            node.prev = head;
            head.next.prev = node;
            head.next = node;
        }

        private void removeNode(Node<K, V> node) {
            if (node.prev != null) node.prev.next = node.next;
            if (node.next != null) node.next.prev = node.prev;
        }

        private Node<K, V> removeTail() {
            Node<K, V> lru = tail.prev;
            if (lru == head) return null;
            removeNode(lru);
            return lru;
        }

        public int size() {
            return map.size();
        }
    }

    // ------------------------------------------------------------------------
    // 3. Multi-Threaded Token Bucket Rate Limiter
    // ------------------------------------------------------------------------
    /**
     * Invariant: Refills tokens lazily based on elapsed time delta (now - lastRefillTimestamp).
     * Eliminates background polling threads while remaining thread-safe.
     */
    public static class TokenBucketRateLimiter {
        private final long maxTokens;
        private final long refillRatePerSecond;
        private double currentTokens;
        private long lastRefillTimestamp;
        private final ReentrantLock lock;

        public TokenBucketRateLimiter(long maxTokens, long refillRatePerSecond) {
            this.maxTokens = maxTokens;
            this.refillRatePerSecond = refillRatePerSecond;
            this.currentTokens = maxTokens;
            this.lastRefillTimestamp = System.currentTimeMillis();
            this.lock = new ReentrantLock();
        }

        public boolean tryAcquire(int tokens) {
            lock.lock();
            try {
                refill();
                if (currentTokens >= tokens) {
                    currentTokens -= tokens;
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsedTime = now - lastRefillTimestamp;
            double tokensToAdd = (elapsedTime / 1000.0) * refillRatePerSecond;
            currentTokens = Math.min(maxTokens, currentTokens + tokensToAdd);
            lastRefillTimestamp = now;
        }
    }

    // ------------------------------------------------------------------------
    // 4. Custom Thread Pool Executor from Scratch
    // ------------------------------------------------------------------------
    public static class CustomThreadPool {
        private final BoundedBlockingQueue<Runnable> taskQueue;
        private final WorkerThread[] workers;
        private volatile boolean isShutdown = false;

        public CustomThreadPool(int poolSize, int queueCapacity) {
            this.taskQueue = new BoundedBlockingQueue<>(queueCapacity);
            this.workers = new WorkerThread[poolSize];
            for (int i = 0; i < poolSize; i++) {
                workers[i] = new WorkerThread("CustomPool-Worker-" + i);
                workers[i].start();
            }
        }

        public void execute(Runnable task) throws InterruptedException {
            if (isShutdown) throw new IllegalStateException("ThreadPool is shutdown");
            taskQueue.put(task);
        }

        public void shutdown() {
            isShutdown = true;
            for (WorkerThread worker : workers) {
                worker.interrupt();
            }
        }

        private class WorkerThread extends Thread {
            public WorkerThread(String name) {
                super(name);
            }

            @Override
            public void run() {
                while (!isShutdown || taskQueue.size() > 0) {
                    try {
                        Runnable task = taskQueue.take();
                        task.run();
                    } catch (InterruptedException e) {
                        if (isShutdown && taskQueue.size() == 0) {
                            break;
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // 5. Alternating Threads Coordination (FooBar using Semaphores)
    // ------------------------------------------------------------------------
    public static class FooBar {
        private final int n;
        private final Semaphore fooSem = new Semaphore(1);
        private final Semaphore barSem = new Semaphore(0);

        public FooBar(int n) {
            this.n = n;
        }

        public void foo(Runnable printFoo) throws InterruptedException {
            for (int i = 0; i < n; i++) {
                fooSem.acquire();
                printFoo.run();
                barSem.release();
            }
        }

        public void bar(Runnable printBar) throws InterruptedException {
            for (int i = 0; i < n; i++) {
                barSem.acquire();
                printBar.run();
                fooSem.release();
            }
        }
    }

    // ------------------------------------------------------------------------
    // Test Suite for Concurrency Primitives
    // ------------------------------------------------------------------------
    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println(" RUNNING CONCURRENCY & THREADING TEST SUITE ");
        System.out.println("=================================================");

        // 1. Test BoundedBlockingQueue with 4 Producers and 4 Consumers
        BoundedBlockingQueue<Integer> queue = new BoundedBlockingQueue<>(5);
        int totalItems = 100;
        AtomicInteger consumedCount = new AtomicInteger(0);
        CountDownLatch producerLatch = new CountDownLatch(4);
        CountDownLatch consumerLatch = new CountDownLatch(4);

        for (int p = 0; p < 4; p++) {
            new Thread(() -> {
                for (int i = 0; i < 25; i++) {
                    try {
                        queue.put(i);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                producerLatch.countDown();
            }).start();
        }

        for (int c = 0; c < 4; c++) {
            new Thread(() -> {
                for (int i = 0; i < 25; i++) {
                    try {
                        queue.take();
                        consumedCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                consumerLatch.countDown();
            }).start();
        }

        assert producerLatch.await(5, TimeUnit.SECONDS);
        assert consumerLatch.await(5, TimeUnit.SECONDS);
        assert consumedCount.get() == totalItems : "Expected 100 items consumed, got: " + consumedCount.get();
        assert queue.size() == 0;

        // 2. Test ThreadSafeLRUCache
        ThreadSafeLRUCache<String, Integer> lru = new ThreadSafeLRUCache<>(3);
        lru.put("A", 1);
        lru.put("B", 2);
        lru.put("C", 3);
        assert lru.get("A") == 1; // "A" becomes MRU, order: B -> C -> A
        lru.put("D", 4);          // Evicts "B"
        assert lru.get("B") == null : "Key B should have been evicted";
        assert lru.get("D") == 4;
        assert lru.get("C") == 3;
        assert lru.get("A") == 1;

        // 3. Test Token Bucket Rate Limiter
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(5, 10);
        assert rateLimiter.tryAcquire(3) : "Should acquire 3 tokens";
        assert rateLimiter.tryAcquire(2) : "Should acquire remaining 2 tokens";
        assert !rateLimiter.tryAcquire(1) : "Should reject when bucket is empty";

        // 4. Test Custom Thread Pool
        CustomThreadPool pool = new CustomThreadPool(3, 10);
        AtomicInteger taskCounter = new AtomicInteger(0);
        CountDownLatch poolLatch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            pool.execute(() -> {
                taskCounter.incrementAndGet();
                poolLatch.countDown();
            });
        }
        assert poolLatch.await(5, TimeUnit.SECONDS);
        assert taskCounter.get() == 10;
        pool.shutdown();

        // 5. Test FooBar Alternating Output
        FooBar fb = new FooBar(3);
        StringBuilder output = new StringBuilder();
        Thread t1 = new Thread(() -> {
            try {
                fb.foo(() -> output.append("Foo"));
            } catch (InterruptedException ignored) {}
        });
        Thread t2 = new Thread(() -> {
            try {
                fb.bar(() -> output.append("Bar"));
            } catch (InterruptedException ignored) {}
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assert output.toString().equals("FooBarFooBarFooBar");

        System.out.println(" CONCURRENCY & THREADING SUITE ALL PASSED!");
        System.out.println("=================================================");
    }
}
