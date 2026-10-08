# 01. Predict the Output: Java Inheritance Code Snippet with Static Methods

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "In Java, **static methods cannot be overridden; they are hidden**. 
>
> When a subclass declares a static method with the exact same signature as a static method in its parent class, this is known as **Method Hiding**, not Method Overriding.
>
> Static method resolution is determined at **compile-time based on the reference type** of the variable using the `invokestatic` bytecode instruction. In contrast, instance methods are resolved at **runtime based on the actual object instance in the heap** via dynamic method dispatch (`invokevirtual`).
>
> Therefore, if we assign a `Child` object to a `Parent` reference variable (e.g. `Parent obj = new Child()`), calling `obj.staticMethod()` will execute the `Parent`'s static method, while calling `obj.instanceMethod()` will dynamically execute the `Child`'s overridden instance method."

---

## 🧠 Method Hiding vs Method Overriding Breakdown

| Characteristic | Static Methods (Method Hiding) | Instance Methods (Method Overriding) |
|---|---|---|
| **Binding Time** | **Compile-Time** (Static / Early Binding) | **Runtime** (Dynamic / Late Binding) |
| **Bytecode Instruction** | `invokestatic` | `invokevirtual` |
| **Resolved By** | Type of the **Reference Variable** | Type of the **Actual Heap Object** |
| **Polymorphism** | No runtime polymorphism | True runtime polymorphism |
| **`@Override` Annotation** | Compiler error if applied | Valid & recommended |

---

## 💻 Code Snippet & Output Prediction

```java
package round2;

class Parent {
    public static void printStatic() {
        System.out.println("Parent static method");
    }

    public void printInstance() {
        System.out.println("Parent instance method");
    }
}

class Child extends Parent {
    // Method Hiding (NOT overriding)
    public static void printStatic() {
        System.out.println("Child static method");
    }

    // Method Overriding
    @Override
    public void printInstance() {
        System.out.println("Child instance method");
    }
}

public class StaticMethodHidingDemo {
    public static void main(String[] args) {
        Parent parentRef = new Parent();
        Child childRef = new Child();
        Parent polymorphicRef = new Child(); // Upcasting

        System.out.println("--- 1. Parent Reference pointing to Parent Object ---");
        parentRef.printStatic();    // Output: Parent static method
        parentRef.printInstance();  // Output: Parent instance method

        System.out.println("\n--- 2. Child Reference pointing to Child Object ---");
        childRef.printStatic();     // Output: Child static method
        childRef.printInstance();   // Output: Child instance method

        System.out.println("\n--- 3. Parent Reference pointing to Child Object (The Interview Trap!) ---");
        polymorphicRef.printStatic();   // Output: Parent static method  <-- COMPILE-TIME BINDING
        polymorphicRef.printInstance(); // Output: Child instance method   <-- RUNTIME DISPATCH
    }
}
```

### 🖨️ Actual Output:
```text
--- 1. Parent Reference pointing to Parent Object ---
Parent static method
Parent instance method

--- 2. Child Reference pointing to Child Object ---
Child static method
Child instance method

--- 3. Parent Reference pointing to Child Object (The Interview Trap!) ---
Parent static method
Child instance method
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "Can you override a static method as an instance method or vice versa?"
**Answer:**
> "No. If a subclass tries to declare an instance method with the same name and signature as a parent's static method (or a static method matching a parent's instance method), the Java compiler throws a **Compile Error: 'Instance method cannot override static method'**."

### 2. "Why is calling static methods on object instances considered bad practice?"
**Answer:**
> "Because it misleads developers into thinking dynamic polymorphism applies. Best practice is always to invoke static methods directly via class name: `Parent.printStatic()` or `Child.printStatic()`."
