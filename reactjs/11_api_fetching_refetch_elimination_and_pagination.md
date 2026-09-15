# Part 11: External API Fetching, Re-Fetch Elimination & Pagination Architectures (Q231 - Q250)

---

### Q231: How do you build a Custom `useFetch` Hook with In-Memory Deduplication to Avoid Duplicate In-Flight Re-Fetches?
**Answer:**
When 5 sibling components mount simultaneously requesting `/api/user/profile`, naive `useEffect` fetchers fire 5 duplicate HTTP requests.
**In-Memory Promise Deduplication** caches active Promises so that all callers share a single network request:

```typescript
import { useState, useEffect } from 'react';

// Global in-memory Promise & data cache
const inFlightRequests = new Map<string, Promise<any>>();
const memoryCache = new Map<string, { data: any; timestamp: number }>();

export function useFetchDeduplicated<T>(url: string, ttlMs: number = 60000) {
  const [data, setData] = useState<T | null>(() => {
    const cached = memoryCache.get(url);
    if (cached && Date.now() - cached.timestamp < ttlMs) {
      return cached.data;
    }
    return null;
  });
  const [loading, setLoading] = useState<boolean>(!data);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let isMounted = true;

    // 1. Check fresh cache
    const cached = memoryCache.get(url);
    if (cached && Date.now() - cached.timestamp < ttlMs) {
      setData(cached.data);
      setLoading(false);
      return;
    }

    // 2. Reuse in-flight Promise or create a new one
    let requestPromise = inFlightRequests.get(url);
    if (!requestPromise) {
      requestPromise = fetch(url)
        .then((res) => {
          if (!res.ok) throw new Error(`HTTP Error: ${res.status}`);
          return res.json();
        })
        .then((result) => {
          memoryCache.set(url, { data: result, timestamp: Date.now() });
          return result;
        })
        .finally(() => {
          inFlightRequests.delete(url); // Clean up active in-flight map
        });
      
      inFlightRequests.set(url, requestPromise);
    }

    setLoading(true);
    requestPromise
      .then((result) => {
        if (isMounted) {
          setData(result);
          setError(null);
        }
      })
      .catch((err) => {
        if (isMounted) setError(err);
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [url, ttlMs]);

  return { data, loading, error };
}
```

---

### Q232: How do you implement Standard Offset-Based Pagination with URL Query Sync in React 19?
**Answer:**
Offset pagination (`?page=2&limit=20`) synchronizes page state with URL search parameters so page links are shareable, bookmarkable, and preserve browser history:

```javascript
import { useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';

async function fetchProducts({ page = 1, limit = 10 }) {
  const res = await fetch(`/api/products?page=${page}&limit=${limit}`);
  if (!res.ok) throw new Error('Failed to fetch');
  return res.json(); // returns { items: [...], totalPages: 10, totalCount: 100 }
}

export function OffsetPaginationTable() {
  const [searchParams, setSearchParams] = useSearchParams();
  const currentPage = parseInt(searchParams.get('page') || '1', 10);
  const pageSize = 10;

  const { data, isPending, isPlaceholderData } = useQuery({
    queryKey: ['products', currentPage],
    queryFn: () => fetchProducts({ page: currentPage, limit: pageSize }),
    placeholderData: (previousData) => previousData, // Keeps previous page visible while fetching next page!
    staleTime: 1000 * 60 * 5 // 5 minutes fresh data
  });

  const goToPage = (page) => {
    setSearchParams({ page: String(page) });
  };

  return (
    <div>
      {isPending ? (
        <p>Loading table...</p>
      ) : (
        <table>
          <thead>
            <tr><th>ID</th><th>Name</th><th>Price</th></tr>
          </thead>
          <tbody style={{ opacity: isPlaceholderData ? 0.6 : 1 }}>
            {data?.items.map((item) => (
              <tr key={item.id}>
                <td>{item.id}</td><td>{item.name}</td><td>${item.price}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {/* Pagination Controls */}
      <div className="pagination-controls">
        <button 
          onClick={() => goToPage(currentPage - 1)} 
          disabled={currentPage === 1 || isPlaceholderData}
        >
          Previous
        </button>
        <span>Page {currentPage} of {data?.totalPages || 1}</span>
        <button 
          onClick={() => goToPage(currentPage + 1)} 
          disabled={currentPage >= (data?.totalPages || 1) || isPlaceholderData}
        >
          Next
        </button>
      </div>
    </div>
  );
}
```

