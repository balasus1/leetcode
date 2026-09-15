# React 19 Master Production Engineering & Technical Interview Guide (250 Questions)

> An exhaustive, production-grade knowledge base with 100% CODE SNIPPETS covering React 19 Core Innovations (React Compiler / Forget, Actions, useActionState, useOptimistic, use() API), Fiber Architecture & Concurrency (Work Loop, Lane Model, Double Buffering, Time Slicing), Custom Hooks Internals, Modern State Management (Zustand, TanStack Query, Redux Toolkit), Rendering Architectures (RSC, Streaming SSR, Suspense, Partial Prerendering PPR), Performance Optimization & Profiling (DevTools Flamegraphs, Virtualization, Web Workers), Microfrontends & Module Federation, Design Patterns (Headless UI, Compound Components, A11y Focus Trap, XSS Security), Re-Render Elimination, Loading State Strategies, Cloudinary & Adaptive Media Delivery, External API Fetching, Re-Fetch Elimination, and Pagination Architectures.

---

## Table of Contents

- **[Part 1: React 19 Core Innovations, React Compiler & Modern APIs (Q1 - Q25)](./01_react19_core_compiler_and_new_features.md)** (25 Questions with Code Snippets)
- **[Part 2: Fiber Architecture, Reconciliation & Concurrent Mode (Q26 - Q50)](./02_fiber_architecture_reconciliation_and_concurrent_mode.md)** (25 Questions with Code Snippets)
- **[Part 3: Hooks Deep Dive, Internals & Advanced Custom Hooks (Q51 - Q75)](./03_hooks_internals_and_advanced_custom_hooks.md)** (25 Questions with Code Snippets)
- **[Part 4: State Management, Server State & Optimistic UI (Q76 - Q100)](./04_state_management_server_state_and_optimistic_updates.md)** (25 Questions with Code Snippets)
- **[Part 5: Rendering Architectures, SSR, RSC, Streaming & PPR (Q101 - Q125)](./05_rendering_patterns_ssr_rsc_streaming_and_ppr.md)** (25 Questions with Code Snippets)
- **[Part 6: Performance Optimization, Profiling & Memory (Q126 - Q150)](./06_performance_optimization_memory_and_profiling.md)** (25 Questions with Code Snippets)
- **[Part 7: Code Splitting, Bundling & Microfrontends (Q151 - Q175)](./07_code_splitting_bundling_and_microfrontends.md)** (15 Questions with Code Snippets)
- **[Part 8: Design Patterns, Architecture, A11y & Security (Q176 - Q200)](./08_design_patterns_architecture_and_security.md)** (10 Questions with Code Snippets)
- **[Part 9: Production Scaling, Testing & Enterprise SaaS Architecture (Q201 - Q210)](./09_production_scaling_testing_and_enterprise_saas.md)** (10 Questions with Code Snippets)
- **[Part 10: Re-Render Elimination, Loading State Strategies & Cloudinary/Media Optimization (Q211 - Q230)](./10_rerender_elimination_and_media_optimization.md)** (20 Questions with Code Snippets)
- **[Part 11: External API Fetching, Re-Fetch Elimination & Pagination Architectures (Q231 - Q250)](./11_api_fetching_refetch_elimination_and_pagination.md)** (20 Questions with Code Snippets)

---

# Part 1: React 19 Core Innovations, React Compiler & Modern APIs (Q1 - Q25)

---

### Q1: What are the fundamental shifts and headline features introduced in React 19?
**Answer:**
React 19 represents a major paradigm shift from manual memoization and client-centric hydration towards automatic compilation, unified server/client execution, and first-class async actions:

```
+─────────────────────────────────────────────────────────────────────────────+
|                             React 19 Ecosystem                              |
+──────────────────────────────────────┬──────────────────────────────────────+
| 1. React Compiler (Forget)           | Auto-memoization (No more useMemo,   |
|                                      | useCallback, or React.memo)          |
+──────────────────────────────────────┼──────────────────────────────────────+
| 2. First-Class Actions & Transitions | useActionState, useFormStatus,       |
|                                      | useOptimistic, Async startTransition |
+──────────────────────────────────────┼──────────────────────────────────────+
| 3. The `use()` API                   | Unwrap Promises and Context in-line  |
|                                      | with Suspense support                |
+──────────────────────────────────────┼──────────────────────────────────────+
| 4. Server Components (RSC) & Actions | Zero-bundle-size server execution    |
|                                      | with direct 'use server' RPCs        |
+──────────────────────────────────────┼──────────────────────────────────────+
| 5. Native Asset & Metadata Preloading| Native `<title>`, `<meta>`, `<link>`,|
|                                      | preload(), preinit() resource hints  |
+──────────────────────────────────────┼──────────────────────────────────────+
| 6. Ref as a Prop & Clean Context     | <Context> provider, ref passed as    |
|                                      | normal prop (forwardRef deprecated)  |
+──────────────────────────────────────┴──────────────────────────────────────+
```

---

### Q2: How does the React Compiler (React Forget) work under the hood and why does it eliminate `useMemo`, `useCallback`, and `React.memo`?
**Answer:**
Historically, React required developers to manually maintain referential equality using `useMemo`, `useCallback`, and `React.memo` to prevent unnecessary re-renders.

**How React Compiler Works:**
1. **Static Analysis & SSA (Static Single Assignment)**: The compiler parses JavaScript AST into high-level SSA intermediate representation (IR).
2. **Reactive Scope Detection**: Identifies dependencies and groups values that mutate together into fine-grained reactive scopes.
3. **Automatic Bytecode / Cache Inlining**: Rewrites JSX and function calls into memoized slots (`c(0)`, `c(1)`) checked via cheap array index comparisons.

```javascript
// Before React 19 (Manual Boilerplate)
const MemoizedCard = React.memo(({ user, onItemClick }) => {
  const formattedName = useMemo(() => formatUser(user), [user]);
  const handleClick = useCallback(() => onItemClick(user.id), [user.id, onItemClick]);
  return <div onClick={handleClick}>{formattedName}</div>;
});

// React 19 with React Compiler (Pure Idiomatic JavaScript!)
function Card({ user, onItemClick }) {
  const formattedName = formatUser(user); // Automatically memoized!
  return <div onClick={() => onItemClick(user.id)}>{formattedName}</div>; // Automatically memoized!
}
```

---

### Q3: What is the new `use()` Hook in React 19 and how does it differ from standard React Hooks?
**Answer:**
The `use()` API is a new React primitive that can read the value of a resource (a **Promise** or a **Context**) inside render.

**Key Differences from Standard Hooks:**
- Standard hooks (`useState`, `useEffect`) **cannot** be called inside conditional statements (`if`, `switch`) or loops.
- **`use()` CAN be called conditionally** inside `if` blocks and loops!
- When passed a Promise, `use(promise)` integrates directly with `<Suspense>`: it suspends rendering until the Promise resolves and automatically bubbles errors to the nearest `<ErrorBoundary>`.

```javascript
import { use, Suspense } from 'react';
import { ThemeContext } from './theme';

function UserProfile({ userPromise, shouldLoadDetails }) {
  // 1. Reading Context without useContext:
  const theme = use(ThemeContext);

  // 2. Reading Promise conditionally:
  if (!shouldLoadDetails) {
    return <div className={theme}>Basic Profile View</div>;
  }

  // Suspends component until userPromise resolves!
  const user = use(userPromise);

  return (
    <div className={theme}>
      <h2>{user.name}</h2>
      <p>{user.email}</p>
    </div>
  );
}
```

