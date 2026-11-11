# 09: React.js, Next.js & Frontend Architecture (Basic → Advanced)

> Resume: React.js, Next.js, TypeScript, TailwindCSS, SCSS, Jest; React admin console for the scheduler; getConsenso (Next.js); System Design Studio (Next.js, React Flow, Mermaid, PostHog); Lighthouse performance work.
> Existing material: [react-interview-showcase/](../react-interview-showcase), [reactjs/](../reactjs).

| Level | Questions |
|---|---|
| Basic | B1–B10 |
| Medium | M1–M12 |
| Advanced | A1–A7 |

---

## Basic

### B1. Virtual DOM and reconciliation?
🎤 "React builds a tree of elements. On a state change it re-renders components into a new tree, **diffs** it against the previous one, and applies the minimal DOM updates. The heuristics: different element types replace the subtree; **keys** identify list items across renders. React 18's Fiber architecture makes rendering interruptible, which enables concurrent features."

### B2. Props vs state?
🎤 "**Props** are inputs passed from the parent, read-only. **State** is data owned by a component that changes over time and triggers re-renders. Derived values should be computed during render, not stored in state. Lift state up to the closest common parent when siblings need it."

### B3. Rules of hooks?
🎤 "Call hooks only at the top level of a component or a custom hook, never inside conditions, loops or nested functions, because React relies on call order to match state to hooks. The ESLint plugin enforces it. Custom hooks (`useJobStatus`) package reusable stateful logic."

### B4. `useEffect`: dependencies and cleanup?
🎤 "Effects synchronize with external systems: subscriptions, timers, imperative APIs. The dependency array lists every reactive value used. Missing deps give stale closures; objects recreated each render cause infinite loops. Return a cleanup function to unsubscribe or abort, for example `AbortController` for fetches, or closing a WebSocket. Don't use effects to derive state or handle events; and for data fetching prefer a library like TanStack Query."

### B5. Why are keys important in lists?
🎤 "Keys tell React which item is which between renders. Use stable unique IDs, not array indexes when the list can reorder, insert or delete. Index keys cause wrong state to stick to the wrong row, such as an input value moving to another item."

### B6. Controlled vs uncontrolled components?
🎤 "**Controlled**: React state is the source of truth for an input's value (`value` + `onChange`), which suits validation and dynamic UIs. **Uncontrolled**: the DOM holds the value, read via a ref, which is simpler and faster for big forms. React Hook Form uses uncontrolled inputs for performance, with schema validation via Zod."

### B7. Next.js rendering modes?
🎤 "**CSR** (client-side), **SSR** (render per request on the server, fresh and SEO-friendly), **SSG** (pre-render at build), **ISR** (static with background revalidation), and with the App Router, **React Server Components** (components that render on the server with zero client JS by default, plus streaming with Suspense). getConsenso uses a mix: static marketing pages for SEO and the 50k monthly visitors, and server or client components for the authenticated app."

### B8. What is JSX?
🎤 "Syntax sugar that compiles to `React.createElement` calls (or the new JSX runtime). Expressions go in `{}`; it's escaped by default, which prevents XSS, unless you use `dangerouslySetInnerHTML`, which I treat as a code-review red flag."

### B9. Context API: when and when not?
🎤 "Context avoids prop drilling for **low-frequency, global** values: theme, auth user, locale, feature flags. It's not a state manager for frequently changing data, because every consumer re-renders when the value changes. Split contexts, memoize the value, or use a store library for hot state."

### B10. Why TypeScript in React?
🎤 "Typed props and state catch errors at compile time, give better IDE refactoring, and make API contracts explicit. I generate API types from OpenAPI (as getConsenso generates TypeScript stubs), so front end and back end can't drift. Use discriminated unions for UI states (`loading | error | success`)."

---

## Medium

### M1. State management choices: Context vs Redux Toolkit vs Zustand vs TanStack Query?
🎤 (~1.5 min) "First split state by type. **Server state** (data from APIs: jobs, bookings) belongs in **TanStack Query** (or RTK Query, SWR): caching, deduplication, background refetch, retries, pagination, optimistic updates. That removes most of the 'global state' people put in Redux. **Client UI state** that's local stays in `useState`/`useReducer`. **Global client state** (auth session, wizard progress across routes): Context for low-frequency values, **Zustand** for simple, fast stores with selectors, **Redux Toolkit** when you need strict patterns, middleware, time-travel devtools and large-team conventions. **URL state** (filters, pagination) goes in the URL so it's shareable. In the scheduler admin console, job lists and run history were server state with polling or refetch; filters lived in the URL; only the auth session was global."