---

### Q233: How do you implement High-Performance Cursor-Based (Keyset) Pagination for Real-Time Streaming Feeds?
**Answer:**
Offset pagination (`OFFSET 50000`) degrades SQL performance ($O(N)$ index scan) and skips/duplicates items if new rows are inserted.
**Cursor-based Pagination** uses an immutable pointer (e.g. `last_seen_id` or timestamp) to query `WHERE id < cursor LIMIT 20` in $O(1)$ time:

```javascript
import { useState } from 'react';

export function CursorFeedList() {
  const [posts, setPosts] = useState([]);
  const [nextCursor, setNextCursor] = useState(null);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);

  async function loadMorePosts() {
    if (loading || !hasMore) return;
    setLoading(true);

    try {
      const url = nextCursor 
        ? `/api/feed?cursor=${encodeURIComponent(nextCursor)}&limit=15` 
        : `/api/feed?limit=15`;
      
      const res = await fetch(url);
      const data = await res.json(); // { items: [...], nextCursor: "eyJpZCI6MTA0NX0=", hasMore: true }

      setPosts((prev) => [...prev, ...data.items]);
      setNextCursor(data.nextCursor);
      setHasMore(data.hasMore);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="feed-container">
      {posts.map((post) => (
        <article key={post.id} className="post-card">
          <h3>{post.title}</h3>
          <p>{post.body}</p>
        </article>
      ))}

      {hasMore && (
        <button onClick={loadMorePosts} disabled={loading}>
          {loading ? 'Loading more posts...' : 'Load More'}
        </button>
      )}
    </div>
  );
}
```

---

### Q234: How do you implement Infinite Scrolling with `IntersectionObserver` and TanStack Query `useInfiniteQuery`?
**Answer:**

```javascript
import { useInfiniteQuery } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';

async function fetchInfiniteUsers({ pageParam = null }) {
  const url = pageParam 
    ? `/api/users?cursor=${pageParam}&limit=20` 
    : `/api/users?limit=20`;
  const res = await fetch(url);
  return res.json(); // { users: [...], nextCursor: 1045 }
}

export function InfiniteUserScroll() {
  const loadMoreRef = useRef(null);

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    status
  } = useInfiniteQuery({
    queryKey: ['infiniteUsers'],
    queryFn: fetchInfiniteUsers,
    initialPageParam: null,
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
    staleTime: 1000 * 60 * 10 // 10 minutes cache
  });

  // Automatically trigger fetchNextPage when sentinel element is visible
  useEffect(() => {
    const sentinel = loadMoreRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting && hasNextPage && !isFetchingNextPage) {
        fetchNextPage();
      }
    }, { threshold: 0.1 });

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  if (status === 'pending') return <p>Loading initial users...</p>;

  return (
    <div className="infinite-scroll-list">
      {data?.pages.map((page) =>
        page.users.map((user) => (
          <div key={user.id} className="user-row">
            <span>{user.name}</span> - <span>{user.email}</span>
          </div>
        ))
      )}

      {/* Sentinel element observed by IntersectionObserver */}
      <div ref={loadMoreRef} style={{ height: 40, textAlign: 'center' }}>
        {isFetchingNextPage ? 'Loading next 20 users...' : hasNextPage ? 'Scroll down for more' : 'All users loaded'}
      </div>
    </div>
  );
}
```

---

### Q235: How do you implement Prefetching on Hover for 0ms Instant Page Transitions?
**Answer:**
When a user moves their mouse over a pagination link or table row, prefetching downloads the target page data in the background **before** they click:

```javascript
import { useQueryClient } from '@tanstack/react-query';

export function PaginationButton({ pageNumber, children }) {
  const queryClient = useQueryClient();

  // Prefetch data into cache on mouse hover or keyboard focus
  const handlePrefetch = () => {
    queryClient.prefetchQuery({
      queryKey: ['products', pageNumber],
      queryFn: () => fetch(`/api/products?page=${pageNumber}`).then(r => r.json()),
      staleTime: 1000 * 60 * 2 // Keeps prefetched data fresh for 2 minutes
    });
  };

  return (
    <button
      onMouseEnter={handlePrefetch}
      onFocus={handlePrefetch}
      onClick={() => goToPage(pageNumber)}
    >
      {children}
    </button>
  );
}
```

---

