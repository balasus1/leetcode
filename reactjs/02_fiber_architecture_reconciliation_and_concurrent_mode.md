# Part 2: Fiber Architecture, Reconciliation & Concurrent Mode (Q26 - Q50)

---

### Q26: What is a React Fiber and how did it replace the legacy Stack Reconciler?
**Answer:**
The **Fiber Reconciler** is React’s core reconciliation engine.

```javascript
// Structure of an individual React Fiber Node:
const fiberNode = {
  tag: 0,                  // FunctionComponent = 0, HostRoot = 3, HostComponent = 5
  key: null,
  elementType: App,
  type: App,
  stateNode: null,         // Real DOM instance or Class instance
  
  // Singly Linked List Tree Pointers
  child: null,             // Pointer to first child Fiber
  sibling: null,           // Pointer to next sibling Fiber
  return: null,            // Pointer to parent Fiber (where work returns)
  
  // State and Work
  memoizedProps: {},
  pendingProps: {},
  memoizedState: null,     // Linked list of hook states
  updateQueue: null,
  
  // Concurrency & Bitmask Priority
  lanes: 0b0000000000000000000000000000001, // 31-bit Lane bitmask
  childLanes: 0,
  
  // Double Buffering
  alternate: null,         // Mirror pointer in workInProgress / current tree
  flags: 0b0000000000000000000000000000100  // Side effects: Placement, Update, Deletion
};
```

---

### Q27: How does Double Buffering work in React Fiber (`current` vs `workInProgress` tree)?
**Answer:**
React uses a **Double Buffering** strategy:

```javascript
// Double Buffering Pointer Swap in React Root Commit:
function commitRoot(root) {
  const finishedWork = root.current.alternate; // workInProgress tree
  
  // 1. Flush DOM mutations atomically to screen
  commitMutationEffects(finishedWork);
  
  // 2. Atomic Pointer Swap (Instant transition to new screen state with 0 visual tearing)
  root.current = finishedWork;
}
```

---

### Q28: What are the two main phases of React rendering: The Render (Reconciliation) Phase vs. The Commit Phase?
**Answer:**

```javascript
// Phase 1: Render Phase (Asynchronous, pure computation, can be paused/aborted)
function performUnitOfWork(fiber) {
  const nextChild = beginWork(fiber); // Computes diffs, runs hooks, flags mutations
  if (!nextChild) {
    completeUnitOfWork(fiber);        // Bubbles up effects
  }
  return nextChild;
}

// Phase 2: Commit Phase (Synchronous, mutates real DOM, cannot be interrupted)
function commitPhase(finishedWork) {
  flushPassiveEffects();              // Cleans up previous useEffects
  commitPlacementAndUpdate(finishedWork); // Real DOM appendChild / update
  flushLayoutEffects(finishedWork);   // Runs useLayoutEffect synchronously
}
```

---

### Q29: What is the Lane Model in React and how does Bitmask Priority scheduling work?
**Answer:**

```javascript
// 31-bit Priority Lanes in React Internals:
const SyncLane            = 0b0000000000000000000000000000001; // User typing, click (Highest)
const InputContinuousLane = 0b0000000000000000000000000000010; // Scroll, drag
const DefaultLane         = 0b0000000000000000000000000010000; // Normal setState
const TransitionLanes     = 0b0000000000000011111111000000000; // startTransition
const IdleLane            = 0b0100000000000000000000000000000; // Low-priority background

// Bitwise operations:
const isHighPriority = (pendingLanes & (SyncLane | InputContinuousLane)) !== 0;
const highestPriorityLane = pendingLanes & -pendingLanes; // O(1) isolation of lowest set bit
```

---

### Q30: How does Time Slicing and Cooperative Scheduling work with `MessageChannel` and `requestHostCallback`?
**Answer:**

```javascript
// Cooperative Work Loop in React Scheduler:
let deadline = 0;
const frameYieldMs = 5; // 5ms frame slice budget

function shouldYieldToHost() {
  return performance.now() >= deadline;
}

const channel = new MessageChannel();
channel.port1.onmessage = function performWorkUntilDeadline() {
  deadline = performance.now() + frameYieldMs;
  
  let hasMoreWork = true;
  while (hasMoreWork && !shouldYieldToHost()) {
    hasMoreWork = workLoopConcurrentStep();
  }

  // If work remains but 5ms budget expired -> Yield to browser paint, then schedule next tick:
  if (hasMoreWork) {
    channel.port2.postMessage(null);
  }
};
```

