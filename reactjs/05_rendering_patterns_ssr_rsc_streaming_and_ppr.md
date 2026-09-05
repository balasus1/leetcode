# Part 5: Rendering Architectures, SSR, RSC, Streaming & PPR (Q101 - Q125)

---

### Q101: What are the fundamental Rendering Architectures in modern React and how do they compare?
**Answer:**

```
+──────────────────────────────────────────────────────────────────────────────────────────────────────────+
| Architecture           | Render Location  | Initial Page Load | SEO / TTFB | Client JS Overhead          |
+────────────────────────┼──────────────────┼───────────────────┼────────────┼─────────────────────────────+
| **CSR (Client-Side)**  | Browser          | Slow (Blank HTML) | Poor       | Heavy (All app code)        |
+────────────────────────┼──────────────────┼───────────────────┼────────────┼─────────────────────────────+
| **SSR (Server-Side)**  | Node/Edge Server | Fast HTML paint   | Excellent  | Heavy (Full Hydration)      |
+────────────────────────┼──────────────────┼───────────────────┼────────────┼─────────────────────────────+
| **SSG (Static Gen)**   | Build Time       | Instant (CDN)     | Perfect    | Heavy (Full Hydration)      |
+────────────────────────┼──────────────────┼───────────────────┼────────────┼─────────────────────────────+
| **ISR (Incremental)**  | Server on demand | Instant (CDN)     | Perfect    | Heavy (Full Hydration)      |
+────────────────────────┼──────────────────┼───────────────────┼────────────┼─────────────────────────────+
| **Streaming SSR**      | Server Stream    | Progressive       | Excellent  | Hydrates incrementally      |
| (with Suspense)        | (Chunk by Chunk) | (Shell -> Chunks) | (Low TTFB) | via Selective Hydration     |
+────────────────────────┼──────────────────┼───────────────────┼────────────┼─────────────────────────────+
| **RSC (Server Comp)**  | Server Only      | Instant Stream    | Excellent  | ZERO JS for Server comps    |
+────────────────────────┼──────────────────┼───────────────────┼────────────┼─────────────────────────────+
| **PPR (Partial Prerender)| Build + Stream  | Instant Static    | Optimal    | Only interactive islands    |
+────────────────────────┴──────────────────┴───────────────────┴────────────┴─────────────────────────────+
```

---

### Q102: How does React Streaming SSR with HTML Suspense (`renderToPipeableStream`) work in Node.js?
**Answer:**
`renderToPipeableStream` replaces legacy `renderToString` (which was blocking and all-or-nothing).

**How it Works:**
1. **Immediate Initial Shell**: React immediately flushes the static HTML wrapper (`<html>`, `<head>`, Navbar, Skeleton loaders) to the browser in the first HTTP chunk.
2. **Suspended Trees**: Components wrapped in `<Suspense>` are paused while data is fetched.
3. **Out-of-Order Inlined Streaming**: As soon as slow data arrives from the DB, React emits a `<template>` chunk containing the rendered HTML, followed by an inline `<script>` tag that swaps the skeleton placeholder with the real DOM in-place!

```javascript
// Node.js Express Server
import { renderToPipeableStream } from 'react-dom/server';
import App from './App';

app.get('/', (req, res) => {
  let didError = false;

  const stream = renderToPipeableStream(<App />, {
    bootstrapScripts: ['/bundle.js'],
    onShellReady() {
      // Shell is ready (Headers, Navbar, Fallbacks) -> Send HTTP 200 immediately!
      res.statusCode = didError ? 500 : 200;
      res.setHeader('Content-type', 'text/html');
      stream.pipe(res);
    },
    onShellError(err) {
      // Error in static shell -> Fallback to client-side rendering
      res.statusCode = 500;
      res.send('<!doctype html><p>Loading error...</p>');
    },
    onError(err) {
      didError = true;
      console.error('Streaming SSR Error:', err);
    }
  });
});
```

---

### Q103: What is the RSC Wire Format (Flight Protocol) and how are Server Components serialized?
**Answer:**
React Server Components do **not** return HTML; they return a compact JSON-like serialized stream (**RSC Payload / Flight Format**).

**Example Wire Format Stream:**
```
M1:{"id":"./components/BuyButton.js","chunks":["client-1"],"name":"BuyButton"}
J0:["$","div",null,{"className":"product-card","children":[["$","h1",null,{"children":"iPhone 16"}],["$","$L1",null,{"price":999}]]}]
```
- Lines prefixed with `M`: Client Component Module references (pointers to client bundle chunks).
- Lines prefixed with `J`: Serialized virtual DOM element trees.
- Allows re-rendering the server tree dynamically while **preserving all existing client component state (input focus, open accordions)**!

---

### Q104: What is the "Poisoning" problem in Server Components and how does the `server-only` package prevent it?
**Answer:**
If a developer accidentally imports a server file containing secret API keys or private DB queries into a Client Component (`'use client'`), the private database code will be compiled into the public browser JavaScript bundle!

