# 🚀 React 19 Interactive Interview & Production Showcase

A cutting-edge interactive React 19 + TypeScript showcase application designed specifically to pass Senior / Principal / Staff Remote Frontend & Fullstack coding interviews.

---

## 📦 What's Inside the Showcase

### 1. 🔢 Advanced Time-Travel & Async Counter Hub (`CounterApp.tsx`)
- **Undo / Redo Ring Buffer**: Custom `useHistoryState<T>` hook with $O(1)$ pointer slicing without state mutations.
- **Race Condition Prevention**: Active in-flight cancellation using `AbortController` and signals to ensure late-arriving promises never overwrite recent state.
- **React 19 Actions & Optimistic UI**: `useOptimistic` instant perceived latency with automatic server rollback on network faults.
- **Bounded Auto-Increment Engine**: Configurable step, min, max, and timer interval with proper functional closure updaters `setCount(prev => prev + step)`.
- **Render Telemetry**: Live re-render tracker counter badge.

### 2. 🌊 Min-Max & Trapping Rain Water Visualizer (`WaterApp.tsx`)
- **Container With Most Water (LeetCode 11)**:
  - Step-by-step interactive simulation of two-pointer greedy optimization ($O(N)$ time, $O(1)$ space).
  - Real-time calculations of $W = R - L$, $H = \min(H[L], H[R])$, and Current Area vs Global Max Area.
  - Live pointers ($L$, $R$) with directional animations.
- **Trapping Rain Water (LeetCode 42)**:
  - Two-pointer elevation trapping simulation ($O(N)$ time, $O(1)$ space).
  - Visual blue water unit grids stacked atop solid elevation pillar bars.
- **Playback Controls**: Auto Play, Step Next, Step Prev, Reset, Speed Slider (0.5x to 5.0x), Random array generator, and custom array inputs.

### 3. 🌐 Enterprise Paginated External API Explorer (`PaginatedListApp.tsx`)
- **Live Remote API Data**: Fetches from public REST APIs with offline fallback.
- **300ms Keystroke Debouncing**: Prevents server request storms.
- **In-Memory SWR Cache**: Fast Map-based cache with TTL and in-flight promise deduplication.
- **Request Cancellation**: Cancels previous fetch requests immediately when users type or switch pages.
- **Hover Pre-fetch**: Pre-fetches Page $N+1$ when hovering the Next button for zero-latency page transitions.
- **Dual Pagination Modes**: Classic URL-friendly pagination vs Infinite Scroll with `IntersectionObserver`.

### 4. ⚡ React 19 Actions & Concurrency Playground (`React19Playground.tsx`)
- **`useActionState` + `useOptimistic`**: Task submission with immediate optimistic UI rendering and automatic error rollback.
- **`useTransition` Benchmark**: Concurrent non-blocking filtering across **8,000 items** keeping input typing responsive at 60 FPS.

---

## 🛠️ Quick Start

```bash
# Navigate to the showcase directory
cd react-interview-showcase

# Install dependencies
npm install

# Start the local development server
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) in your browser.

---

## 🏗️ Production Build

```bash
npm run build
npm run preview
```
