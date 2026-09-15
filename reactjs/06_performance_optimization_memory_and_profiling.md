# Part 6: Performance Optimization, Profiling & Memory (Q126 - Q150)

---

### Q126: How do you read and interpret a React DevTools Profiler Flamegraph and Ranked Chart?
**Answer:**

```javascript
// Programmatic Profiling Callback:
function onProfileRender(id, phase, actualDuration, baseDuration) {
  console.log(`[Profiler] ${id} - ${phase}: ${actualDuration.toFixed(2)}ms (Base: ${baseDuration.toFixed(2)}ms)`);
}
```

---

### Q127: What is the difference between Component Render Time (Self Duration) and Subtree Duration (Base Duration)?
**Answer:**

```javascript
// Profiler duration breakdown:
// Self Duration: Pure computation inside this component alone
// Base Duration: Estimated worst-case time to render all children without memoization
<Profiler id="UserFeed" onRender={(id, phase, actualDuration, baseDuration) => {
  if (actualDuration > 16) console.warn('Frame dropped: took > 16ms');
}}>
  <UserFeed />
</Profiler>
```

---

### Q128: What are the primary causes of Unnecessary Re-Renders in React applications?
**Answer:**

```javascript
// ❌ Re-render Trap: Inline object reference constructed every render:
<UserProfile config={{ theme: 'dark', role: 'admin' }} />

// ✅ Fix: Move static objects outside component or use React Compiler:
const STATIC_CONFIG = { theme: 'dark', role: 'admin' };
<UserProfile config={STATIC_CONFIG} />
```

---

### Q129: How do you implement List Virtualization using `@tanstack/react-virtual` for 100,000 items?
**Answer:**

```javascript
import { useVirtualizer } from '@tanstack/react-virtual';
import { useRef } from 'react';

export function VirtualizedList({ items }) {
  const parentRef = useRef(null);

  const virtualizer = useVirtualizer({
    count: items.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 50,
    overscan: 5
  });

  return (
    <div ref={parentRef} style={{ height: '400px', overflow: 'auto' }}>
      <div style={{ height: `${virtualizer.getTotalSize()}px`, position: 'relative' }}>
        {virtualizer.getVirtualItems().map(virtualRow => (
          <div key={virtualRow.index} style={{ position: 'absolute', top: 0, transform: `translateY(${virtualRow.start}px)` }}>
            {items[virtualRow.index].name}
          </div>
        ))}
      </div>
    </div>
  );
}
```

---

### Q130: How do you offload heavy CPU computation (e.g. Data Parsing, Image Processing) to Web Workers in React?
**Answer:**

```javascript
// Web Worker Bridge:
const worker = new Worker(new URL('./filterWorker.js', import.meta.url));
worker.postMessage({ dataset: raw100kRows, query: 'searchTerm' });
worker.onmessage = (e) => setFilteredResults(e.data);
```

---

### Q131: What are the most common Memory Leaks in React and how do you diagnose them?
**Answer:**

```javascript
// ❌ Memory Leak: Uncleared global listener retaining component scope:
useEffect(() => {
  window.addEventListener('resize', onResize);
  // Missing return () => window.removeEventListener('resize', onResize);
}, []);
```

---

### Q132: What is the "Children as Props" / Element Lifting optimization and why does it prevent child re-renders without `memo`?
**Answer:**

```javascript
// ✅ HeavyChild NEVER re-renders on count state updates:
function CounterWrapper({ children }) {
  const [count, setCount] = useState(0);
  return (
    <div onClick={() => setCount(c => c + 1)}>
      <span>{count}</span>
      {children} {/* Element reference is identical! */}
    </div>
  );
}
```

---

### Q133: How does React Batching work in React 18 & 19 and how does it optimize network rendering?
**Answer:**

```javascript
// Automatic Batching across all async callbacks:
setTimeout(() => {
  setCount(1);
  setUser('Alice');
  setTheme('dark');
  // Exactly 1 re-render pass!
}, 100);
```

---

### Q134: What is CSS `content-visibility: auto` and how does it supercharge large React page rendering?
**Answer:**

```css
/* Skips layout & painting for offscreen React DOM nodes: */
.react-virtual-card {
  content-visibility: auto;
  contain-intrinsic-size: 0 400px;
}
```

---

### Q135: What is the Core Web Vitals (INP, LCP, CLS) metric impact of React components?
**Answer:**

```javascript
// Optimize INP with startTransition:
function onSearchTyping(text) {
  setImmediateInput(text); // Fast SyncLane
  startTransition(() => {
    setFilteredResults(filterDataset(text)); // Low priority TransitionLane
  });
}
```

---

### Q136: How do you use the `<Profiler>` component in React for programmatic performance telemetry?
**Answer:**

```javascript
<Profiler id="CheckoutFlow" onRender={(id, phase, duration) => {
  datadogRum.addDurationMetric(id, duration);
}}>
  <CheckoutForm />
</Profiler>
```

---

### Q137: What is the cost of Anonymous Functions in JSX and when does it actually matter?
**Answer:**

