# Part 4: State Management, Server State & Optimistic UI (Q76 - Q100)

---

### Q76: What is the fundamental difference between Server State and Client State?
**Answer:**

```
+─────────────────────────────────────────────────────────────────────────────+
| Feature              | Client State (UI State)      | Server State          |
+──────────────────────┼──────────────────────────────┼───────────────────────+
| **Ownership**        | Owned 100% by the browser    | Owned remotely by DB  |
| **Persistence**      | Ephemeral (lost on refresh)  | Persistent in database|
| **Concurrency**      | Synchronous, single-user     | Asynchronous, shared  |
|                      |                              | across multiple users |
| **Integrity**        | Always accurate & immediate  | Out of date (stale)   |
|                      |                              | as soon as fetched    |
| **Examples**         | Modal open, theme, tab index | User profile, cart,   |
|                      | draft form inputs            | notifications, orders |
| **Best Tool**        | useState, useReducer, Zustand| TanStack Query, SWR,  |
|                      |                              | React 19 Actions      |
+──────────────────────┴──────────────────────────────┴───────────────────────+
```

---

### Q77: Why has the industry shifted from global Redux stores to Server State Managers (TanStack Query / SWR) + Minimal Client Stores (Zustand)?
**Answer:**
In early React applications, developers dumped everything into a massive monolithic Redux store:
- Required writing 50+ lines of boilerplate per endpoint (`FETCH_START`, `FETCH_SUCCESS`, `FETCH_ERROR`, reducers, action creators).
- Suffered from cache staleness, missing garbage collection, manual deduplication, and lack of automatic background refetching on window focus.

**Modern Architecture**:
1. **Server State (90% of app data)**: Delegated to **TanStack Query / SWR** (automatic caching, refetching, deduping, pagination, mutation rollbacks).
2. **Client State (10% of app data)**: Kept in lightweight atomic/slice stores like **Zustand** or **Jotai**.

---

### Q78: How does Zustand implement high-performance state management without Context re-render bloat?
**Answer:**
Zustand lives outside the React component tree and uses **`useSyncExternalStore` with selective subscriptions**.

```javascript
import { create } from 'zustand';

// Store definition
export const useCartStore = create((set) => ({
  items: [],
  isOpen: false,
  toggleCart: () => set((state) => ({ isOpen: !state.isOpen })),
  addItem: (item) => set((state) => ({ items: [...state.items, item] }))
}));

// Component A subscribes ONLY to isOpen:
export function CartDrawer() {
  const isOpen = useCartStore((state) => state.isOpen); // Re-renders ONLY when isOpen changes!
  return isOpen ? <aside>Cart Drawer Content</aside> : null;
}

// Component B reads action without subscribing to any state (ZERO re-renders!):
export function OpenCartButton() {
  const toggleCart = useCartStore((state) => state.toggleCart);
  return <button onClick={toggleCart}>Open Cart</button>;
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
    
    // 1. When mutation is fired:
    onMutate: async (newTodo) => {
      // Cancel outgoing refetches (so they don't overwrite optimistic update)
      await queryClient.cancelQueries({ queryKey: ['todos'] });

      // Snapshot previous value for rollback
      const previousTodos = queryClient.getQueryData(['todos']);

      // Optimistically update query cache
      queryClient.setQueryData(['todos'], (old = []) =>
        old.map((todo) => (todo.id === newTodo.id ? { ...todo, ...newTodo } : todo))
      );

      return { previousTodos }; // Return context with snapshot
    },

    // 2. If mutation fails, roll back to snapshot:
    onError: (err, newTodo, context) => {
      queryClient.setQueryData(['todos'], context.previousTodos);
    },

    // 3. Always refetch after error or success to guarantee backend sync:
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['todos'] });
    }
  });
}
```

---

### Q80: What is State Normalization (Normalizr / Redux EntityAdapter) and why is it required in complex nested relational data?
**Answer:**
Storing nested arrays (e.g. `users -> posts -> comments -> author`) causes duplicate copies of the same entity in memory. If a user edits their name, updating every nested post/comment requires deep tree mutations.

**Normalized Structure (Database Table format):**
```javascript
{
  entities: {
    users: { 1: { id: 1, name: 'Alice' } },
    posts: { 101: { id: 101, authorId: 1, commentIds: [201, 202] } },
    comments: { 201: { id: 201, text: 'Great post!', userId: 1 } }
  },
  ids: [101]
}
```
Updating `users[1].name` updates the name across the entire application in $O(1)$ time.

---