---

### Q31: What is the Diffing Algorithm in React and what are the 3 foundational heuristic assumptions?
**Answer:**

```javascript
// React Diffing Heuristics:
// 1. Different element types destroy and rebuild entire subtree:
// <div><Counter /></div> -> <span><Counter /></span> (Counter unmounts and remounts from scratch)

// 2. Element identity preserved via stable keys across renders:
// Old: [<li key="a">A</li>, <li key="b">B</li>]
// New: [<li key="b">B</li>, <li key="a">A</li>] -> Reused without DOM destruction

// 3. Level-by-level comparison:
function reconcileChildren(current, workInProgress, nextChildren) {
  if (current === null) {
    workInProgress.child = mountChildFibers(workInProgress, null, nextChildren);
  } else {
    workInProgress.child = reconcileChildFibers(workInProgress, current.child, nextChildren);
  }
}
```

---

### Q32: Why is using array index as a `key` dangerous in dynamic lists?
**Answer:**

```javascript
// 🚨 BUGGY BEHAVIOR with Index Keys:
// Initial state: [{ text: 'Task 1' }, { text: 'Task 2' }]
// When prepending 'Task 0':
// Index 0 receives 'Task 0', Index 1 receives 'Task 1', Index 2 receives 'Task 2'
// Any local uncontrolled <input> or focus state on Index 0 stays stuck on the wrong item!

// ✅ SECURE with Stable IDs:
{items.map((item) => (
  <TodoItem key={item.uniqueId} item={item} />
))}
```

---

### Q33: How does React Reconciliation handle single-element vs. multi-child array diffing (`reconcileChildrenArray`)?
**Answer:**

```javascript
// Two-Pass Multi-Child Reconciliation in React Fiber:
function reconcileChildrenArray(returnFiber, currentFirstChild, newChildren) {
  let oldFiber = currentFirstChild;
  let newIdx = 0;
  
  // Pass 1: Sequential lockstep match
  for (; oldFiber !== null && newIdx < newChildren.length; newIdx++) {
    if (oldFiber.key !== newChildren[newIdx].key) break;
    oldFiber = oldFiber.sibling;
  }

  // Pass 2: Map-based lookup for inserted/reordered nodes
  const existingChildrenMap = new Map();
  while (oldFiber !== null) {
    existingChildrenMap.set(oldFiber.key || oldFiber.index, oldFiber);
    oldFiber = oldFiber.sibling;
  }

  // Re-use matching fibers in O(1) from Map:
  for (; newIdx < newChildren.length; newIdx++) {
    const matchedFiber = existingChildrenMap.get(newChildren[newIdx].key);
    // Reuse matchedFiber and mark Placement flags for repositioned nodes
  }
}
```

---

### Q34: What are Fiber WorkTags and what is the difference between `HostComponent`, `HostRoot`, and `FunctionComponent`?
**Answer:**

```javascript
// React Internal WorkTag Constants:
export const FunctionComponent = 0;
export const ClassComponent = 1;
export const HostRoot = 3;          // Mounted via ReactDOM.createRoot(container)
export const HostComponent = 5;     // Native HTML nodes ('div', 'span')
export const HostText = 6;          // Raw text strings
export const SuspenseComponent = 13;// <Suspense> boundaries
export const OffscreenComponent = 22; // <Activity mode="hidden">
```

---

### Q35: What is the `workLoopSync` vs. `workLoopConcurrent` loop in React Fiber?
**Answer:**

```javascript
// Synchronous Work Loop (Blocking execution)
function workLoopSync() {
  while (workInProgress !== null) {
    performUnitOfWork(workInProgress);
  }
}

// Concurrent Work Loop (Time-sliced execution)
function workLoopConcurrent() {
  while (workInProgress !== null && !shouldYieldToHost()) {
    performUnitOfWork(workInProgress);
  }
}
```

---

### Q36: How does `beginWork` and `completeWork` traverse the Fiber tree in a Depth-First Search (DFS)?
**Answer:**

```javascript
// React Fiber DFS Traversal Engine:
function performUnitOfWork(unitOfWork) {
  const current = unitOfWork.alternate;
  
  // 1. Step Down (Evaluate component and return first child):
  let next = beginWork(current, unitOfWork, renderLanes);
  unitOfWork.memoizedProps = unitOfWork.pendingProps;

  if (next === null) {
    // 2. Leaf reached -> Step Right (siblings) or Step Up (parent return):
    completeUnitOfWork(unitOfWork);
  } else {
    workInProgress = next;
  }
}
```

