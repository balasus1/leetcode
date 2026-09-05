# Part 9: Production Scaling, Testing & Enterprise SaaS Architecture (Q201 - Q225)

---

### Q201: How do you write robust Unit and Integration Tests using Vitest and React Testing Library (RTL)?
**Answer:**
RTL tests components **from the end-user perspective** (interacting with rendered buttons, labels, and roles) rather than testing internal implementation details (state, hooks).

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

    // Query by Accessible Roles and Labels (User Perspective)
    const emailInput = screen.getByLabelText(/email/i);
    const passwordInput = screen.getByLabelText(/password/i);
    const submitBtn = screen.getByRole('button', { name: /sign in/i });

    await user.type(emailInput, 'alice@enterprise.com');
    await user.type(passwordInput, 'SecretPassword123!');
    await user.click(submitBtn);

    expect(handleSubmit).toHaveBeenCalledTimes(1);
    expect(handleSubmit).toHaveBeenCalledWith({
      email: 'alice@enterprise.com',
      password: 'SecretPassword123!'
    });
  });
});
```

---

### Q202: How do you mock network requests cleanly using Mock Service Worker (MSW v2)?
**Answer:**
MSW intercepts network requests at the **browser/Node network service worker layer**, allowing exact same fetch/axios code to run during tests without mocking API modules.

```javascript
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

export const handlers = [
  http.get('https://api.domain.com/user/profile', () => {
    return HttpResponse.json({ id: 'u_1', name: 'Alice', plan: 'Enterprise' });
  }),
  http.post('https://api.domain.com/checkout', async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({ orderId: 'ord_99', status: 'PAID' }, { status: 201 });
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

test('User can complete multi-step checkout with optimistic update', async ({ page }) => {
  await page.goto('https://staging.store.com/products/iphone-16');

  // Verify LCP hero loaded
  await expect(page.getByRole('heading', { name: 'iPhone 16' })).toBeVisible();

  // Add to cart (Triggers React 19 Action)
  await page.getByRole('button', { name: 'Add to Cart' }).click();

  // Verify Optimistic UI badge updates immediately
  const cartBadge = page.getByTestId('cart-count');
  await expect(cartBadge).toHaveText('1');

  // Navigate to checkout
  await page.getByRole('link', { name: 'Checkout' }).click();
  await expect(page).toHaveURL(/.*checkout/);
});
```

---

### Q205: How do you build an Enterprise Multi-Tenant White-Label Design System with CSS Variables and Design Tokens?
**Answer:**
Use semantic design tokens (`--color-primary`, `--radius-card`) mapped to tenant themes.
Tenants inject a dynamic theme configuration on load; the entire component library adapts without altering React component code:

```css
/* Base Theme */
:root {
  --primary: 220 90% 56%;
  --radius: 8px;
}

/* Tenant A Brand */
[data-tenant="tenant-acme"] {
  --primary: 142 76% 36%;
  --radius: 16px;
}
```

---

### Q206: How do you monitor Real-User Monitoring (RUM) metrics and Sentry error tracking in React?
**Answer:**
Wrap the root tree in Sentry ErrorBoundary and monitor interaction transactions:

```javascript
import * as Sentry from '@sentry/react';

Sentry.init({
  dsn: 'https://key@sentry.io/123',
  integrations: [Sentry.browserTracingIntegration(), Sentry.replayIntegration()],
  tracesSampleRate: 0.1,
  replaysSessionSampleRate: 0.1
});

export const AppWithErrorTracking = Sentry.withErrorBoundary(App, {
  fallback: <p>An unexpected error occurred. Our engineering team has been notified.</p>
});
```

---

### Q207: How do you implement Feature Flags and Canary Rollouts (LaunchDarkly) in React?
**Answer:**
Evaluate feature flags inside custom hooks or server components:

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
Run bundle visualizers in CI/CD. Identify:
1. Accidental duplicate packages (e.g. `lodash` and `lodash-es`).
2. Heavy libraries (`moment.js` $\rightarrow$ replace with `date-fns` / native `Intl`).
3. Large icons / animations bundled in the main entry chunk.

---

### Q209: What is the difference between Shallow Rendering and Full DOM Rendering in tests?
**Answer:**
- **Shallow Rendering (Enzyme - Obsolete)**: Renders only the component itself, without rendering any of its children. Fragile and tests implementation details.
- **Full DOM Rendering (React Testing Library)**: Renders the complete child tree in a simulated jsdom environment, verifying real user interactions and output.

---

### Q210: What are the Architectural Best Practices for designing a Production-Grade, Fault-Tolerant Enterprise React 19 Application?
**Answer:**

```
+─────────────────────────────────────────────────────────────────────────────+
|                         Enterprise React 19 Blueprint                       |
+──────────────────────────────────────┬──────────────────────────────────────+
| 1. Compiler-Driven Core              | React Compiler enabled for automatic |
|                                      | fine-grained memoization             |
+──────────────────────────────────────┼──────────────────────────────────────+
| 2. Hybrid RSC & Streaming SSR        | Server Components for 0KB DB queries |
|                                      | Streaming Suspense for instant TTFB  |
+──────────────────────────────────────┼──────────────────────────────────────+
| 3. Action-Driven Mutations           | useActionState + useOptimistic for   |
|                                      | instant UI and automatic rollbacks   |
+──────────────────────────────────────┼──────────────────────────────────────+
| 4. Bifurcated State Architecture     | Server State: TanStack Query         |
|                                      | Client State: Lightweight Zustand    |
+──────────────────────────────────────┼──────────────────────────────────────+
| 5. Resilient Microfrontends / Modular| Module Federation / Single-SPA with  |
|                                      | isolated ErrorBoundaries and shared  |
|                                      | singletons                           |
+──────────────────────────────────────┼──────────────────────────────────────+
| 6. Rigorous Observability & Testing  | RTL + Vitest + Playwright E2E with   |
|                                      | Core Web Vitals RUM telemetry        |
+──────────────────────────────────────┴──────────────────────────────────────+
```
