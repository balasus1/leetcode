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