---

### Q4: How does `useActionState` (formerly `useFormState`) streamline async mutations, form submissions, and pending states in React 19?
**Answer:**
`useActionState` manages asynchronous action lifecycles, capturing pending status, returned state, and optimistic updates automatically.

```javascript
import { useActionState } from 'react';

async function updateUserNameAction(previousState, formData) {
  const newName = formData.get('name');
  try {
    const updated = await api.updateUser({ name: newName });
    return { success: true, user: updated, error: null };
  } catch (err) {
    return { success: false, user: previousState.user, error: err.message };
  }
}

export function ProfileForm({ currentUser }) {
  const [state, formAction, isPending] = useActionState(updateUserNameAction, {
    success: false,
    user: currentUser,
    error: null
  });

  return (
    <form action={formAction}>
      <input name="name" defaultValue={state.user.name} disabled={isPending} />
      <button type="submit" disabled={isPending}>
        {isPending ? 'Saving...' : 'Update Name'}
      </button>
      {state.error && <p className="error">{state.error}</p>}
      {state.success && <p className="success">Profile updated!</p>}
    </form>
  );
}
```

---

### Q5: How does `useOptimistic` work in React 19 for instantaneous UI updates?
**Answer:**
`useOptimistic` allows displaying an immediate speculative UI state while an async action (e.g. server request) is in flight, automatically rolling back if the action fails.

```javascript
import { useOptimistic, useActionState } from 'react';

export function CommentThread({ comments, sendCommentAction }) {
  const [optimisticComments, addOptimisticComment] = useOptimistic(
    comments,
    (currentComments, newCommentText) => [
      ...currentComments,
      { id: 'temp_' + Date.now(), text: newCommentText, sending: true }
    ]
  );

  async function handleAction(formData) {
    const text = formData.get('comment');
    // 1. Immediately render optimistic item in UI
    addOptimisticComment(text);
    // 2. Perform actual server mutation
    await sendCommentAction(text);
  }

  const [, formAction, isPending] = useActionState(handleAction, null);

  return (
    <div>
      {optimisticComments.map(c => (
        <p key={c.id} style={{ opacity: c.sending ? 0.6 : 1 }}>
          {c.text} {c.sending && ' (Posting...)'}
        </p>
      ))}
      <form action={formAction}>
        <input name="comment" required disabled={isPending} />
        <button type="submit">Post Comment</button>
      </form>
    </div>
  );
}
```

---

### Q6: What is `useFormStatus` and how does it enable modular, decoupled form submission components?
**Answer:**
`useFormStatus` is a hook that reads the status of the parent `<form>` without requiring props to be passed down through intermediate components.

```javascript
import { useFormStatus } from 'react-dom';

// Reusable nested button component - automatically detects parent form state!
function SubmitButton({ label = 'Submit' }) {
  const { pending, data, method, action } = useFormStatus();

  return (
    <button type="submit" disabled={pending} aria-busy={pending}>
      {pending ? 'Processing...' : label}
    </button>
  );
}

// Parent form
export function CheckoutForm({ checkoutAction }) {
  return (
    <form action={checkoutAction}>
      <input name="cardNumber" required />
      <SubmitButton label="Pay Now" />
    </form>
  );
}
```

---

### Q7: How has `ref` handling changed in React 19, and why is `forwardRef` deprecated?
**Answer:**
In React 19, **`ref` is passed as a standard prop** to function components just like `className` or `id`.
`React.forwardRef` is officially deprecated and no longer needed.

```javascript
// React 18 (Deprecated Pattern)
const OldInput = React.forwardRef((props, ref) => {
  return <input ref={ref} {...props} />;
});

// React 19 (Modern Clean Pattern)
function ModernInput({ ref, label, ...props }) {
  return (
    <label>
      {label}
      <input ref={ref} {...props} />
    </label>
  );
}
```

---

### Q8: How has Context Provider syntax changed in React 19?
**Answer:**
Instead of rendering `<ThemeContext.Provider value={...}>`, React 19 allows rendering `<ThemeContext value={...}>` directly as a provider.

```javascript
import { createContext, useState } from 'react';

export const ThemeContext = createContext('light');

export function AppThemeProvider({ children }) {
  const [theme, setTheme] = useState('dark');

  // React 19: <ThemeContext> replaces <ThemeContext.Provider>
  return (
    <ThemeContext value={theme}>
      {children}
    </ThemeContext>
  );
}
```

---

### Q9: How does React 19 support Native Document Metadata (`<title>`, `<meta>`, `<link>`) and Stylesheet Precedence?
**Answer:**
In React 19, document metadata tags can be rendered anywhere in the component tree. React automatically hoists them to the HTML `<head>` and deduplicates tags based on `rel`, `href`, or `name`.

```javascript
export function ProductPage({ product }) {
  return (
    <article>
      {/* Automatically hoisted to <head> by React 19 */}
      <title>{product.title} - Enterprise Store</title>
      <meta name="description" content={product.summary} />
      <meta property="og:image" content={product.imageUrl} />
      <link rel="canonical" href={`https://store.com/products/${product.id}`} />

      {/* Stylesheet precedence: React loads and deduplicates before rendering children */}
      <link rel="stylesheet" href="/styles/product.css" precedence="high" />

      <h1>{product.title}</h1>
      <p>{product.description}</p>
    </article>
  );
}
```

---

### Q10: What are the new Resource Preloading APIs (`preload`, `preinit`, `preconnect`, `prefetchDNS`) in `react-dom`?
**Answer:**
React 19 introduces native imperative resource hinting APIs to warm connections and load critical assets early during component render.

```javascript
import { preload, preinit, preconnect, prefetchDNS } from 'react-dom';

