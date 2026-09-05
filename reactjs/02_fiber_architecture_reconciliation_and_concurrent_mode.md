# Part 2: Fiber Architecture, Reconciliation & Concurrent Mode (Q26 - Q50)

---

### Q26: What is a React Fiber and how did it replace the legacy Stack Reconciler?
**Answer:**
The **Fiber Reconciler** is React’s core reconciliation engine.

```
Legacy Stack Reconciler (React <16):
- Recursive synchronous call stack traversal.
- Cannot be paused, aborted, or prioritized.
- Heavy re-renders block the browser main thread (>16ms), causing dropped frames (jank).

Fiber Architecture (React 16 -> 19):
- Virtual stack frame implemented as a JavaScript object (Fiber Node).
- Uses a Singly Linked List tree structure (child, sibling, return pointers).
- Work can be paused, split into chunks, aborted, prioritized, and resumed!
```

**Structure of a Fiber Node:**
```javascript
const fiberNode = {
  tag: WorkTag,            // FunctionComponent, ClassComponent, HostRoot, etc.
  key: null,
  elementType: App,
  type: App,
  stateNode: null,         // Real DOM node or Class instance
  
  // Singly Linked List Tree Pointers
  child: FiberNode,        // First child
  sibling: FiberNode,      // Next sibling
  return: FiberNode,       // Parent fiber (where work returns)
  
  // Work and State
  memoizedProps: {},
  pendingProps: {},
  memoizedState: {},       // Hook linked list
  updateQueue: null,
  
  // Concurrency & Priority
  lanes: Lane,             // Bitmask priority lanes
  childLanes: Lane,
  
  // Double Buffering
  alternate: FiberNode,    // Pointer to mirror fiber in workInProgress / current tree
  flags: Flags,            // Side effects (Placement, Update, Deletion)
};
```

---

### Q27: How does Double Buffering work in React Fiber (`current` vs `workInProgress` tree)?
**Answer:**
Similar to graphics rendering (OpenGL/DirectX), React uses a **Double Buffering** strategy to prevent half-rendered, flickering UI states from appearing on the screen.

```
[Screen Display] <── Linked to ── [current Tree] (Committed, visible DOM)
                                        │
                                 alternate pointer
                                        │
[Background Render] ───────────> [workInProgress Tree] (Draft / Being computed)
```

1. **`current` Tree**: Represents the fibers currently mounted and visible on the screen.
2. **`workInProgress` Tree**: Constructed in memory during the asynchronous render phase.
3. When the `workInProgress` tree finishes and is committed to the real DOM, React simply swaps a single pointer (`root.current = workInProgress`), turning the draft tree into the visible tree in a single atomic operation!

---

### Q28: What are the two main phases of React rendering: The Render (Reconciliation) Phase vs. The Commit Phase?
**Answer:**

| Phase | Characteristics | Schedulable? | Side Effects Allowed? |
|---|---|---|---|
| **1. Render Phase**<br>(Reconciliation) | Computes JSX diffs, runs hooks, constructs `workInProgress` fiber tree, flags mutations (`Placement`, `Update`). | **Asynchronous & Interruptible** (Can pause or yield to high-priority user input). | **Pure computation only** (NO DOM mutations or network triggers). |
| **2. Commit Phase** | Flushes DOM mutations, runs `useLayoutEffect`, paints DOM, runs `useEffect` asynchronously. | **Synchronous & Non-interruptible** (Executes in a single atomic tick to prevent visual tearing). | **Yes** (Mutates real DOM, binds event listeners). |

---

### Q29: What is the Lane Model in React and how does Bitmask Priority scheduling work?
**Answer:**
Prior to React 17, React used numeric priorities (Expiration Times). React modern Fiber uses a **31-bit Bitmask Lane Model** to represent task priorities and concurrent lanes.

```javascript
// Internal Lane Constants (31-bit integer bitmasks)
const TotalLanes = 31;
const SyncLane               = 0b0000000000000000000000000000001; // Blocking User Input (Click, Type)
const InputContinuousLane    = 0b0000000000000000000000000000010; // Drag, Scroll, MouseMove
const DefaultLane            = 0b0000000000000000000000000010000; // Normal useState update
const TransitionLanes        = 0b0000000000000011111111000000000; // startTransition updates
const IdleLane               = 0b0100000000000000000000000000000; // Offscreen / Low priority
```

