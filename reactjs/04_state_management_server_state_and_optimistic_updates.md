# Part 4: State Management, Server State & Optimistic UI (Q76 - Q100)

---

### Q76: What is the fundamental difference between Server State and Client State?
**Answer:**

```javascript
// 1. Client State: Ephemeral UI interactions (Local ownership)
const [isDrawerOpen, setIsDrawerOpen] = useState(false);

// 2. Server State: Remote data, cached and synchronized asynchronously
const { data: userProfile } = useQuery({
  queryKey: ['user', userId],
  queryFn: () => fetch(`/api/users/${userId}`).then(r => r.json())
});
```

---

### Q77: Why has the industry shifted from global Redux stores to Server State Managers (TanStack Query / SWR) + Minimal Client Stores (Zustand)?
**Answer:**

```javascript
// Modern Minimal Separation of Concerns:
// Client Store (Zustand - only UI state):
export const useUIStore = create((set) => ({
  theme: 'dark',
  sidebarOpen: true,
  toggleSidebar: () => set(s => ({ sidebarOpen: !s.sidebarOpen }))
}));

// Server State (TanStack Query - handles async caching, refetching, deduping):
export function useUserOrders(userId) {
  return useQuery({
    queryKey: ['orders', userId],
    queryFn: () => api.getOrders(userId),
    staleTime: 1000 * 60 * 5 // 5 min fresh
  });
}
```

---

### Q78: How does Zustand implement high-performance state management without Context re-render bloat?
**Answer:**

```javascript
import { create } from 'zustand';

export const useCartStore = create((set) => ({
  items: [],
  isOpen: false,
  toggleCart: () => set((state) => ({ isOpen: !state.isOpen })),
  addItem: (item) => set((state) => ({ items: [...state.items, item] }))
}));

// Granular Selector: Component re-renders ONLY when items.length changes!
export function CartBadge() {
  const itemCount = useCartStore((state) => state.items.length);
  return <span className="badge">{itemCount}</span>;
}
```

---

### Q79: How do you implement robust Optimistic Updates with Rollback in TanStack Query (React Query)?
**Answer:**

```javascript
import { useMutation, useQueryClient } from '@tanstack/react-query';

export function useUpdateTodo() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (updatedTodo) => api.patchTodo(updatedTodo),
    onMutate: async (newTodo) => {
      await queryClient.cancelQueries({ queryKey: ['todos'] });
      const previousTodos = queryClient.getQueryData(['todos']);

      queryClient.setQueryData(['todos'], (old = []) =>
        old.map((t) => (t.id === newTodo.id ? { ...t, ...newTodo } : t))
      );

      return { previousTodos };
    },
    onError: (err, newTodo, context) => {
      queryClient.setQueryData(['todos'], context.previousTodos); // Rollback!
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['todos'] });
    }
  });
}
```

---

### Q80: What is State Normalization (Normalizr / Redux EntityAdapter) and why is it required in complex nested relational data?
**Answer:**

```javascript
import { createEntityAdapter, createSlice } from '@reduxjs/toolkit';

const usersAdapter = createEntityAdapter();

const usersSlice = createSlice({
  name: 'users',
  initialState: usersAdapter.getInitialState(),
  reducers: {
    userUpdated: usersAdapter.updateOne, // O(1) by ID update!
    usersReceived: usersAdapter.setAll
  }
});
```

---

### Q81: What is the difference between Redux Toolkit (RTK) and legacy Redux?
**Answer:**

```javascript
import { createSlice, configureStore } from '@reduxjs/toolkit';

const counterSlice = createSlice({
  name: 'counter',
  initialState: { value: 0 },
  reducers: {
    increment: (state) => {
      state.value += 1; // Immer safely produces immutable updates
    }
  }
});

export const store = configureStore({
  reducer: { counter: counterSlice.reducer }
});
```

---

### Q82: What are Atomic State Managers (Jotai / Recoil) and how do they differ from Slice-based stores (Zustand / Redux)?
**Answer:**

```javascript
import { atom, useAtom } from 'jotai';

export const countAtom = atom(0);
export const doubleCountAtom = atom((get) => get(countAtom) * 2); // Computed atom

function Counter() {
  const [count, setCount] = useAtom(countAtom);
  const [doubleCount] = useAtom(doubleCountAtom);

  return <button onClick={() => setCount(c => c + 1)}>Count: {count} ({doubleCount})</button>;
}
```

---

### Q83: How do you prevent Context Re-render Cascades in large React component trees?
**Answer:**

```javascript
// Split Context values:
const UserStateContext = createContext(null);
const UserActionsContext = createContext(null);

export function UserProvider({ children }) {
  const [user, setUser] = useState(null);
  const actions = useMemo(() => ({ login: () => {}, logout: () => {} }), []);

  return (
    <UserActionsContext value={actions}>
      <UserStateContext value={user}>
        {children}
      </UserStateContext>
    </UserActionsContext>
  );
}
```