export function VideoPlayerPage({ videoSource }) {
  // Pre-connect to video CDN
  preconnect('https://video-cdn.global.com');
  prefetchDNS('https://analytics.global.com');

  // Preload critical font & poster image
  preload('https://fonts.cdn.com/inter.woff2', { as: 'font', type: 'font/woff2' });
  preload('https://video-cdn.global.com/poster.webp', { as: 'image' });

  // Pre-initialize and execute analytics script immediately
  preinit('https://analytics.global.com/tracker.js', { as: 'script' });

  return <video src={videoSource} controls />;
}
```

---

### Q11: What is the difference between React Server Components (RSC) and Server-Side Rendering (SSR)?
**Answer:**

```
+─────────────────────────────────────────────────────────────────────────────+
| Feature              | Server-Side Rendering (SSR) | React Server Components|
+──────────────────────┼─────────────────────────────┼────────────────────────+
| **Execution**        | Executes on server to emit  | Executes ONLY on server|
|                      | initial HTML string         | emits serialized stream|
+──────────────────────┼─────────────────────────────┼────────────────────────+
| **Client JS Bundle** | 100% of component code sent | 0 KB JS sent to client |
|                      | to browser for Hydration    | (Zero Bundle Overhead) |
+──────────────────────┼─────────────────────────────┼────────────────────────+
| **Direct DB Access** | No (requires API route)     | YES (Direct SQL/ORMs)  |
+──────────────────────┼─────────────────────────────┼────────────────────────+
| **Client State/Hooks**| Supported (useState, etc.) | NOT supported (Stateless|
+──────────────────────┼─────────────────────────────┼────────────────────────+
| **Re-execution**     | Only on initial page load   | Re-fetchable on-demand |
|                      | or full navigation          | preserving client state|
+──────────────────────┴─────────────────────────────┴────────────────────────+
```

---

### Q12: How does the `'use server'` and `'use client'` Directive Boundary Model work in React 19?
**Answer:**
- **`'use client'`**: Marks the boundary between Server-rendered components and interactive Client components. All imported sub-components become part of the client bundle.
- **`'use server'`**: Declares **Server Actions** (callable async functions executing strictly on the server that can be passed across the network boundary to client components).

```javascript
// actions/user.js (Server Action Module)
'use server';

import db from '@/lib/db';

export async function deleteUserAction(userId) {
  // Direct server-side DB access with server authentication
  await db.users.delete({ where: { id: userId } });
  return { success: true };
}

// components/DeleteButton.jsx (Client Component)
'use client';

import { useTransition } from 'react';
import { deleteUserAction } from '@/actions/user';

export function DeleteButton({ userId }) {
  const [isPending, startTransition] = useTransition();

  return (
    <button 
      disabled={isPending}
      onClick={() => startTransition(async () => await deleteUserAction(userId))}
    >
      {isPending ? 'Deleting...' : 'Delete Account'}
    </button>
  );
}
```

---

### Q13: How do Async Transitions work with `startTransition` in React 19?
**Answer:**
In React 18, `startTransition` only accepted synchronous functions.
In React 19, `startTransition` **natively accepts `async` functions**, tracking `isPending` state throughout the entire Promise lifecycle.

```javascript
import { useState, useTransition } from 'react';

export function TabNavigator() {
  const [tab, setTab] = useState('feed');
  const [isPending, startTransition] = useTransition();

  function selectTab(nextTab) {
    startTransition(async () => {
      // Async transition tracks pending state until network + render completes!
      await api.prefetchTabAnalytics(nextTab);
      setTab(nextTab);
    });
  }

  return (
    <div>
      <button onClick={() => selectTab('feed')}>Feed</button>
      <button onClick={() => selectTab('settings')}>Settings</button>
      {isPending && <span className="spinner">Switching...</span>}
      {tab === 'feed' ? <FeedView /> : <SettingsView />}
    </div>
  );
}
```

---

### Q14: How does Cleanup Function support work in `ref` callbacks in React 19?
**Answer:**
In React 19, `ref` callback functions can return a **cleanup function** that executes when the DOM node is removed (mirroring `useEffect` cleanup).

```javascript
function AutoResizeTextarea() {
  return (
    <textarea
      ref={(domNode) => {
        if (!domNode) return;
        
        const resizeObserver = new ResizeObserver(entries => {
          console.log('Textarea resized:', entries[0].contentRect);
        });
        resizeObserver.observe(domNode);

        // ✅ React 19 ref cleanup function!
        return () => {
          resizeObserver.disconnect();
        };
      }}
    />
  );
}
```

---

### Q15: What is the new `hydration` error reporting improvement in React 19?
**Answer:**
Hydration mismatch errors (e.g. server rendered `<div>Server</div>`, client rendered `<div>Client</div>`) in older React displayed cryptic `Minified React error #418`.
React 19 prints a **side-by-side colorized visual diff** directly in the browser console showing the exact mismatched DOM nodes:

```javascript
// React 19 Hydration Mismatch Diff Log in Console:
// Uncaught Error: Hydration failed because the initial UI does not match what was rendered on the server.
//   <App>
//     <div>
// -     "Server Rendered Text"
// +     "Client Rendered Text"
//     </div>
//   </App>
```

---

### Q16: What is the difference between Server Actions and traditional REST/tRPC API routes?
**Answer:**
- **REST/tRPC**: Explicit endpoints requiring URL routing, serialization boilerplate, and separate client fetch code.
- **Server Actions**: Remote Procedure Calls (RPC) generated transparently by the React bundler:

```javascript
// Server Action ('use server'):
'use server';
export async function updateEmail(userId, newEmail) {
  await db.user.update({ where: { id: userId }, data: { email: newEmail } });
  return { success: true };
}

// Client Component: Import and invoke directly like a local async function!
'use client';
import { updateEmail } from './actions';

export function EmailForm({ userId }) {
  return <button onClick={() => updateEmail(userId, 'alice@domain.com')}>Update</button>;
}
```

---

### Q17: How does React 19 handle Custom Elements / Web Components seamlessly?
**Answer:**
In React 19, React checks if a prop exists on the Custom Element instance as a property:

```javascript
// React 19 Native Custom Element Integration:
export function WebComponentWrapper() {
  // Complex objects & custom events pass directly without manual ref.addEventListener:
  return (
    <my-custom-chart
      chartData={[{ x: 1, y: 10 }, { x: 2, y: 20 }]} // Passed directly as property!
      onchartclick={(e) => console.log('Custom event triggered:', e.detail)}
    />
  );
}
```

---

### Q18: What is Partial Prerendering (PPR) in the context of React 19 and Next.js?
**Answer:**
PPR combines Static Site Generation (SSG) and Dynamic Streaming Server-Side Rendering into a single HTTP response:

```javascript
// next.config.js - Partial Prerendering (PPR):
export const experimental = { ppr: true };

// Page Component:
export default function Page() {
  return (
    <div>
      <StaticNavbar /> {/* Instant static shell from Edge CDN */}
      <Suspense fallback={<FeedSkeleton />}>
        <DynamicPersonalizedFeed /> {/* Streams in dynamically over same HTTP stream */}
      </Suspense>
    </div>
  );
}
```

---

### Q19: How do you migrate from `React.lazy` to the `use()` API for dynamic component loading?
**Answer:**

```javascript
import { use, Suspense } from 'react';

// Dynamic import returns a Promise
const dynamicChartPromise = import('./HeavyChart').then(mod => mod.HeavyChart);

function AnalyticsDashboard() {
  return (
    <Suspense fallback={<p>Loading chart widget...</p>}>
      <ChartContainer chartPromise={dynamicChartPromise} />
    </Suspense>
  );
}

function ChartContainer({ chartPromise }) {
  const ChartComponent = use(chartPromise); // Unwraps dynamic module directly!
  return <ChartComponent data={[10, 20, 30]} />;
}
```

---

### Q20: What are the memory and execution performance trade-offs of Server Components vs. Client Components?
**Answer:**
- **Server Components**: Executed on server, 0 KB client JS.
- **Client Components**: Downloaded, parsed, hydrated in browser RAM.

```javascript
// Server Component: 0KB JavaScript sent to client (Heavy markdown parser runs on server!)
import { marked } from 'marked'; // 50KB library NEVER sent to browser!

export async function ServerMarkdownViewer({ markdownText }) {
  const html = marked.parse(markdownText);
  return <div dangerouslySetInnerHTML={{ __html: html }} />;
}
```

---

### Q21: How do you handle file uploads with React 19 Server Actions without third-party multipart libraries?
**Answer:**

```javascript
// server/uploadAction.js
'use server';

import fs from 'node:fs/promises';
import path from 'node:path';

export async function uploadAvatarAction(formData) {
  const file = formData.get('avatar'); // Standard Web File API
  if (!file || file.size === 0) throw new Error('No file uploaded');

  const buffer = Buffer.from(await file.arrayBuffer());
  const filePath = path.join(process.cwd(), 'uploads', `${Date.now()}_${file.name}`);
  await fs.writeFile(filePath, buffer);

  return { success: true, url: `/uploads/${file.name}` };
}
```

---

### Q22: What happens when an error is thrown inside a Server Component vs. a Client Component?
**Answer:**
Server errors are sanitized in production to avoid leaking database passwords:

```javascript
// Error Boundary surrounding Server Component Suspense boundary:
<ErrorBoundary fallback={<p>Unable to load recommendations. Please try again.</p>}>
  <Suspense fallback={<Skeleton />}>
    <AsyncServerRecommendations /> {/* Sanitized server crash caught safely! */}
  </Suspense>
</ErrorBoundary>
```

---

### Q23: How does React 19 deduplicate Promises passed into the `use()` Hook?
**Answer:**
Passing the same Promise reference unwraps simultaneously across sibling components:

```javascript
// Shared Promise instance:
const userPromise = fetchUser(userId);

function SiblingA() {
  const user = use(userPromise); // Unwraps shared promise
  return <h1>{user.name}</h1>;
}

function SiblingB() {
  const user = use(userPromise); // Reuses exact same settled value without duplicate network query!
  return <p>{user.email}</p>;
}
```

---

### Q24: What is the difference between React 19 Actions and useEffect-based data mutation?
**Answer:**
Actions integrate natively with Transitions and rollbacks:

```javascript
// React 19 Action: Clean pending state and error boundary integration
const [state, formAction, isPending] = useActionState(async (prev, formData) => {
  return await mutateData(formData);
}, initial);

return <form action={formAction}><button disabled={isPending}>Save</button></form>;
```

---

### Q25: How do you set up the React Compiler in a modern Vite / Next.js / Webpack project?
**Answer:**

```bash
npm install -D babel-plugin-react-compiler
```

```javascript
// vite.config.js
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: [['babel-plugin-react-compiler', { target: '19' }]],
      },
    }),
  ],
});
```


---

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


---

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


---

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


---

# Part 5: Rendering Architectures, SSR, RSC, Streaming & PPR (Q101 - Q125)

---

### Q101: What are the fundamental Rendering Architectures in modern React and how do they compare?
**Answer:**

```javascript
// Architecture Matrix in Next.js / React 19:
// 1. Static (SSG): export const dynamic = 'force-static';
// 2. Dynamic (SSR): export const dynamic = 'force-dynamic';
// 3. ISR: export const revalidate = 60; // 60s cache
// 4. Client Island (CSR): 'use client';
```

---

### Q102: How does React Streaming SSR with HTML Suspense (`renderToPipeableStream`) work in Node.js?
**Answer:**

```javascript
import { renderToPipeableStream } from 'react-dom/server';
import App from './App';

app.get('/', (req, res) => {
  const stream = renderToPipeableStream(<App />, {
    bootstrapScripts: ['/bundle.js'],
    onShellReady() {
      res.setHeader('Content-type', 'text/html');
      stream.pipe(res); // Stream HTML shell immediately!
    }
  });
});
```

---

### Q103: What is the RSC Wire Format (Flight Protocol) and how are Server Components serialized?
**Answer:**

```javascript
// RSC Flight Wire Format Stream:
// M1:{"id":"./components/Button.js","chunks":["client-1"],"name":"Button"}
// J0:["$","div",null,{"className":"card","children":["$L1",{"label":"Buy Now"}]]
```

---

### Q104: What is the "Poisoning" problem in Server Components and how does the `server-only` package prevent it?
**Answer:**

```javascript
// lib/secrets.js
import 'server-only'; // Throws compile error if imported in 'use client' bundle!

export const PRIVATE_API_KEY = process.env.STRIPE_SECRET_KEY;
```

---

### Q105: How does Hydration work and what are the primary causes of Hydration Mismatches?
**Answer:**

```javascript
// ❌ Hydration Mismatch (Server renders one date, client renders another):
// <div>{new Date().toLocaleTimeString()}</div>

// ✅ Fix: Render on client only after mount:
export function SafeClientTime() {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  if (!mounted) return null;
  return <span>{new Date().toLocaleTimeString()}</span>;
}
```

---

### Q106: How do you suppress Hydration Mismatch warnings safely when differences are intentional?
**Answer:**

```javascript
export function DynamicTimestamp() {
  return (
    <time dateTime="2026-09-05" suppressHydrationWarning>
      {new Date().toLocaleDateString()}
    </time>
  );
}
```

---

### Q107: What is the Island Architecture (Astro / Fresh) and how does it compare to React Server Components?
**Answer:**

```html
<!-- Astro Island Architecture: HTML static page + isolated interactive island -->
<header>Static Company Header (0KB JS)</header>
<main>
  <ReactInteractiveCart client:visible /> <!-- Hydrates ONLY when scrolled into view -->
</main>
```

---

### Q108: How can a Server Component pass data to a Client Component, and what are the serialization rules?
**Answer:**

```javascript
// Server Component (app/page.js)
import { ClientCard } from './ClientCard';

export default async function Page() {
  const user = await db.user.findFirst();
  // Safe: Passing JSON-serializable plain object across network boundary:
  return <ClientCard user={{ id: user.id, name: user.name }} />;
}
```

---

### Q109: How does Interleaving Server Components inside Client Components work via the `children` prop pattern?
**Answer:**

```javascript
// Client Component ('use client')
'use client';
export function ClientAccordion({ children }) {
  const [open, setOpen] = useState(false);
  return (
    <div>
      <button onClick={() => setOpen(!open)}>Toggle</button>
      {open && children} {/* Server Component rendered inside Client Component! */}
    </div>
  );
}
```

---

### Q110: What is Edge Rendering (Cloudflare Workers / Vercel Edge) and how does `renderToReadableStream` function?
**Answer:**

```javascript
import { renderToReadableStream } from 'react-dom/server';
import App from './App';

export default {
  async fetch(request) {
    const stream = await renderToReadableStream(<App />);
    return new Response(stream, { headers: { 'content-type': 'text/html' } });
  }
};
```

---

### Q111: What is Incremental Static Regeneration (ISR) and On-Demand Revalidation (`revalidatePath` / `revalidateTag`)?
**Answer:**

```javascript
// Server Action triggering instant ISR Cache Purge:
'use server';
import { revalidateTag } from 'next/cache';

export async function updateArticle(id, data) {
  await db.article.update({ where: { id }, data });
  revalidateTag('articles'); // Purges cached static HTML instantly across Edge CDNs!
}
```

---

### Q112: How do you prevent Waterfall Requests in React Server Component architectures?
**Answer:**

```javascript
// ✅ Parallel Data Fetching in Server Components:
export default async function DashboardPage() {
  const [userPromise, statsPromise] = [
    db.getUser(),
    db.getStats()
  ];
  const [user, stats] = await Promise.all([userPromise, statsPromise]);
  return <DashboardView user={user} stats={stats} />;
}
```

---

### Q113: What is the difference between `<Suspense>` on the Server vs. `<Suspense>` on the Client?
**Answer:**

```javascript
// Streaming Suspense on Server:
<Suspense fallback={<NavbarSkeleton />}>
  <AsyncServerNavbar /> {/* Streams HTML chunk as soon as DB finishes */}
</Suspense>
```

---

### Q114: How does React Server Component Caching (`React.cache`) work and what is its request lifecycle?
**Answer:**

```javascript
import { cache } from 'react';
import db from '@/lib/db';

export const getSiteConfig = cache(async () => {
  return await db.config.findFirst(); // Memoized for the lifetime of 1 server request
});
```

---

### Q115: What is the impact of CSS Delivery on Streaming SSR (Critical CSS Inlining)?
**Answer:**

```html
<!-- React 19 Stylesheet Precedence: Inlined before streamed HTML chunk -->
<link rel="stylesheet" href="/styles/streamed-widget.css" precedence="high" />
```

---

### Q116: How do you handle Authentication and Cookies inside React Server Components?
**Answer:**

```javascript
import { cookies } from 'next/headers';

export default async function ProfileServerComponent() {
  const cookieStore = await cookies();
  const token = cookieStore.get('auth_token')?.value;
  const user = await verifyJwtAndGetUser(token);

  return <div>Welcome back, {user.name}</div>;
}
```

---

### Q117: What is Static Site Generation (SSG) with Dynamic Route Parameters (`generateStaticParams`)?
**Answer:**

```javascript
export async function generateStaticParams() {
  const products = await db.products.findMany({ select: { id: true } });
  return products.map(p => ({ id: String(p.id) }));
}
```

---

### Q118: How does Server-Side Request Deduplication work across `fetch()` calls?
**Answer:**

```javascript
// Next.js automatic fetch deduplication:
// Calling fetch() with same URL in 5 separate server components fires 1 single HTTP request:
const res = await fetch('https://api.domain.com/user', { next: { revalidate: 3600 } });
```

---

### Q119: What are Client-Side Navigations in an RSC Application and why is it faster than standard MPAs?
**Answer:**

```javascript
import Link from 'next/link';

// Navigates via fetch('/rsc-payload') without full browser page reload:
<Link href="/analytics" prefetch={true}>Analytics</Link>
```

---

### Q120: How do you handle Flash of Layout Shift (CLS) when streaming with Suspense?
**Answer:**

```javascript
// Dimension-Locked Skeleton matching exact pixel height of final grid:
<Suspense fallback={<div style={{ height: '350px', width: '100%' }} className="skeleton-box" />}>
  <AsyncProductGrid />
</Suspense>
```

---

### Q121: What is the difference between Static Export (`output: 'export'`) and Server Runtimes?
**Answer:**

```javascript
// next.config.js - Static HTML export mode:
module.exports = {
  output: 'export', // Produces pure static HTML/JS/CSS files in /out directory
  images: { unoptimized: true }
};
```

---

### Q122: How do you build an SEO-optimized Dynamic Open Graph (OG) Image Generator in React?
**Answer:**

```javascript
import { ImageResponse } from 'next/og';

export async function GET(request) {
  const { searchParams } = new URL(request.url);
  const title = searchParams.get('title') || 'Default Title';

  return new ImageResponse(
    (
      <div style={{ fontSize: 48, background: '#111', color: '#fff', width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {title}
      </div>
    ),
    { width: 1200, height: 630 }
  );
}
```

---

### Q123: What is the role of React Server Actions in Form Progressive Enhancement?
**Answer:**

```javascript
// Works with 0 JavaScript enabled in browser via standard HTTP POST:
<form action={myServerAction}>
  <input name="email" required />
  <button type="submit">Subscribe</button>
</form>
```

---

### Q124: How do you handle Stale Search Crawlers and Social Media Bots with Streaming SSR?
**Answer:**

```javascript
const isBot = /bot|googlebot|crawler|spider/i.test(req.headers['user-agent'] || '');
const stream = renderToPipeableStream(<App />, {
  [isBot ? 'onAllReady' : 'onShellReady']() {
    stream.pipe(res); // Bots receive 100% complete HTML before status code is sent!
  }
});
```

---

### Q125: What is the future roadmap of React Server Functions beyond HTTP (WebSockets / gRPC)?
**Answer:**

```javascript
// Protocol-agnostic Server Action Invocation:
'use server';
export async function executeDistributedAction(rpcPayload) {
  return await grpcClient.dispatch(rpcPayload);
}
```


---

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


---

# Part 7: Code Splitting, Bundling & Microfrontends (Q151 - Q175)

---

### Q151: What is a Microfrontend Architecture and what problems does it solve for large enterprise teams?
**Answer:**

```javascript
// Microfrontend Container Mount Point:
import { lazy, Suspense } from 'react';

const RemoteBillingApp = lazy(() => import('billing/App'));

export function MainShell() {
  return (
    <div>
      <GlobalNavbar />
      <Suspense fallback={<p>Loading Microfrontend...</p>}>
        <RemoteBillingApp />
      </Suspense>
    </div>
  );
}
```

---

### Q152: How does Webpack 5 Module Federation work under the hood?
**Answer:**

```javascript
// webpack.config.js - Module Federation Plugin:
const { ModuleFederationPlugin } = require('webpack').container;

module.exports = {
  plugins: [
    new ModuleFederationPlugin({
      name: 'host',
      remotes: {
        mfe_cart: 'mfe_cart@https://cdn.domain.com/cart/remoteEntry.js'
      },
      shared: {
        react: { singleton: true, requiredVersion: '^19.0.0' },
        'react-dom': { singleton: true, requiredVersion: '^19.0.0' }
      }
    })
  ]
};
```

---

### Q153: What is the `shared: { singleton: true }` rule in Module Federation and why is it critical for React?
**Answer:**

```javascript
// Enforces exactly 1 shared copy of React runtime in memory:
shared: {
  react: { singleton: true, eager: false, requiredVersion: '^19.0.0' },
  'react-dom': { singleton: true, eager: false, requiredVersion: '^19.0.0' }
}
```

---

### Q154: How does Single-SPA compare with Webpack Module Federation?
**Answer:**

```javascript
// Single-SPA Lifecycle Contract:
import React from 'react';
import ReactDOMClient from 'react-dom/client';
import singleSpaReact from 'single-spa-react';
import App from './root.component';

const lifecycles = singleSpaReact({
  React,
  ReactDOMClient,
  rootComponent: App,
  errorBoundary(err, info, props) {
    return <div>Microfrontend error</div>;
  }
});

export const { bootstrap, mount, unmount } = lifecycles;
```

---

### Q155: How do Microfrontends communicate without Tight Coupling (EventBus / Custom Events / BroadcastChannel)?
**Answer:**

```javascript
// Decoupled Custom Event Bus:
export const emitMfeEvent = (name, data) => {
  window.dispatchEvent(new CustomEvent(`mfe:${name}`, { detail: data }));
};

export function useMfeListener(name, callback) {
  useEffect(() => {
    const handler = (e) => callback(e.detail);
    window.addEventListener(`mfe:${name}`, handler);
    return () => window.removeEventListener(`mfe:${name}`, handler);
  }, [name, callback]);
}
```

---

### Q156: How do you handle Route-Based Code Splitting with `React.lazy` and dynamic `import()`?
**Answer:**

```javascript
import { lazy, Suspense } from 'react';
import { Routes, Route } from 'react-router-dom';

const Analytics = lazy(() => import('./routes/Analytics'));
const Settings = lazy(() => import('./routes/Settings'));

export function RouterConfig() {
  return (
    <Suspense fallback={<div className="skeleton-page" />}>
      <Routes>
        <Route path="/analytics" element={<Analytics />} />
        <Route path="/settings" element={<Settings />} />
      </Routes>
    </Suspense>
  );
}
```

---

### Q157: How do you implement Component-Level Dynamic Pre-fetching on Mouse Hover?
**Answer:**

```javascript
const prefetchModal = () => import('./HeavyModal');

export function NavLink() {
  return (
    <button onMouseEnter={prefetchModal} onFocus={prefetchModal}>
      Open Analytics
    </button>
  );
}
```

---

### Q158: What is Native Federation (ESM / Import Maps) and how does it work without Webpack?
**Answer:**

```html
<script type="importmap">
{
  "imports": {
    "react": "https://esm.sh/react@19.0.0",
    "mfeOrder/Widget": "https://orders.domain.com/dist/widget.js"
  }
}
</script>
```

---

### Q159: How do you isolate CSS and prevent style collisions across Microfrontends (Shadow DOM, CSS Modules, Tailwind Prefix)?
**Answer:**

```javascript
// tailwind.config.js per microfrontend:
module.exports = {
  prefix: 'mfe-billing-', // Namespaces all generated utility classes!
  content: ['./src/**/*.{js,jsx}']
};
```

---

### Q160: How do you handle Global Error Boundaries in Microfrontend architectures?
**Answer:**

```javascript
export function IsolatedMicrofrontendWrapper({ children }) {
  return (
    <ErrorBoundary fallback={<div className="mfe-fallback">Widget Unavailable</div>}>
      <Suspense fallback={<div className="mfe-skeleton" />}>
        {children}
      </Suspense>
    </ErrorBoundary>
  );
}
```

---

### Q161: What is Chunk Splitting Strategy (`splitChunks`) in enterprise Webpack/Vite configs?
**Answer:**

```javascript
// Webpack SplitChunks Optimization:
optimization: {
  splitChunks: {
    chunks: 'all',
    cacheGroups: {
      vendors: {
        test: /[\\/]node_modules[\\/]/,
        name: 'vendor-bundle',
        priority: 10
      }
    }
  }
}
```

---

### Q162: What is Version Skew and how do you handle backward compatibility in Microfrontend APIs?
**Answer:**

```javascript
// Versioned Event Payload Contract:
emitMfeEvent('CART_UPDATED', {
  schemaVersion: '2.1',
  payload: { cartId: 'c_99', total: 150 }
});
```

---

### Q163: How do you test Microfrontends in isolation vs. End-to-End integration?
**Answer:**

```javascript
// Vitest Isolated Unit Test for MFE Component:
import { render, screen } from '@testing-library/react';
import { BillingWidget } from './BillingWidget';

test('renders standalone MFE widget with mocked shell props', () => {
  render(<BillingWidget tenantId="tenant_123" />);
  expect(screen.getByText(/invoice/i)).toBeInTheDocument();
});
```

---

### Q164: What is the overhead of Microfrontends and when is it an ANTI-PATTERN?
**Answer:**

```javascript
// Anti-Pattern: Single small team splitting 5 pages into 5 separate repos:
// Monorepo (Turborepo / Nx) with shared packages is 10x faster and simpler!
```

---

### Q165: How do you implement Cross-Microfrontend Global Authentication state?
**Answer:**

```javascript
// Broadcast Channel Auth Token Sharing:
const authBroadcast = new BroadcastChannel('mfe_auth');

export function loginAcrossMfes(token) {
  authBroadcast.postMessage({ type: 'TOKEN_UPDATED', token });
}
```


---

# Part 8: Design Patterns, Architecture, A11y & Security (Q176 - Q200)

---

### Q176: What is the Compound Component Pattern and how do you implement an Accessible Accordion / Tabs component in React 19?
**Answer:**

```javascript
import { createContext, useContext, useState } from 'react';

const TabsContext = createContext(null);

export function Tabs({ defaultValue, children }) {
  const [activeTab, setActiveTab] = useState(defaultValue);
  return (
    <TabsContext value={{ activeTab, setActiveTab }}>
      <div className="tabs-root">{children}</div>
    </TabsContext>
  );
}

Tabs.Trigger = function TabTrigger({ value, children }) {
  const { activeTab, setActiveTab } = useContext(TabsContext);
  return (
    <button
      role="tab"
      aria-selected={activeTab === value}
      onClick={() => setActiveTab(value)}
    >
      {children}
    </button>
  );
};

Tabs.Content = function TabContent({ value, children }) {
  const { activeTab } = useContext(TabsContext);
  if (activeTab !== value) return null;
  return <div role="tabpanel">{children}</div>;
};
```

---

### Q177: What is the Headless Component / Hook Pattern (Radix UI / TanStack Table / React Aria)?
**Answer:**

```javascript
// Headless Table Hook (100% logic, 0% forced styles):
import { useReactTable, getCoreRowModel } from '@tanstack/react-table';

export function HeadlessGrid({ data, columns }) {
  const table = useReactTable({ data, columns, getCoreRowModel: getCoreRowModel() });
  return (
    <table>
      <tbody>
        {table.getRowModel().rows.map(row => (
          <tr key={row.id}>
            {row.getVisibleCells().map(cell => (
              <td key={cell.id}>{cell.renderValue()}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
```

---

### Q178: How do you build a Polymorphic Component in React with TypeScript (`as` prop)?
**Answer:**

```typescript
import React from 'react';

type PolymorphicProps<C extends React.ElementType, Props = {}> = React.PropsWithChildren<Props & { as?: C }> &
  Omit<React.ComponentPropsWithoutRef<C>, keyof (Props & { as?: C })>;

export function Button<C extends React.ElementType = 'button'>({ as, children, ...props }: PolymorphicProps<C>) {
  const Component = as || 'button';
  return <Component {...props}>{children}</Component>;
}
```

---

### Q179: How do XSS (Cross-Site Scripting) vulnerabilities happen in React and how do you prevent them?
**Answer:**

```javascript
import DOMPurify from 'dompurify';

// 🛡️ XSS Prevention:
export function SafeHtmlViewer({ userSuppliedHtml }) {
  return (
    <div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(userSuppliedHtml) }} />
  );
}
```

---

### Q180: What is Focus Management and how do you implement a Focus Trap for Accessible Modal Dialogs?
**Answer:**

```javascript
export function useFocusTrap(modalRef, isOpen) {
  useEffect(() => {
    if (!isOpen || !modalRef.current) return;
    const focusable = modalRef.current.querySelectorAll('button, [href], input, [tabindex="0"]');
    focusable[0]?.focus();

    const handleKey = (e) => {
      if (e.key === 'Tab') {
        if (e.shiftKey && document.activeElement === focusable[0]) {
          focusable[focusable.length - 1].focus();
          e.preventDefault();
        } else if (!e.shiftKey && document.activeElement === focusable[focusable.length - 1]) {
          focusable[0].focus();
          e.preventDefault();
        }
      }
    };
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [isOpen, modalRef]);
}
```

---

### Q181: What is the Render Props Pattern and when is it still useful in modern React?
**Answer:**

```javascript
export function MouseTracker({ render }) {
  const [pos, setPos] = useState({ x: 0, y: 0 });
  return (
    <div onMouseMove={e => setPos({ x: e.clientX, y: e.clientY })}>
      {render(pos)}
    </div>
  );
}
```

---

### Q182: What is Prop Drilling and what are the best techniques to eliminate it?
**Answer:**

```javascript
// ✅ Component Composition (Slot Pattern) completely eliminates prop drilling:
function Layout({ header, content, sidebar }) {
  return (
    <div className="layout">
      <header>{header}</header>
      <aside>{sidebar}</aside>
      <main>{content}</main>
    </div>
  );
}
```

---

### Q183: What is the Controlled vs. Uncontrolled Component pattern in React forms?
**Answer:**

```javascript
// 1. Controlled (State driven):
<input value={text} onChange={e => setText(e.target.value)} />

// 2. Uncontrolled (DOM driven - 0 re-renders):
<input ref={inputRef} defaultValue="Initial" />
```

---

### Q184: How do you build an Accessible Live Region (`aria-live`) for dynamic screen reader announcements?
**Answer:**

```javascript
export function LiveAnnouncer({ message }) {
  return (
    <div role="status" aria-live="polite" aria-atomic="true" style={{ position: 'absolute', clip: 'rect(0 0 0 0)' }}>
      {message}
    </div>
  );
}
```

---

### Q185: What is the Higher-Order Component (HOC) Pattern and what are its drawbacks?
**Answer:**

```javascript
// Higher-Order Component with Authentication Guard:
export function withAuth(Component) {
  return function AuthenticatedWrapper(props) {
    const { isAuthenticated } = useAuth();
    if (!isAuthenticated) return <Navigate to="/login" />;
    return <Component {...props} />;
  };
}
```


---

# Part 9: Production Scaling, Testing & Enterprise SaaS Architecture (Q201 - Q210)

---

### Q201: How do you write robust Unit and Integration Tests using Vitest and React Testing Library (RTL)?
**Answer:**

```javascript
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { LoginForm } from './LoginForm';

describe('LoginForm Component', () => {
  it('submits form with valid user credentials', async () => {
    const user = userEvent.setup();
    const handleSubmit = vi.fn();

    render(<LoginForm onSubmit={handleSubmit} />);

    await user.type(screen.getByLabelText(/email/i), 'alice@enterprise.com');
    await user.type(screen.getByLabelText(/password/i), 'SecretPassword123!');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(handleSubmit).toHaveBeenCalledTimes(1);
  });
});
```

---

### Q202: How do you mock network requests cleanly using Mock Service Worker (MSW v2)?
**Answer:**

```javascript
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

export const handlers = [
  http.get('https://api.domain.com/user/profile', () => {
    return HttpResponse.json({ id: 'u_1', name: 'Alice', plan: 'Enterprise' });
  })
];

export const server = setupServer(...handlers);
```

---

### Q203: How do you test Custom React Hooks with `@testing-library/react` `renderHook` and `act`?
**Answer:**

```javascript
import { renderHook, act } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { useCounter } from './useCounter';

describe('useCounter Hook', () => {
  it('increments counter correctly', () => {
    const { result } = renderHook(() => useCounter(10));
    expect(result.current.count).toBe(10);

    act(() => {
      result.current.increment();
    });

    expect(result.current.count).toBe(11);
  });
});
```

---

### Q204: How do you write resilient End-to-End (E2E) tests with Playwright for React 19 applications?
**Answer:**

```typescript
import { test, expect } from '@playwright/test';

test('User can complete checkout with optimistic update', async ({ page }) => {
  await page.goto('https://staging.store.com/products/iphone-16');
  await expect(page.getByRole('heading', { name: 'iPhone 16' })).toBeVisible();

  await page.getByRole('button', { name: 'Add to Cart' }).click();
  await expect(page.getByTestId('cart-count')).toHaveText('1');
});
```

---

### Q205: How do you build an Enterprise Multi-Tenant White-Label Design System with CSS Variables and Design Tokens?
**Answer:**

```css
:root {
  --theme-primary: #3b82f6;
  --theme-radius: 8px;
}

[data-tenant="acme"] {
  --theme-primary: #10b981;
  --theme-radius: 16px;
}
```

---

### Q206: How do you monitor Real-User Monitoring (RUM) metrics and Sentry error tracking in React?
**Answer:**

```javascript
import * as Sentry from '@sentry/react';

Sentry.init({
  dsn: 'https://key@sentry.io/123',
  integrations: [Sentry.browserTracingIntegration()]
});

export const RootApp = Sentry.withErrorBoundary(App, {
  fallback: <p>Something went wrong.</p>
});
```

---

### Q207: How do you implement Feature Flags and Canary Rollouts (LaunchDarkly) in React?
**Answer:**

```javascript
import { useFlags } from 'launchdarkly-react-client-sdk';

export function CheckoutButton() {
  const { newOneClickCheckout } = useFlags();
  return newOneClickCheckout ? <OneClickCheckout /> : <StandardCheckout />;
}
```

---

### Q208: How do you optimize React Bundle Size with Bundle Analyzers (`vite-bundle-visualizer` / `webpack-bundle-analyzer`)?
**Answer:**

```javascript
// vite.config.js with Visualizer Plugin:
import { visualizer } from 'rollup-plugin-visualizer';

export default {
  plugins: [visualizer({ open: true, filename: 'bundle-report.html' })]
};
```

---

### Q209: What is the difference between Shallow Rendering and Full DOM Rendering in tests?
**Answer:**

```javascript
// React Testing Library executes Full DOM rendering inside simulated JSDOM:
import { render, screen } from '@testing-library/react';

test('Full DOM render mounts nested child elements', () => {
  render(<ParentWithChildren />);
  expect(screen.getByRole('button')).toBeInTheDocument(); // Real button is rendered!
});
```

---

### Q210: What are the Architectural Best Practices for designing a Production-Grade, Fault-Tolerant Enterprise React 19 Application?
**Answer:**

```javascript
// Enterprise Composition Root Blueprint:
export function EnterpriseApp() {
  return (
    <ErrorBoundary fallback={<FatalCrashScreen />}>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <AuthProvider>
            <AppRouter />
          </AuthProvider>
        </ThemeProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
```


---

# Part 10: Re-Render Elimination, Loading State Strategies & Cloudinary/Media Optimization (Q211 - Q230)

---

### Q211: What is the Complete Taxonomy of Re-Render Elimination Strategies in React 19?
**Answer:**

```javascript
// React Compiler inlines fine-grained memoization slots:
function Card({ user, onItemClick }) {
  const formattedName = formatUser(user); // Auto-memoized!
  return <div onClick={() => onItemClick(user.id)}>{formattedName}</div>;
}
```

---

### Q212: How does State Colocation eliminate massive re-render trees in complex enterprise forms and dashboards?
**Answer:**

```javascript
// State Colocation isolates state to leaf component:
function SearchInput({ onSearch }) {
  const [query, setQuery] = useState('');
  return <input value={query} onChange={e => { setQuery(e.target.value); onSearch(e.target.value); }} />;
}
```

---

### Q213: How does React 19 `React.cache()` eliminate duplicate database and API calls across Server Components?
**Answer:**

```javascript
import { cache } from 'react';
import db from '@/lib/db';

export const getCachedProduct = cache(async (id) => {
  return await db.product.findUnique({ where: { id } });
});
```

---

### Q214: How do you avoid "Loading Spinner Thrashing" using React 19 `useTransition` and `useDeferredValue`?
**Answer:**

```javascript
import { useState, useTransition } from 'react';

export function ProductCatalog() {
  const [category, setCategory] = useState('all');
  const [isPending, startTransition] = useTransition();

  const handleSelect = (cat) => {
    startTransition(() => {
      setCategory(cat);
    });
  };

  return (
    <div style={{ opacity: isPending ? 0.6 : 1 }}>
      <ProductGrid category={category} />
    </div>
  );
}
```

---

### Q215: How do you optimize Images with Cloudinary in React for Maximum Core Web Vitals (LCP/CLS) Performance?
**Answer:**

```javascript
export function OptimizedImage({ publicId, alt, width, height, isHero = false }) {
  const url = `https://res.cloudinary.com/my-cloud/image/upload/f_auto,q_auto,w_${width},h_${height},c_fill/${publicId}`;

  return (
    <img
      src={url}
      alt={alt}
      width={width}
      height={height}
      loading={isHero ? 'eager' : 'lazy'}
      fetchPriority={isHero ? 'high' : 'auto'}
      style={{ aspectRatio: `${width}/${height}`, objectFit: 'cover' }}
    />
  );
}
```

---

### Q216: How do you implement Blur-Up Low-Quality Image Placeholders (LQIP) with Cloudinary and React?
**Answer:**

```javascript
import { useState } from 'react';

