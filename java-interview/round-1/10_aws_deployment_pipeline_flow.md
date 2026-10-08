# 10. Explain Full AWS Deployment Flow: From Docker Image Creation in CI to Deployment in AWS Container

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "The end-to-end deployment flow from code commit to AWS container deployment follows a 5-stage automated pipeline:
>
> 1. **Code & CI Trigger**: Developer pushes code to GitHub/GitLab. The CI pipeline triggers, compiling the Java 21 Spring Boot app, executing Unit/Integration tests with Testcontainers, and passing SonarQube quality gates.
> 2. **Container Image Build & Security Scan**: CI builds an optimized multi-stage Docker image using Eclipse Temurin JRE / Distroless base image. Trivy scans the image for CVE vulnerabilities.
> 3. **Image Registry Push**: CI authenticates with **Amazon ECR (Elastic Container Registry)** and pushes the image tagged with the Git commit SHA (e.g. `12345.dkr.ecr.us-east-1.amazonaws.com/order-service:a1b2c3d`).
> 4. **CD & GitOps Orchestration**: The CI pipeline updates the image tag in the GitOps repository. **ArgoCD (for EKS)** or **AWS CodeDeploy / ECS CLI (for ECS Fargate)** detects the new revision.
> 5. **Zero-Downtime Deployment & Traffic Switch**: In ECS Fargate or EKS, a **Rolling Update** launches new container tasks. AWS ALB routes traffic to the new containers only after Spring Boot Actuator's `/actuator/health/readiness` probe returns 200 OK, smoothly terminating old tasks."

---

## 🧠 End-to-End Architectural Flow Diagram

```
[Developer Push]
       │
       ▼
[GitHub Actions CI] ───► [Gradle Build & Tests] ───► [SonarQube & Trivy Scan]
                                                             │
                                                             ▼
                                                    [Docker Multi-Stage Build]
                                                             │
                                                             ▼
                                                    [Push to Amazon ECR]
                                                             │
                                                             ▼
                                                    [Update GitOps / Task Def]
                                                             │
       ┌─────────────────────────────────────────────────────┘
       ▼
[AWS ECS / EKS Container Deployment]
   ├── 1. Pull Image from ECR using IAM Role
   ├── 2. Provision Fargate Task / K8s Pod
   ├── 3. Inject Secrets from AWS Secrets Manager
   ├── 4. Evaluate Spring Boot Readiness Probe (/actuator/health/readiness)
   ├── 5. Application Load Balancer (ALB) registers new target
   └── 6. Drain and terminate old container tasks (Zero Downtime)
```

---

## 💻 Multi-Stage Production Dockerfile Example

```dockerfile
# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Extract layers for Spring Boot layer caching
RUN java -Djarmode=layertools -jar build/libs/*.jar extract

# Stage 2: Minimal Distroless / Alpine Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /application
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "How do you rollback if a bug is detected in production after deployment?"
**Answer:**
> "Because image tags in ECR use immutable Git commit SHAs, rolling back is instant:
> - In **ArgoCD**: We click `Rollback` or `git revert` the config commit, and ArgoCD points the deployment back to the previous stable image SHA in under 30 seconds.
> - In **AWS ECS**: We roll back the ECS Service to the previous Task Definition revision."

### 2. "Why use Spring Boot layered jar extraction in Dockerfiles?"
**Answer:**
> "By splitting the jar into `dependencies`, `spring-boot-loader`, and `application` layers, Docker caches third-party dependencies. When code changes, Docker only rebuilds the tiny `application` layer (a few megabytes), reducing build & upload times from minutes to seconds."