---

### Q37: What is Selective Hydration and how does React prioritize user interactions on un-hydrated components?
**Answer:**

```javascript
// Selective Hydration in React 18/19:
// User clicks un-hydrated <CommentSection /> while <Navbar /> is hydrating:
function dispatchDiscreteEvent(domEvent) {
  const targetFiber = getFiberFromDOMNode(domEvent.target);
  
  if (isFiberUnmergedAndUnhydrated(targetFiber)) {
    // 1. Intercept and hold click event in memory
    // 2. Elevate targetFiber priority to SyncLane
    // 3. Hydrate targetFiber immediately!
    // 4. Replay click event on newly hydrated component!
  }
}
```

---

### Q38: What are React Fiber Flags (formerly `effectTag`) and how are they committed to the DOM?
**Answer:**

```javascript
// Fiber Mutation Flags Bitmasks:
export const NoFlags         = 0b00000000000000000000;
export const Placement       = 0b00000000000000000010; // appendChild / insertBefore
export const Update          = 0b00000000000000000100; // commitUpdate (props/text)
export const Deletion        = 0b00000000000000001000; // removeChild
export const Passive         = 0b00000000000010000000; // useEffect callback
export const Layout          = 0b00000000000001000000; // useLayoutEffect callback
```

---

### Q39: What is Tearing in Concurrent React and how does `useSyncExternalStore` prevent it?
**Answer:**

```javascript
import { useSyncExternalStore } from 'react';

// Custom external store subscriber preventing concurrent tearing
function createTearFreeStore(initialState) {
  let state = initialState;
  const listeners = new Set();

  return {
    getState: () => state,
    setState: (next) => {
      state = next;
      listeners.forEach(l => l());
    },
    subscribe: (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    }
  };
}

const store = createTearFreeStore({ theme: 'dark' });

export function ThemeWatcher() {
  const state = useSyncExternalStore(store.subscribe, store.getState);
  return <div>Current Theme: {state.theme}</div>;
}
```

---

### Q40: How does `useDeferredValue` differ from `useTransition` and standard Debouncing?
**Answer:**

```javascript
import { useState, useDeferredValue, useTransition } from 'react';

export function SearchDashboard({ fullData }) {
  const [query, setQuery] = useState('');
  
  // 1. useDeferredValue: Defers derived value rendering without blocking typing
  const deferredQuery = useDeferredValue(query);

  // 2. useTransition: Wraps state setter directly
  const [isPending, startTransition] = useTransition();

  const handleSelectTab = (tab) => {
    startTransition(() => {
      setActiveTab(tab);
    });
  };

  return (
    <div>
      <input value={query} onChange={e => setQuery(e.target.value)} />
      <HeavyList query={deferredQuery} items={fullData} />
    </div>
  );
}
```

---

### Q41: How does React manage the Hook Linked List inside `fiber.memoizedState`?
**Answer:**

```javascript
// Hook Linked List Representation on Fiber Node:
const hook = {
  memoizedState: 0,         // Current state value
  baseState: 0,
  baseQueue: null,
  queue: {
    pending: null,          // Circular linked list of pending update actions
    dispatch: null,
    lastRenderedReducer: null,
    lastRenderedState: null
  },
  next: null                // Pointer to next hook in component!
};
```

---

### Q42: What is the Offscreen API / `<Activity>` component in React 19?
**Answer:**

```javascript
import { Activity, useState } from 'react';

export function TabSwitcher() {
  const [tab, setTab] = useState('home');

  return (
    <div>
      <button onClick={() => setTab('home')}>Home</button>
      <button onClick={() => setTab('profile')}>Profile</button>

      {/* Keeps components mounted in RAM; DOM hidden and throttled to IdleLane */}
      <Activity mode={tab === 'home' ? 'visible' : 'hidden'}>
        <HeavyHomeFeed />
      </Activity>

      <Activity mode={tab === 'profile' ? 'visible' : 'hidden'}>
        <UserProfileForm />
      </Activity>
    </div>
  );
}
```

---

### Q43: How does React handle Synthetic Events and Event Delegation in React 18 & 19?
**Answer:**

```javascript
// React 18 & 19 Event Delegation Root Attachment:
function listenToAllSupportedEvents(rootContainerElement) {
  const allNativeEvents = ['click', 'keydown', 'input', 'scroll', 'pointerdown'];
  allNativeEvents.forEach(eventType => {
    // Attached strictly to root DOM container node (#root), NOT document!
    rootContainerElement.addEventListener(eventType, dispatchSyntheticEvent);
  });
}
```