### Q236: How do you configure Window Focus and Network Reconnect Refetch rules in TanStack Query to Avoid Unwanted Re-Fetches?
**Answer:**
By default, TanStack Query refetches all active queries whenever the browser window regains focus (`refetchOnWindowFocus: true`) or network reconnects. In dashboards with 50 live charts, this triggers a storm of network requests.

**Fine-Grained Re-Fetch Prevention Config:**
```javascript
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // 🛡️ Prevent unwanted refetches:
      staleTime: 1000 * 60 * 5,       // Data considered fresh for 5 minutes (No background refetch on mount)
      gcTime: 1000 * 60 * 30,          // Keep unused cache in memory for 30 minutes
      refetchOnWindowFocus: false,     // Disable automatic refetch when user switches browser tabs
      refetchOnReconnect: 'always',    // Refetch only when network drops and reconnects
      refetchOnMount: false,           // Do not refetch on component remount if data is not stale
      retry: 2,                        // Retry failed network requests twice before throwing error
      retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 30000) // Exponential backoff with cap
    }
  }
});
```

---

### Q237: How do you implement Bi-Directional Infinite Scrolling (Chat History / Timeline with upward & downward scroll)?
**Answer:**
In chat applications (Slack/Discord), initial loading lands in the middle or bottom of history. Scrolling **up** fetches older historical messages, while scrolling **down** fetches newer messages:

```javascript
import { useInfiniteQuery } from '@tanstack/react-query';
import { useRef, useLayoutEffect } from 'react';

async function fetchChatMessages({ pageParam = 0, direction }) {
  const res = await fetch(`/api/chat?cursor=${pageParam}&direction=${direction}`);
  return res.json();
}

export function BiDirectionalChat({ initialMessageId }) {
  const containerRef = useRef(null);
  const previousScrollHeightRef = useRef(0);

  const {
    data,
    fetchPreviousPage,
    fetchNextPage,
    hasPreviousPage,
    hasNextPage,
    isFetchingPreviousPage
  } = useInfiniteQuery({
    queryKey: ['chat', initialMessageId],
    queryFn: ({ pageParam }) => fetchChatMessages({ pageParam, direction: 'older' }),
    initialPageParam: initialMessageId,
    getPreviousPageParam: (firstPage) => firstPage.olderCursor ?? undefined,
    getNextPageParam: (lastPage) => lastPage.newerCursor ?? undefined
  });

  // Preserve scroll position when older messages are prepended to top of chat
  useLayoutEffect(() => {
    if (containerRef.current && isFetchingPreviousPage) {
      const diff = containerRef.current.scrollHeight - previousScrollHeightRef.current;
      containerRef.current.scrollTop += diff;
    }
    if (containerRef.current) {
      previousScrollHeightRef.current = containerRef.current.scrollHeight;
    }
  }, [data, isFetchingPreviousPage]);

  return (
    <div ref={containerRef} style={{ height: '600px', overflowY: 'auto' }}>
      {hasPreviousPage && (
        <button onClick={() => fetchPreviousPage()}>Load Older Messages</button>
      )}

      {data?.pages.map((page) =>
        page.messages.map((msg) => (
          <div key={msg.id} className="chat-bubble">
            <strong>{msg.sender}:</strong> {msg.text}
          </div>
        ))
      )}

      {hasNextPage && (
        <button onClick={() => fetchNextPage()}>Load Newer Messages</button>
      )}
    </div>
  );
}
```

---

### Q238: How do you build an Auto-Polling Real-Time Hook with Adaptive Backoff when User is Idle?
**Answer:**
Polling every 2 seconds when the user is inactive or has minimized the tab wastes server bandwidth.
**Adaptive Polling**: Polls at 2s when active, slows down to 30s when window loses focus or user is idle:

