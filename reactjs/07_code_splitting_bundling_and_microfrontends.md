# Part 7: Code Splitting, Bundling & Microfrontends (Q151 - Q175)

---

### Q151: What is a Microfrontend Architecture and what problems does it solve for large enterprise teams?
**Answer:**
A **Microfrontend** breaks a monolithic frontend application into independently developed, tested, and deployed frontend sub-applications owned by distinct cross-functional teams.

```
+─────────────────────────────────────────────────────────────────────────────+
|                         Container / Shell Application                       |
|          (Global Authentication, Top Navbar, Shell Router, Design Tokens)   |
+──────────────────────────┬───────────────────────────┬──────────────────────+
                           │                           │
+──────────────────────────▼───+           +───────────▼──────────────────────+
|   Billing Microfrontend      |           |     Analytics Microfrontend      |
|   (Team A - React 19 / Vite) |           |     (Team B - React 18 / Next.js)|
|   Deployed to: S3/CloudFront |           |     Deployed to: Vercel / Edge   |
+──────────────────────────────+           +──────────────────────────────────+
```

**Key Advantages:**
1. **Independent CI/CD**: Team A can deploy a hotfix to the Billing page in 2 minutes without building or deploying the rest of the application.
2. **Autonomous Tech Stacks**: Microfrontends can upgrade dependencies (e.g. React 19) independently.
3. **Fault Isolation**: If the Analytics widget crashes, an Error Boundary prevents it from crashing the main checkout shell.

---

### Q152: How does Webpack 5 Module Federation work under the hood?
**Answer:**
Module Federation enables a JavaScript application to dynamically load asynchronous module chunks from a **remote build at runtime**, sharing common libraries (like `react`, `react-dom`) to avoid duplicate downloads.

```javascript
// host (Shell App) - webpack.config.js
const { ModuleFederationPlugin } = require('webpack').container;

module.exports = {
  plugins: [
    new ModuleFederationPlugin({
      name: 'shell_app',
      remotes: {
        billingApp: 'billingApp@https://billing.enterprise.com/remoteEntry.js'
      },
      shared: {
        react: { singleton: true, requiredVersion: '^19.0.0' },
        'react-dom': { singleton: true, requiredVersion: '^19.0.0' }
      }
    })
  ]
};

// Inside Shell React Component:
import { lazy, Suspense } from 'react';
const RemoteBillingInvoice = lazy(() => import('billingApp/InvoiceWidget'));

export function BillingPage() {
  return (
    <Suspense fallback={<p>Loading remote billing widget...</p>}>
      <RemoteBillingInvoice invoiceId="inv_9981" />
    </Suspense>
  );
}
```

---

### Q153: What is the `shared: { singleton: true }` rule in Module Federation and why is it critical for React?
**Answer:**
If two microfrontends bundle separate copies of React into memory:
- React's internal global Dispatcher (`ReactCurrentDispatcher`) will be initialized twice.
- Calling hooks (`useState`, `useEffect`) across microfrontend boundaries throws:
  `Invalid hook call. Hooks can only be called inside the body of a function component.`
- **`singleton: true`** forces Webpack to load and initialize **exactly one shared copy of React** in browser memory.

---

### Q154: How does Single-SPA compare with Webpack Module Federation?
**Answer:**
- **Single-SPA**: A client-side router/orchestrator that mounts and unmounts microfrontends on URL route changes using life-cycle contracts (`bootstrap`, `mount`, `unmount`). Works across different frameworks (React + Vue + Angular on the same page).
- **Module Federation**: A build-time/runtime module resolution protocol that allows sharing components, utilities, and stores at the component level inside the same React tree.

---

### Q155: How do Microfrontends communicate without Tight Coupling (EventBus / Custom Events / BroadcastChannel)?
**Answer:**
Microfrontends should **never** share direct in-memory JavaScript references or tightly coupled stores.

```javascript
// Universal Custom Event Bus Pattern
export const MicroAppEvents = {
  emit(event, data) {
    window.dispatchEvent(new CustomEvent(`mfe:${event}`, { detail: data }));
  },
  on(event, callback) {
    const handler = (e) => callback(e.detail);
    window.addEventListener(`mfe:${event}`, handler);
    return () => window.removeEventListener(`mfe:${event}`, handler);
  }
};

// Microfrontend A emits:
MicroAppEvents.emit('USER_LOGGED_IN', { userId: 'u_101', name: 'Alice' });

// Microfrontend B listens in React:
useEffect(() => {
  return MicroAppEvents.on('USER_LOGGED_IN', (user) => {
    console.log('Synchronized user in Microfrontend B:', user);
  });
}, []);
```

---

### Q156: How do you handle Route-Based Code Splitting with `React.lazy` and dynamic `import()`?
**Answer:**

