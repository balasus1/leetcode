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

---

### Q52: What is the Stale Closure problem in `useEffect` and how is it resolved in modern React?
**Answer:**

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

// ✅ Fix: Functional State Updater
useEffect(() => {
  const timer = setInterval(() => {
    setCount(c => c + 1); // Always gets freshest state
  }, 1000);
  return () => clearInterval(timer);
}, []);
```

---

### Q53: What is `useId` and why is it required for Accessible (a11y) form controls in SSR/Streaming?
**Answer:**

```javascript
import { useId } from 'react';

export function AccessibleInputField({ label }) {
  const id = useId(); // Deterministic unique ID stable across SSR & Client (e.g. ":r1:")

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

```javascript
import { useImperativeHandle, useRef } from 'react';

export function CustomVideoPlayer({ ref }) {
  const videoRef = useRef(null);

  useImperativeHandle(ref, () => ({
    playVideo: () => videoRef.current.play(),
    pauseVideo: () => videoRef.current.pause(),
    getCurrentTime: () => videoRef.current.currentTime
  }));

  return <video ref={videoRef} src="/media/clip.mp4" />;
}
```

---

### Q55: What is `useInsertionEffect` and why is it used exclusively by CSS-in-JS libraries (Emotion / Styled-Components)?
**Answer:**

```javascript
import { useInsertionEffect } from 'react';

// CSS-in-JS library runtime style injector:
export function useDynamicCSS(className, cssRules) {
  useInsertionEffect(() => {
    // Injects <style> tags BEFORE DOM mutations & layout effects
    const styleTag = document.createElement('style');
    styleTag.textContent = `.${className} { ${cssRules} }`;
    document.head.appendChild(styleTag);

    return () => document.head.removeChild(styleTag);
  }, [className, cssRules]);
}
```

---

### Q56: How do you implement a production-grade `useDebounce` and `useThrottle` custom hook in TypeScript?
**Answer:**

```typescript
import { useState, useEffect, useRef } from 'react';

export function useDebounce<T>(value: T, delayMs: number = 300): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const handler = setTimeout(() => setDebouncedValue(value), delayMs);
    return () => clearTimeout(handler);
  }, [value, delayMs]);

  return debouncedValue;
}
```

---

### Q57: How do you build a `useAsync` / `useFetch` hook with Race Condition protection and AbortController?
**Answer:**

```javascript
import { useState, useEffect } from 'react';

export function useFetchData(url) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
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
        if (err.name !== 'AbortError') setError(err.message);
      } finally {
        setLoading(false);
      }
    }

    fetchData();
    return () => controller.abort(); // Cancel on unmount/re-fetch!
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
    return () => observer.disconnect();
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
  const [matches, setMatches] = useState(() => 
    typeof window !== 'undefined' ? window.matchMedia(query).matches : false
  );

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
  const savedHandler = useRef(handler);

  useEffect(() => {
    savedHandler.current = handler;
  }, [handler]);

  useEffect(() => {
    const targetElement = element?.current ?? element;
    if (!targetElement?.addEventListener) return;

    const eventListener = (event) => savedHandler.current(event);
    targetElement.addEventListener(eventName, eventListener);

    return () => targetElement.removeEventListener(eventName, eventListener);
  }, [eventName, element]);
}
```

---

### Q61: What is the difference between `useCallback(fn, deps)` and `useRef(fn)` for event handler callbacks?
**Answer:**

```javascript
import { useRef, useEffect, useCallback } from 'react';

// useEventCallback: Retains 100% stable reference with freshest closure state:
export function useEventCallback(fn) {
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
    const valueToStore = value instanceof Function ? value(storedValue) : value;
    setStoredValue(valueToStore);
    window.localStorage.setItem(key, JSON.stringify(valueToStore));
  };

  useEffect(() => {
    const handleStorageChange = (e) => {
      if (e.key === key && e.newValue) setStoredValue(JSON.parse(e.newValue));
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
    ref.current = value;
  }, [value]);
  return ref.current;
}
```

---

### Q64: What is `useReducer` and when should it be preferred over `useState`?
**Answer:**

```javascript
import { useReducer } from 'react';

function formReducer(state, action) {
  switch (action.type) {
    case 'SET_FIELD':
      return { ...state, [action.field]: action.value };
    case 'SET_ERROR':
      return { ...state, errors: { ...state.errors, [action.field]: action.error } };
    case 'RESET':
      return { values: {}, errors: {} };
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
      if (!ref.current || ref.current.contains(event.target)) return;
      callback(event);
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

```javascript
// StrictMode mounts -> unmounts -> mounts in development:
function ChatConnection({ roomId }) {
  useEffect(() => {
    const socket = connectSocket(roomId);
    
    // 🛡️ Proper cleanup prevents duplicate connections in StrictMode:
    return () => {
      socket.disconnect();
    };
  }, [roomId]);
}
```

---

### Q67: How do you implement a `useVirtualList` hook for rendering 100,000 items at 60 FPS?
**Answer:**

```javascript
export function useVirtualList({ itemCount, itemHeight, containerHeight, scrollTop }) {
  const totalHeight = itemCount * itemHeight;
  const startIndex = Math.max(0, Math.floor(scrollTop / itemHeight) - 2);
  const endIndex = Math.min(itemCount - 1, Math.floor((scrollTop + containerHeight) / itemHeight) + 2);

  const visibleItems = [];
  for (let i = startIndex; i <= endIndex; i++) {
    visibleItems.push({ index: i, offsetTop: i * itemHeight });
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

```javascript
// 1. Lazy State Initialization (Executed ONCE on Mount only):
const [state, setState] = useState(() => {
  return parseLargeDataSchema(initialBlob); // Runs only once!
});

// 2. useMemo (Recomputes whenever dependencies change):
const filteredList = useMemo(() => {
  return items.filter(i => i.active);
}, [items]);
```

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
  return useSyncExternalStore(subscribe, () => navigator.onLine, () => true);
}
```

---

### Q72: How do you build a `useInterval` hook that handles dynamic delays and pause states?
**Answer:**

```javascript
import { useEffect, useRef } from 'react';

export function useInterval(callback, delayMs) {
  const savedCallback = useRef(callback);
  useEffect(() => { savedCallback.current = callback; }, [callback]);

  useEffect(() => {
    if (delayMs === null || delayMs === undefined) return;
    const id = setInterval(() => savedCallback.current(), delayMs);
    return () => clearInterval(id);
  }, [delayMs]);
}
```

---

### Q73: What is the risk of Object / Array dependencies in `useEffect` and how is it fixed?
**Answer:**

```javascript
// ❌ ANTI-PATTERN: [{ id }] creates new reference on every render -> Infinite Effect Loop!
useEffect(() => {
  fetchData(options);
}, [{ id: 10 }]); 

// ✅ FIX: Destructure primitive values in dependency array:
const { id } = options;
useEffect(() => {
  fetchData(id);
}, [id]);
```

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
          changesObj[key] = { from: previousProps.current[key], to: props[key] };
        }
      });
      if (Object.keys(changesObj).length) console.log('[WhyDidYouUpdate]', name, changesObj);
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
  const [coords, setCoords] = useState({ latitude: null, longitude: null });

  useEffect(() => {
    if (!navigator.geolocation) return;
    const watchId = navigator.geolocation.watchPosition(
      (pos) => setCoords({ latitude: pos.coords.latitude, longitude: pos.coords.longitude }),
      console.error,
      options
    );
    return () => navigator.geolocation.clearWatch(watchId);
  }, []);

  return coords;
}
```