```javascript
import { useEffect, useState, useRef } from 'react';

export function useAdaptivePolling(fetchFn, { activeInterval = 2000, idleInterval = 30000 }) {
  const [data, setData] = useState(null);
  const [isIdle, setIsIdle] = useState(false);
  const savedFetch = useRef(fetchFn);

  useEffect(() => { savedFetch.current = fetchFn; });

  // Detect user activity and tab focus
  useEffect(() => {
    let idleTimer;
    const resetIdle = () => {
      setIsIdle(false);
      clearTimeout(idleTimer);
      idleTimer = setTimeout(() => setIsIdle(true), 60000); // Idle after 60s of no mouse/keyboard events
    };

    const handleVisibility = () => setIsIdle(document.hidden);

    window.addEventListener('mousemove', resetIdle);
    window.addEventListener('keydown', resetIdle);
    document.addEventListener('visibilitychange', handleVisibility);

    return () => {
      window.removeEventListener('mousemove', resetIdle);
      window.removeEventListener('keydown', resetIdle);
      document.removeEventListener('visibilitychange', handleVisibility);
      clearTimeout(idleTimer);
    };
  }, []);

  useEffect(() => {
    let isCancelled = false;
    const currentInterval = isIdle ? idleInterval : activeInterval;

    const poll = async () => {
      try {
        const result = await savedFetch.current();
        if (!isCancelled) setData(result);
      } catch (err) {
        console.error('Polling error:', err);
      }
    };

    poll(); // Initial run
    const intervalId = setInterval(poll, currentInterval);

    return () => {
      isCancelled = true;
      clearInterval(intervalId);
    };
  }, [isIdle, activeInterval, idleInterval]);

  return data;
}
```

---

### Q239: How do you combine Virtualization and Infinite Fetching with `@tanstack/react-virtual`?
**Answer:**

```javascript
import { useVirtualizer } from '@tanstack/react-virtual';
import { useInfiniteQuery } from '@tanstack/react-query';
import { useRef, useEffect } from 'react';

export function VirtualInfiniteTable() {
  const parentRef = useRef(null);

  const { data, fetchNextPage, hasNextPage, isFetchingNextPage } = useInfiniteQuery({
    queryKey: ['virtualRows'],
    queryFn: ({ pageParam = 0 }) => fetch(`/api/rows?cursor=${pageParam}`).then(r => r.json()),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => lastPage.nextCursor
  });

  const allRows = data ? data.pages.flatMap(d => d.rows) : [];

  const rowVirtualizer = useVirtualizer({
    count: hasNextPage ? allRows.length + 1 : allRows.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 50,
    overscan: 5
  });

  useEffect(() => {
    const [lastItem] = [...rowVirtualizer.getVirtualItems()].reverse();
    if (!lastItem) return;

    if (lastItem.index >= allRows.length - 1 && hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [hasNextPage, fetchNextPage, allRows.length, isFetchingNextPage, rowVirtualizer.getVirtualItems()]);

  return (
    <div ref={parentRef} style={{ height: '500px', overflow: 'auto' }}>
      <div style={{ height: `${rowVirtualizer.getTotalSize()}px`, width: '100%', position: 'relative' }}>
        {rowVirtualizer.getVirtualItems().map((virtualRow) => {
          const isLoaderRow = virtualRow.index > allRows.length - 1;
          const row = allRows[virtualRow.index];

          return (
            <div
              key={virtualRow.index}
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                height: `${virtualRow.size}px`,
                transform: `translateY(${virtualRow.start}px)`
              }}
            >
              {isLoaderRow ? 'Loading more rows...' : `Row ${row.id}: ${row.title}`}
            </div>
          );
        })}
      </div>
    </div>
  );
}
```

---

### Q240: What is the SWR (Stale-While-Revalidate) Cache Hydration and Mutate API in Vercel's `swr` library?
**Answer:**
SWR returns cached data first (stale), then fetches fresh data in the background (revalidate), and finally updates state with zero UI lag:

```javascript
import useSWR, { mutate } from 'swr';

const fetcher = (url) => fetch(url).then((res) => res.json());

export function UserDashboard({ userId }) {
  const { data: user, error, isLoading } = useSWR(`/api/user/${userId}`, fetcher, {
    revalidateOnFocus: false,
    dedupingInterval: 60000 // Deduplicate requests within 1 minute
  });

  // Imperative Optimistic Mutation across any component
  const handleUpdateName = async (newName) => {
    // 1. Update UI optimistically
    mutate(`/api/user/${userId}`, { ...user, name: newName }, false);

    // 2. Perform actual API call
    await fetch(`/api/user/${userId}`, {
      method: 'PATCH',
      body: JSON.stringify({ name: newName })
    });

    // 3. Trigger background revalidation to guarantee sync
    mutate(`/api/user/${userId}`);
  };

  if (isLoading) return <p>Loading...</p>;
  return (
    <div>
      <h2>{user.name}</h2>
      <button onClick={() => handleUpdateName('New Name')}>Update Name</button>
    </div>
  );
}
```