### Q81: What is the difference between Redux Toolkit (RTK) and legacy Redux?
**Answer:**
- **Legacy Redux**: Required manual action types, switch-statement reducers, `redux-thunk` configuration, and manual immutable spread operators (`...state`).
- **Redux Toolkit (`createSlice`, `createAsyncThunk`)**:
  - Uses **Immer** under the hood, allowing "mutating" syntax (`state.count++`) that safely produces immutable updates.
  - Generates action creators and action types automatically.
  - Standardizes store configuration with good defaults (`configureStore`).

```javascript
import { createSlice, configureStore } from '@reduxjs/toolkit';

const counterSlice = createSlice({
  name: 'counter',
  initialState: { value: 0 },
  reducers: {
    increment: (state) => {
      state.value += 1; // Immer safely handles immutable cloning!
    }
  }
});
```

---

### Q82: What are Atomic State Managers (Jotai / Recoil) and how do they differ from Slice-based stores (Zustand / Redux)?
**Answer:**
- **Slice/Store model (Zustand/Redux)**: Top-down state tree. Components select slices.
- **Atomic model (Jotai)**: Bottom-up composition of discrete state cells (**atoms**).
  - Atoms can depend on other atoms (Derived Atoms / Computed State).
  - Eliminates context provider wrappers.

```javascript
import { atom, useAtom } from 'jotai';

export const countAtom = atom(0);
export const doubleCountAtom = atom((get) => get(countAtom) * 2); // Derived read-only atom

function Counter() {
  const [count, setCount] = useAtom(countAtom);
  const [doubleCount] = useAtom(doubleCountAtom);

  return <button onClick={() => setCount(c => c + 1)}>Count: {count} (Double: {doubleCount})</button>;
}
```

---

### Q83: How do you prevent Context Re-render Cascades in large React component trees?
**Answer:**
When a Context value object changes (`value={{ user, theme }}`), **every single component calling `useContext(MyContext)` re-renders**, even if it only uses `theme`!

**Solutions:**
1. **Split Contexts**: Separate frequently changing state (`UserContext`) from static state (`ThemeContext`).
2. **Context Selectors** (`use-context-selector` or Zustand).
3. **Memoize Context Value**:
   ```javascript
   const contextValue = useMemo(() => ({ user, setUser }), [user]);
   return <UserContext value={contextValue}>{children}</UserContext>;
   ```

---

### Q84: What is the difference between Cache Invalidation (`queryClient.invalidateQueries`) and Cache Reset (`queryClient.resetQueries`)?
**Answer:**
- **`invalidateQueries`**: Marks queries as stale immediately. If the query is currently mounted/visible on the screen, it refetches in the background without clearing current UI data.
- **`resetQueries`**: Resets query state back to its initial `initialData`, removing all cached values and showing loading skeletons.

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
    getNextPageParam: (lastPage, allPages) => {
      return lastPage.hasMore ? allPages.length + 1 : undefined;
    }
  });
}
```

---

### Q86: How do you handle WebSocket / Server-Sent Events (SSE) live updates with TanStack Query Cache?
**Answer:**
Listen to incoming WebSocket messages and mutate the TanStack Query cache directly using `queryClient.setQueryData()`:

```javascript
useEffect(() => {
  const socket = new WebSocket('wss://api.domain.com/live');

  socket.onmessage = (event) => {
    const newNotification = JSON.parse(event.data);
    
    // Inject real-time update directly into query cache!
    queryClient.setQueryData(['notifications'], (old = []) => [
      newNotification,
      ...old
    ]);
  };

  return () => socket.close();
}, [queryClient]);
```

---

### Q87: What is Finite State Machine (FSM) architecture with XState in complex UI workflows?
**Answer:**
Complex multi-step flows (e.g. Stripe checkout, multi-factor auth) suffer from "impossible states" (e.g. `isLoading: true, isError: true, isSuccess: true`).
**XState FSM** guarantees that a component can only exist in exactly one deterministic state at a time with strict allowed transitions.

---

### Q88: How do you build an Offline-First Sync queue in React?
**Answer:**
1. Intercept mutations when `navigator.onLine === false`.
2. Persist failed mutation payloads to IndexedDB (via `idb-keyval`).
3. Listen to `window.addEventListener('online', ...)` and replay queued mutations in sequential order.

---

### Q89: How do you synchronize state between URL Search Parameters and React state (Nuqs / React Router)?
**Answer:**
Storing filter and pagination state in URL search parameters (`?tab=billing&page=2`) ensures that links are bookmarkable, shareable, and support browser back/forward history navigation seamlessly.

---

### Q90: What is the Single Source of Truth principle and how do derived state anti-patterns violate it?
**Answer:**
- **Anti-Pattern**: Copying props into local state (`const [email, setEmail] = useState(props.email)`). If `props.email` changes from the parent, the local state becomes out of sync!
- **Best Practice**: Compute derived values on the fly during render:
  ```javascript
  function UserCard({ user }) {
    const fullName = `${user.firstName} ${user.lastName}`; // Derived on the fly!
  }
  ```

---

### Q91: How does Redux Saga compare with Redux Thunk for asynchronous side effects?
**Answer:**
- **Redux Thunk**: Simple functions returning async/await. Harder to test and cancel.
- **Redux Saga**: Uses ES6 Generator functions (`yield takeEvery`, `yield race`). Provides declarative, cancellable side effects suitable for complex financial/orchestration flows.

---

### Q92: What is Stale-While-Revalidate caching policy inside TanStack Query (`staleTime` vs `gcTime`)?
**Answer:**
- **`staleTime` (Default: 0)**: How long fetched data is considered "fresh". Queries with active staleTime will **not** trigger a background refetch when components mount.
- **`gcTime` / `cacheTime` (Default: 5 minutes)**: How long unused/unmounted query data stays in memory before being garbage collected from the cache.

---

### Q93: How do you implement Multi-Tab Broadcast Channel synchronization with Zustand?
**Answer:**

```javascript
import { create } from 'zustand';

