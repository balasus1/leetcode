# 12. Explain Kafka Architecture & Schema Registry. How Did You Configure It?

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "**Apache Kafka** is a distributed, append-only event streaming platform built on Topics, Partitions, Brokers, and Consumer Groups.
>
> 1. **Topics & Partitions**: A Topic is partitioned for parallel scale. Each partition is an ordered, immutable sequence of records. Producers write messages using a key (hashed to determine the partition, ensuring strict per-key ordering).
> 2. **Consumer Groups**: Multiple consumers share a group ID to load-balance partitions. Each partition is consumed by exactly one consumer within a group at any time.
> 3. **Schema Registry (Confluent)**: In distributed systems, producer and consumer contracts can break. Schema Registry serves as a centralized metadata repository for **Apache Avro, Protobuf, or JSON Schemas**.
>
> When a Producer publishes an Avro event, Kafka SerDe registers the schema, prepends a 5-byte Magic Byte + Schema ID to the message payload, and serializes it in compact binary format. The Consumer reads the Schema ID, fetches the schema from Schema Registry cache, and deserializes it safely.
>
> We configure **Backward Compatibility** mode, ensuring consumers can safely read new payloads even if producers add optional fields with defaults."

---

## 🧠 Kafka + Schema Registry Workflow

```
[Avro Java Class]
       │
       ▼
[Kafka Producer] ──(1. Register/Get ID)──► [Confluent Schema Registry]
       │                                              │
       ▼ (2. Prepend Schema ID + Compact Binary)      │ (3. Fetch Schema by ID)
[Kafka Broker / Topic Partition]                      ▼
       │                                     [Kafka Consumer]
       └──────────────(4. Consume Event)──────────────┘
```

---

## 💻 Spring Boot Kafka + Avro Configuration

### `application.yml`
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
      properties:
        schema.registry.url: http://localhost:8081
        auto.register.schemas: true
    consumer:
      group-id: order-fulfillment-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
      properties:
        schema.registry.url: http://localhost:8081
        specific.avro.reader: true
```

### Spring Boot Producer & Consumer:
```java
@Service
public class OrderKafkaService {

    private final KafkaTemplate<String, OrderPlacedEventAvro> kafkaTemplate;

    public OrderKafkaService(KafkaTemplate<String, OrderPlacedEventAvro> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Producer publishes with OrderId as partition key
    public void publishOrder(OrderPlacedEventAvro event) {
        kafkaTemplate.send("order-placed-topic", event.getOrderId().toString(), event);
    }

    // Consumer reads Avro event with strict schema validation
    @KafkaListener(topics = "order-placed-topic", groupId = "order-fulfillment-group")
    public void handleOrderPlaced(OrderPlacedEventAvro event, Acknowledgment ack) {
        System.out.println("Processing order: " + event.getOrderId() + " for total: $" + event.getTotalAmount());
        ack.acknowledge(); // Manual Ack commit
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "What are the different Schema Registry Compatibility types?"
**Answer:**
> - **BACKWARD** (Default): New schema can read data written with previous schema. (Consumers upgraded before Producers).
> - **FORWARD**: Old schema can read data written with new schema. (Producers upgraded before Consumers).
> - **FULL**: Both Backward and Forward compatible.
> - **NONE**: No schema validation enforced.

### 2. "How do you guarantee Exactly-Once Processing (EOS) in Kafka?"
**Answer:**
> "By enabling `enable.idempotence=true` on the Producer, assigning a unique `transactional.id`, setting `isolation.level=read_committed` on the Consumer, and using Kafka Transactions (`@Transactional` with `KafkaTransactionManager`)."