**Bitwise Efficiency:**
- Combining lanes: `lanes = laneA | laneB`
- Checking intersection: `(lanes & SyncLane) !== 0`
- Selecting highest priority lane: `lanes & -lanes` (isolate lowest set bit in $O(1)$ assembly instructions).

---

### Q30: How does Time Slicing and Cooperative Scheduling work with `MessageChannel` and `requestHostCallback`?
**Answer:**
React avoids `window.requestIdleCallback` because of poor browser support and low 20fps refresh caps.
Instead, React’s Scheduler uses **`MessageChannel`** (a micro-macrotask primitive with ~0ms delay):

1. React sets a target frame budget of **5ms per work unit**.
2. Inside `workLoopConcurrent()`, React checks `shouldYieldToHost()` after processing each fiber node:
   ```javascript
   function shouldYieldToHost() {
     return performance.now() >= deadline; // 5ms budget exceeded!
   }
   ```
3. If 5ms is exceeded and high-priority browser input is pending, React yields control back to the browser to paint and process mouse events, then posts a message on `MessageChannel` to resume work in the next frame.

---

### Q31: What is the Diffing Algorithm in React and what are the 3 foundational heuristic assumptions?
**Answer:**
A general tree comparison algorithm has $O(n^3)$ time complexity (1,000 nodes = 1 billion comparisons). React reduces this to **$O(n)$** using 3 heuristics:

1. **Two elements of different types produce different trees**: If `<div>` changes to `<span>`, React destroys the entire subtree and builds a new one from scratch.
2. **Component Identity via Keys**: Keys must be stable, unique, and predictable between renders to match child elements across list re-orderings.
3. **Breadth-First Level-by-Level Diffing**: React only compares nodes at the same tree depth; it does not attempt to match nodes moved across different tree branches.

---

### Q32: Why is using array index as a `key` dangerous in dynamic lists?
**Answer:**
Using index as a key confuses the reconciler when items are inserted, prepended, or deleted:

```javascript
// Initial:
[0: 'Item A', 1: 'Item B']

// Prepend 'Item New':
[0: 'Item New', 1: 'Item A', 2: 'Item B']
```
React compares index `0` (`Item A` vs `Item New`). Because both share key `0`, React retains the old component instance and local DOM input states, causing **unintended state bleeding, incorrect input focus, and animation glitches**.

---

### Q33: How does React Reconciliation handle single-element vs. multi-child array diffing (`reconcileChildrenArray`)?
**Answer:**
When reconciling an array of children, React uses a **two-pass algorithm** to avoid nested loops:

1. **Pass 1 (Fast-Path Sequential Match)**: Iterates through old and new child arrays in lockstep while keys match. Stops on the first mismatch.
2. **Pass 2 (Map-Based Lookup for Reordering/Insertions)**:
   - Remaining old fibers are placed into a `Map<key | index, FiberNode>`.
   - React iterates through the remaining new elements, querying the Map in $O(1)$ time to reuse existing fibers and marking `Placement` flags for moved nodes.
   - Any unused fibers remaining in the Map are marked with the `Deletion` flag.

---

### Q34: What are Fiber WorkTags and what is the difference between `HostComponent`, `HostRoot`, and `FunctionComponent`?
**Answer:**
`fiber.tag` identifies the type of work unit:
- **`HostRoot` (3)**: The root node of the React component tree mounted via `createRoot`.
- **`HostComponent` (5)**: Native DOM elements (`<div>`, `<span>`, `<button>`).
- **`FunctionComponent` (0)**: Functional React components.
- **`SuspenseComponent` (13)**: Suspense boundary nodes.
- **`OffscreenComponent` (22)**: Nodes hidden or pre-rendered offscreen.

---

### Q35: What is the `workLoopSync` vs. `workLoopConcurrent` loop in React Fiber?
**Answer:**

```javascript
// Synchronous Work Loop (Blocking, cannot be paused)
function workLoopSync() {
  while (workInProgress !== null) {
    performUnitOfWork(workInProgress);
  }
}

// Concurrent Work Loop (Time-sliced, cooperative multitasking)
function workLoopConcurrent() {
  while (workInProgress !== null && !shouldYield()) {
    performUnitOfWork(workInProgress); // Process 1 fiber node
  }
}
```

---

### Q36: How does `beginWork` and `completeWork` traverse the Fiber tree in a Depth-First Search (DFS)?
**Answer:**
React traverses the Fiber tree using a two-phase DFS without recursion:

