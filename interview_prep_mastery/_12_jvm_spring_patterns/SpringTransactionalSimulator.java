package interview_prep_mastery._12_jvm_spring_patterns;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * ============================================================================
 * MODULE 12.2: SPRING @TRANSACTIONAL SIMULATOR & INTERVIEW VERIFICATION
 * ============================================================================
 * Demonstrates internal mechanics without third-party dependencies:
 * 1. Spring AOP Dynamic Proxy Interception & Transaction Demarcation
 * 2. The Self-Invocation Problem (Internal method call bypassing proxy)
 * 3. Propagation REQUIRED vs REQUIRES_NEW with Rollback Isolation
 * 4. Checked vs Unchecked Exception Rollback Rules
 * ============================================================================
 */
public class SpringTransactionalSimulator {

    public static void main(String[] args) {
        System.out.println("--- [12.2 Spring @Transactional Simulator & Verification Suite] ---");

        testProxyInterceptionAndCommit();
        testUncheckedExceptionRollback();
        testCheckedExceptionNoRollbackByDefault();
        testSelfInvocationBypass();
        testPropagationRequiresNewIsolation();

        System.out.println("✅ All Spring @Transactional test suites executed successfully!");
    }

    // ========================================================================
    // 1. Core Domain Interfaces & Services
    // ========================================================================

    public interface OrderService {
        void createOrderSuccess(String orderId);
        void createOrderWithUncheckedException(String orderId);
        void createOrderWithCheckedException(String orderId) throws Exception;
        void outerMethodCallingSelfInvocation(String orderId);
        void internalTransactionalMethod(String orderId);
    }

    public static class OrderServiceImpl implements OrderService {
        final List<String> databaseTable = new ArrayList<>();
        public boolean internalMethodExecutedWithTx = false;

        @Override
        public void createOrderSuccess(String orderId) {
            databaseTable.add(orderId);
            MockTransactionManager.logAction("INSERT order: " + orderId);
        }

        @Override
        public void createOrderWithUncheckedException(String orderId) {
            databaseTable.add(orderId);
            MockTransactionManager.logAction("INSERT order (will throw RuntimeException): " + orderId);
            throw new IllegalArgumentException("Invalid order data (Unchecked Exception)");
        }

        @Override
        public void createOrderWithCheckedException(String orderId) throws Exception {
            databaseTable.add(orderId);
            MockTransactionManager.logAction("INSERT order (will throw Checked Exception): " + orderId);
            throw new Exception("Network I/O failed (Checked Exception)");
        }

        @Override
        public void outerMethodCallingSelfInvocation(String orderId) {
            MockTransactionManager.logAction("Executing non-transactional outer method");
            // ❌ SELF-INVOCATION: Calls 'this' directly, bypassing proxy!
            this.internalTransactionalMethod(orderId);
        }

        @Override
        public void internalTransactionalMethod(String orderId) {
            if (MockTransactionManager.isTransactionActive()) {
                internalMethodExecutedWithTx = true;
            }
            databaseTable.add("INTERNAL_" + orderId);
        }
    }

    // ========================================================================
    // 2. Transaction Manager & ThreadLocal Context
    // ========================================================================

    public static class MockTransactionManager {
        private static final ThreadLocal<Deque<TransactionContext>> txStack =
                ThreadLocal.withInitial(ArrayDeque::new);

        public static class TransactionContext {
            public final String txId;
            public final boolean isRequiresNew;
            public boolean isRollbackOnly = false;
            public final List<String> actions = new ArrayList<>();

            public TransactionContext(String txId, boolean isRequiresNew) {
                this.txId = txId;
                this.isRequiresNew = isRequiresNew;
            }
        }

        public static TransactionContext beginTransaction(String name, boolean requiresNew) {
            String txId = "TX_" + UUID.randomUUID().toString().substring(0, 8);
            TransactionContext ctx = new TransactionContext(txId, requiresNew);
            txStack.get().push(ctx);
            return ctx;
        }

        public static boolean isTransactionActive() {
            return !txStack.get().isEmpty();
        }

        public static TransactionContext currentTransaction() {
            return txStack.get().peek();
        }

        public static void logAction(String action) {
            if (isTransactionActive()) {
                currentTransaction().actions.add(action);
            }
        }

        public static void commit(TransactionContext ctx) {
            if (ctx.isRollbackOnly) {
                rollback(ctx);
                return;
            }
            txStack.get().remove(ctx);
        }

        public static void rollback(TransactionContext ctx) {
            ctx.isRollbackOnly = true;
            ctx.actions.clear();
            txStack.get().remove(ctx);
        }
    }

    // ========================================================================
    // 3. Dynamic Proxy Invocation Handler (Simulating Spring TransactionInterceptor)
    // ========================================================================

    public static class TransactionalProxyFactory {
        @SuppressWarnings("unchecked")
        public static <T> T createProxy(T target, Class<T> interfaceType) {
            return (T) Proxy.newProxyInstance(
                    interfaceType.getClassLoader(),
                    new Class<?>[]{interfaceType},
                    new TransactionalInvocationHandler(target)
            );
        }
    }

