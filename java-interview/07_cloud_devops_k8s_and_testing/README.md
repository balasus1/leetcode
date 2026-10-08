# 07. Cloud (AWS), Kubernetes, DevOps & Unit Testing (Mockito) Mastery

Production troubleshooting, CI/CD triage, Kubernetes log commands, enterprise observability stacks, and Mockito testing patterns.

---

## 📑 Topics Index
1. [Amazon S3: Object Limits & Multipart Uploads](#1-amazon-s3-object-limits--multipart-uploads)
2. [What to Do When a Production Deployment Pipeline Fails Midway](#2-what-to-do-when-a-production-deployment-pipeline-fails-midway)
3. [Where to Check CI/CD Pipeline Logs](#3-where-to-check-cicd-pipeline-logs)
4. [Where & How to Check Kubernetes Pod Logs (CLI Diagnostics)](#4-where--how-to-check-kubernetes-pod-logs-cli-diagnostics)
5. [Centralized Production Logging Toolset (ELK, Loki, CloudWatch)](#5-centralized-production-logging-toolset-elk-loki-cloudwatch)
6. [Mockito `@Mock` vs `@Spy` (Partial Mocking with Code)](#6-mockito-mock-vs-spy-partial-mocking-with-code)

---

### 1. Amazon S3: Object Limits & Multipart Uploads
#### 🎙️ 60-Second Verbal Script
> "In Amazon S3 (Simple Storage Service):
>
> - **Maximum Size of a Single S3 Object**: **5 Terabytes (5 TB)**.
> - **Single PUT Upload Limit**: Up to **5 Gigabytes (5 GB)** in a single HTTP PUT operation.
> - **Multipart Upload**: Mandatory for objects larger than 5 GB, and highly recommended for any file exceeding 100 MB. Multipart upload breaks large files into up to 10,000 distinct parts (ranging from 5 MB to 5 GB each), uploading them concurrently in parallel with automatic retries per part."

---

### 2. What to Do When a Production Deployment Pipeline Fails Midway
#### 🎙️ 60-Second Verbal Script
> "If a production deployment pipeline fails midway, I follow a disciplined 5-step incident response:
>
> 1. **Containment & Freeze**: Immediately verify that active user traffic is unaffected. If Kubernetes / ECS is in the middle of a rolling update, ensure the orchestrator halts traffic shift to unready pods.
> 2. **Pipeline Inspection**: Check the exact failing step in the CI/CD dashboard (e.g. Unit tests, SonarQube quality gate, Trivy security CVE scan, Docker build, Helm deployment timeout, or Pod crash in health checks).
> 3. **Fast Rollback (If needed)**: If bad code was deployed, execute an immediate one-click rollback in **ArgoCD** or `helm rollback` to revert to the previous immutable Git commit SHA.
> 4. **Cluster Diagnostics**: Run `kubectl describe pod` and `kubectl logs` to inspect `CrashLoopBackOff`, failed database Flyway migrations, or missing environment secrets in AWS Secrets Manager.
> 5. **Fix & Blameless Post-Mortem**: Apply the fix in a hotfix branch, verify through staging, and conduct a post-mortem to add automated prevention guards."

---

### 3. Where to Check CI/CD Pipeline Logs
#### 🎙️ 60-Second Verbal Script
> "Depending on the enterprise CI/CD platform:
> - **GitHub Actions**: Navigate to the repository's **'Actions' tab**, select the failing workflow run, and expand the failing step (e.g., `Run tests` or `Container Security Scan`).
> - **Jenkins**: Open the build run and click **'Console Output'** or view Blue Ocean stage execution logs.
> - **GitLab CI**: Open **CI/CD -> Pipelines -> Jobs**, click the failed job to view full terminal stdout/stderr."

---

### 4. Where & How to Check Kubernetes Pod Logs (CLI Diagnostics)
#### 🎙️ 60-Second Verbal Script
> "In Kubernetes, we diagnose pod failures using `kubectl` commands:
>
> 1. **Live Log Stream**: `kubectl logs -f <pod-name> -n <namespace>` (or with container flag `-c app-container` for multi-container pods).
> 2. **Previous Crashed Instance Logs**: `kubectl logs -p <pod-name> -n <namespace>` (crucial for inspecting stack traces from pods that just died with `OOMKilled` or exit 1).
> 3. **Pod Events & State**: `kubectl describe pod <pod-name> -n <namespace>` to see lifecycle events like `ImagePullBackOff`, `OOMKilled`, or failed Readiness/Liveness probes.
> 4. **Namespace Events**: `kubectl get events -n <namespace> --sort-by='.metadata.creationTimestamp'`."

---

### 5. Centralized Production Logging Toolset
#### 🎙️ 60-Second Verbal Script
> "In distributed production architectures, logs from hundreds of pods are aggregated in real-time into centralized logging platforms:
>
> 1. **ELK / EFK Stack (Elasticsearch, FluentBit / Logstash, Kibana)**: The gold standard for full-text search and indexing of structured JSON application logs.
> 2. **Grafana Loki + Promtail**: High-efficiency, cost-effective log aggregation that indexes metadata labels (like Kubernetes namespace and pod name) rather than full log text, seamlessly correlating metrics in Grafana dashboards.
> 3. **AWS CloudWatch Logs Insights**: Managed AWS solution for querying container logs using SQL-like syntax with CloudWatch Alarms.
> 4. **Datadog / Dynatrace**: Unified APM platforms correlating logs, distributed traces, and infrastructure metrics in a single pane of glass."

---

### 6. Mockito `@Mock` vs `@Spy` (Partial Mocking with Code)
#### 🎙️ 60-Second Verbal Script
> "In unit testing with Mockito:
>
> - **`@Mock` (Full Mock / Dummy)**: Creates a complete dummy proxy instance. **No real methods are ever executed**. Unless explicitly stubbed with `when().thenReturn()`, all methods return default values (`null`, `0`, `false`, or empty collections).
> - **`@Spy` (Partial Mock / Wrapper)**: Wraps a **real, existing object instance**. Real methods are executed by default unless explicitly stubbed using `doReturn().when()`.
>
> In practice, we use `@Mock` for isolating external dependencies (like repositories or REST clients), and `@Spy` when we want to test a real legacy service while overriding just one specific internal helper method."

```java
package testing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MockVsSpyTest {

    @Mock
    private List<String> mockedList;

    @Spy
    private List<String> spiedList = new ArrayList<>();

    @Test
    void testMockBehavior() {
        // @Mock does NOT call real methods!
        mockedList.add("Java");
        assertEquals(0, mockedList.size()); // Size remains 0
        assertNull(mockedList.get(0));      // Returns default null

        // Explicit stubbing
        when(mockedList.size()).thenReturn(100);
        assertEquals(100, mockedList.size());
    }

    @Test
    void testSpyBehavior() {
        // @Spy calls REAL methods on the actual ArrayList!
        spiedList.add("Spring Boot");
        spiedList.add("Kafka");

        assertEquals(2, spiedList.size()); // Real size is 2
        assertEquals("Spring Boot", spiedList.get(0));

        // Partial stubbing on real object (use doReturn to avoid calling real method)
        doReturn(50).when(spiedList).size();
        assertEquals(50, spiedList.size());
    }
}
```