---

### Q44: What is the difference between `flushSync()` and automatic batching?
**Answer:**

```javascript
import { useState } from 'react';
import { flushSync } from 'react-dom';

export function AutoScrollChat() {
  const [messages, setMessages] = useState([]);

  function sendMessage(text) {
    // Force immediate synchronous DOM render before executing next line:
    flushSync(() => {
      setMessages(prev => [...prev, text]);
    });
    
    // Guaranteed that DOM contains new message element here:
    chatBoxRef.current.scrollTop = chatBoxRef.current.scrollHeight;
  }
}
```

---

### Q45: How does React Error Boundary catch errors and why can't it catch errors in async callbacks or SSR?
**Answer:**

```javascript
import React from 'react';

export class GlobalErrorBoundary extends React.Component {
  state = { hasError: false, error: null };

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('[Error Boundary Caught]', error, errorInfo.componentStack);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-fallback">
          <h2>Application Crash Prevented</h2>
          <p>{this.state.error?.message}</p>
          <button onClick={() => this.setState({ hasError: false })}>Try Again</button>
        </div>
      );
    }
    return this.props.children;
  }
}
```

---

### Q46: What is the difference between `useLayoutEffect` and `useEffect` execution timing?
**Answer:**

```javascript
import { useState, useLayoutEffect, useEffect, useRef } from 'react';

export function TooltipPositioner({ targetRect }) {
  const tooltipRef = useRef(null);
  const [coords, setCoords] = useState({ top: 0, left: 0 });

  // Synchronous BEFORE Paint: Prevents tooltip from visibly jumping on screen!
  useLayoutEffect(() => {
    const height = tooltipRef.current.offsetHeight;
    setCoords({
      top: targetRect.top - height - 10,
      left: targetRect.left
    });
  }, [targetRect]);

  // Asynchronous AFTER Paint: Telemetry / Subscriptions
  useEffect(() => {
    analytics.track('tooltip_viewed');
  }, []);

  return <div ref={tooltipRef} style={{ position: 'fixed', top: coords.top, left: coords.left }}>Tooltip</div>;
}
```

---

### Q47: How does React deduplicate multiple setState calls inside the same tick?
**Answer:**

```javascript
// Internal Update Queue processing in React Fiber:
function processUpdateQueue(workInProgress, props, queue, renderLanes) {
  let update = queue.firstBaseUpdate;
  let newState = queue.baseState;

  while (update !== null) {
    // Process queued actions in sequence into final state value:
    if (typeof update.action === 'function') {
      newState = update.action(newState);
    } else {
      newState = update.action;
    }
    update = update.next;
  }
  workInProgress.memoizedState = newState; // Single state committed!
}
```

---

### Q48: What is the role of `React.Children` and why is it discouraged in modern React 19?
**Answer:**

```javascript
// ❌ Discouraged Legacy Pattern:
function OldTabs({ children }) {
  return React.Children.map(children, child => {
    return React.cloneElement(child, { active: true }); // Opaque, fragile prop cloning!
  });
}

// ✅ Modern React 19 Pattern (Compound Components with Context):
const TabContext = createContext({ active: false });
function ModernTabs({ children }) {
  return <TabContext value={{ active: true }}>{children}</TabContext>;
}
```

---

### Q49: How does React 19 detect and warn about Infinite Re-render Loops?
**Answer:**

```javascript
// React Re-render Guard in Fiber Work Loop:
let nestedUpdateCount = 0;
const NESTED_UPDATE_LIMIT = 50;

function ensureRootIsScheduled(root) {
  nestedUpdateCount++;
  if (nestedUpdateCount > NESTED_UPDATE_LIMIT) {
    nestedUpdateCount = 0;
    throw new Error('Maximum update depth exceeded. This can happen when a component repeatedly calls setState inside render.');
  }
}
```

---

### Q50: How does Concurrent Mode prioritize User Input over Data Fetching?
**Answer:**

```javascript
// Fiber Scheduler Priority Preemption:
function requestUpdateLane(fiber) {
  // If event triggered by user keyboard/click -> Assign SyncLane
  if ((executionContext & DiscreteEventContext) !== 0) {
    return SyncLane;
  }
  // If transition update -> Assign TransitionLane
  if (currentTransition !== null) {
    return TransitionLanes;
  }
  return DefaultLane;
}
```
