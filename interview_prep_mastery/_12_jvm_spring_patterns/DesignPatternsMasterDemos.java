package interview_prep_mastery._12_jvm_spring_patterns;

import java.util.*;
import java.util.concurrent.*;

/**
 * ============================================================================
 * MODULE 12.3: DESIGN PATTERNS MASTER COMPANION SUITE & VERIFICATION
 * ============================================================================
 * Validates production implementations of:
 * 1. Thread-Safe Singletons (Bill Pugh, Double-Checked Locking, Enum)
 * 2. Immutable Builder Pattern with Validation
 * 3. Factory & Abstract Factory Patterns
 * 4. Strategy Pattern (Runtime Interchangeable Algorithms)
 * 5. Observer Pattern (Subject / Listener Event Dispatch)
 * 6. Adapter vs Decorator Patterns
 * 7. Template Method Pattern
 * ============================================================================
 */
public class DesignPatternsMasterDemos {

    public static void main(String[] args) throws Exception {
        System.out.println("--- [12.3 Java Design Patterns Master Verification Suite] ---");

        testThreadSafeSingletons();
        testImmutableBuilderPattern();
        testFactoryAndAbstractFactory();
        testStrategyPattern();
        testObserverPattern();
        testAdapterVsDecorator();
        testTemplateMethodPattern();

        System.out.println("✅ All Design Patterns test suites executed successfully!");
    }

    // ========================================================================
    // 1. Thread-Safe Singletons
    // ========================================================================

    // Bill Pugh (Initialization-on-Demand Holder)
    public static class BillPughSingleton {
        private BillPughSingleton() {}
        private static class Holder {
            private static final BillPughSingleton INSTANCE = new BillPughSingleton();
        }
        public static BillPughSingleton getInstance() {
            return Holder.INSTANCE;
        }
    }

    // Double-Checked Locking (DCL)
    public static class DoubleCheckedSingleton {
        private static volatile DoubleCheckedSingleton instance;
        private DoubleCheckedSingleton() {}
        public static DoubleCheckedSingleton getInstance() {
            if (instance == null) {
                synchronized (DoubleCheckedSingleton.class) {
                    if (instance == null) {
                        instance = new DoubleCheckedSingleton();
                    }
                }
            }
            return instance;
        }
    }

    // Enum Singleton
    public enum EnumSingleton {
        INSTANCE;
        public String getStatus() { return "ACTIVE"; }
    }

    public static void testThreadSafeSingletons() throws Exception {
        System.out.print("Running Thread-Safe Singletons Test (Concurrent Stress)... ");

        ExecutorService executor = Executors.newFixedThreadPool(16);
        Set<Integer> billPughHashes = ConcurrentHashMap.newKeySet();
        Set<Integer> dclHashes = ConcurrentHashMap.newKeySet();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(() -> {
                billPughHashes.add(System.identityHashCode(BillPughSingleton.getInstance()));
                dclHashes.add(System.identityHashCode(DoubleCheckedSingleton.getInstance()));
                return null;
            });
        }
        executor.invokeAll(tasks);
        executor.shutdown();

        assert billPughHashes.size() == 1 : "Bill Pugh Singleton must produce exactly 1 instance across threads";
        assert dclHashes.size() == 1 : "Double-Checked Locking must produce exactly 1 instance across threads";
        assert EnumSingleton.INSTANCE.getStatus().equals("ACTIVE") : "Enum Singleton must be valid";

