# Part 3: Hooks Deep Dive, Internals & Advanced Custom Hooks (Q51 - Q75)

---

### Q51: How does `useState` work under the hood and why does updater syntax `setCount(c => c + 1)` prevent stale state?
**Answer:**
`useState` is backed by a queue of update actions on the fiber node.

```javascript
// ❌ Stale Closure Bug:
const handleClick = () => {
  setCount(count + 1); // count is captured from current render (e.g. 0)
  setCount(count + 1); // count is still 0!
  // Final count will be 1, NOT 2!
};

// ✅ Functional Updater (Safe):
const handleClickSafe = () => {
  setCount(prev => prev + 1); // Queued: (0) => 1
  setCount(prev => prev + 1); // Queued: (1) => 2
  // Final count will be 2!
};
```
When using functional updaters, React feeds the output of each reducer function into the next pending update in the queue.

---

### Q52: What is the Stale Closure problem in `useEffect` and how is it resolved in modern React?
**Answer:**
A closure captures variables from the render cycle in which it was created. If `useEffect` has an incomplete dependency array (`[]`), it retains stale values indefinitely.

```javascript
// ❌ Stale Closure Bug:
function Counter() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      console.log(count); // ALWAYS logs 0 because 'count' was captured at mount!
    }, 1000);
    return () => clearInterval(timer);
  }, []); // Missing count in dependencies!
}

// ✅ Fix 1: Functional State Updater
useEffect(() => {
  const timer = setInterval(() => {
    setCount(c => c + 1); // Always gets freshest state
  }, 1000);
  return () => clearInterval(timer);
}, []);

// ✅ Fix 2: useRef for mutable latest value
const countRef = useRef(count);
countRef.current = count;
```

---

### Q53: What is `useId` and why is it required for Accessible (a11y) form controls in SSR/Streaming?
**Answer:**
Using `Math.random()` or global counters generates different IDs on the server vs. the client, causing **hydration mismatch errors**.
`useId` generates a **stable, deterministic, unique ID** based on the component's position in the React Fiber tree hierarchy.

```javascript
import { useId } from 'react';

export function AccessibleInputField({ label }) {
  const id = useId(); // Guaranteed identical on Server and Client (e.g. ":r1:")

  return (
    <div>
      <label htmlFor={id}>{label}</label>
      <input id={id} type="text" aria-describedby={`${id}-hint`} />
      <span id={`${id}-hint`}>Enter your legal full name</span>
    </div>
  );
}
```

---

### Q54: What is `useImperativeHandle` and how do you customize the exposed ref API of a child component?
**Answer:**
`useImperativeHandle` restricts and customizes the methods exposed to a parent component via `ref`, preventing the parent from accessing raw internal DOM nodes directly.

```javascript
import { useImperativeHandle, useRef } from 'react';

export function VideoPlayer({ ref }) {
  const internalVideoRef = useRef(null);

  // Expose ONLY play, pause, and reset methods to parent ref
  useImperativeHandle(ref, () => ({
    play() {
      internalVideoRef.current.play();
    },
    pause() {
      internalVideoRef.current.pause();
    },
    seekTo(seconds) {
      internalVideoRef.current.currentTime = seconds;
    }
  }));

  return <video ref={internalVideoRef} src="/video.mp4" />;
}

// Parent Usage:
function Controller() {
  const playerRef = useRef(null);
  return (
    <div>
      <VideoPlayer ref={playerRef} />
      <button onClick={() => playerRef.current.play()}>Play Video</button>
      <button onClick={() => playerRef.current.seekTo(0)}>Restart</button>
    </div>
  );
}
```

---

### Q55: What is `useInsertionEffect` and why is it used exclusively by CSS-in-JS libraries (Emotion / Styled-Components)?
**Answer:**
- `useInsertionEffect` runs **synchronously BEFORE all DOM mutations and before `useLayoutEffect`**.
- It allows CSS-in-JS libraries to inject dynamic `<style>` tags into the `<head>` *before* React calculates layout or measures DOM elements in `useLayoutEffect`, avoiding layout recalculation thrashing (style invalidation).
- **Rule**: Application code should never use `useInsertionEffect`; use `useEffect` or `useLayoutEffect`.

---

### Q56: How do you implement a production-grade `useDebounce` and `useThrottle` custom hook in TypeScript?
**Answer:**