```
                  Root
                 /    \
            Navbar     Sidebar
            /    \
        Logo     Links

1. beginWork: Root -> Navbar -> Logo (Drills down via .child pointers)
2. completeWork: Logo finishes, moves to sibling -> Links
3. completeWork: Links finishes, bubbles up to Navbar via .return pointer
4. beginWork: Navbar finishes, moves to sibling -> Sidebar
```

- **`beginWork(current, workInProgress, renderLanes)`**: Evaluates props, executes hook functions, runs reconciliation diff, and returns the next `.child` fiber.
- **`completeWork(current, workInProgress, renderLanes)`**: Bubbles up from leaves, constructs real DOM instances, attaches event listeners, and aggregates subtree update flags.

---

### Q37: What is Selective Hydration and how does React prioritize user interactions on un-hydrated components?
**Answer:**
In traditional SSR, the entire HTML page is non-interactive until all JavaScript bundles download and hydrate.
**Selective Hydration (React 18/19 with `<Suspense>`)**:
1. Components wrapped in `<Suspense>` hydrate independently as their JS chunks load.
2. If a user clicks on an **un-hydrated button**, React captures the click event, **immediately pauses ongoing background hydration, prioritizes and hydrates the clicked component synchronously**, and then replays the user's click event seamlessly!

---

### Q38: What are React Fiber Flags (formerly `effectTag`) and how are they committed to the DOM?
**Answer:**
During the render phase, React assigns bitmask flags to fibers describing the DOM operation needed:
- `Placement (0b00000000000010)`: Insert new DOM node (`appendChild` / `insertBefore`).
- `Update (0b00000000000100)`: Update attributes, styles, or text content.
- `Deletion (0b00000000001000)`: Remove DOM node (`removeChild`).

In the **Commit Phase**, React traverses only the fibers with flags and executes the minimal set of real DOM mutations.

---

### Q39: What is Tearing in Concurrent React and how does `useSyncExternalStore` prevent it?
**Answer:**
**Tearing** occurs in concurrent rendering when a non-React external store (e.g. Redux, Zustand, RxJS) updates *in the middle of an asynchronous render phase*. Component A renders with version 1 of the store, React yields, the store mutates to version 2, and Component B renders with version 2—causing visual inconsistencies on the same screen!

**`useSyncExternalStore` Solution**:
Forces synchronous consistency for external stores, guaranteeing zero tearing.

```javascript
import { useSyncExternalStore } from 'react';

function subscribe(callback) {
  window.addEventListener('online', callback);
  window.addEventListener('offline', callback);
  return () => {
    window.removeEventListener('online', callback);
    window.removeEventListener('offline', callback);
  };
}

function getSnapshot() {
  return navigator.onLine;
}

export function NetworkStatusIndicator() {
  // Tear-free synchronization with browser online status
  const isOnline = useSyncExternalStore(subscribe, getSnapshot);
  return <div>Status: {isOnline ? '🟢 Online' : '🔴 Offline'}</div>;
}
```

---

### Q40: How does `useDeferredValue` differ from `useTransition` and standard Debouncing?
**Answer:**

```
+─────────────────────────────────────────────────────────────────────────────+
| Feature           | Debounce (setTimeout) | useTransition | useDeferredValue|
+───────────────────┼───────────────────────┼───────────────┼─────────────────+
| **Mechanism**     | Artificial fixed delay| Low-priority  | Low-priority    |
|                   | (e.g. 300ms lag)      | State Update  | Derived Value   |
+───────────────────┼───────────────────────┼───────────────┼─────────────────+
| **CPU Awareness** | No (Fires blindly)    | Yes (Adapts to| Yes (Adapts to  |
|                   |                       | device speed) | device speed)   |
+───────────────────┼───────────────────────┼───────────────┼─────────────────+
| **Access to State**| Direct setter        | Wraps setter  | Wraps value     |
|                   |                       | (setCount)    | (query)         |
+───────────────────┼───────────────────────┼───────────────┼─────────────────+
| **Pending State** | Manual flag           | `isPending`   | `value !==      |
|                   |                       | boolean       | deferredValue`  |
+───────────────────┴───────────────────────┴───────────────┴─────────────────+
```

```javascript
import { useState, useDeferredValue } from 'react';

export function SearchFilter({ largeDataset }) {
  const [query, setQuery] = useState('');
  const deferredQuery = useDeferredValue(query); // Adapts rendering to frame budget

  const isStale = query !== deferredQuery;

  return (
    <div>
      <input value={query} onChange={e => setQuery(e.target.value)} />
      <div style={{ opacity: isStale ? 0.5 : 1 }}>
        <HeavyList query={deferredQuery} items={largeDataset} />
      </div>
    </div>
  );
}
```