---

### Q241: How do you build a `useNetwork` hook with Offline Mutation Queueing in React?
**Answer:**
Intercepts network requests when offline, stores failed mutation requests in IndexedDB, and automatically flushes them in sequence when the device regains connectivity:

```javascript
import { useState, useEffect } from 'react';
import { get, set } from 'idb-keyval';

export function useOfflineQueue() {
  const [isOnline, setIsOnline] = useState(navigator.onLine);

  useEffect(() => {
    const handleOnline = async () => {
      setIsOnline(true);
      // Flush offline queue when reconnected:
      const queue = (await get('offline_mutations')) || [];
      for (const item of queue) {
        await fetch(item.url, { method: item.method, body: JSON.stringify(item.body) });
      }
      await set('offline_mutations', []);
    };

    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const queueMutation = async (mutation) => {
    const queue = (await get('offline_mutations')) || [];
    queue.push(mutation);
    await set('offline_mutations', queue);
  };

  return { isOnline, queueMutation };
}
```

---

### Q242: How do you handle Dependent (Sequential) API queries in TanStack Query without waterfall bugs (`enabled: !!data`)?
**Answer:**
Ensures that Query B executes **only after** Query A has successfully resolved:

```javascript
import { useQuery } from '@tanstack/react-query';

export function UserOrganizationView({ email }) {
  // Query 1: Fetch user by email
  const { data: user } = useQuery({
    queryKey: ['user', email],
    queryFn: () => fetch(`/api/user?email=${email}`).then(r => r.json())
  });

  const userId = user?.id;

  // Query 2: Dependent query - waits until userId is defined!
  const { data: organizations, isPending } = useQuery({
    queryKey: ['orgs', userId],
    queryFn: () => fetch(`/api/users/${userId}/orgs`).then(r => r.json()),
    enabled: !!userId // 🛡️ Prevents execution until userId is truthy!
  });

  if (isPending) return <p>Loading organizations...</p>;
  return <ul>{organizations?.map(o => <li key={o.id}>{o.name}</li>)}</ul>;
}
```

---

### Q243: How do you handle Dynamic Parallel API Queries with `useQueries` in TanStack Query?
**Answer:**
When querying an array of dynamic IDs where `useQuery` cannot be called inside a loop:

```javascript
import { useQueries } from '@tanstack/react-query';

export function MultiUserCards({ userIds }) {
  // Executes dynamic parallel queries with unified caching:
  const userQueries = useQueries({
    queries: userIds.map((id) => ({
      queryKey: ['user', id],
      queryFn: () => fetch(`/api/users/${id}`).then(r => r.json()),
      staleTime: 1000 * 60 * 5
    }))
  });

  const isLoading = userQueries.some((q) => q.isPending);

  if (isLoading) return <p>Loading users...</p>;
  return (
    <div>
      {userQueries.map(({ data: user }) => (
        <div key={user.id}>{user.name}</div>
      ))}
    </div>
  );
}
```

---

### Q244: How do you prevent Double-Fetch on Component Mount in React 18/19 StrictMode?
**Answer:**
In development, `StrictMode` mounts, unmounts, and re-mounts components to test cleanups.
Using TanStack Query or `useRef` guard prevents duplicate network calls:

```javascript
import { useEffect, useRef } from 'react';

export function StrictModeSafeFetch() {
  const isFetched = useRef(false);

  useEffect(() => {
    // 🛡️ Prevent duplicate fetch in StrictMode:
    if (isFetched.current) return;
    isFetched.current = true;

    fetchData();
  }, []);
}
```

---

### Q245: How do you build a Client-Side API Rate Limiter / Request Throttler in React?
**Answer:**

```javascript
class ClientRateLimiter {
  constructor(maxRequests, perWindowMs) {
    this.maxRequests = maxRequests;
    this.perWindowMs = perWindowMs;
    this.queue = [];
  }

  async acquire() {
    const now = Date.now();
    this.queue = this.queue.filter(t => t > now - this.perWindowMs);
    if (this.queue.length >= this.maxRequests) {
      const waitTime = this.perWindowMs - (now - this.queue[0]);
      await new Promise(resolve => setTimeout(resolve, waitTime));
      return this.acquire();
    }
    this.queue.push(Date.now());
  }
}

const limiter = new ClientRateLimiter(5, 1000); // 5 requests per second

export async function rateLimitedFetch(url, options) {
  await limiter.acquire();
  return fetch(url, options);
}
```