export function ProgressiveImage({ publicId, alt, width, height }) {
  const [loaded, setLoaded] = useState(false);
  const lqip = `https://res.cloudinary.com/my-cloud/image/upload/w_20,c_fill,e_blur:1000,f_auto,q_10/${publicId}`;
  const full = `https://res.cloudinary.com/my-cloud/image/upload/w_${width},h_${height},c_fill,f_auto,q_auto/${publicId}`;

  return (
    <div style={{ position: 'relative', width, height }}>
      <img src={lqip} alt="" style={{ position: 'absolute', inset: 0, filter: 'blur(20px)', opacity: loaded ? 0 : 1 }} />
      <img src={full} alt={alt} onLoad={() => setLoaded(true)} style={{ position: 'relative', opacity: loaded ? 1 : 0 }} />
    </div>
  );
}
```

---

### Q217: How do you optimize High-Scale Video Streaming in React using Cloudinary Adaptive HLS/DASH Streaming?
**Answer:**

```javascript
import { useEffect, useRef } from 'react';
import Hls from 'hls.js';

export function HlsVideoPlayer({ publicId }) {
  const videoRef = useRef(null);
  const hlsUrl = `https://res.cloudinary.com/my-cloud/video/upload/sp_auto/f_m3u8/${publicId}.m3u8`;

  useEffect(() => {
    if (Hls.isSupported()) {
      const hls = new Hls();
      hls.loadSource(hlsUrl);
      hls.attachMedia(videoRef.current);
      return () => hls.destroy();
    }
  }, [hlsUrl]);

  return <video ref={videoRef} controls playsInline style={{ width: '100%', aspectRatio: '16/9' }} />;
}
```

---

### Q218: How do you use React 19 Native Resource Hints (`preload`, `preinit`, `preconnect`) to supercharge LCP Hero Images and Fonts?
**Answer:**

```javascript
import { preload, preconnect } from 'react-dom';