        System.out.println("PASSED");
    }

    // ========================================================================
    // 2. Builder Pattern (Immutable + Validation)
    // ========================================================================

    public static final class DatabaseConfig {
        private final String host;
        private final int port;
        private final int maxConnections;
        private final boolean ssl;

        private DatabaseConfig(Builder b) {
            this.host = b.host;
            this.port = b.port;
            this.maxConnections = b.maxConnections;
            this.ssl = b.ssl;
        }

        public String getHost() { return host; }
        public int getPort() { return port; }
        public int getMaxConnections() { return maxConnections; }
        public boolean isSsl() { return ssl; }

        public static class Builder {
            private String host = "localhost";
            private int port = 5432;
            private int maxConnections = 10;
            private boolean ssl = false;

            public Builder host(String host) { this.host = host; return this; }
            public Builder port(int port) { this.port = port; return this; }
            public Builder maxConnections(int max) { this.maxConnections = max; return this; }
            public Builder ssl(boolean ssl) { this.ssl = ssl; return this; }

            public DatabaseConfig build() {
                if (host == null || host.trim().isEmpty()) throw new IllegalArgumentException("Host cannot be blank");
                if (port <= 0 || port > 65535) throw new IllegalArgumentException("Invalid port range");
                return new DatabaseConfig(this);
            }
        }
    }

    public static void testImmutableBuilderPattern() {
        System.out.print("Running Immutable Builder Pattern Test... ");

        DatabaseConfig config = new DatabaseConfig.Builder()
                .host("db.production.internal")
                .port(5432)
                .maxConnections(50)
                .ssl(true)
                .build();

        assert config.getHost().equals("db.production.internal");
        assert config.getPort() == 5432;
        assert config.getMaxConnections() == 50;
        assert config.isSsl();

        // Test Validation Exception
        boolean validationCaught = false;
        try {
            new DatabaseConfig.Builder().port(-1).build();
        } catch (IllegalArgumentException e) {
            validationCaught = true;
        }
        assert validationCaught : "Builder must validate input parameters before construction";

        System.out.println("PASSED");
    }

    // ========================================================================
    // 3. Factory & Abstract Factory Patterns
    // ========================================================================

    public interface Button { String render(); }
    public interface Checkbox { String render(); }

    public static class DarkButton implements Button { public String render() { return "DarkButton"; } }
    public static class DarkCheckbox implements Checkbox { public String render() { return "DarkCheckbox"; } }
    public static class LightButton implements Button { public String render() { return "LightButton"; } }
    public static class LightCheckbox implements Checkbox { public String render() { return "LightCheckbox"; } }

    public interface GUIFactory {
        Button createButton();
        Checkbox createCheckbox();
    }

    public static class DarkThemeFactory implements GUIFactory {
        public Button createButton() { return new DarkButton(); }
        public Checkbox createCheckbox() { return new DarkCheckbox(); }
    }

    public static class LightThemeFactory implements GUIFactory {
        public Button createButton() { return new LightButton(); }
        public Checkbox createCheckbox() { return new LightCheckbox(); }
    }

    public static void testFactoryAndAbstractFactory() {
        System.out.print("Running Factory & Abstract Factory Test... ");

        GUIFactory darkFactory = new DarkThemeFactory();
        assert darkFactory.createButton().render().equals("DarkButton");
        assert darkFactory.createCheckbox().render().equals("DarkCheckbox");

        GUIFactory lightFactory = new LightThemeFactory();
        assert lightFactory.createButton().render().equals("LightButton");
        assert lightFactory.createCheckbox().render().equals("LightCheckbox");

        System.out.println("PASSED");
    }

    // ========================================================================
    // 4. Strategy Pattern
    // ========================================================================

    public interface PaymentStrategy {
        String pay(double amount);
    }

    public static class CreditCardStrategy implements PaymentStrategy {
        public String pay(double amount) { return "Paid $" + amount + " via CreditCard"; }
    }

    public static class CryptoStrategy implements PaymentStrategy {
        public String pay(double amount) { return "Paid $" + amount + " via Crypto"; }
    }

    public static class CheckoutProcessor {
        private PaymentStrategy strategy;
        public CheckoutProcessor(PaymentStrategy strategy) { this.strategy = strategy; }
        public void setStrategy(PaymentStrategy strategy) { this.strategy = strategy; }
        public String checkout(double amount) { return strategy.pay(amount); }
    }

    public static void testStrategyPattern() {
        System.out.print("Running Strategy Pattern Test... ");

        CheckoutProcessor checkout = new CheckoutProcessor(new CreditCardStrategy());
        assert checkout.checkout(100.0).equals("Paid $100.0 via CreditCard");

        // Swap strategy at runtime
        checkout.setStrategy(new CryptoStrategy());
        assert checkout.checkout(250.0).equals("Paid $250.0 via Crypto");

        System.out.println("PASSED");
    }

    // ========================================================================
    // 5. Observer Pattern
    // ========================================================================

    public interface OrderEventListener {
        void onOrderCreated(String orderId);
    }

    public static class OrderPublisher {
        private final List<OrderEventListener> listeners = new CopyOnWriteArrayList<>();

        public void subscribe(OrderEventListener listener) { listeners.add(listener); }
        public void unsubscribe(OrderEventListener listener) { listeners.remove(listener); }

        public void publishOrder(String orderId) {
            for (OrderEventListener listener : listeners) {
                listener.onOrderCreated(orderId);
            }
        }
    }

    public static void testObserverPattern() {
        System.out.print("Running Observer Pattern Test... ");

        OrderPublisher publisher = new OrderPublisher();
        List<String> auditLogs = new ArrayList<>();
        List<String> emailNotifications = new ArrayList<>();

        publisher.subscribe(orderId -> auditLogs.add("AUDIT: " + orderId));
        publisher.subscribe(orderId -> emailNotifications.add("EMAIL: " + orderId));

        publisher.publishOrder("ORD-999");

        assert auditLogs.contains("AUDIT: ORD-999");
        assert emailNotifications.contains("EMAIL: ORD-999");

        System.out.println("PASSED");
    }

    // ========================================================================
    // 6. Adapter vs Decorator Patterns
    // ========================================================================

    // Target Interface
    public interface PaymentGateway {
        String processJsonPayment(String jsonPayload);
    }

    // Legacy Incompatible Class (Needs Adapter)
    public static class LegacyXmlPaymentService {
        public String sendXmlRequest(String xml) {
            return "XML_SUCCESS: " + xml;
        }
    }

    // Adapter Pattern: Converts XML service to JSON PaymentGateway interface
    public static class XmlToJsonPaymentAdapter implements PaymentGateway {
        private final LegacyXmlPaymentService legacyService;

        public XmlToJsonPaymentAdapter(LegacyXmlPaymentService legacyService) {
            this.legacyService = legacyService;
        }

        @Override
        public String processJsonPayment(String jsonPayload) {
            String xmlPayload = "<xml>" + jsonPayload + "</xml>";
            return legacyService.sendXmlRequest(xmlPayload);
        }
    }

    // Decorator Pattern: Enhances PaymentGateway with Logging and Metrics (Same Interface)
    public static class LoggingPaymentDecorator implements PaymentGateway {
        private final PaymentGateway delegate;
        public boolean logged = false;

        public LoggingPaymentDecorator(PaymentGateway delegate) {
            this.delegate = delegate;
        }

        @Override
        public String processJsonPayment(String jsonPayload) {
            this.logged = true;
            return delegate.processJsonPayment(jsonPayload);
        }
    }

    public static void testAdapterVsDecorator() {
        System.out.print("Running Adapter vs Decorator Test... ");

        LegacyXmlPaymentService legacy = new LegacyXmlPaymentService();
        PaymentGateway adapter = new XmlToJsonPaymentAdapter(legacy);
        LoggingPaymentDecorator decorator = new LoggingPaymentDecorator(adapter);

        String response = decorator.processJsonPayment("{\"amount\": 500}");

        assert response.contains("XML_SUCCESS: <xml>{\"amount\": 500}</xml>");
        assert decorator.logged : "Decorator must attach logging behavior seamlessly";

        System.out.println("PASSED");
    }

    // ========================================================================
    // 7. Template Method Pattern
    // ========================================================================

    public static abstract class DataMiner {
        // Template method: Invariant algorithm skeleton marked final
        public final List<String> mine() {
            List<String> result = new ArrayList<>();
            result.add(openFile());
            result.add(parseData());
            result.add(closeFile());
            return result;
        }

        protected abstract String openFile();
        protected abstract String parseData();
        protected String closeFile() { return "Closed Default File"; }
    }

    public static class PdfDataMiner extends DataMiner {
        @Override protected String openFile() { return "Opened PDF"; }
        @Override protected String parseData() { return "Parsed PDF Text Stream"; }
    }

    public static class CsvDataMiner extends DataMiner {
        @Override protected String openFile() { return "Opened CSV"; }
        @Override protected String parseData() { return "Parsed CSV Delimited Rows"; }
    }

    public static void testTemplateMethodPattern() {
        System.out.print("Running Template Method Test... ");

        DataMiner pdfMiner = new PdfDataMiner();
        List<String> pdfSteps = pdfMiner.mine();
        assert pdfSteps.get(0).equals("Opened PDF");
        assert pdfSteps.get(1).equals("Parsed PDF Text Stream");

        DataMiner csvMiner = new CsvDataMiner();
        List<String> csvSteps = csvMiner.mine();
        assert csvSteps.get(0).equals("Opened CSV");
        assert csvSteps.get(1).equals("Parsed CSV Delimited Rows");

        System.out.println("PASSED");
    }
}
