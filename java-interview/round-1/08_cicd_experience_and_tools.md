# 08. Explain Your CI/CD Experience and Tools Used

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "In my recent projects, I designed and maintained enterprise CI/CD pipelines following modern **GitOps and Trunk-Based Development** practices using **GitHub Actions Workflows, SonarQube, Trivy, Docker, and ArgoCD for Kubernetes**.
>
> On the **Continuous Integration (CI)** side, every pull request triggers automated Gradle builds, executes Unit and Integration tests with Testcontainers, performs static code analysis via **SonarQube** enforcing strict Quality Gates (80%+ code coverage, zero security vulnerabilities), and scans container dependencies using **Trivy / Snyk**.
>
> Upon merge to `main`, the CI pipeline builds an optimized multi-stage Distroless Docker image, tags it with the immutable Git commit SHA, and pushes it to **AWS ECR**.
>
> For **Continuous Delivery (CD)**, we use **ArgoCD**. The CI pipeline creates a PR to our Helm GitOps repository updating the image tag. ArgoCD detects the change and orchestrates a Zero-Downtime Rolling or Canary deployment into our Amazon EKS cluster, running smoke tests and health checks before routing 100% traffic."

---

## 🧠 Key Technical Pipeline Architecture

```
┌─────────────┐       ┌──────────────────────┐       ┌──────────────────────┐
│  Developer  │ ────► │    GitHub Actions    │ ────► │      SonarQube       │
│  Git Commit │       │  Build & Unit Tests  │       │ Quality Gate (80%+)  │
└─────────────┘       └──────────┬───────────┘       └──────────────────────┘
                                 │
                                 ▼
                      ┌──────────────────────┐       ┌──────────────────────┐
                      │    Trivy / Snyk      │ ────► │ Multi-Stage Docker   │
                      │ Security Vulnerab.   │       │   Push to AWS ECR    │
                      └──────────────────────┘       └──────────┬───────────┘
                                                                │
                                                                ▼
┌──────────────────────┐                             ┌──────────────────────┐
│ AWS EKS / ECS Cluster│ ◄────────────────────────── │   ArgoCD (GitOps)    │
│ Zero-Downtime Rollout│   Auto-syncs Helm manifests │ Watches Git Config   │
└──────────────────────┘                             └──────────────────────┘
```

---

## 💻 Sample Production GitHub Actions Workflow (`.github/workflows/ci-cd.yml`)

```yaml
name: Production CI/CD Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Run Tests & Generate JaCoCo Coverage
        run: ./gradlew clean test jacocoTestReport

      - name: SonarQube Quality Gate Check
        uses: sonarsource/sonarqube-scan-action@master
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}

      - name: Build Multi-Stage Docker Image
        run: docker build -t ${{ secrets.AWS_ECR_REPO }}:${{ github.sha }} .

      - name: Container Security Scan with Trivy
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ secrets.AWS_ECR_REPO }}:${{ github.sha }}
          exit-code: '1'
          severity: 'CRITICAL,HIGH'

      - name: Push to AWS ECR (Main Branch Only)
        if: github.ref == 'refs/heads/main'
        run: |
          aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.us-east-1.amazonaws.com
          docker push ${{ secrets.AWS_ECR_REPO }}:${{ github.sha }}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "How do you achieve Zero-Downtime deployments?"
**Answer:**
> "By combining Kubernetes `readinessProbe` and `livenessProbe` with a `RollingUpdate` deployment strategy (or Blue/Green via Argo Rollouts / AWS ECS target group switching). Traffic is only routed to the new pod after its `/actuator/health/readiness` endpoint returns HTTP 200."

### 2. "How do you manage environment secrets in CI/CD without committing them to Git?"
**Answer:**
> "We store build secrets in GitHub Encrypted Secrets or HashiCorp Vault. In production Kubernetes, we use **External Secrets Operator (ESO)** to dynamically synchronize secrets from **AWS Secrets Manager** or AWS SSM Parameter Store directly into Kubernetes Secrets at runtime."