**Solution (`server-only`)**:
```javascript
// lib/db.js
import 'server-only'; // 🛡️ Throws a build error if imported in client code!
import { PrismaClient } from '@prisma/client';

export const db = new PrismaClient();
```

---

### Q105: How does Hydration work and what are the primary causes of Hydration Mismatches?
**Answer:**
Hydration is the process where React in the browser attaches event listeners and builds the internal Fiber tree on top of existing server-rendered HTML.

**Common Causes of Hydration Mismatch:**
1. Using browser-only APIs during render (`window.innerWidth`, `localStorage.getItem()`).
2. Timestamps / Dates rendered without fixed timezone (`new Date().toLocaleTimeString()`).
3. Invalid HTML nesting (e.g. `<p><div>Block</div></p>` where the browser auto-corrects the DOM, breaking React's matching index).
4. Browser extensions modifying DOM (e.g. Password managers injecting `<svg>` into inputs).

---

### Q106: How do you suppress Hydration Mismatch warnings safely when differences are intentional?
**Answer:**
Use `suppressHydrationWarning` on the specific element (e.g. for dynamic client timestamps):

```javascript
export function LiveClock() {
  return (
    <time dateTime="2026-09-05" suppressHydrationWarning>
      {new Date().toLocaleTimeString()}
    </time>
  );
}
```

---

### Q107: What is the Island Architecture (Astro / Fresh) and how does it compare to React Server Components?
**Answer:**
- **Islands Architecture**: Static HTML page by default. Interactive React components are isolated "islands" hydrated independently with separate JavaScript bundles.
- **React Server Components (RSC)**: A unified component tree where Server and Client components can be deeply interleaved (Server component can render a Client component which renders a Server component via `children`).

---

### Q108: How can a Server Component pass data to a Client Component, and what are the serialization rules?
**Answer:**
Props passed across the Server $\rightarrow$ Client boundary must be **JSON-serializable** or supported primitives:
- **Allowed**: Strings, Numbers, Booleans, Arrays, Plain Objects, Sets, Maps, TypedArrays, Buffers, Promises, React Elements (`JSX`), and Server Actions (`'use server'`).
- **Forbidden**: JavaScript Functions, Symbols, Class instances, DOM elements.

---

### Q109: How does Interleaving Server Components inside Client Components work via the `children` prop pattern?
**Answer:**
A Client Component cannot directly `import` and instantiate a Server Component.
However, a Client Component can receive a Server Component as a **`children` prop**!

```javascript
// components/ClientCollapsible.jsx ('use client')
'use client';
import { useState } from 'react';

export function ClientCollapsible({ children }) {
  const [open, setOpen] = useState(false);
  return (
    <div>
      <button onClick={() => setOpen(!open)}>Toggle Details</button>
      {open && children} {/* Render server-rendered children with 0KB client JS! */}
    </div>
  );
}

// app/page.jsx (Server Component)
import { ClientCollapsible } from './ClientCollapsible';
import { HeavyServerDataWidget } from './HeavyServerDataWidget'; // Server Component

export default function Page() {
  return (
    <ClientCollapsible>
      <HeavyServerDataWidget /> {/* Executed on server, passed as pre-rendered JSX prop */}
    </ClientCollapsible>
  );
}
```

---

### Q110: What is Edge Rendering (Cloudflare Workers / Vercel Edge) and how does `renderToReadableStream` function?
**Answer:**
Edge rendering runs React SSR on V8 isolates located at CDN edge points (within 10ms of users worldwide) without standard Node.js APIs (`fs`, `net`).
Uses standard Web API **`renderToReadableStream`**:

```javascript
import { renderToReadableStream } from 'react-dom/server';
import App from './App';

export default {
  async fetch(request) {
    const stream = await renderToReadableStream(<App />, {
      bootstrapScripts: ['/main.js']
    });
    return new Response(stream, {
      headers: { 'content-type': 'text/html; charset=utf-8' }
    });
  }
};
```

---

### Q111: What is Incremental Static Regeneration (ISR) and On-Demand Revalidation (`revalidatePath` / `revalidateTag`)?
**Answer:**
ISR generates static pages at build time and updates them in the background when requested after a revalidation period.
- **Time-based ISR**: `export const revalidate = 60;` (Regenerates every 60s).
- **On-Demand ISR**: Triggered via webhooks when CMS updates:
  ```javascript
  'use server';
  import { revalidateTag, revalidatePath } from 'next/cache';

  export async function onProductUpdated(productId) {
    revalidateTag('products'); // Purges edge cache for all product pages instantly
  }
  ```

---

### Q112: How do you prevent Waterfall Requests in React Server Component architectures?
**Answer:**
- **Waterfall Anti-pattern (Sequential Awaits)**:
  ```javascript
  const user = await db.getUser(); // Takes 50ms
  const orders = await db.getOrders(user.id); // Takes 50ms (Total: 100ms)
  ```
- **Parallel Fetching**:
  ```javascript
  const [user, globalConfig] = await Promise.all([
    db.getUser(),
    db.getGlobalConfig()
  ]);
  ```

---

### Q113: What is the difference between `<Suspense>` on the Server vs. `<Suspense>` on the Client?
**Answer:**
- **Client `<Suspense>`**: Displays a fallback spinner in the DOM while a dynamic import (`React.lazy`) or client fetch (`use()`) resolves in the browser.
- **Server `<Suspense>`**: Flushes the surrounding page HTML shell immediately, pauses the suspended branch, and streams the finished HTML chunk down the open HTTP connection when ready.

---

### Q114: How does React Server Component Caching (`React.cache`) work and what is its request lifecycle?
**Answer:**
`React.cache()` creates a **per-request memoized function**.
If 5 different components call `getUser(id)` during the same server render pass, the database query executes **only once**.
The cache is automatically garbage collected at the end of the server request, preventing memory leaks between different users.

```javascript
import { cache } from 'react';
import db from '@/lib/db';

export const getUser = cache(async (id) => {
  return await db.user.findUnique({ where: { id } });
});
```

---

### Q115: What is the impact of CSS Delivery on Streaming SSR (Critical CSS Inlining)?
**Answer:**
If HTML chunks stream to the browser before corresponding CSS stylesheets arrive, the user experiences **Flash of Unstyled Content (FOUC)**.
React 19 native `<link rel="stylesheet" precedence="default">` pauses rendering of the streamed HTML chunk until the corresponding CSS has loaded, eliminating FOUC.

---

### Q116: How do you handle Authentication and Cookies inside React Server Components?
**Answer:**
Server Components can read request cookies and headers (read-only), but **cannot set cookies directly** during render (headers are already flushed in streaming).
Setting cookies must be performed inside **Server Actions** or **Route Handlers / Middleware**.

---

### Q117: What is Static Site Generation (SSG) with Dynamic Route Parameters (`generateStaticParams`)?
**Answer:**
Pre-renders dynamic parameterized routes at build time (e.g. `/blog/post-1`, `/blog/post-2`):

```javascript
export async function generateStaticParams() {
  const posts = await db.posts.findMany({ select: { slug: true } });
  return posts.map(p => ({ slug: p.slug }));
}
```

---

### Q118: How does Server-Side Request Deduplication work across `fetch()` calls?
**Answer:**
Modern React frameworks (Next.js) monkey-patch or wrap the global `fetch()` to automatically deduplicate identical `GET` requests within the same render tree pass.

---

### Q119: What are Client-Side Navigations in an RSC Application and why is it faster than standard MPAs?
**Answer:**
On link click (`<Link href="/dashboard">`):
1. Browser does **not** perform a full page reload.
2. React fetches the new route's **RSC Payload stream** over `fetch()`.
3. Merges the new Server Components into the existing DOM tree, preserving client state (e.g. audio player continues playing without interruption!).

---

### Q120: How do you handle Flash of Layout Shift (CLS) when streaming with Suspense?
**Answer:**
Ensure that the `<Suspense fallback={<Skeleton />}>` skeleton component matches the **exact pixel dimensions and aspect ratio** of the loaded content to prevent layout shifts when the HTML chunk arrives.

---

### Q121: What is the difference between Static Export (`output: 'export'`) and Server Runtimes?
**Answer:**
- **Static Export**: Generates purely static HTML/CSS/JS files hostable on GitHub Pages or S3/Nginx (No Node.js server, No Server Actions, No dynamic SSR).
- **Server Runtime**: Requires Node.js or Edge runtime to process Server Actions and streaming on demand.

---

### Q122: How do you build an SEO-optimized Dynamic Open Graph (OG) Image Generator in React?
**Answer:**
Use `@vercel/og` (or Satori) to render standard JSX into PNG images at the Edge dynamically for `<meta property="og:image">`.

---

### Q123: What is the role of React Server Actions in Form Progressive Enhancement?
**Answer:**
Forms with native `<form action={serverAction}>` work even if JavaScript is completely disabled or fails to load on a mobile device (executes standard HTTP POST and redirects), and hydrates to smooth AJAX when JS is active.

---

### Q124: How do you handle Stale Search Crawlers and Social Media Bots with Streaming SSR?
**Answer:**
Crawlers (Googlebot, Bingbot) do not always handle streaming HTML well.
Server framework inspects `user-agent` header:
- If Bot: Wait for `onAllReady` to flush the entire completed HTML in one single blocking response.
- If User Browser: Use `onShellReady` for fast progressive streaming.

---

### Q125: What is the future roadmap of React Server Functions beyond HTTP (WebSockets / gRPC)?
**Answer:**
React is abstracting Server Actions into protocol-agnostic Remote Procedure Calls, allowing Server Actions to execute across WebSocket connections, worker threads, and IPC channels.
