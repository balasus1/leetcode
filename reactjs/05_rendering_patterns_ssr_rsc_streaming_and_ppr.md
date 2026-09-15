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