```javascript
// When passing to memoized children in a list:
// ❌ Inlined arrow creates new reference on every tick:
<MemoizedListItem onClick={() => handleSelect(item.id)} />

// ✅ Pass ID and stable handler:
<MemoizedListItem onSelect={handleSelect} itemId={item.id} />
```

---

### Q138: How do you optimize React SVGs: Inline SVG vs. Icon Fonts vs. Sprite Sheets?
**Answer:**

```html
<!-- SVG Sprite Sheet (0KB JS bundle overhead): -->
<svg className="icon-star"><use href="/icons.svg#star" /></svg>
```

---

### Q139: What is Tree-Shaking in modern bundlers (Rollup / Webpack / ESBuild) and why do Barrel Files (`index.js`) harm React performance?
**Answer:**

```javascript
// ❌ Heavy Barrel File Import (Pulls all 100 icons into bundle):
// import { StarIcon } from '@/icons';

// ✅ Direct Path Import (Pulls ONLY StarIcon):
import { StarIcon } from '@/icons/StarIcon';
```

---

### Q140: How do you measure and prevent Memory Leaks with Detached DOM Nodes?
**Answer:**

```javascript
// Ensure refs are cleaned up on unmount:
useEffect(() => {
  return () => {
    cachedDOMElementRef.current = null; // Release pointer for V8 Garbage Collector!
  };
}, []);
```

---

### Q141: What is the Performance Cost of Deep Context Nesting (`<Theme><Auth><Locale><Cart>...`)?
**Answer:**

```javascript
// Combine multiple flat contexts into a single composition root:
export function AppProviders({ children }) {
  return (
    <ThemeProvider>
      <AuthProvider>
        <CartProvider>{children}</CartProvider>
      </AuthProvider>
    </ThemeProvider>
  );
}
```

---

### Q142: How do you optimize React State Updates during rapid drag-and-drop or scroll events?
**Answer:**

```javascript
// Direct DOM transformation for 60fps animations:
function onDrag(e) {
  elementRef.current.style.transform = `translate3d(${e.clientX}px, ${e.clientY}px, 0)`;
}
```

---

### Q143: What is the difference between `React.memo` custom comparator and standard shallow comparison?
**Answer:**

```javascript
export const MemoUserRow = React.memo(UserRow, (prev, next) => {
  return prev.user.id === next.user.id && prev.user.version === next.user.version;
});
```

---

### Q144: How does `useCallback` sometimes degrade performance instead of improving it?
**Answer:**

```javascript
// ❌ Wasteful: Child is not memoized, so useCallback adds pure array/closure allocation overhead:
const handleClick = useCallback(() => console.log('clicked'), []);
<StandardUnmemoizedButton onClick={handleClick} />
```

---

### Q145: How do you build an FPS (Frames Per Second) Monitor in React?
**Answer:**

```javascript
export function useFPSMonitor() {
  const [fps, setFps] = useState(60);
  useEffect(() => {
    let frame = 0, last = performance.now(), anim;
    const loop = () => {
      frame++;
      const now = performance.now();
      if (now - last >= 1000) {
        setFps(Math.round((frame * 1000) / (now - last)));
        frame = 0;
        last = now;
      }
      anim = requestAnimationFrame(loop);
    };
    anim = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(anim);
  }, []);
  return fps;
}
```

---

### Q146: What is the impact of Unchecked Dependency Arrays on CPU and Memory?
**Answer:**

```javascript
// ❌ Missing empty array causes infinite fetch on every single re-render:
useEffect(() => {
  fetchData(); // Runs on EVERY render cycle!
}); // Missing [] !
```

---

### Q147: How do you profile React Component Memory Footprints with Chrome DevTools Heap Snapshots?
**Answer:**

```javascript
// Identify retained Fiber instances in Chrome Memory Profiler by constructor:
// Filter by 'FiberNode' and inspect 'Retained Size'
```

---

### Q148: What is the difference between Synchronous Layout Thrashing and Batched DOM Reads/Writes?
**Answer:**

```javascript
// ❌ Layout Thrashing (Interleaving reads and writes):
const w1 = el1.offsetWidth; // Read
el1.style.width = w1 + 'px'; // Write (Invalidates layout!)
const w2 = el2.offsetWidth; // Read (Forces layout recalculation!)

// ✅ Batched (All reads first, then all writes):
const w1 = el1.offsetWidth;
const w2 = el2.offsetWidth;
el1.style.width = w1 + 'px';
el2.style.width = w2 + 'px';
```

---

### Q149: How do you optimize React Animations using Framer Motion / Web Animations API (WAAPI)?
**Answer:**

```javascript
// GPU Accelerated CSS Transforms (Zero layout recalcs):
<motion.div animate={{ x: 100, opacity: 1 }} transition={{ duration: 0.3 }} />
```

---

### Q150: What is the React Compiler's impact on Runtime Profiling metrics?
**Answer:**

```javascript
// React Compiler inlines auto-memoization cache check:
function CompiledComponent(props) {
  const $ = _c(2); // Memo cache slot
  let formatted;
  if ($[0] !== props.text) {
    formatted = heavyFormat(props.text);
    $[0] = props.text;
    $[1] = formatted;
  } else {
    formatted = $[1];
  }
  return <div>{formatted}</div>;
}
```