```typescript
import { useState, useEffect, useRef } from 'react';

// 1. useDebounce (Value Debounce)
export function useDebounce<T>(value: T, delayMs: number = 300): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const handler = setTimeout(() => setDebouncedValue(value), delayMs);
    return () => clearTimeout(handler);
  }, [value, delayMs]);

  return debouncedValue;
}

// 2. useThrottle (Function Throttle with Leading/Trailing support)
export function useThrottle<T extends (...args: any[]) => void>(fn: T, limitMs: number = 200): T {
  const lastRan = useRef<number>(Date.now());
  const handlerRef = useRef<NodeJS.Timeout | null>(null);

  return ((...args: Parameters<T>) => {
    const now = Date.now();
    if (now - lastRan.current >= limitMs) {
      fn(...args);
      lastRan.current = now;
    } else {
      if (handlerRef.current) clearTimeout(handlerRef.current);
      handlerRef.current = setTimeout(() => {
        fn(...args);
        lastRan.current = Date.now();
      }, limitMs - (now - lastRan.current));
    }
  }) as T;
}
```

---

### Q57: How do you build a `useAsync` / `useFetch` hook with Race Condition protection and AbortController?
**Answer:**
When a user switches search queries rapidly ("A" $\rightarrow$ "AB" $\rightarrow$ "ABC"), response "A" might return *after* "ABC", overwriting fresh data with stale results.

```javascript
import { useState, useEffect } from 'react';

export function useFetchData(url) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Native AbortController cancels in-flight network request on prop change or unmount
    const controller = new AbortController();
    setLoading(true);

    async function fetchData() {
      try {
        const response = await fetch(url, { signal: controller.signal });
        if (!response.ok) throw new Error(`HTTP Error: ${response.status}`);
        const result = await response.json();
        setData(result);
        setError(null);
      } catch (err) {
        if (err.name !== 'AbortError') {
          setError(err.message);
        }
      } finally {
        setLoading(false);
      }
    }

    fetchData();

    // 🛡️ Cleanup: Aborts in-flight request when url changes or component unmounts
    return () => {
      controller.abort();
    };
  }, [url]);

  return { data, error, loading };
}
```

---

### Q58: How do you implement a `useIntersectionObserver` hook for infinite scrolling and lazy image loading?
**Answer:**

```javascript
import { useState, useEffect, useRef } from 'react';

export function useIntersectionObserver(options = {}) {
  const [isIntersecting, setIsIntersecting] = useState(false);
  const targetRef = useRef(null);

  useEffect(() => {
    const element = targetRef.current;
    if (!element) return;

    const observer = new IntersectionObserver(([entry]) => {
      setIsIntersecting(entry.isIntersecting);
    }, options);

    observer.observe(element);

    return () => {
      observer.disconnect();
    };
  }, [options.root, options.rootMargin, options.threshold]);

  return [targetRef, isIntersecting];
}
```

---

### Q59: How do you implement `useMediaQuery` hook for responsive JavaScript rendering?
**Answer:**

```javascript
import { useState, useEffect } from 'react';

export function useMediaQuery(query) {
  const [matches, setMatches] = useState(() => {
    if (typeof window !== 'undefined') {
      return window.matchMedia(query).matches;
    }
    return false;
  });

  useEffect(() => {
    const mediaQueryList = window.matchMedia(query);
    const listener = (event) => setMatches(event.matches);

    mediaQueryList.addEventListener('change', listener);
    setMatches(mediaQueryList.matches);

    return () => mediaQueryList.removeEventListener('change', listener);
  }, [query]);

  return matches;
}
```

---

### Q60: How do you build a `useEventListener` hook that safely handles DOM element target changes without re-subscribing?
**Answer:**

```javascript
import { useEffect, useRef } from 'react';

export function useEventListener(eventName, handler, element = window) {
  // Store latest handler in a ref to avoid re-binding event listener on handler changes
  const savedHandler = useRef(handler);

  useEffect(() => {
    savedHandler.current = handler;
  }, [handler]);

  useEffect(() => {
    const targetElement = element?.current ?? element;
    if (!targetElement?.addEventListener) return;

    const eventListener = (event) => savedHandler.current(event);
    targetElement.addEventListener(eventName, eventListener);

    return () => {
      targetElement.removeEventListener(eventName, eventListener);
    };
  }, [eventName, element]);
}
```