export function HeroBanner({ publicId }) {
  preconnect('https://res.cloudinary.com');
  preload(`https://res.cloudinary.com/my-cloud/image/upload/f_auto,q_auto,w_1200/${publicId}`, {
    as: 'image',
    fetchPriority: 'high'
  });

  return <img src={`https://res.cloudinary.com/my-cloud/image/upload/f_auto,q_auto,w_1200/${publicId}`} alt="Hero" />;
}
```

---

### Q219: How do you build an IntersectionObserver Video Autoplay/Pause Component to Save CPU and Memory?
**Answer:**

```javascript
import { useEffect, useRef } from 'react';

export function LazyVideo({ src }) {
  const videoRef = useRef(null);

  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) videoRef.current?.play().catch(() => {});
      else videoRef.current?.pause();
    }, { threshold: 0.5 });

    if (videoRef.current) observer.observe(videoRef.current);
    return () => observer.disconnect();
  }, []);

  return <video ref={videoRef} src={src} muted loop playsInline />;
}
```

---

### Q220: How do Uncontrolled Form Inputs with `useRef` eliminate 100% of keystroke re-renders?
**Answer:**

```javascript
import { useRef } from 'react';

export function FastUncontrolledInput({ onSearch }) {
  const inputRef = useRef(null);

  const handleSubmit = (e) => {
    e.preventDefault();
    onSearch(inputRef.current.value); // 0 re-renders during typing!
  };

  return (
    <form onSubmit={handleSubmit}>
      <input ref={inputRef} defaultValue="" />
      <button type="submit">Search</button>
    </form>
  );
}
```

---

### Q221: What is Context Splitting and why is it superior to passing a single monolithic context object?
**Answer:**

```javascript
export const StateContext = createContext(null);
export const DispatchContext = createContext(null);

