# Part 8: Design Patterns, Architecture, A11y & Security (Q176 - Q200)

---

### Q176: What is the Compound Component Pattern and how do you implement an Accessible Accordion / Tabs component in React 19?
**Answer:**
Compound Components work together to share implicit state and logic while giving consumers full declarative control over JSX layout (similar to HTML `<select>` and `<option>`).

```javascript
import { createContext, useContext, useState } from 'react';

const TabsContext = createContext(null);

// 1. Root Container Component
export function Tabs({ defaultValue, children }) {
  const [activeTab, setActiveTab] = useState(defaultValue);
  return (
    <TabsContext value={{ activeTab, setActiveTab }}>
      <div className="tabs-root">{children}</div>
    </TabsContext>
  );
}

// 2. Tab Trigger Button
Tabs.Trigger = function TabTrigger({ value, children }) {
  const { activeTab, setActiveTab } = useContext(TabsContext);
  const isActive = activeTab === value;

  return (
    <button
      role="tab"
      aria-selected={isActive}
      className={isActive ? 'tab-active' : 'tab-inactive'}
      onClick={() => setActiveTab(value)}
    >
      {children}
    </button>
  );
};

// 3. Tab Content Panel
Tabs.Content = function TabContent({ value, children }) {
  const { activeTab } = useContext(TabsContext);
  if (activeTab !== value) return null;
  return <div role="tabpanel" className="tab-panel">{children}</div>;
};

// Consumer Usage (Clean & Declarative):
function App() {
  return (
    <Tabs defaultValue="overview">
      <div className="tabs-header">
        <Tabs.Trigger value="overview">Overview</Tabs.Trigger>
        <Tabs.Trigger value="analytics">Analytics</Tabs.Trigger>
      </div>
      <Tabs.Content value="overview"><OverviewContent /></Tabs.Content>
      <Tabs.Content value="analytics"><AnalyticsContent /></Tabs.Content>
    </Tabs>
  );
}
```

---

### Q177: What is the Headless Component / Hook Pattern (Radix UI / TanStack Table / React Aria)?
**Answer:**
**Headless UI** separates **logic, state, keyboard navigation, and accessibility (WAI-ARIA)** from **visual styling**.
- The library provides pure hooks or unstyled primitives (`useTable`, `useDialog`, `<Dialog.Root>`).
- The developer applies their own custom styles (Tailwind, CSS Modules) without battling predefined theme CSS.

---

### Q178: How do you build a Polymorphic Component in React with TypeScript (`as` prop)?
**Answer:**
A Polymorphic Component can render as different underlying HTML elements or components (e.g. `<Button as="a" href="..." />` or `<Button as="button" />`) while maintaining strict TypeScript type safety.

```typescript
import React from 'react';

type AsProp<C extends React.ElementType> = {
  as?: C;
};

type PolymorphicProps<C extends React.ElementType, Props = {}> = React.PropsWithChildren<Props & AsProp<C>> &
  Omit<React.ComponentPropsWithoutRef<C>, keyof (Props & AsProp<C>)>;

export function Button<C extends React.ElementType = 'button'>({
  as,
  children,
  ...restProps
}: PolymorphicProps<C, { variant?: 'primary' | 'secondary' }>) {
  const Component = as || 'button';
  return <Component {...restProps}>{children}</Component>;
}

// Usage with 100% Type Safety:
<Button as="a" href="https://google.com" target="_blank">External Link</Button>
<Button as="button" onClick={() => console.log('clicked')}>Action Button</Button>
```

---

### Q179: How do XSS (Cross-Site Scripting) vulnerabilities happen in React and how do you prevent them?
**Answer:**
By default, React escapes all strings inside JSX expressions (`<div>{userInput}</div>`), converting `<script>` to `&lt;script&gt;`.

**Vulnerabilities occur when:**
1. **`dangerouslySetInnerHTML`**:
   ```javascript
   // 🚨 VULNERABLE:
   <div dangerouslySetInnerHTML={{ __html: userSuppliedMarkdown }} />

   // ✅ SECURE: Sanitize with DOMPurify first:
   import DOMPurify from 'dompurify';
   <div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(userSuppliedMarkdown) }} />
   ```
2. **`javascript:` URLs in href attributes**:
   ```javascript
   // 🚨 VULNERABLE: <a href="javascript:stealData()">
   <a href={userLink}>Profile</a>

   // ✅ SECURE: Validate URL protocol
   const isSafeUrl = /^https?:\/\//i.test(userLink);
   <a href={isSafeUrl ? userLink : '#'}>Profile</a>
   ```

---

### Q180: What is Focus Management and how do you implement a Focus Trap for Accessible Modal Dialogs?
**Answer:**
When an accessible modal opens:
1. Focus must move to the modal container.
2. Pressing `Tab` or `Shift+Tab` must cycle **only through elements inside the modal** (cannot escape to background page).
3. Pressing `Escape` must close the modal and return focus to the trigger button.

```javascript
import { useEffect, useRef } from 'react';

export function useFocusTrap(isActive) {
  const containerRef = useRef(null);

  useEffect(() => {
    if (!isActive || !containerRef.current) return;

    const element = containerRef.current;
    const focusableElements = element.querySelectorAll(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );
    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    firstElement?.focus();

    const handleKeyDown = (e) => {
      if (e.key !== 'Tab') return;

      if (e.shiftKey) {
        if (document.activeElement === firstElement) {
          lastElement?.focus();
          e.preventDefault();
        }
      } else {
        if (document.activeElement === lastElement) {
          firstElement?.focus();
          e.preventDefault();
        }
      }
    };

    element.addEventListener('keydown', handleKeyDown);
    return () => element.removeEventListener('keydown', handleKeyDown);
  }, [isActive]);

  return containerRef;
}
```

---

### Q181: What is the Render Props Pattern and when is it still useful in modern React?
**Answer:**
A component passes dynamic internal state to a child function: `<DataProvider render={(data) => <View data={data} />} />`.
While custom hooks have replaced 90% of render props, Render Props are still valuable for **flexible UI slot customization in headless component libraries**.

---

### Q182: What is Prop Drilling and what are the best techniques to eliminate it?
**Answer:**
Passing props down 5+ levels of intermediate components that don't need them.
**Solutions:**
1. **Component Composition**: Pass fully formed elements via `children` or named slots (`<Layout sidebar={<UserSidebar />} />`).
2. **Context / Slice Stores**: Use Zustand or Context for deeply nested state.

---

### Q183: What is the Controlled vs. Uncontrolled Component pattern in React forms?
**Answer:**
- **Controlled Component**: Form input value is driven strictly by React state (`value={state}` + `onChange={setState}`). Re-renders on every keystroke.
- **Uncontrolled Component**: Form input value is managed directly by the browser DOM (`defaultValue="init"`). Read on submit via `useRef` or `FormData`. Faster for huge forms.

---

### Q184: How do you build an Accessible Live Region (`aria-live`) for dynamic screen reader announcements?
**Answer:**

```javascript
export function LiveAnnouncer({ message, politeness = 'polite' }) {
  return (
    <div
      role="status"
      aria-live={politeness}
      aria-atomic="true"
      style={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0,0,0,0)' }}
    >
      {message}
    </div>
  );
}
```

---

### Q185: What is the Higher-Order Component (HOC) Pattern and what are its drawbacks?
**Answer:**
A function that takes a component and returns an enhanced component (`withAuth(Dashboard)`).
**Drawbacks**: Prop name collisions, wrapper hell in DevTools, complex TypeScript typing. Replaced almost entirely by Custom Hooks.