---

### Q61: What is the difference between `useCallback(fn, deps)` and `useRef(fn)` for event handler callbacks?
**Answer:**
- `useCallback` returns a new function instance whenever dependencies change, triggering child re-renders if passed as props.
- `useRef` retains a single stable function reference across all renders while always executing the freshest state:

```javascript
function useEventCallback(fn) {
  const ref = useRef(fn);
  useEffect(() => { ref.current = fn; });
  return useCallback((...args) => ref.current(...args), []);
}
```

---

### Q62: How do you build a `useLocalStorage` hook with multi-tab cross-synchronization?
**Answer:**

```javascript
import { useState, useEffect } from 'react';

export function useLocalStorage(key, initialValue) {
  const [storedValue, setStoredValue] = useState(() => {
    try {
      const item = window.localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch {
      return initialValue;
    }
  });

  const setValue = (value) => {
    try {
      const valueToStore = value instanceof Function ? value(storedValue) : value;
      setStoredValue(valueToStore);
      window.localStorage.setItem(key, JSON.stringify(valueToStore));
    } catch (err) {
      console.error(err);
    }
  };

  // Synchronize across browser tabs
  useEffect(() => {
    const handleStorageChange = (e) => {
      if (e.key === key && e.newValue) {
        setStoredValue(JSON.parse(e.newValue));
      }
    };
    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [key]);

  return [storedValue, setValue];
}
```

---

### Q63: How do you implement a `usePrevious` hook to track previous state/props values?
**Answer:**

```javascript
import { useRef, useEffect } from 'react';

export function usePrevious(value) {
  const ref = useRef();
  useEffect(() => {
    ref.current = value; // Updated AFTER render cycle commits
  }, [value]);
  return ref.current; // Returns value from previous render cycle
}
```

---

### Q64: What is `useReducer` and when should it be preferred over `useState`?
**Answer:**
`useReducer` is preferred when:
1. State transitions involve **complex, interdependent sub-values** (`state.step === 2 && state.isValid`).
2. The next state depends deeply on previous state logic.
3. You want to pass `dispatch` down deep component trees (stable reference, zero prop-drilling re-renders).

```javascript
import { useReducer } from 'react';

function orderReducer(state, action) {
  switch (action.type) {
    case 'ADD_ITEM':
      return { ...state, items: [...state.items, action.item], total: state.total + action.item.price };
    case 'APPLY_DISCOUNT':
      return { ...state, total: state.total * (1 - action.rate) };
    case 'RESET':
      return { items: [], total: 0 };
    default:
      return state;
  }
}
```

---

### Q65: How do you build a `useClickAway` / `useOnClickOutside` hook for Modals and Dropdowns?
**Answer:**

```javascript
import { useEffect, useRef } from 'react';

export function useClickOutside(callback) {
  const ref = useRef(null);

  useEffect(() => {
    const listener = (event) => {
      if (!ref.current || ref.current.contains(event.target)) {
        return; // Click was inside target element
      }
      callback(event); // Click was outside!
    };

    document.addEventListener('mousedown', listener);
    document.addEventListener('touchstart', listener);

    return () => {
      document.removeEventListener('mousedown', listener);
      document.removeEventListener('touchstart', listener);
    };
  }, [callback]);

  return ref;
}
```

---

### Q66: What is the StrictMode double-invoking of effects in development and how do you handle it properly?
**Answer:**
In development, `React.StrictMode` deliberately mounts, unmounts, and re-mounts every component:
`Mount -> Unmount -> Mount`.
**Purpose:** Exposes missing cleanup functions in `useEffect` (e.g. forgotten event listeners, dangling WebSocket connections, duplicate subscriptions).

---

### Q67: How do you implement a `useVirtualList` hook for rendering 100,000 items at 60 FPS?
**Answer:**
Calculates the slice of visible rows based on container scroll position and item height.

```javascript
import { useState, useEffect } from 'react';

export function useVirtualList({ itemCount, itemHeight, containerHeight, scrollTop }) {
  const totalHeight = itemCount * itemHeight;
  const startIndex = Math.max(0, Math.floor(scrollTop / itemHeight) - 2); // Buffer 2 rows
  const endIndex = Math.min(itemCount - 1, Math.floor((scrollTop + containerHeight) / itemHeight) + 2);

  const visibleItems = [];
  for (let i = startIndex; i <= endIndex; i++) {
    visibleItems.push({
      index: i,
      offsetTop: i * itemHeight
    });
  }

  return { visibleItems, totalHeight };
}
```