export function Provider({ children }) {
  const [state, dispatch] = useReducer(reducer, initial);
  return (
    <DispatchContext value={dispatch}>
      <StateContext value={state}>{children}</StateContext>
    </DispatchContext>
  );
}
```

---

### Q222: How does the React 19 `<Activity mode="hidden">` API eliminate Tab Switching Re-mounts?
**Answer:**

```javascript
import { Activity, useState } from 'react';

export function TabContainer() {
  const [tab, setTab] = useState('a');
  return (
    <div>
      <Activity mode={tab === 'a' ? 'visible' : 'hidden'}><HeavyTabA /></Activity>
      <Activity mode={tab === 'b' ? 'visible' : 'hidden'}><HeavyTabB /></Activity>
    </div>
  );
}
```

---

### Q223: What is the "Event Callback Ref" pattern (`useEvent`) and how does it guarantee permanent function identity?
**Answer:**

```javascript
import { useRef, useLayoutEffect, useCallback } from 'react';

export function useEvent(handler) {
  const handlerRef = useRef(handler);
  useLayoutEffect(() => { handlerRef.current = handler; });
  return useCallback((...args) => handlerRef.current(...args), []);
}
```

---

### Q224: How do you prevent Cumulative Layout Shift (CLS) when loading Dynamic React Banners?
**Answer:**

```css
/* Reserve static slot dimensions before async banner loads: */
.dynamic-ad-slot {
  min-height: 250px;
  aspect-ratio: 16 / 9;
  contain-intrinsic-size: 0 250px;
}
```

---

### Q225: What is the Performance Cost of Prop Drilling vs. Context vs. Signals (Zustand)?
**Answer:**

```javascript
// Zustand fine-grained subscription bypasses intermediate component tree:
const userAvatar = useUserStore(s => s.user.avatarUrl); // Re-renders ONLY on avatar change!
```

---

### Q226: How do you configure Cloudinary Client-Side Uploads directly from React without passing through backend servers?
**Answer:**

```javascript
export async function uploadToCloudinary(file) {
  const fd = new FormData();
  fd.append('file', file);
  fd.append('upload_preset', 'unsigned_preset');
  const res = await fetch('https://api.cloudinary.com/v1_1/my-cloud/image/upload', {
    method: 'POST',
    body: fd
  });
  return await res.json();
}
```

---

### Q227: What is Skeleton Dimension Locking and why is it required in Suspense fallbacks?
**Answer:**

```javascript
// Skeleton locked to exact 300px card height:
<Suspense fallback={<div style={{ height: '300px', width: '100%' }} className="skeleton" />}>
  <AsyncCard />