---

### Q41: How does React manage the Hook Linked List inside `fiber.memoizedState`?
**Answer:**
Hooks inside a component are stored as a **singly linked list** on `fiber.memoizedState`:

```
fiber.memoizedState ──> [Hook 1: useState]
                               │ .next
                               ▼
                        [Hook 2: useEffect]
                               │ .next
                               ▼
                        [Hook 3: useRef]
```
Each hook node has `{ memoizedState, baseState, queue, next }`.
This is why **Hooks must never be called inside conditions or loops**: altering the call order breaks the fixed index pointers in the linked list!

---

### Q42: What is the Offscreen API / `<Activity>` component in React 19?
**Answer:**
`<Activity mode="hidden">` (formerly `<Offscreen>`) allows keeping a component mounted in memory while removing its DOM nodes from the visual screen.
- Preserves local state, scroll position, and active focus.
- Lowers priority of child fibers to `IdleLane`.
- Perfect for instant multi-tab switching and virtualized view caching without re-mounting overhead.

---

### Q43: How does React handle Synthetic Events and Event Delegation in React 18 & 19?
**Answer:**
- In React 16 and earlier, React attached global event listeners to `document`.
- In React 17, 18, and 19, React attaches event listeners to the **Root DOM Container (`#root`)** where `createRoot()` was mounted.
- **SyntheticEvent**: A cross-browser wrapper conforming to W3C standards with event pooling eliminated in modern React.

---

### Q44: What is the difference between `flushSync()` and automatic batching?
**Answer:**
- **Automatic Batching (Default in React 18/19)**: Multiple state updates across promises, `setTimeout`, or native event handlers are grouped into a single re-render.
- **`flushSync(callback)`**: Forces React to synchronously re-render and flush DOM updates immediately (useful for measuring DOM elements immediately after state update).

```javascript
import { useState } from 'react';
import { flushSync } from 'react-dom';

function ScrollToBottom() {
  const [messages, setMessages] = useState([]);

  function handleSend(newMsg) {
    flushSync(() => {
      setMessages(prev => [...prev, newMsg]); // Forces immediate synchronous DOM render!
    });
    // DOM is guaranteed to be updated here:
    chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
  }
}
```

---

### Q45: How does React Error Boundary catch errors and why can't it catch errors in async callbacks or SSR?
**Answer:**
- Error Boundaries catch errors thrown during **Render Phase, Lifecycle methods (`componentDidMount`), and Constructors**.
- **Cannot catch**:
  1. Errors in async callbacks (`setTimeout`, `onClick`) $\rightarrow$ Use `try/catch` or `useActionState`.
  2. Server-Side Rendering (SSR) $\rightarrow$ Handled at server request level.
  3. Errors thrown inside the Error Boundary component itself.

---

### Q46: What is the difference between `useLayoutEffect` and `useEffect` execution timing?
**Answer:**
- **`useLayoutEffect`**: Runs **synchronously after DOM mutations but BEFORE the browser paints**. Used strictly for measuring DOM layouts (e.g. tooltips, popover positioning) to prevent visual flickering.
- **`useEffect`**: Runs **asynchronously AFTER the browser paints**. Used for network requests, telemetry, and subscriptions.

---

### Q47: How does React deduplicate multiple setState calls inside the same tick?
**Answer:**
State updates are appended to the fiber's `updateQueue` circular linked list. During the render phase, React processes all pending updates in sequence, calculating the final `memoizedState` in a single pass.

---

### Q48: What is the role of `React.Children` and why is it discouraged in modern React 19?
**Answer:**
`React.Children` (`map`, `forEach`, `toArray`) was used to manipulate child elements dynamically.
**Discouraged in React 19** because it relies on inspecting opaque React elements, breaks server components, and is easily replaced by explicit render props or compound component Context.

---

### Q49: How does React 19 detect and warn about Infinite Re-render Loops?
**Answer:**
React caps consecutive synchronous re-renders at **50 iterations**. If a component triggers state updates during render without a terminating condition, React aborts and throws `Maximum update depth exceeded`.

---

### Q50: How does Concurrent Mode prioritize User Input over Data Fetching?
**Answer:**
User input (keyboard typing, clicks) is assigned to `SyncLane` (highest priority). When a keypress occurs while a low-priority `TransitionLane` (e.g. graph rendering) is computing, React **aborts the background work in progress, renders the keypress immediately in under 5ms, and restarts the graph render**.