### M2. Memoization: `useMemo`, `useCallback`, `React.memo`: when?
🎤 (~1 min) "Only when profiling shows a cost. `React.memo` skips re-rendering a child when its props are shallow-equal. `useCallback` and `useMemo` keep reference stability so memoized children don't re-render, or cache expensive computations. Overusing them adds complexity and memory for no gain. The **React Compiler** automates much of this now. Better first fixes: move state down, split components, pass children as props, and virtualize long lists."

### M3. Code splitting and lazy loading? ✅
🎤 (~1 min) "Split by route (`React.lazy` + `Suspense`, or Next.js does it automatically per route), lazy-load heavy components (charts, editors, the **React Flow** canvas in System Design Studio, Mermaid rendering) via dynamic `import()`, and prefetch likely next routes. Analyse bundles with a bundle analyzer, remove heavy dependencies (Moment.js → date-fns), and use tree-shakable imports. That was part of the Lighthouse-driven fix in ticketing."

### M4. How do you improve Core Web Vitals? ✅
🎤 (~1.5 min) "**LCP** (largest contentful paint): optimize the hero image (Next.js `<Image>`, preload, modern formats), SSR or SSG for first paint, CDN, cut render-blocking CSS and JS. **INP** (interaction to next paint): break up long tasks, defer non-critical JS, use `useTransition` for expensive updates, avoid heavy synchronous work in handlers, virtualize lists. **CLS** (layout shift): reserve dimensions for images and ads, avoid injecting content above existing content, use font-display strategies. Measure with **Lighthouse** in CI (budgets that fail the build) and **real user monitoring** in production, since lab and field data differ. I used Lighthouse as part of end-to-end performance diagnosis, alongside JMeter on the back end."

### M5. Error boundaries?
🎤 (~45 s) "Class components (or the `react-error-boundary` library) that catch render errors in their subtree and show a fallback instead of a blank screen. Place them per route or widget, report errors to monitoring (Sentry) with the trace ID, and offer retry. They don't catch errors in event handlers or async code; handle those explicitly."

### M6. Testing React apps? ✅
🎤 (~1 min) "**Jest or Vitest + React Testing Library**: test behaviour the way users interact (roles, labels), not implementation details. **MSW** to mock the network at the HTTP layer, including error and streaming cases. **Playwright** for end-to-end critical journeys (login, booking, job pause with confirmation). Accessibility checks with axe. Visual regression for the design system. Coverage thresholds in CI, which I enforced in my OpenHands quality gates. In getConsenso I used Vitest, for example full coverage on the circuit-breaker SDK module."

### M7. Forms at scale: validation and UX?
🎤 (~45 s) "React Hook Form + **Zod** schemas, reusing the same schema types for API payloads, which can be generated from OpenAPI. Async validation (is this cron expression valid?) debounced. Accessible error messages. Disable double-submits and send an idempotency key for critical submits like payments."

### M8. Data fetching patterns and caching with TanStack Query?
🎤 (~1 min) "Query keys model the resource (`['jobs', {status, page}]`); `staleTime` per data type; background refetch on focus; pagination with `keepPreviousData` or infinite queries; **optimistic updates** with rollback on error (for example, show a job as PAUSED instantly, roll back on 409); invalidation after mutations; and suspense mode with streaming in Next.js. Plus retries with backoff, but not for 4xx responses."

### M9. Real-time updates in the UI: WebSocket vs SSE vs polling? ✅
🎤 (~1 min) "**Polling**: simplest, fine at low frequency (job status every 10 seconds). **SSE**: server-to-client streaming over HTTP, automatic reconnect, works through most proxies; ideal for status feeds and LLM token streaming. **WebSocket**: bidirectional, low latency, for chat, collaboration, live tracking at high frequency. For live train movement we used socket-based push with caching and a CDN. Today I'd use managed WebSockets or SSE behind a gateway. Handle reconnection with backoff, resubscription, missed-message catch-up (via a last event ID), and throttle UI updates with `requestAnimationFrame` or batching."

### M10. Accessibility as an architect's concern?
🎤 (~45 s) "WCAG 2.2 AA as an NFR: semantic HTML, keyboard navigation, focus management in modals, ARIA only when needed, colour contrast, labels for inputs, announcing live updates (ARIA live regions for job status). Automated axe checks in CI plus manual screen-reader testing on key journeys. It's also a legal requirement for many EPAM clients."

### M11. Styling approach: Tailwind vs SCSS vs CSS-in-JS? ✅
🎤 (~45 s) "I've used both **Tailwind** and **SCSS**. Tailwind: utility classes, a consistent design-token system, tiny production CSS, fast iteration; my choice for getConsenso and System Design Studio. SCSS modules: good for teams with traditional CSS skills and complex bespoke components. Runtime CSS-in-JS (styled-components) has a cost and conflicts with Server Components, so I avoid it in new Next.js apps."

