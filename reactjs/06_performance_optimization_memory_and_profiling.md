# Part 6: Performance Optimization, Profiling & Memory (Q126 - Q150)

---

### Q126: How do you read and interpret a React DevTools Profiler Flamegraph and Ranked Chart?
**Answer:**

```
+─────────────────────────────────────────────────────────────────────────────+
|                        React DevTools Profiler Views                        |
+──────────────────────────────────────┬──────────────────────────────────────+
| 1. Flamegraph View                   | Visualizes component hierarchy tree; |
|                                      | width = time taken, color = intensity|
|                                      | (Yellow/Orange = Slow, Grey = Skipped|
+──────────────────────────────────────┼──────────────────────────────────────+
| 2. Ranked Chart View                 | Sorts all rendered components in     |
|                                      | order of execution duration (Fastest |
|                                      | way to find bottleneck components)   |
+──────────────────────────────────────┼──────────────────────────────────────+
| 3. "Why did this render?" Tooltip    | Explains exact cause: props changed, |
|                                      | state changed, context changed, or   |
|                                      | parent re-rendered.                  |
+──────────────────────────────────────┴──────────────────────────────────────+
```

---

### Q127: What is the difference between Component Render Time (Self Duration) and Subtree Duration (Base Duration)?
**Answer:**
- **Self Duration**: The time spent rendering this specific component alone (excluding time spent in child components). High self duration indicates heavy synchronous computation in the component body.
- **Base Duration**: The estimated time it would take to re-render the entire component subtree without any memoization (worst-case cost).

---

### Q128: What are the primary causes of Unnecessary Re-Renders in React applications?
**Answer:**

1. **Parent Re-rendering Unmemoized Children**: When a parent re-renders, all child components re-render by default.
2. **Inline Object / Array Prop Literals**: Passing `<Component config={{ theme: 'dark' }} />` creates a new object reference every render.
3. **Anonymous Inline Arrow Functions**: `<button onClick={() => handleClick(id)} />` creates a new function pointer every render.
4. **Context Value Object Thrashing**: Passing `value={{ user, theme }}` without `useMemo`.
5. **Missing or Volatile Keys in Lists**: Using `key={Math.random()}` forces full component unmount and remount.

---

### Q129: How do you implement List Virtualization using `@tanstack/react-virtual` for 100,000 items?
**Answer:**
Rendering 10,000 DOM nodes creates 10,000 physical DOM tree elements, consuming 500MB+ RAM and lagging scroll performance.
**Virtualization** renders ONLY the 15-20 DOM nodes currently inside the visible viewport.

```javascript
import { useVirtualizer } from '@tanstack/react-virtual';
import { useRef } from 'react';

export function VirtualizedUserList({ users }) {
  const parentRef = useRef(null);

  const rowVirtualizer = useVirtualizer({
    count: users.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 60, // Estimated row height in px
    overscan: 5             // Render 5 buffer items outside viewport
  });

  return (
    <div ref={parentRef} style={{ height: '500px', overflow: 'auto', border: '1px solid #ccc' }}>
      <div style={{ height: `${rowVirtualizer.getTotalSize()}px`, width: '100%', position: 'relative' }}>
        {rowVirtualizer.getVirtualItems().map((virtualRow) => {
          const user = users[virtualRow.index];
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
              <strong>{user.name}</strong> - {user.email}
            </div>
          );
        })}
      </div>
    </div>
  );
}
```

---

### Q130: How do you offload heavy CPU computation (e.g. Data Parsing, Image Processing) to Web Workers in React?
**Answer:**
Heavy synchronous loops block the browser main thread and freeze user typing. Web Workers run in an isolated background thread.

```javascript
// worker.js (Dedicated Web Worker)
self.onmessage = (event) => {
  const { data } = event;
  // Heavy computation: Sort and filter 1,000,000 records
  const result = data.filter(item => item.value > 500).sort((a, b) => b.value - a.value);
  self.postMessage(result);
};

// React Component
import { useState, useEffect, useRef } from 'react';

export function HeavyDataView({ rawData }) {
  const [processedData, setProcessedData] = useState([]);
  const workerRef = useRef(null);

  useEffect(() => {
    workerRef.current = new Worker(new URL('./worker.js', import.meta.url));
    workerRef.current.onmessage = (e) => setProcessedData(e.data);

    return () => workerRef.current.terminate();
  }, []);

  const handleCompute = () => {
    workerRef.current.postMessage(rawData); // Offload to background thread
  };

  return (
    <div>
      <button onClick={handleCompute}>Process 1M Items</button>
      <p>Processed items: {processedData.length}</p>
    </div>
  );
}
```

---

### Q131: What are the most common Memory Leaks in React and how do you diagnose them?
**Answer:**

