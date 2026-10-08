# 15. Angular Architecture, Component Lifecycle & Directives (Full-Stack Java)

Comprehensive guides, 60-second verbal scripts, component structures, and TypeScript/HTML code for Full-Stack Java Developers.

---

## 📑 Topics Index
1. [Angular Lifecycle Hooks: Complete Execution Sequence](#1-angular-lifecycle-hooks-complete-execution-sequence)
2. [Structure of an Angular Component](#2-structure-of-an-angular-component)
3. [Directives in Angular (Structural vs Attribute vs Custom)](#3-directives-in-angular)

---

### 1. Angular Lifecycle Hooks: Complete Execution Sequence

#### 🎙️ 60-Second Verbal Script
> "An Angular component undergoes a managed lifecycle from instantiation to destruction, triggering specific lifecycle hook interfaces in a deterministic order:
>
> 1. **`ngOnChanges(changes: SimpleChanges)`**: Triggered first and whenever an `@Input()` data-bound property value changes.
> 2. **`ngOnInit()`**: Called once after the first `ngOnChanges`. This is the **primary place to initialize component state, subscribe to backend REST API services**, and setup data.
> 3. **`ngDoCheck()`**: Invoked during every change detection run for custom change detection algorithms.
> 4. **`ngAfterContentInit()` & `ngAfterContentChecked()`**: Called after Angular projects external content into the component via `<ng-content>`.
> 5. **`ngAfterViewInit()` & `ngAfterViewChecked()`**: Called after the component's view and all child views/DOM elements are fully rendered. This is where you access `@ViewChild()` or DOM elements safely.
> 6. **`ngOnDestroy()`**: Called immediately before Angular destroys the component. Crucial for **unsubscribing from RxJS Observables, clearing intervals/timers, and detaching event listeners** to prevent memory leaks."

```
      [Constructor] (Dependency Injection ONLY)
           │
           ▼
     [ngOnChanges] (Input properties initialized/changed)
           │
           ▼
      [ngOnInit] (Primary Backend API Data Fetching)
           │
           ▼
     [ngDoCheck]
           │
           ▼
 [ngAfterContentInit] ──► [ngAfterContentChecked] (Content Projection)
           │
           ▼
   [ngAfterViewInit]  ──► [ngAfterViewChecked] (DOM & Child Views Ready)
           │
           ▼ (Component Teardown)
     [ngOnDestroy] (RxJS Unsubscriptions & Cleanup)
```

---

### 2. Structure of an Angular Component

#### 🎙️ 60-Second Verbal Script
> "An Angular component is the fundamental building block of the user interface. It consists of 4 distinct parts:
>
> 1. **Component Decorator (`@Component`)**: Supplies metadata configuration, including `selector` (custom HTML tag), `templateUrl` / `template` (HTML layout), `styleUrls` (scoped CSS/SCSS), and `standalone: true` (in modern Angular 17+).
> 2. **TypeScript Class**: Houses application state, member properties, event handlers, and business logic.
> 3. **HTML Template**: Defines the layout using Angular template syntax (property binding `[src]`, event binding `(click)`, two-way binding `[(ngModel)]`, and control flow `@if` / `@for`).
> 4. **Scoped Styles**: CSS/SCSS encapsulated directly to this component view."

```typescript
import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ProductService, Product } from './product.service';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-card.component.html',
  styleUrls: ['./product-card.component.scss']
})
export class ProductCardComponent implements OnInit, OnDestroy {
  @Input() category: string = 'Electronics';
  products: Product[] = [];
  isLoading: boolean = true;
  private sub?: Subscription;

  constructor(private productService: ProductService) {}

  ngOnInit(): void {
    this.sub = this.productService.getProductsByCategory(this.category).subscribe({
      next: (data) => {
        this.products = data;
        this.isLoading = false;
      },
      error: (err) => console.error('Failed to load products', err)
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe(); // Prevent memory leaks
  }
}
```

---

### 3. Directives in Angular (Structural vs Attribute vs Custom)

#### 🎙️ 60-Second Verbal Script
> "In Angular, **Directives** add custom behavior or modify DOM elements. They are categorized into 3 types:
>
> 1. **Component Directives**: Components are directives with an attached HTML template.
> 2. **Structural Directives**: Modify the **DOM layout** by adding, removing, or manipulating elements. They use the `*` prefix (or modern Angular `@if` / `@for` control flow):
>    - `*ngIf` / `@if (isLoaded)`: Conditionally renders DOM elements.
>    - `*ngFor="let p of products"` / `@for (p of products; track p.id)`: Iterates over collections.
>    - `*ngSwitch`: Evaluates multi-branch conditions.
> 3. **Attribute Directives**: Modify the **appearance or behavior of an existing DOM element** without altering the DOM tree:
>    - `[ngClass]="{ 'active-row': isSelected }"`: Dynamically toggles CSS classes.
>    - `[ngStyle]="{ 'color': status === 'FAILED' ? 'red' : 'green' }"`: Dynamically applies inline styles.
> 4. **Custom Attribute Directives**: Created using `@Directive` to encapsulate reusable UI behaviors, such as auto-focusing inputs or highlight-on-hover."

```html
<!-- HTML Template Demonstration -->
<div class="container">
  <!-- Structural Directive: Conditional Rendering -->
  <div *ngIf="isLoading; else dataBlock" class="spinner">
    Loading products from Spring Boot API...
  </div>

  <ng-template #dataBlock>
    <!-- Structural Directive: List Iteration -->
    <div *ngFor="let item of products" class="product-item">
      <h3>{{ item.name }}</h3>
      
      <!-- Attribute Directive: Dynamic Class & Style -->
      <span [ngClass]="{ 'in-stock': item.stock > 0, 'out-of-stock': item.stock === 0 }"
            [ngStyle]="{ 'font-weight': 'bold' }">
        {{ item.stock > 0 ? 'In Stock' : 'Out of Stock' }}
      </span>
    </div>
  </ng-template>
</div>
```