const authBroadcast = new BroadcastChannel('auth_channel');

export const useAuthStore = create((set) => ({
  token: null,
  login: (token) => {
    set({ token });
    authBroadcast.postMessage({ type: 'LOGIN', token });
  },
  logout: () => {
    set({ token: null });
    authBroadcast.postMessage({ type: 'LOGOUT' });
  }
}));

authBroadcast.onmessage = (event) => {
  if (event.data.type === 'LOGOUT') {
    useAuthStore.setState({ token: null });
  }
};
```

---

### Q94: What is Server-Sent Query Hydration (`HydrationBoundary` / `dehydrate`) in TanStack Query?
**Answer:**
Allows prefetching queries on the server inside Next.js/Remix Server Components, serializing (`dehydrate`) the cache into the HTML stream, and rehydrating on the client with zero initial client-side network fetch.

---

### Q95: How do you manage Form State at scale: React Hook Form vs. Formik vs. Native React 19 Actions?
**Answer:**
- **Formik**: Controlled components (re-renders form on every keystroke). Slow on forms with >30 fields.
- **React Hook Form**: Uncontrolled components via refs with isolated subscription rendering (Sub-millisecond typing performance).
- **React 19 Actions (`useActionState`)**: Native progressive enhancement with zero bundle dependency.

---

### Q96: What is Selector Memoization in Reselect (`createSelector`)?
**Answer:**
`createSelector` creates memoized selectors that only recompute expensive transformations (e.g. filtering 10,000 items) when the input arguments change referentially.

---

### Q97: How do you implement Undo / Redo Time-Travel state in React?
**Answer:**
Maintain a triple state: `{ past: [], present: value, future: [] }`.
- **Undo**: Move `present` to `future`, pop last item from `past` to `present`.
- **Redo**: Move `present` to `past`, pop first item from `future` to `present`.

---

### Q98: How do you handle Global Modals and Notifications without Context re-renders?
**Answer:**
Use an imperatively callable event store (Zustand or EventBus) where `<ToastContainer />` subscribes exclusively to toast queues, allowing any function `toast.success('Done')` to trigger toasts without wrapping root components in heavy contexts.

---

### Q99: What is the Immutability requirement in React state and why does direct mutation break React?
**Answer:**
React relies on **shallow object reference equality (`oldState === newState`)** to detect state changes.
Mutating an object directly (`state.items.push(x)`) preserves the same memory address reference, so React’s reconciler concludes that nothing changed and **skips re-rendering completely**!

---

### Q100: How do you handle State Persistence with Versioning and Migrations in Zustand / Redux Persist?
**Answer:**
When updating application data structures, old persisted schemas in user browsers will crash the app. Use migration schemas:

```javascript
import { persist, createJSONStorage } from 'zustand/middleware';

export const useSettingsStore = create(
  persist(
    (set) => ({ theme: 'dark', fontSize: 14 }),
    {
      name: 'app-settings',
      version: 2, // Incremented version
      migrate: (persistedState, version) => {
        if (version === 0) {
          persistedState.fontSize = 14; // Add missing key in v2
        }
        return persistedState;
      }
    }
  )
);
```