1. **Uncleared Event Listeners**: Adding `window.addEventListener` inside `useEffect` without a cleanup function.
2. **Dangling Timers / Intervals**: `setInterval` continuing to run and retain closures after component unmounts.
3. **Un-aborted Async Requests**: Updating state on unmounted components.
4. **Global Subscriptions / Event Emitters**: Subscribing to an external store without un-subscribing.
5. **Retained DOM Node References in Refs**: Keeping detached DOM references in global variables.

**Diagnosis in Chrome DevTools:**
1. Open DevTools -> "Memory" tab.
2. Record **Allocation Instrumentation on Timeline**.
3. Mount and unmount the component 10 times. Blue allocation spikes that are not garbage-collected represent leaking objects.

---

### Q132: What is the "Children as Props" / Element Lifting optimization and why does it prevent child re-renders without `memo`?
**Answer:**
When a parent component re-renders, components passed via the **`children` prop do NOT re-render** because their JSX element reference was created outside the parent's scope!

```javascript
// ❌ Slower: HeavyChild re-renders whenever count updates:
function ExpensiveParent() {
  const [count, setCount] = useState(0);
  return (
    <div onClick={() => setCount(c => c + 1)}>
      <p>Count: {count}</p>
      <HeavyChild />
    </div>
  );
}

// ✅ 100% Faster: HeavyChild NEVER re-renders on count updates (Lifted Element):
function OptimizedWrapper({ children }) {
  const [count, setCount] = useState(0);
  return (
    <div onClick={() => setCount(c => c + 1)}>
      <p>Count: {count}</p>
      {children} {/* React reuses same element reference! */}
    </div>
  );
}

// Usage:
<OptimizedWrapper>
  <HeavyChild />
</OptimizedWrapper>
```

---

### Q133: How does React Batching work in React 18 & 19 and how does it optimize network rendering?
**Answer:**
React groups multiple state updates into a single re-render pass, reducing layout recalcs and DOM repaints:

```javascript
// In React 18/19, automatic batching applies everywhere:
setTimeout(() => {
  setCount(c => c + 1);
  setFlag(f => !f);
  setUser('Alice');
  // Exactly 1 re-render occurs for all 3 state updates!
}, 1000);
```

---

### Q134: What is CSS `content-visibility: auto` and how does it supercharge large React page rendering?
**Answer:**
CSS `content-visibility: auto` tells the browser rendering engine to **skip layout and painting for off-screen DOM elements** until the user scrolls near them.
- Reduces Initial Render time by up to **70%**.
- Requires `contain-intrinsic-size: 0 500px` to prevent scrollbar jumping.

```css
.react-card-item {
  content-visibility: auto;
  contain-intrinsic-size: 0 350px;
}
```

---

### Q135: What is the Core Web Vitals (INP, LCP, CLS) metric impact of React components?
**Answer:**
- **INP (Interaction to Next Paint)**: Measures UI responsiveness to user clicks/taps. Degraded by heavy synchronous render loops. Solved by `useTransition` and time-slicing.
- **LCP (Largest Contentful Paint)**: Main hero image/text render speed. Optimized by SSR streaming and `<link rel="preload">`.
- **CLS (Cumulative Layout Shift)**: Unexpected visual jumps. Prevented by fixed aspect-ratio skeleton loaders in `<Suspense>`.

---

### Q136: How do you use the `<Profiler>` component in React for programmatic performance telemetry?
**Answer:**

```javascript
import { Profiler } from 'react';

function onRenderCallback(id, phase, actualDuration, baseDuration, startTime, commitTime) {
  if (actualDuration > 16) { // Flag render frames taking longer than 16ms (sub-60fps)
    console.warn(`[Performance Alert] ${id} (${phase}) took ${actualDuration.toFixed(2)}ms`);
    // Forward metric to Datadog / OpenTelemetry
  }
}

export function MonitoredDashboard() {
  return (
    <Profiler id="AnalyticsGrid" onRender={onRenderCallback}>
      <AnalyticsGrid />
    </Profiler>
  );
}
```

---

### Q137: What is the cost of Anonymous Functions in JSX and when does it actually matter?
**Answer:**
- In small, leaf components (e.g. `<button onClick={() => doSomething()} />`), creating an inline arrow function has negligible cost ($<0.001\text{ms}$).
- **When it matters**: When passed as a prop to a **memoized child or virtualized list row** (`<VirtualizedRow onClick={() => onSelect(id)} />`), the new function reference invalidates memoization, forcing 1,000 child rows to re-render!

---

### Q138: How do you optimize React SVGs: Inline SVG vs. Icon Fonts vs. Sprite Sheets?
**Answer:**
- **Inline JSX SVGs (Anti-pattern at scale)**: 500 inlined SVGs balloon JS bundle size by 300KB and increase React DOM node memory.
- **SVG Sprite Sheet (`<svg><use href="#icon-star" /></svg>`)**: 1 cached `.svg` file loaded once over HTTP; references rendered with zero JS bundle overhead.