### M12. Securing a React application?
🎤 (~1 min) "Rely on React's escaping, and never use `dangerouslySetInnerHTML` with untrusted content (sanitize with DOMPurify if unavoidable). A **Content Security Policy**. **Tokens are not stored in localStorage**: prefer a BFF with httpOnly cookies and CSRF protection, or in-memory tokens with PKCE. Validate on the server always, since client checks are UX only. Dependency scanning, Subresource Integrity for third-party scripts, no secrets in front-end env vars (anything `NEXT_PUBLIC_` is public), and hide UI by role but enforce authorization on the API."

---

## Advanced

### A1. Micro-frontends: when, and how?
🎤 (~1.5 min) "When **multiple teams** need to deliver parts of one large UI independently, on different release cadences, sometimes with different frameworks during a migration. Approaches: **build-time composition** (npm packages: simple, but coupled releases), **runtime composition with Module Federation** (independent deploys, shared dependencies), **server-side composition** or edge includes, or **iframes** (strong isolation, poor UX). Costs: duplicated dependencies, consistency of design and state, routing and auth sharing, and performance. Mitigate with a shared design system, a thin app shell owning auth and routing, contracts between micro-frontends (events or a props API), and strict shared-dependency versions. For a single team, a well-modularized monolith in a monorepo (Nx, Turborepo) is better."

### A2. Frontend architecture for the scheduler admin console ✅
🎤 (~1.5 min) "A React + TypeScript SPA. Routes: Jobs, Job Detail (definition, chain view, run history), Runs, Audit, Admin. **Server state** via TanStack Query against the OpenAPI-generated client, so types match the contract. **Real-time status** via polling for lists and SSE for an open job detail. **Authorization**: OIDC PKCE through the BFF, with UI actions (pause, resume, stop, delete) shown by role but enforced by the API. **Optimistic transitions** with rollback on a 409 conflict. **Destructive actions** need a confirmation dialog showing impact (downstream chained jobs). **Large tables** are virtualized with server-side pagination using cursors. **Chained job visualization** uses a graph library like React Flow, the same tool I used in System Design Studio. **Observability**: front-end errors to Sentry with trace IDs, and Web Vitals reported. **Testing**: RTL plus MSW, and Playwright for the 'pause job' journey."

### A3. React 18/19 concurrency features: practical use?
🎤 (~1 min) "`useTransition` and `startTransition` mark updates as non-urgent, so typing stays responsive while a large filtered list re-renders. `useDeferredValue` lags an expensive derived view. **Suspense** for data and code with streaming SSR in Next.js shows the shell immediately and streams slower parts. React 19 adds **Actions** and `useActionState`/`useOptimistic` for forms and mutations, `use()` for promises, and Server Components and Server Actions in Next.js. The architectural impact is less client JS, and loading states declared rather than hand-managed."

### A4. Performance with very large interactive canvases (System Design Studio with React Flow) ✅
🎤 (~1 min) "With hundreds of nodes and edges: keep node components **memoized** and lightweight; store the graph in a store with selectors (React Flow uses Zustand internally), so dragging one node doesn't re-render all; render only what's visible (viewport culling; React Flow's `onlyRenderVisibleElements`); debounce expensive recalculations (layout, cost estimates); offload heavy computation to a **Web Worker**; lazy-load Mermaid and diagram rendering. PostHog analytics showed which features users actually used, which told us where to spend optimization effort."

### A5. Monorepo and design system strategy?
🎤 (~1 min) "A monorepo (Nx or Turborepo with pnpm workspaces) holding apps, a shared UI library (design tokens, accessible components documented in **Storybook** with visual tests), shared API clients generated from OpenAPI, and lint and TS configs. Affected-only builds and tests, remote caching, semantic versioning of shared packages if published. It gives consistency across products and faster CI."

### A6. SSR/RSC vs SPA for an enterprise app: decision criteria?
🎤 (~1 min) "SEO and first-load performance needs (public pages → SSR, SSG or RSC); data freshness; the authentication model (cookies work well with SSR); hosting constraints (a Node server vs static hosting on S3 + CloudFront); team skills; and interactivity level. Internal admin consoles like the scheduler UI are fine as SPAs on a CDN. Public product sites like getConsenso's marketing pages benefit from Next.js static and server rendering."

### A7. Frontend decisions that could be better ✅
🎤 (~45 s) "In early projects we kept server data in Redux with hand-written fetching, caching and loading flags, a lot of code that TanStack Query replaces. We stored tokens in localStorage before moving to a BFF. And performance budgets weren't in CI, so bundle bloat crept in until Lighthouse flagged it. Now budgets fail the build."
