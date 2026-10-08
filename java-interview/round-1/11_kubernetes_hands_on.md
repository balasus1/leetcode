# 11. Explain Kubernetes and Your Hands-On Experience

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "**Kubernetes (K8s)** is an open-source container orchestration platform designed to automate the deployment, scaling, healing, and management of containerized applications.
>
> In my daily work with **Amazon EKS (Elastic Kubernetes Service)**, I manage:
> 1. **Deployments & Pods**: Declaring desired replicas, rolling update strategies, resource requests, and limits (CPU/Memory).
> 2. **Services & Ingress**: Exposing internal pods via `ClusterIP` and routing external HTTPS traffic via AWS ALB Ingress Controller with TLS termination.
> 3. **ConfigMaps & Secrets**: Separating configuration from code, and utilizing **External Secrets Operator** to pull credentials from AWS Secrets Manager.
> 4. **Self-Healing & Lifecycle Probes**: Configuring **Liveness** (restarts unresponsive pods), **Readiness** (stops routing traffic until Spring Boot is ready), and **Startup probes**.
> 5. **Auto-Scaling**: Implementing **HPA (Horizontal Pod Autoscaler)** based on CPU utilization and custom Prometheus metrics (e.g. SQS queue depth or HTTP RPS)."

---

## 🧠 Core Kubernetes Architecture

```
                       Control Plane (Master Node)
   ┌─────────────────────────────────────────────────────────────────┐
   │ API Server ◄──► etcd (State Store) ◄──► Controller Manager      │
   │      ▲                                      ▲                   │
   │      │                                      │                   │
   │      └───────────────── Scheduler ──────────┘                   │
   └────────────────────────────────┬────────────────────────────────┘
                                    │
               Worker Nodes (Kubelet + Kube-Proxy + CRI)
   ┌────────────────────────────────┴────────────────────────────────┐
   │  Worker Node 1                      Worker Node 2               │
   │  ┌───────────────────────────┐      ┌─────────────────────────┐ │
   │  │ Pod (Spring Boot App)     │      │ Pod (Spring Boot App)   │ │
   │  │ - Liveness & Readiness    │      │ - HPA Scaled Replica    │ │
   │  └───────────────────────────┘      └─────────────────────────┘ │
   └─────────────────────────────────────────────────────────────────┘
```

---

## 💻 Production-Ready Kubernetes Manifest for Spring Boot (`deployment.yaml`)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: production
  labels:
    app: order-service
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: 12345.dkr.ecr.us-east-1.amazonaws.com/order-service:v1.2.0
          ports:
            - containerPort: 8080
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1024Mi"
              cpu: "1000m"
          # Spring Boot Actuator Probes
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 45
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 5
          envFrom:
            - configMapRef:
                name: order-service-config
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "What is the difference between Liveness and Readiness probes?"
**Answer:**
> - **Liveness Probe**: Determines if the application is alive. If it fails, Kubernetes kills the container and restarts it. (Used for detecting deadlocks).
> - **Readiness Probe**: Determines if the container is ready to accept incoming user traffic. If it fails, Kubernetes removes the pod IP from the Service Endpoints so no traffic reaches it, but **does not restart** the pod. (Used while warming up caches or connecting to DB)."

### 2. "What happens if a Java process exceeds its memory limit in Kubernetes?"
**Answer:**
> "The Linux kernel triggers an **OOMKilled (Exit Code 137)** event and kills the container immediately. To prevent this in Java 21, we configure `-XX:MaxRAMPercentage=75.0` so the JVM Heap respects cgroup memory boundaries with sufficient headroom for non-heap native memory (Metaspace, Thread stacks)."