</Suspense>
```

---

### Q228: How do you eliminate re-renders in Window Resize and Scroll Event Handlers?
**Answer:**

```javascript
// Direct CSS variable mutation without React state re-renders:
window.addEventListener('scroll', () => {
  document.documentElement.style.setProperty('--scroll-top', `${window.scrollY}px`);
}, { passive: true });
```

---

### Q229: How do you use the React 19 `useOptimistic` hook with Cloudinary Image Likes/Favorites?
**Answer:**

```javascript
import { useOptimistic, useActionState } from 'react';

export function ImageLikeButton({ initialLikes, imageId, likeAction }) {
  const [likes, setOptimisticLikes] = useOptimistic(initialLikes, (curr, delta) => curr + delta);

  async function handleLike() {
    setOptimisticLikes(1);
    await likeAction(imageId);
  }

  const [, formAction] = useActionState(handleLike, null);
  return <form action={formAction}><button type="submit">❤️ {likes}</button></form>;
}
```

---

### Q230: What is the Enterprise Checklist for Zero-Waste React 19 Production Performance?
**Answer:**

```javascript
// Performance Audit Check in CI/CD:
// 1. React Compiler enabled
// 2. Cloudinary f_auto, q_auto, responsive srcset
// 3. fetchPriority="high" on Hero image
// 4. @tanstack/react-virtual on large tables
```


---

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