---

### Q84: What is the difference between Cache Invalidation (`queryClient.invalidateQueries`) and Cache Reset (`queryClient.resetQueries`)?
**Answer:**

```javascript
import { useQueryClient } from '@tanstack/react-query';

function CacheManager() {
  const queryClient = useQueryClient();

  // 1. Invalidate: Marks stale; background refetch preserves visible data without flicker
  const handleRefresh = () => {
    queryClient.invalidateQueries({ queryKey: ['orders'] });
  };

  // 2. Reset: Wipes cached data immediately, restoring initial empty/skeleton state
  const handleFullReset = () => {
    queryClient.resetQueries({ queryKey: ['orders'] });
  };
}
```

---

### Q85: How do you implement Infinite Query Pagination with Virtualization in TanStack Query?
**Answer:**

```javascript
import { useInfiniteQuery } from '@tanstack/react-query';

export function useInfiniteFeed() {
  return useInfiniteQuery({
    queryKey: ['feed'],
    queryFn: ({ pageParam = 1 }) => fetch(`/api/posts?page=${pageParam}&limit=20`).then(r => r.json()),
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => lastPage.hasMore ? allPages.length + 1 : undefined
  });
}
```

---

### Q86: How do you handle WebSocket / Server-Sent Events (SSE) live updates with TanStack Query Cache?
**Answer:**

```javascript
useEffect(() => {
  const sse = new EventSource('/api/live-events');

  sse.onmessage = (event) => {
    const newRecord = JSON.parse(event.data);
    queryClient.setQueryData(['liveFeed'], (oldData = []) => [newRecord, ...oldData]);
  };

  return () => sse.close();
}, [queryClient]);
```

---

### Q87: What is Finite State Machine (FSM) architecture with XState in complex UI workflows?
**Answer:**

```javascript
import { createMachine } from 'xstate';
import { useMachine } from '@xstate/react';

const authFlowMachine = createMachine({
  id: 'auth',
  initial: 'idle',
  states: {
    idle: { on: { SUBMIT: 'authenticating' } },
    authenticating: {
      on: {
        SUCCESS: 'authenticated',
        FAILURE: 'error'
      }
    },
    authenticated: { on: { LOGOUT: 'idle' } },
    error: { on: { RETRY: 'authenticating' } }
  }
});
```

---

### Q88: How do you build an Offline-First Sync queue in React?
**Answer:**

```javascript
import { get, set } from 'idb-keyval';

async function queueOfflineMutation(mutation) {
  const queue = (await get('offline_queue')) || [];
  queue.push(mutation);
  await set('offline_queue', queue);
}

// Replay on network online:
window.addEventListener('online', async () => {
  const queue = (await get('offline_queue')) || [];
  for (const item of queue) {
    await fetch(item.url, { method: item.method, body: JSON.stringify(item.body) });
  }
  await set('offline_queue', []);
});
```

---

### Q89: How do you synchronize state between URL Search Parameters and React state (Nuqs / React Router)?
**Answer:**

```javascript
import { useSearchParams } from 'react-router-dom';

export function TabbedFilter() {
  const [searchParams, setSearchParams] = useSearchParams();
  const currentTab = searchParams.get('tab') || 'overview';

  return (
    <div>
      <button onClick={() => setSearchParams({ tab: 'overview' })}>Overview</button>
      <button onClick={() => setSearchParams({ tab: 'billing' })}>Billing</button>
      <p>Active view: {currentTab}</p>
    </div>
  );
}
```

---

### Q90: What is the Single Source of Truth principle and how do derived state anti-patterns violate it?
**Answer:**

```javascript
// ❌ ANTI-PATTERN: Duplicating prop in local state
function UserCard({ user }) {
  const [fullName, setFullName] = useState(`${user.firstName} ${user.lastName}`); // Stale if prop changes!
}

// ✅ CLEAN DERIVED STATE:
function UserCardClean({ user }) {
  const fullName = `${user.firstName} ${user.lastName}`; // Always 100% in sync!
}
```

---

### Q91: How does Redux Saga compare with Redux Thunk for asynchronous side effects?
**Answer:**

```javascript
// Redux Saga Cancellable Side Effect:
import { takeLatest, call, put } from 'redux-saga/effects';

function* fetchUserSaga(action) {
  try {
    const user = yield call(api.fetchUser, action.payload.userId);
    yield put({ type: 'USER_FETCH_SUCCEEDED', user });
  } catch (e) {
    yield put({ type: 'USER_FETCH_FAILED', message: e.message });
  }
}

export function* rootSaga() {
  yield takeLatest('USER_FETCH_REQUESTED', fetchUserSaga); // Automatically cancels previous in-flight requests!
}
```