```javascript
import { lazy, Suspense } from 'react';
import { Routes, Route } from 'react-router-dom';

// Split routes into separate network chunks
const Dashboard = lazy(() => import('./routes/Dashboard'));
const Settings = lazy(() => import('./routes/Settings'));
const Billing = lazy(() => import('./routes/Billing'));

export function AppRouter() {
  return (
    <Suspense fallback={<div className="page-skeleton-loader" />}>
      <Routes>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/settings" element={<Settings />} />
        <Route path="/billing" element={<Billing />} />
      </Routes>
    </Suspense>
  );
}
```

---

### Q157: How do you implement Component-Level Dynamic Pre-fetching on Mouse Hover?
**Answer:**
Waiting for the user to click a link before starting to download the code chunk adds a 200–500ms delay.
**Pre-fetch on Hover** initiates the chunk download the instant the user moves their mouse over the navigation button:

```javascript
const loadModal = () => import('./HeavyAnalyticsModal');

export function AnalyticsTriggerButton() {
  const [showModal, setShowModal] = useState(false);

  return (
    <div>
      <button
        onMouseEnter={loadModal} // Pre-fetch JS chunk when hovered!
        onFocus={loadModal}      // Pre-fetch on keyboard focus
        onClick={() => setShowModal(true)}
      >
        View Detailed Analytics
      </button>
      {showModal && <LazyAnalyticsModal onClose={() => setShowModal(false)} />}
    </div>
  );
}
```

---

### Q158: What is Native Federation (ESM / Import Maps) and how does it work without Webpack?
**Answer:**
Modern browsers natively support ES Modules and **Import Maps**.
**Native Federation** uses browser-native `import('https://cdn.../widget.mjs')` and import maps, allowing Microfrontends in **Vite, Rollup, and ESBuild** without Webpack runtime overhead.

```html
<!-- Import Map in index.html -->
<script type="importmap">
{
  "imports": {
    "react": "https://esm.sh/react@19.0.0",
    "react-dom": "https://esm.sh/react-dom@19.0.0",
    "remoteOrderApp/Widget": "https://orders.enterprise.com/dist/widget.js"
  }
}
</script>
```

---

### Q159: How do you isolate CSS and prevent style collisions across Microfrontends (Shadow DOM, CSS Modules, Tailwind Prefix)?
**Answer:**

1. **Shadow DOM Encapsulation**: Completely isolates styles; host styles cannot penetrate shadow root.
2. **Tailwind CSS Prefixing**: Configure `prefix: 'mfe-billing-'` in `tailwind.config.js`.
3. **CSS Modules / Scoped BEM**: Automatically hashes class names (`button_mfe_billing__x8z`).

---

### Q160: How do you handle Global Error Boundaries in Microfrontend architectures?
**Answer:**
Wrap **every remote microfrontend mount point** in an isolated `<ErrorBoundary>` with a retry fallback so that a crash in a third-party microfrontend does not break the host shell.

```javascript
export function RemoteWidgetWrapper({ children, widgetName }) {
  return (
    <ErrorBoundary
      fallbackRender={({ error, resetErrorBoundary }) => (
        <div className="widget-error-card">
          <h4>Failed to load {widgetName}</h4>
          <p>{error.message}</p>
          <button onClick={resetErrorBoundary}>Retry</button>
        </div>
      )}
    >
      <Suspense fallback={<WidgetSkeleton />}>
        {children}
      </Suspense>
    </ErrorBoundary>
  );
}
```

---

### Q161: What is Chunk Splitting Strategy (`splitChunks`) in enterprise Webpack/Vite configs?
**Answer:**
Configure chunk boundaries:
1. **`vendor` chunk**: Long-lived dependencies (`react`, `react-dom`, `lodash`) cached for months.
2. **`common` chunk**: Code shared by 2 or more routes.
3. **`async` / `route` chunks**: Code loaded strictly on demand.

---

### Q162: What is Version Skew and how do you handle backward compatibility in Microfrontend APIs?
**Answer:**
When Shell App is running version 1.0 and Remote App deploys version 2.0 with modified props:
- Always pass versioned payload envelopes: `{ schemaVersion: '2.0', payload: { ... } }`.
- Maintain backward-compatible prop defaults in remote components.

---

### Q163: How do you test Microfrontends in isolation vs. End-to-End integration?
**Answer:**
- **Unit / Isolation Test**: Run tests inside the microfrontend repo with mock container shell props.
- **E2E Integration Test**: Run Playwright/Cypress against a local Docker-compose environment running all microfrontends together.

---

### Q164: What is the overhead of Microfrontends and when is it an ANTI-PATTERN?
**Answer:**
**Anti-Pattern when**:
- Small teams (<20 engineers).
- Simple applications that don't need independent deployments.
- Microfrontends lead to duplicated network bundles, complex CI/CD orchestration, CSS bleed, and increased debugging complexity.

---

### Q165: How do you implement Cross-Microfrontend Global Authentication state?
**Answer:**
Store JWT tokens in `httpOnly` secure cookies on the root domain (`.enterprise.com`), or share an in-memory authentication broadcast manager in the Container Shell that passes tokens via custom events or top-level props.