---

### Q246: How do you handle Pagination State Synchronization with Browser Back/Forward buttons?
**Answer:**

```javascript
import { useSearchParams } from 'react-router-dom';

export function PaginationSync() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = parseInt(searchParams.get('page') || '1', 10);

  // Updates browser history stack:
  const changePage = (newPage) => {
    setSearchParams({ page: String(newPage) }); // Pushes state to history!
  };

  return (
    <div>
      <button onClick={() => changePage(page - 1)} disabled={page <= 1}>Prev</button>
      <span>Current Page: {page}</span>
      <button onClick={() => changePage(page + 1)}>Next</button>
    </div>
  );
}
```

---

### Q247: How do you implement Infinite Virtualized Grid Pagination (2D matrix) with `@tanstack/react-virtual`?
**Answer:**

```javascript
import { useVirtualizer } from '@tanstack/react-virtual';
import { useRef } from 'react';

export function VirtualGrid({ items, columnCount = 4 }) {
  const parentRef = useRef(null);
  const rowCount = Math.ceil(items.length / columnCount);

  const rowVirtualizer = useVirtualizer({
    count: rowCount,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 180,
    overscan: 3
  });

  return (
    <div ref={parentRef} style={{ height: '600px', overflow: 'auto' }}>
      <div style={{ height: `${rowVirtualizer.getTotalSize()}px`, position: 'relative' }}>
        {rowVirtualizer.getVirtualItems().map(virtualRow => {
          const startIndex = virtualRow.index * columnCount;
          const rowItems = items.slice(startIndex, startIndex + columnCount);

          return (
            <div key={virtualRow.index} style={{ position: 'absolute', top: 0, transform: `translateY(${virtualRow.start}px)`, display: 'grid', gridTemplateColumns: `repeat(${columnCount}, 1fr)`, width: '100%' }}>
              {rowItems.map(item => (
                <div key={item.id} className="grid-card">{item.title}</div>
              ))}
            </div>
          );
        })}
      </div>
    </div>
  );
}
```

---

### Q248: How do you cache API responses in LocalStorage / IndexedDB with TanStack Query Persist Client?
**Answer:**

```javascript
import { QueryClient } from '@tanstack/react-query';
import { persistQueryClient } from '@tanstack/react-query-persist-client';
import { createSyncStoragePersister } from '@tanstack/query-sync-storage-persister';

export const queryClient = new QueryClient({
  defaultOptions: { queries: { gcTime: 1000 * 60 * 60 * 24 } } // 24 hours
});

const localStoragePersister = createSyncStoragePersister({
  storage: typeof window !== 'undefined' ? window.localStorage : undefined
});

persistQueryClient({
  queryClient,
  persister: localStoragePersister,
  maxAge: 1000 * 60 * 60 * 24 // 24-hour persistent offline cache!
});
```

---

### Q249: What is the difference between Keyset Pagination and Offset Pagination in SQL queries from React?
**Answer:**

```javascript
// 1. Offset Pagination (Slow on large tables, index scanning overhead):
// SQL: SELECT * FROM items ORDER BY id LIMIT 20 OFFSET 100000;

// 2. Keyset / Cursor Pagination (Fast O(1) index lookup):
// SQL: SELECT * FROM items WHERE id > 100000 ORDER BY id LIMIT 20;
async function fetchKeysetPage(lastSeenId, limit = 20) {
  const url = lastSeenId ? `/api/items?after=${lastSeenId}&limit=${limit}` : `/api/items?limit=${limit}`;
  return fetch(url).then(r => r.json());
}
```

---

### Q250: What is the Enterprise Checklist for Resilient, Zero-Miss External API Fetching and Pagination in React 19?
**Answer:**

```javascript
// Enterprise API Fetching & Pagination Architecture Checklist:
// [✓] Promise Deduplication active for concurrent identical calls.
// [✓] AbortController wired to useEffect / query cleanup.
// [✓] staleTime configured (min 5 min) to prevent refetch on tab switch.
// [✓] Cursor-based pagination used for real-time and large datasets.
// [✓] Prefetching on Hover configured for 0ms page transitions.
// [✓] URL query parameters synchronized with pagination state.
// [✓] Bi-directional scrolling state preserves scroll position via useLayoutEffect.
// [✓] Persistent offline caching with IndexedDB / LocalStorage persisters.
```
