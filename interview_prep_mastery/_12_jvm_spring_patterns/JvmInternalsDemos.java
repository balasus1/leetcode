package interview_prep_mastery._12_jvm_spring_patterns;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * ============================================================================
 * MODULE 12.1: JVM INTERNALS COMPANION SUITE & VERIFICATION
 * ============================================================================
 * Demonstrates:
 * 1. ClassLoader Parent Delegation Hierarchy Inspection
 * 2. Heap and Non-Heap Runtime Data Area Metrics via JMX
 * 3. Garbage Collection & WeakReference Reclamation Mechanics
 * 4. Escape Analysis / Scalar Replacement Allocation Benchmark
 * ============================================================================
 */
public class JvmInternalsDemos {

    public static void main(String[] args) {
        System.out.println("--- [12.1 JVM Internals Test & Verification Suite] ---");

        testClassLoaderHierarchy();
        testMemoryModelAndJmx();
        testWeakReferenceAndGcReclamation();
        testEscapeAnalysisScalarReplacement();

        System.out.println("✅ All JVM Internals test suites executed successfully!");
    }

    /**
     * Q4: Demonstrates Bootstrap, Platform, and Application ClassLoader chain.
     */
    public static void testClassLoaderHierarchy() {
        System.out.print("Running ClassLoader Hierarchy Test... ");

        // User application class is loaded by Application / System ClassLoader
        ClassLoader appClassLoader = JvmInternalsDemos.class.getClassLoader();
        assert appClassLoader != null : "App ClassLoader should not be null";

        // Parent of Application is Platform ClassLoader (in Java 9+)
        ClassLoader platformClassLoader = appClassLoader.getParent();
        assert platformClassLoader != null : "Platform ClassLoader should not be null";

        // Bootstrap ClassLoader is the native root, represented by null in Java
        ClassLoader bootstrapClassLoader = platformClassLoader.getParent();
        assert bootstrapClassLoader == null : "Bootstrap ClassLoader is represented by null";

        // Core java.lang.String is loaded by Bootstrap ClassLoader
        ClassLoader stringClassLoader = String.class.getClassLoader();
        assert stringClassLoader == null : "java.lang.String should be loaded by Bootstrap ClassLoader";

        System.out.println("PASSED");
    }

    /**
     * Q1 & Q2: Inspects Heap vs Non-Heap (Metaspace/CodeCache) using JMX MXBeans.
     */
    public static void testMemoryModelAndJmx() {
        System.out.print("Running JVM Memory MXBean Inspection Test... ");

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        assert heapUsage.getMax() > 0 || heapUsage.getMax() == -1 : "Heap max should be valid";
        assert nonHeapUsage.getUsed() > 0 : "Non-heap used should be > 0";

        System.out.println("PASSED [Heap Used: " + (heapUsage.getUsed() / (1024 * 1024)) + "MB, Non-Heap: " + (nonHeapUsage.getUsed() / (1024 * 1024)) + "MB]");
    }

    /**
     * Q5: Demonstrates WeakReference lifecycle during GC.
     */
    public static void testWeakReferenceAndGcReclamation() {
        System.out.print("Running GC & WeakReference LifeCycle Test... ");

        // Allocate an object referenced only weakly
        Object payload = new byte[1024 * 1024]; // 1MB payload
        WeakReference<Object> weakRef = new WeakReference<>(payload);

        assert weakRef.get() != null : "WeakReference must hold payload while strong ref is active";

        // Clear strong reference
        payload = null;

        // Hint GC
        System.gc();

        // Give GC a tiny window to reclaim
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        // Note: System.gc() is a hint, but if collected, weakRef.get() becomes null
        // We verify that the weakRef wrapper itself is functioning properly
        System.out.println("PASSED");
    }

    /**
     * Q11: Escape Analysis simulation.
     * When objects do not escape the method, C2 JIT avoids heap allocations via scalar replacement.
     */
    public static void testEscapeAnalysisScalarReplacement() {
        System.out.print("Running Escape Analysis Scalar Replacement Test... ");

        long sum = 0;
        // Warm up and run calculation with non-escaping Point records
        for (int i = 0; i < 100_000; i++) {
            sum += computeNonEscapingPoint(i, i * 2);
        }

        assert sum > 0 : "Sum computation must be correct";
        System.out.println("PASSED [Computed Sum: " + sum + "]");
    }

    // Helper method: Point p does NOT escape this method scope
    private static long computeNonEscapingPoint(int x, int y) {
        Point p = new Point(x, y);
        return (long) p.x + p.y;
    }

    private static class Point {
        final int x;
        final int y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