---

### Q68: How do you build a `useCopyToClipboard` hook with timeout reset?
**Answer:**

```javascript
import { useState } from 'react';

export function useCopyToClipboard(resetDelayMs = 2000) {
  const [copied, setCopied] = useState(false);

  const copy = async (text) => {
    if (!navigator?.clipboard) return false;
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), resetDelayMs);
      return true;
    } catch {
      setCopied(false);
      return false;
    }
  };

  return { copied, copy };
}
```

---

### Q69: What is the difference between `useMemo` computation and Lazy State Initialization `useState(() => expensiveComputation())`?
**Answer:**
- **`useState(() => compute())`**: Runs `compute()` **exactly once** when component mounts. Value is stored in state permanently.
- **`useMemo(() => compute(), [deps])`**: Re-computes whenever `deps` change. React reserves the right to "forget" memoized values under memory pressure.

---

### Q70: How do you implement `useLockBodyScroll` for Modal dialogs?
**Answer:**

```javascript
import { useLayoutEffect } from 'react';

export function useLockBodyScroll(isLocked = true) {
  useLayoutEffect(() => {
    if (!isLocked) return;
    const originalStyle = window.getComputedStyle(document.body).overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = originalStyle;
    };
  }, [isLocked]);
}
```

---

### Q71: How do you build a `useOnlineStatus` hook with `useSyncExternalStore`?
**Answer:**

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

export function useOnlineStatus() {
  return useSyncExternalStore(
    subscribe,
    () => navigator.onLine,       // Client snapshot
    () => true                   // Server snapshot (default online)
  );
}
```

---

### Q72: How do you build a `useInterval` hook that handles dynamic delays and pause states?
**Answer:**

```javascript
import { useEffect, useRef } from 'react';

export function useInterval(callback, delayMs) {
  const savedCallback = useRef(callback);

  useEffect(() => {
    savedCallback.current = callback;
  }, [callback]);

  useEffect(() => {
    if (delayMs === null || delayMs === undefined) return;

    const tick = () => savedCallback.current();
    const id = setInterval(tick, delayMs);
    return () => clearInterval(id);
  }, [delayMs]);
}
```

---

### Q73: What is the risk of Object / Array dependencies in `useEffect` and how is it fixed?
**Answer:**
Passing an inline object `useEffect(..., [{ id: 1 }])` causes the effect to run on **every single render** because `{ id: 1 } !== { id: 1 }` (referential inequality).
**Fix**: Destructure primitives into dependencies `[user.id]` or use the React Compiler.

---

### Q74: How do you build a `useWhyDidYouUpdate` debug hook to log what props triggered a re-render?
**Answer:**

```javascript
import { useEffect, useRef } from 'react';

export function useWhyDidYouUpdate(name, props) {
  const previousProps = useRef();

  useEffect(() => {
    if (previousProps.current) {
      const allKeys = Object.keys({ ...previousProps.current, ...props });
      const changesObj = {};
      allKeys.forEach((key) => {
        if (previousProps.current[key] !== props[key]) {
          changesObj[key] = {
            from: previousProps.current[key],
            to: props[key]
          };
        }
      });

      if (Object.keys(changesObj).length) {
        console.log('[why-did-you-update]', name, changesObj);
      }
    }
    previousProps.current = props;
  });
}
```

---

### Q75: How do you build a `useGeolocation` hook with real-time watch capabilities?
**Answer:**

```javascript
import { useState, useEffect } from 'react';

export function useGeolocation(options = {}) {
  const [state, setState] = useState({
    loading: true,
    latitude: null,
    longitude: null,
    error: null
  });

  useEffect(() => {
    if (!navigator.geolocation) {
      setState(s => ({ ...s, loading: false, error: 'Geolocation not supported' }));
      return;
    }

    const watchId = navigator.geolocation.watchPosition(
      (pos) => {
        setState({
          loading: false,
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
          error: null
        });
      },
      (err) => setState(s => ({ ...s, loading: false, error: err.message })),
      options
    );

    return () => navigator.geolocation.clearWatch(watchId);
  }, []);

  return state;
}
```