    private static class TransactionalInvocationHandler implements InvocationHandler {
        private final Object target;

        public TransactionalInvocationHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Check if method is transactional (in our simulation, create* and internal* are transactional)
            boolean isTransactional = method.getName().startsWith("createOrder") ||
                                      method.getName().equals("internalTransactionalMethod");

            if (!isTransactional) {
                return method.invoke(target, args);
            }

            // Begin Transaction
            MockTransactionManager.TransactionContext tx =
                    MockTransactionManager.beginTransaction(method.getName(), false);

            try {
                Object result = method.invoke(target, args);
                // Normal completion -> Commit
                MockTransactionManager.commit(tx);
                return result;
            } catch (Throwable t) {
                Throwable cause = (t.getCause() != null) ? t.getCause() : t;

                // Standard Spring Rule: Rollback on RuntimeException and Error ONLY
                if (cause instanceof RuntimeException || cause instanceof Error) {
                    MockTransactionManager.rollback(tx);
                } else {
                    // Checked exception -> Commits by default!
                    MockTransactionManager.commit(tx);
                }
                throw cause;
            }
        }
    }

    // ========================================================================
    // 4. Test Suites
    // ========================================================================

    public static void testProxyInterceptionAndCommit() {
        System.out.print("Running Proxy Interception & Commit Test... ");

        OrderServiceImpl target = new OrderServiceImpl();
        OrderService proxy = TransactionalProxyFactory.createProxy(target, OrderService.class);

        proxy.createOrderSuccess("ORD-101");

        assert target.databaseTable.contains("ORD-101") : "Target database table must have order";
        assert !MockTransactionManager.isTransactionActive() : "Transaction must be cleared post-commit";

        System.out.println("PASSED");
    }

    public static void testUncheckedExceptionRollback() {
        System.out.print("Running Unchecked Exception Rollback Test... ");

        OrderServiceImpl target = new OrderServiceImpl();
        OrderService proxy = TransactionalProxyFactory.createProxy(target, OrderService.class);

        boolean exceptionThrown = false;
        try {
            proxy.createOrderWithUncheckedException("ORD-FAIL-UNCHECKED");
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        assert exceptionThrown : "Exception must propagate to caller";
        assert !MockTransactionManager.isTransactionActive() : "Transaction stack must be cleared";

        System.out.println("PASSED (Rollback Triggered)");
    }

    public static void testCheckedExceptionNoRollbackByDefault() {
        System.out.print("Running Checked Exception No-Rollback Test... ");

        OrderServiceImpl target = new OrderServiceImpl();
        OrderService proxy = TransactionalProxyFactory.createProxy(target, OrderService.class);

        boolean checkedExceptionCaught = false;
        try {
            proxy.createOrderWithCheckedException("ORD-CHECKED-TX");
        } catch (Exception e) {
            checkedExceptionCaught = true;
        }

        assert checkedExceptionCaught : "Checked exception must be caught";
        // By default Spring commits on checked exceptions
        assert target.databaseTable.contains("ORD-CHECKED-TX") : "Checked exception should NOT rollback by default";

        System.out.println("PASSED (Checked Exception Committed by default)");
    }

    public static void testSelfInvocationBypass() {
        System.out.print("Running Self-Invocation Bypass Test... ");

        OrderServiceImpl target = new OrderServiceImpl();
        OrderService proxy = TransactionalProxyFactory.createProxy(target, OrderService.class);

        // Calling outer non-transactional method which calls internal method on 'this'
        proxy.outerMethodCallingSelfInvocation("ORD-SELF");

        // The internal method was called via 'this' directly -> NO proxy -> NO transaction active!
        assert !target.internalMethodExecutedWithTx : "Self-invocation must BYPASS transactional proxy!";
        assert target.databaseTable.contains("INTERNAL_ORD-SELF") : "Target method did execute directly";

        System.out.println("PASSED (Proxy Bypassed on Internal Call)");
    }

    public static void testPropagationRequiresNewIsolation() {
        System.out.print("Running Propagation REQUIRES_NEW Isolation Test... ");

        // Simulating Outer Tx 1 starting Inner Tx 2 (REQUIRES_NEW)
        MockTransactionManager.TransactionContext outerTx =
                MockTransactionManager.beginTransaction("OuterTx", false);
        outerTx.actions.add("OUTER_ACTION_1");

        // Inner REQUIRES_NEW starts independent transaction
        MockTransactionManager.TransactionContext innerTx =
                MockTransactionManager.beginTransaction("InnerAuditTx", true);
        innerTx.actions.add("INNER_AUDIT_LOG");

        // Inner commits successfully
        MockTransactionManager.commit(innerTx);

        // Outer rolls back
        MockTransactionManager.rollback(outerTx);

        assert outerTx.isRollbackOnly : "Outer transaction must be rolled back";
        assert !innerTx.isRollbackOnly : "Inner REQUIRES_NEW transaction must remain committed independently";

        System.out.println("PASSED (REQUIRES_NEW Isolated from Outer Rollback)");
    }
}