---

### Q139: What is Tree-Shaking in modern bundlers (Rollup / Webpack / ESBuild) and why do Barrel Files (`index.js`) harm React performance?
**Answer:**
**Barrel File Problem**:
`import { Button } from '@/components'` imports an `index.js` file that re-exports 200 components.
- The bundler parses and includes code/dependencies for all 200 components (including heavy charts and date pickers) in the initial bundle!
- **Fix**: Direct path imports `import { Button } from '@/components/Button'` or configure `sideEffects: false` in `package.json`.

---

### Q140: How do you measure and prevent Memory Leaks with Detached DOM Nodes?
**Answer:**
A detached DOM node occurs when an element is removed from the DOM tree, but a JavaScript closure, Map, or Ref still holds a reference to it.
**Prevention**: Always set `ref.current = null` in cleanup handlers when caching DOM nodes.

---

### Q141: What is the Performance Cost of Deep Context Nesting (`<Theme><Auth><Locale><Cart>...`)?
**Answer:**
Deeply nested Context providers increase Fiber tree depth. While the initial provider traversal is cheap, any context state mutation traverses all children down the branch. Keep context providers flat or combine them into a single composition root.

---

### Q142: How do you optimize React State Updates during rapid drag-and-drop or scroll events?
**Answer:**
Never write to React state (`useState`) on 60fps mousemove or scroll events!
**Solution**: Direct DOM mutation via `useRef` + `requestAnimationFrame`, or CSS transform manipulation:

```javascript
function onMouseMove(e) {
  dragRef.current.style.transform = `translate3d(${e.clientX}px, ${e.clientY}px, 0)`;
}
```

---

### Q143: What is the difference between `React.memo` custom comparator and standard shallow comparison?
**Answer:**
`React.memo(Component, arePropsEqual)`:
- Returning `true` **skips** re-render.
- Returning `false` **forces** re-render.

```javascript
export const UserCard = React.memo(CardComponent, (prevProps, nextProps) => {
  return prevProps.user.id === nextProps.user.id && prevProps.user.updatedAt === nextProps.user.updatedAt;
});
```

---

### Q144: How does `useCallback` sometimes degrade performance instead of improving it?
**Answer:**
`useCallback(fn, deps)` requires allocating the inline function *and* allocating a dependencies array on every render, plus running array equality checks.
If the child component is **not** memoized, `useCallback` adds pure overhead with zero benefit!

---

### Q145: How do you build an FPS (Frames Per Second) Monitor in React?
**Answer:**

```javascript
import { useState, useEffect, useRef } from 'react';

export function FPSMonitor() {
  const [fps, setFps] = useState(60);
  const frameCount = useRef(0);
  const lastTime = useRef(performance.now());

  useEffect(() => {
    let animId;
    const loop = () => {
      frameCount.current++;
      const now = performance.now();
      if (now - lastTime.current >= 1000) {
        setFps(Math.round((frameCount.current * 1000) / (now - lastTime.current)));
        frameCount.current = 0;
        lastTime.current = now;
      }
      animId = requestAnimationFrame(loop);
    };
    animId = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(animId);
  }, []);

  return <div style={{ position: 'fixed', bottom: 10, right: 10 }}>FPS: {fps}</div>;
}
```

---

### Q146: What is the impact of Unchecked Dependency Arrays on CPU and Memory?
**Answer:**
Omitting dependencies or passing newly constructed objects on every tick causes effects to fire constantly in tight loops, locking up the browser thread and preventing garbage collection.

---

### Q147: How do you profile React Component Memory Footprints with Chrome DevTools Heap Snapshots?
**Answer:**
Take two Heap Snapshots (Before Action vs. After Action), filter by `FiberNode` or component constructor name, and inspect the **Retained Size** to detect un-freed instances.

---

### Q148: What is the difference between Synchronous Layout Thrashing and Batched DOM Reads/Writes?
**Answer:**
Interleaving DOM reads (`offsetHeight`) and DOM writes (`style.height = ...`) forces the browser to synchronously recalculate layout multiple times per frame.
Batch all reads first, then execute all writes inside `requestAnimationFrame`.

---

### Q149: How do you optimize React Animations using Framer Motion / Web Animations API (WAAPI)?
**Answer:**
Animate only GPU-accelerated CSS properties: **`transform` and `opacity`** (bypasses layout and paint stages). Avoid animating `width`, `height`, `top`, or `margin`.

---

### Q150: What is the React Compiler's impact on Runtime Profiling metrics?
**Answer:**
With the React Compiler active, component self-duration drops by 40-70% across medium-to-large apps, and ranked charts show near-zero time spent in unchanged leaf components.