---

### Q92: What is Stale-While-Revalidate caching policy inside TanStack Query (`staleTime` vs `gcTime`)?
**Answer:**

```javascript
const { data } = useQuery({
  queryKey: ['products'],
  queryFn: fetchProducts,
  staleTime: 1000 * 60 * 5, // 5 min: considered fresh, 0 network calls on component remount
  gcTime: 1000 * 60 * 30    // 30 min: unused cache persists in RAM before garbage collection
});
```

---

### Q93: How do you implement Multi-Tab Broadcast Channel synchronization with Zustand?
**Answer:**

```javascript
import { create } from 'zustand';

const channel = new BroadcastChannel('app_auth');

export const useAuth = create((set) => ({
  user: null,
  setUser: (user) => {
    set({ user });
    channel.postMessage({ type: 'SYNC_USER', user });
  }
}));

channel.onmessage = (event) => {
  if (event.data.type === 'SYNC_USER') {
    useAuth.setState({ user: event.data.user });
  }
};
```

---

### Q94: What is Server-Sent Query Hydration (`HydrationBoundary` / `dehydrate`) in TanStack Query?
**Answer:**

```javascript
// Next.js / Remix Server Component prefetching:
import { dehydrate, HydrationBoundary, QueryClient } from '@tanstack/react-query';

export default async function Page() {
  const queryClient = new QueryClient();
  await queryClient.prefetchQuery({ queryKey: ['posts'], queryFn: getPosts });

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <PostsListClientComponent />
    </HydrationBoundary>
  );
}
```

---

### Q95: How do you manage Form State at scale: React Hook Form vs. Formik vs. Native React 19 Actions?
**Answer:**

```javascript
import { useForm } from 'react-hook-form';

export function HighPerformanceForm() {
  const { register, handleSubmit, formState: { errors } } = useForm();

  // Uncontrolled inputs with refs -> 0 re-renders while typing!
  return (
    <form onSubmit={handleSubmit(data => console.log(data))}>
      <input {...register('username', { required: true })} />
      {errors.username && <span>Username is required</span>}
      <button type="submit">Submit</button>
    </form>
  );
}
```

---

### Q96: What is Selector Memoization in Reselect (`createSelector`)?
**Answer:**

```javascript
import { createSelector } from '@reduxjs/toolkit';

const selectItems = (state) => state.cart.items;
const selectTaxRate = (state) => state.cart.taxRate;

// Recomputes ONLY when items or taxRate change referentially:
export const selectCartTotal = createSelector(
  [selectItems, selectTaxRate],
  (items, taxRate) => items.reduce((sum, item) => sum + item.price, 0) * (1 + taxRate)
);
```

---

### Q97: How do you implement Undo / Redo Time-Travel state in React?
**Answer:**

```javascript
import { useState } from 'react';

export function useTimeTravel(initialPresent) {
  const [history, setHistory] = useState({ past: [], present: initialPresent, future: [] });

  const set = (newPresent) => setHistory(h => ({
    past: [...h.past, h.present],
    present: newPresent,
    future: []
  }));

  const undo = () => setHistory(h => {
    if (h.past.length === 0) return h;
    const previous = h.past[h.past.length - 1];
    return {
      past: h.past.slice(0, -1),
      present: previous,
      future: [h.present, ...h.future]
    };
  });

  return { state: history.present, set, undo };
}
```

---

### Q98: How do you handle Global Modals and Notifications without Context re-renders?
**Answer:**

```javascript
// Standalone Toast Store (Zero Context re-render cascades):
import { create } from 'zustand';

export const useToast = create((set) => ({
  toasts: [],
  notify: (msg) => set(s => ({ toasts: [...s.toasts, { id: Date.now(), msg }] })),
  dismiss: (id) => set(s => ({ toasts: s.toasts.filter(t => t.id !== id) }))
}));

// Any function can call: useToast.getState().notify('Order Saved!')
```

---

### Q99: What is the Immutability requirement in React state and why does direct mutation break React?
**Answer:**

```javascript
// ❌ WRONG: Mutating object preserves memory pointer -> React skips re-render!
user.name = 'Bob';
setUser(user);

// ✅ CORRECT: New object reference triggers re-render:
setUser({ ...user, name: 'Bob' });
```

---

### Q100: How do you handle State Persistence with Versioning and Migrations in Zustand / Redux Persist?
**Answer:**

```javascript
import { persist } from 'zustand/middleware';

export const useSettings = create(
  persist(
    (set) => ({ theme: 'dark', layout: 'grid' }),
    {
      name: 'user_settings',
      version: 2,
      migrate: (persistedState, version) => {
        if (version === 1) persistedState.layout = 'grid'; // Add missing migration field
        return persistedState;
      }
    }
  )
);
```
