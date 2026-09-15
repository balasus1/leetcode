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
