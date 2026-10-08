# 09. Which AWS Services Have You Used, and How Did You Integrate Them?

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "In our cloud-native Spring Boot microservices architecture, I have deep hands-on experience integrating a comprehensive suite of AWS services using **Spring Cloud AWS** and the **AWS Java SDK v2**:
>
> 1. **Compute & Containers**: **AWS ECS Fargate** and **Amazon EKS** for running containerized microservices without managing EC2 infrastructure, coupled with **AWS Lambda** for event-driven async tasks.
> 2. **Messaging & Event Streaming**: **Amazon SQS** for reliable asynchronous worker queues with Dead-Letter Queues (DLQ), and **Amazon SNS** for pub/sub fanout. We integrated with `@SqsListener` and `SnsTemplate`.
> 3. **Database & Storage**: **Amazon RDS PostgreSQL (Aurora Serverless)** for relational transactional data via Spring Data JPA + HikariCP, and **Amazon S3** with pre-signed URLs for secure document upload/download.
> 4. **Caching & Performance**: **Amazon ElastiCache Redis** using Spring Data Redis for distributed session caching and rate-limiting.
> 5. **Security & Configuration**: **AWS Secrets Manager** and **SSM Parameter Store** for injecting database credentials and API keys dynamically at startup using IAM Roles for Service Accounts (IRSA), eliminating hardcoded secrets."

---

## 🧠 AWS Architecture Ecosystem Summary

| Category | AWS Service | Integration in Spring Boot | Use Case |
|---|---|---|---|
| **Compute** | Amazon EKS / ECS Fargate | Docker container image | Microservices hosting |
| **Messaging** | Amazon SQS / SNS | `io.awspring.cloud:spring-cloud-aws-starter-sqs` | Async task queues, fan-out event pub/sub |
| **Database** | Amazon RDS Aurora PostgreSQL | `org.postgresql:postgresql` + HikariCP | Multi-AZ ACID relational persistence |
| **Object Store**| Amazon S3 | `software.amazon.awssdk:s3` | File storage, multipart upload, pre-signed URLs |
| **Caching** | ElastiCache Redis | `spring-boot-starter-data-redis` | Distributed caching, Token bucket rate limiting |
| **Security** | AWS Secrets Manager / KMS | Spring Cloud AWS Secrets Manager Starter | Secret rotation, envelope encryption |
| **Observability**| CloudWatch / X-Ray | Micrometer AWS metrics exporter / OpenTelemetry | Distributed tracing, logs, alarms |

---

## 💻 Code Example: AWS SQS & S3 Integration in Spring Boot 3

```java
package com.example.aws.integration;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class OrderEventProcessingService {

    private final S3Client s3Client;
    private final String s3BucketName = "my-enterprise-invoice-bucket";

    public OrderEventProcessingService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // 1. SQS Listener for asynchronous incoming order messages
    @SqsListener("${aws.sqs.order-queue-name}")
    public void processOrderEvent(OrderEventDto event) {
        System.out.println("Received SQS Order: " + event.orderId());

        // 2. Generate PDF and upload to AWS S3
        byte[] invoicePdf = generateInvoicePdf(event);
        String s3Key = "invoices/" + event.orderId() + ".pdf";

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3BucketName)
                .key(s3Key)
                .contentType("application/pdf")
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(invoicePdf));
        System.out.println("Invoice successfully persisted to S3: " + s3Key);
    }

    private byte[] generateInvoicePdf(OrderEventDto event) {
        return ("Invoice data for Order ID: " + event.orderId()).getBytes();
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "How do you authenticate your Spring Boot app with AWS services in production?"
**Answer:**
> "We **never** use hardcoded Access Keys/Secret Keys. In Kubernetes (EKS), we use **IAM Roles for Service Accounts (IRSA)**, which maps a Kubernetes ServiceAccount to an AWS IAM Role via OIDC tokens. The AWS SDK automatically retrieves temporary credentials via the `DefaultCredentialsProvider` chain."

### 2. "How do you handle SQS message failure and poison pills?"
**Answer:**
> "We configure a **Dead-Letter Queue (DLQ)** with a `maxReceiveCount` (e.g. 3 attempts). If a message fails processing 3 times due to business logic errors, SQS automatically moves it to the DLQ, triggering a CloudWatch Alarm for investigation."
