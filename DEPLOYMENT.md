# Deployment & Operations Guide

## Interview Platform Backend

**Repository:** https://github.com/Madhan13K/interview-platform-backend  
**Stack:** Spring Boot 4.0.6 | Java 21 | PostgreSQL 16 | Redis 7 | Kafka | Docker  
**Default Branch:** `master` (protected - PR only)

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Environment Variables Reference](#environment-variables-reference)
4. [Infrastructure Services](#infrastructure-services)
5. [External Service Credentials](#external-service-credentials)
6. [Deployment Environments](#deployment-environments)
7. [CI/CD Pipeline](#cicd-pipeline)
8. [Secret Management (Vault)](#secret-management-vault)
9. [Database Management](#database-management)
10. [Monitoring & Observability](#monitoring--observability)
11. [Security Configuration](#security-configuration)
12. [Feature Flags](#feature-flags)
13. [Troubleshooting](#troubleshooting)
14. [Runbooks](#runbooks)

---

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java (JDK) | 21+ | Application runtime |
| Maven | 3.9+ | Build tool (wrapper included) |
| Docker | 24+ | Container runtime |
| Docker Compose | 2.20+ | Local infrastructure |
| Git | 2.40+ | Version control |
| GitHub CLI (`gh`) | 2.40+ | Repository management |
| PostgreSQL Client (`psql`) | 16+ | Database management (optional) |
| OpenSSL | 3.0+ | Key/cert generation |

---

## Local Development Setup

### Quick Start (Everything in Docker)

```bash
# Clone repository
git clone https://github.com/Madhan13K/interview-platform-backend.git
cd interview-platform-backend

# Copy environment file
cp .env.example .env
# Edit .env with your credentials (see Environment Variables section)

# Start all infrastructure + application
docker compose up --build

# Application available at: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
# Jaeger (traces): http://localhost:16686
# Vault UI: http://localhost:8200 (token: dev-root-token)
```

### Development Mode (App outside Docker)

```bash
# Start infrastructure only
docker compose up -d postgres kafka redis localstack otel-collector jaeger vault

# Wait for services to be healthy
docker compose ps

# Run application with dev profile (all features enabled)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# OR run with OpenTelemetry agent for full tracing
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=interview-platform-backend \
     -Dotel.exporter.otlp.endpoint=http://localhost:4318 \
     -jar target/interview-platform-backend-0.0.1-SNAPSHOT.jar \
     --spring.profiles.active=dev
```

### Build & Test

```bash
# Compile + unit tests (no Docker services needed - default)
./mvnw clean install

# Compile only (no tests)
./mvnw compile

# Unit tests only (same as clean install - integration tests excluded by default)
./mvnw test

# Integration tests (requires: docker compose up -d)
./mvnw verify -PintegrationTests

# Package JAR (skip tests)
./mvnw package -DskipTests

# Run OWASP dependency check
./mvnw dependency-check:check

# Run SonarQube analysis
./mvnw sonar:sonar -Dsonar.token=$SONAR_TOKEN
```

---

## Environment Variables Reference

### Core Application

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Yes | Active profiles: `dev`, `prod`, `vault` |
| `SERVER_PORT` | `8080` | No | HTTP server port |
| `FRONTEND_URL` | `http://localhost:5173` | Yes | Frontend SPA URL (for redirects) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Yes | CORS whitelist |

### Database (PostgreSQL)

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5433/interview_platform` | Yes | JDBC connection URL |
| `DB_USERNAME` | `admin` | Yes | Application DB username (DML) |
| `DB_PASSWORD` | `postgres` | Yes | Application DB password |
| `DB_POOL_MAX` | `10` | No | HikariCP max pool size |
| `DB_SEPARATE_USERS` | `false` | No | Enable DDL/DML user separation |
| `DB_DDL_URL` | (same as DB_URL) | No | DDL user JDBC URL |
| `DB_DDL_USERNAME` | `ddl_admin` | No | Flyway migrations user |
| `DB_DDL_PASSWORD` | - | Cond. | DDL user password (required if separate-users=true) |

### JWT & Security

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `JWT_SECRET` | Dev fallback | Yes (prod) | HMAC secret for access tokens (min 256 bits) |
| `JWT_EXPIRATION` | `86400000` | No | Access token TTL (ms) - default 24h |
| `JWT_REFRESH_SECRET` | Dev fallback | Yes (prod) | HMAC secret for refresh tokens |
| `JWT_REFRESH_EXPIRATION` | `1209600000` | No | Refresh token TTL (ms) - default 14 days |
| `RSA_FROM_VAULT` | `false` | No | Load RSA keys from Vault instead of classpath |

### OAuth2 Providers

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `GOOGLE_CLIENT_ID` | `placeholder` | Yes (for OAuth) | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | `placeholder` | Yes (for OAuth) | Google OAuth2 client secret |
| `GITHUB_CLIENT_ID` | `placeholder` | Yes (for OAuth) | GitHub OAuth2 client ID |
| `GITHUB_CLIENT_SECRET` | `placeholder` | Yes (for OAuth) | GitHub OAuth2 client secret |
| `MICROSOFT_CLIENT_ID` | `placeholder` | Yes (for OAuth) | Azure AD client ID |
| `MICROSOFT_CLIENT_SECRET` | `placeholder` | Yes (for OAuth) | Azure AD client secret |
| `OAUTH2_BASE_URL` | `http://localhost:8080` | No | OAuth2 redirect base URL |

### Kafka

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Yes | Kafka broker addresses |
| `KAFKA_ENABLED` | `true` | No | Enable Kafka listeners & producers |

### Redis

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `REDIS_HOST` | `localhost` | Yes | Redis server hostname |
| `REDIS_PORT` | `6379` | Yes | Redis server port |

### Email (SMTP)

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `MAIL_HOST` | `smtp.gmail.com` | Yes | SMTP server |
| `MAIL_PORT` | `587` | No | SMTP port |
| `MAIL_USERNAME` | - | Yes | SMTP username/email |
| `MAIL_PASSWORD` | - | Yes | SMTP password or app password |

### AWS S3 / LocalStack

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `AWS_S3_BUCKET_NAME` | `interview-platform-documents` | Yes | S3 bucket name |
| `AWS_S3_REGION` | `us-east-1` | No | AWS region |
| `AWS_S3_ACCESS_KEY` | `test` | Yes | AWS access key (or LocalStack `test`) |
| `AWS_S3_SECRET_KEY` | `test` | Yes | AWS secret key |
| `AWS_S3_ENDPOINT` | `http://localhost:4566` | No | S3 endpoint (LocalStack for dev) |

### HashiCorp Vault

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `VAULT_ENABLED` | `false` | No | Enable Vault secret backend |
| `VAULT_HOST` | `localhost` | Cond. | Vault server host |
| `VAULT_PORT` | `8200` | Cond. | Vault server port |
| `VAULT_SCHEME` | `http` | No | `http` for dev, `https` for prod |
| `VAULT_TOKEN` | `dev-root-token` | Cond. | Vault authentication token |

### Data Encryption

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `ENCRYPTION_ENABLED` | `true` | No | Enable field-level PII encryption |
| `ENCRYPTION_SECRET_KEY` | (auto-generated in dev) | Yes (prod) | AES-256 key (base64, 32 bytes). Generate: `openssl rand -base64 32` |

### Account Lockout

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `LOCKOUT_ENABLED` | `true` | No | Enable account lockout |
| `LOCKOUT_MAX_ATTEMPTS` | `5` | No | Failed attempts before lock |
| `LOCKOUT_DURATION_MINUTES` | `30` | No | Lock duration (0 = permanent) |
| `LOCKOUT_MAX_IP_ATTEMPTS` | `20` | No | Failed attempts per IP before block |
| `LOCKOUT_IP_BLOCK_MINUTES` | `60` | No | IP block duration |
| `LOCKOUT_ALERTS_ENABLED` | `true` | No | Send email alerts on suspicious activity |

### Code Execution Engine

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `CODE_EXECUTION_ENABLED` | `true` | No | Enable sandboxed code execution |
| `DOCKER_HOST` | `unix:///var/run/docker.sock` | Yes (for exec) | Docker daemon socket |
| `CODE_EXECUTION_MAX_TIMEOUT` | `30000` | No | Max execution timeout (ms) |
| `CODE_EXECUTION_MEMORY_LIMIT` | `268435456` | No | Container memory limit (bytes, 256MB) |
| `CODE_EXECUTION_MAX_CONCURRENT` | `10` | No | Max parallel executions |

### Calendar Sync

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `CALENDAR_SYNC_ENABLED` | `true` | No | Enable calendar sync feature |
| `CALENDAR_SYNC_INTERVAL` | `15` | No | Sync interval in minutes |

### E-Signature (Offer Letters)

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `DOCUSIGN_ENABLED` | `true` | No | Enable DocuSign integration |
| `DOCUSIGN_API_URL` | `https://demo.docusign.net/restapi` | No | DocuSign API base URL |
| `DOCUSIGN_ACCOUNT_ID` | - | Cond. | DocuSign account ID |
| `DOCUSIGN_INTEGRATION_KEY` | - | Cond. | DocuSign integration/client key |
| `HELLOSIGN_ENABLED` | `true` | No | Enable HelloSign/Dropbox Sign |
| `HELLOSIGN_API_KEY` | - | Cond. | HelloSign API key |

### Integrations

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `SLACK_ENABLED` | `true` | No | Enable Slack notifications |
| `SLACK_WEBHOOK_URL` | - | Cond. | Slack incoming webhook URL |
| `TEAMS_ENABLED` | `true` | No | Enable Microsoft Teams notifications |
| `TEAMS_WEBHOOK_URL` | - | Cond. | Teams incoming webhook URL |
| `ZOOM_ENABLED` | `true` | No | Enable Zoom meeting generation |
| `ZOOM_API_KEY` | - | Cond. | Zoom JWT API key |
| `ZOOM_API_SECRET` | - | Cond. | Zoom JWT API secret |
| `GOOGLE_MEET_ENABLED` | `true` | No | Enable Google Meet links |
| `GOOGLE_MEET_CLIENT_ID` | - | Cond. | Google Meet/Calendar client ID |
| `SMS_ENABLED` | `true` | No | Enable SMS notifications |
| `SMS_PROVIDER` | `twilio` | No | SMS provider (twilio/log) |

### Observability (OpenTelemetry)

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `OTEL_SERVICE_NAME` | `interview-platform-backend` | No | Service name in traces |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4318` | No | OTel Collector endpoint |
| `OTEL_TRACES_SAMPLER_ARG` | `1.0` | No | Sampling rate (0.0-1.0) |

---

## Infrastructure Services

### Docker Compose Services

| Service | Port | Image | Purpose |
|---------|------|-------|---------|
| `postgres` | 5433 | `postgres:16-alpine` | Primary database |
| `kafka` | 9092 | `confluentinc/cp-kafka:7.6.0` | Event streaming |
| `zookeeper` | 2181 | `confluentinc/cp-zookeeper:7.6.0` | Kafka coordination |
| `redis` | 6379 | `redis:7-alpine` | Caching & rate limiting |
| `localstack` | 4566 | `localstack/localstack:3.4` | AWS S3 emulation |
| `vault` | 8200 | `hashicorp/vault:1.15` | Secret management |
| `otel-collector` | 4317,4318,8889 | `otel/opentelemetry-collector-contrib:0.102.0` | Telemetry pipeline |
| `jaeger` | 16686 | `jaegertracing/all-in-one:1.57` | Trace visualization |

### Health Check URLs

| Service | Health Check |
|---------|-------------|
| Application | `http://localhost:8080/actuator/health` |
| PostgreSQL | `pg_isready -U admin -d interview_platform` |
| Redis | `redis-cli ping` |
| Kafka | `kafka-broker-api-versions --bootstrap-server localhost:9092` |
| LocalStack | `http://localhost:4566/_localstack/health` |
| Vault | `http://localhost:8200/v1/sys/health` |
| OTel Collector | `http://localhost:13133/` |
| Jaeger | `http://localhost:14269/` |

---

## External Service Credentials

### What You Need to Sign Up For

| Service | Free Tier | Sign Up URL | What You Get |
|---------|-----------|-------------|--------------|
| **Google Cloud Console** | Yes | https://console.cloud.google.com | OAuth2 client ID/secret, Calendar API, Meet |
| **GitHub OAuth App** | Yes | https://github.com/settings/developers | OAuth2 client ID/secret |
| **Microsoft Azure AD** | Yes | https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps | OAuth2 client ID/secret, Outlook Calendar |
| **Gmail App Password** | Yes | https://myaccount.google.com/apppasswords | SMTP email sending |
| **Twilio** | Free trial | https://www.twilio.com/try-twilio | SMS sending |
| **Slack Webhook** | Yes | https://api.slack.com/messaging/webhooks | Slack notifications |
| **Microsoft Teams Webhook** | Yes | Teams channel > Connectors > Incoming Webhook | Teams notifications |
| **Zoom Marketplace** | Yes | https://marketplace.zoom.us/develop/create | Meeting link generation |
| **DocuSign Developer** | Free sandbox | https://developers.docusign.com | E-signature for offer letters |
| **Dropbox Sign (HelloSign)** | Free tier | https://www.hellosign.com/api | E-signature alternative |
| **SonarCloud** | Free (public repos) | https://sonarcloud.io | SAST code quality scanning |

### Credential Setup Instructions

#### 1. Google OAuth2 + Calendar + Meet

```
1. Go to https://console.cloud.google.com
2. Create project "Interview Platform"
3. APIs & Services > Credentials > Create OAuth 2.0 Client ID
4. Application type: Web application
5. Authorized redirect URIs:
   - http://localhost:8080/login/oauth2/code/google
   - https://your-domain.com/login/oauth2/code/google
6. Enable APIs:
   - Google Calendar API
   - Google People API
7. Copy Client ID and Client Secret to .env:
   GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=GOCSPX-xxx
   GOOGLE_MEET_CLIENT_ID=xxx.apps.googleusercontent.com
```

#### 2. GitHub OAuth App

```
1. Go to https://github.com/settings/developers
2. OAuth Apps > New OAuth App
3. Application name: Interview Platform
4. Homepage URL: http://localhost:5173
5. Authorization callback URL: http://localhost:8080/login/oauth2/code/github
6. Copy Client ID and Client Secret to .env:
   GITHUB_CLIENT_ID=xxx
   GITHUB_CLIENT_SECRET=xxx
```

#### 3. Microsoft Azure AD (OAuth2 + Outlook Calendar)

```
1. Go to https://portal.azure.com
2. Azure Active Directory > App registrations > New registration
3. Name: Interview Platform
4. Supported account types: Accounts in any organizational directory
5. Redirect URI: http://localhost:8080/login/oauth2/code/microsoft
6. API Permissions > Add:
   - Microsoft Graph > Delegated > openid, profile, email
   - Microsoft Graph > Delegated > Calendars.ReadWrite
7. Certificates & secrets > New client secret
8. Copy to .env:
   MICROSOFT_CLIENT_ID=xxx
   MICROSOFT_CLIENT_SECRET=xxx
```

#### 4. Gmail SMTP (App Password)

```
1. Go to https://myaccount.google.com/security
2. Enable 2-Factor Authentication (required)
3. Go to https://myaccount.google.com/apppasswords
4. Generate password for "Mail" > "Other" > "Interview Platform"
5. Copy to .env:
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=xxxx xxxx xxxx xxxx
```

#### 5. Twilio SMS

```
1. Go to https://www.twilio.com/try-twilio
2. Get Account SID, Auth Token, and a phone number
3. Copy to .env:
   SMS_ENABLED=true
   SMS_PROVIDER=twilio
   TWILIO_ACCOUNT_SID=ACxxx
   TWILIO_AUTH_TOKEN=xxx
   TWILIO_PHONE_NUMBER=+1234567890
```

#### 6. Slack Webhook

```
1. Go to https://api.slack.com/apps > Create New App
2. Features > Incoming Webhooks > Activate
3. Add New Webhook to Workspace > Select channel
4. Copy to .env:
   SLACK_ENABLED=true
   SLACK_WEBHOOK_URL=https://hooks.slack.com/services/T.../B.../xxx
```

#### 7. Microsoft Teams Webhook

```
1. Open Teams > Go to the channel
2. Click ... > Connectors > Incoming Webhook
3. Name it "Interview Platform" > Create
4. Copy to .env:
   TEAMS_ENABLED=true
   TEAMS_WEBHOOK_URL=https://xxx.webhook.office.com/webhookb2/...
```

#### 8. Zoom

```
1. Go to https://marketplace.zoom.us/develop/create
2. Create JWT App (or Server-to-Server OAuth)
3. Copy API Key and Secret:
   ZOOM_ENABLED=true
   ZOOM_API_KEY=xxx
   ZOOM_API_SECRET=xxx
```

#### 9. DocuSign (E-Signature)

```
1. Go to https://developers.docusign.com
2. Create developer account (free sandbox)
3. Go to Apps and Keys
4. Create new app > Get Integration Key
5. Copy to .env:
   DOCUSIGN_ENABLED=true
   DOCUSIGN_API_URL=https://demo.docusign.net/restapi
   DOCUSIGN_ACCOUNT_ID=xxx
   DOCUSIGN_INTEGRATION_KEY=xxx
```

#### 10. Dropbox Sign / HelloSign

```
1. Go to https://app.hellosign.com/home/myAccount#api
2. Generate API Key
3. Copy to .env:
   HELLOSIGN_ENABLED=true
   HELLOSIGN_API_KEY=xxx
```

#### 11. SonarCloud (CI/CD)

```
1. Go to https://sonarcloud.io
2. Sign in with GitHub
3. Import your repository
4. My Account > Security > Generate Token
5. Add as GitHub repo secret:
   Settings > Secrets > Actions > SONAR_TOKEN
```

---

## Deployment Environments

### Environment Profiles

| Profile | Purpose | Command |
|---------|---------|---------|
| `dev` | Local development (all services enabled, verbose logging) | `--spring.profiles.active=dev` |
| `dev,vault` | Local dev with Vault secrets | `--spring.profiles.active=dev,vault` |
| `prod` | Production (JSON logging, Vault required, DB separation) | `--spring.profiles.active=prod` |
| `prod,vault` | Production with explicit Vault profile | `--spring.profiles.active=prod,vault` |

### Production Deployment Checklist

- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Configure Vault with all secrets (run `scripts/vault/init-vault.sh`)
- [ ] Set `ENCRYPTION_SECRET_KEY` (generate: `openssl rand -base64 32`)
- [ ] Set `JWT_SECRET` and `JWT_REFRESH_SECRET` (generate: `openssl rand -base64 48`)
- [ ] Configure real OAuth2 client IDs/secrets
- [ ] Configure SMTP credentials
- [ ] Set `DB_SEPARATE_USERS=true` and run `scripts/db/setup-users.sql`
- [ ] Set `OTEL_TRACES_SAMPLER_ARG=0.1` (10% sampling in prod)
- [ ] Disable `spring.jpa.show-sql`
- [ ] Set proper `CORS_ALLOWED_ORIGINS`
- [ ] Configure SSL/TLS termination at load balancer
- [ ] Verify all health checks pass

### Docker Production Build

```bash
# Build production image
docker build -t interview-platform-backend:latest .

# Run with production config
docker run -d \
  --name interview-platform \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://prod-db:5432/interview_platform \
  -e DB_USERNAME=app_user \
  -e DB_PASSWORD=secure_password \
  -e VAULT_ENABLED=true \
  -e VAULT_HOST=vault.internal \
  -e VAULT_TOKEN=s.xxxxx \
  -e ENCRYPTION_SECRET_KEY=$(openssl rand -base64 32) \
  interview-platform-backend:latest
```

### Kubernetes (Example)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: interview-platform-backend
spec:
  replicas: 3
  template:
    spec:
      containers:
        - name: app
          image: ghcr.io/madhan13k/interview-platform-backend:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: VAULT_ENABLED
              value: "true"
            - name: VAULT_HOST
              value: "vault.vault-system.svc.cluster.local"
          envFrom:
            - secretRef:
                name: interview-platform-secrets
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 45
            periodSeconds: 15
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
```

---

## CI/CD Pipeline

### Pipeline Stages

```
Push/PR to master
       │
       ▼
┌──────────────┐    ┌─────────────────────┐    ┌──────────────┐
│ Build & Test │───▶│ Integration Tests   │───▶│ Docker Build │
│   (Maven)    │    │ (Testcontainers)    │    │ + Trivy Scan │
└──────────────┘    └─────────────────────┘    └──────────────┘
       │                                              │
       ├────────────────────────────────┐             │
       ▼                                ▼             ▼
┌──────────────┐    ┌─────────────────────┐    ┌──────────────┐
│ SAST Scan    │    │ OWASP Dependency    │    │  Migration   │
│ (SonarCloud) │    │   Check (CVEs)      │    │  Validation  │
└──────────────┘    └─────────────────────┘    └──────────────┘
       │                     │                        │
       └─────────────────────┴────────────────────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │  Security Gate   │
                    │ (pass/fail all)  │
                    └──────────────────┘
                             │
                             ▼ (master only)
                    ┌──────────────────┐
                    │ Push to GHCR     │
                    │ (Container Reg)  │
                    └──────────────────┘
```

### Required GitHub Secrets

| Secret | Where to get it |
|--------|-----------------|
| `SONAR_TOKEN` | https://sonarcloud.io > My Account > Security |
| `GITHUB_TOKEN` | Auto-provided (no setup needed) |

### Running Pipeline Locally

```bash
# Simulate the build stage
./mvnw verify -DskipIntegrationTests

# Simulate OWASP scan
./mvnw dependency-check:check

# Simulate Docker build + Trivy scan
docker build -t interview-platform-backend:test .
docker run --rm aquasec/trivy image interview-platform-backend:test
```

---

## Secret Management (Vault)

### Local Development Setup

```bash
# Start Vault
docker compose up -d vault

# Access Vault UI
open http://localhost:8200
# Token: dev-root-token

# Initialize with application secrets
./scripts/vault/init-vault.sh
```

### Vault Secret Paths

| Path | Contents |
|------|----------|
| `secret/interview-platform` | All application secrets (DB, JWT, RSA, encryption, OAuth2) |
| `secret/interview-platform/dev` | Dev-specific overrides |
| `secret/interview-platform/prod` | Production-specific overrides |

### Production Vault Setup

```bash
# Use AppRole authentication (not token) in production
VAULT_ADDR=https://vault.your-domain.com

# Get role-id and secret-id from Vault admin
vault read auth/approle/role/interview-platform/role-id
vault write -force auth/approle/role/interview-platform/secret-id

# Application authenticates with:
VAULT_AUTH_METHOD=approle
VAULT_ROLE_ID=xxx
VAULT_SECRET_ID=xxx
```

---

## Database Management

### Schema Migrations (Flyway)

Migrations are in `src/main/resources/db/migration/`:

| Version | Description |
|---------|-------------|
| V1 | Auth & RBAC tables |
| V2 | Interview tables |
| V3-V8 | Token, auth provider, password reset, email verification |
| V9 | Coding sessions, question bank, feedback |
| V10 | Notifications |
| V11 | Interview templates |
| V12 | Evaluation scorecards |
| V13 | Hiring pipelines |
| V14 | Documents |
| V15 | Job positions |
| V16 | Scheduling, reminders, self-service, teams, tags |
| V17 | AI, video, whiteboard, webhooks, tenant, feedback, activity |
| V18 | MFA, GDPR, API keys |
| V19 | Code executions |
| V20 | SSO/SAML configurations |
| V21 | Account lockout tables |
| V22 | Encryption column alterations |
| V23 | Job applications |
| V24 | Offer letters & approvals |
| V25 | Calendar sync |

### User Separation (DDL vs DML)

```bash
# Create separate database users
PGPASSWORD=postgres psql -h localhost -p 5433 -U admin -d interview_platform \
  -f scripts/db/setup-users.sql

# Enable in application config
DB_SEPARATE_USERS=true
DB_DDL_USERNAME=ddl_admin
DB_DDL_PASSWORD=your_ddl_password
DB_USERNAME=app_user
DB_PASSWORD=your_app_password
```

| User | Can Do | Cannot Do |
|------|--------|-----------|
| `ddl_admin` | CREATE TABLE, ALTER, DROP, ALL | - |
| `app_user` | SELECT, INSERT, UPDATE, DELETE | CREATE, ALTER, DROP, TRUNCATE |

### Backup & Restore

```bash
# Backup
pg_dump -h localhost -p 5433 -U admin interview_platform > backup_$(date +%Y%m%d).sql

# Restore
psql -h localhost -p 5433 -U admin interview_platform < backup_20260618.sql
```

---

## Monitoring & Observability

### Structured Logging

| Environment | Format | Output |
|-------------|--------|--------|
| Dev | Human-readable with correlation ID | Console |
| Production | JSON (Logstash format) | Console + `/var/log/app/application.json` |

### Log Fields (Production JSON)

```json
{
  "@timestamp": "2026-06-18T12:00:00.000Z",
  "level": "INFO",
  "logger_name": "c.i.s.AuthService",
  "message": "Login successful",
  "service": "interview-platform-backend",
  "environment": "prod",
  "correlationId": "abc-123-def-456",
  "traceId": "4bf92f3577b34da6...",
  "spanId": "00f067aa0ba902b7",
  "userId": "admin@example.com",
  "requestMethod": "POST",
  "requestUri": "/api/v1/auth/login",
  "clientIp": "192.168.1.1"
}
```

### Dashboards

| Tool | URL | Purpose |
|------|-----|---------|
| Jaeger | http://localhost:16686 | Distributed traces |
| Prometheus metrics | http://localhost:8889/metrics | OTel Collector metrics |
| Spring Actuator | http://localhost:8080/actuator | App health, metrics, info |
| Vault UI | http://localhost:8200 | Secret management |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation |

---

## Security Configuration

### Features Enabled by Default (Dev)

| Feature | Status | Config Key |
|---------|--------|-----------|
| Account Lockout | Enabled | `LOCKOUT_ENABLED=true` |
| IP Blocking | Enabled | `LOCKOUT_IP_BLOCKING=true` |
| PII Encryption | Enabled | `ENCRYPTION_ENABLED=true` |
| Rate Limiting | Enabled | Built-in filter |
| XSS Sanitization | Enabled | Built-in filter |
| CORS | Configured | `CORS_ALLOWED_ORIGINS` |
| CSRF | Disabled (stateless API) | - |
| Security Headers | HSTS, CSP, X-Frame-Options | - |
| JWT RS256 | Enabled | RSA keys |
| MFA/TOTP | Available | Per-user opt-in |

### Generating Secrets

```bash
# AES-256 encryption key
openssl rand -base64 32

# JWT HMAC secret (384 bits)
openssl rand -base64 48

# RSA key pair
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
```

---

## Feature Flags

All features can be toggled via environment variables:

| Feature | Env Variable | Default (Dev) |
|---------|-------------|---------------|
| Kafka event streaming | `KAFKA_ENABLED` | `true` |
| Email notifications | `NOTIFICATIONS_ENABLED` | `true` |
| SMS notifications | `SMS_ENABLED` | `true` |
| Slack integration | `SLACK_ENABLED` | `true` |
| Teams integration | `TEAMS_ENABLED` | `true` |
| Code execution engine | `CODE_EXECUTION_ENABLED` | `true` |
| Calendar sync | `CALENDAR_SYNC_ENABLED` | `true` |
| Account lockout | `LOCKOUT_ENABLED` | `true` |
| PII encryption | `ENCRYPTION_ENABLED` | `true` |
| DocuSign e-signature | `DOCUSIGN_ENABLED` | `true` |
| HelloSign e-signature | `HELLOSIGN_ENABLED` | `true` |
| Vault secret management | `VAULT_ENABLED` | `false` |
| DB user separation | `DB_SEPARATE_USERS` | `false` |
| Zoom meetings | `ZOOM_ENABLED` | `true` |
| Google Meet | `GOOGLE_MEET_ENABLED` | `true` |

---

## Troubleshooting

### Common Issues

| Issue | Cause | Fix |
|-------|-------|-----|
| `Connection refused: localhost:5433` | PostgreSQL not running | `docker compose up -d postgres` |
| `Could not resolve placeholder 'DB_URL'` | Missing .env file | `cp .env.example .env` |
| `Flyway migration failed` | Schema conflict | Delete DB volume: `docker compose down -v` |
| `Redis connection timeout` | Redis not running | `docker compose up -d redis` |
| `Kafka: Topic not found` | Kafka not ready | Wait 30s after startup; check `docker compose logs kafka` |
| `Code execution timeout` | Docker not running | Ensure Docker daemon is active |
| `JWT signature invalid` | Key mismatch | Regenerate keys or check Vault secrets |
| `OWASP check fails` | Known CVE | Add suppression to `dependency-check-suppressions.xml` |
| `Branch protection: push rejected` | Direct push to master | Create PR: `gh pr create --base master` |

### Useful Commands

```bash
# Check all container health
docker compose ps

# View application logs
docker compose logs -f app

# Connect to PostgreSQL
PGPASSWORD=postgres psql -h localhost -p 5433 -U admin -d interview_platform

# Redis CLI
docker compose exec redis redis-cli

# Kafka topics
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Vault status
docker compose exec vault vault status

# Check active Spring profiles
curl http://localhost:8080/actuator/info
```

---

## Runbooks

### Unlock a Locked Account

```bash
# Via API (requires ADMIN token)
curl -X POST http://localhost:8080/api/v1/security/lockout/user@example.com/unlock \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Via database directly
PGPASSWORD=postgres psql -h localhost -p 5433 -U admin -d interview_platform \
  -c "UPDATE account_lockouts SET locked=false, failed_attempts=0 WHERE email='user@example.com';"
```

### Unblock an IP Address

```bash
curl -X POST http://localhost:8080/api/v1/security/unblock-ip/192.168.1.100 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Rotate Encryption Key

```bash
# 1. Generate new key
NEW_KEY=$(openssl rand -base64 32)

# 2. Update in Vault (or env)
vault kv patch secret/interview-platform encryption.secret-key="$NEW_KEY"

# 3. Run migration to re-encrypt existing data with new key
java -jar app.jar --spring.profiles.active=dev,encrypt-migrate
```

### Rotate RSA Keys

```bash
# 1. Generate new key pair
openssl genrsa -out new-private.pem 2048
openssl rsa -in new-private.pem -pubout -out new-public.pem

# 2. Store in Vault
vault kv patch secret/interview-platform \
  rsa.public-key-pem="$(cat new-public.pem)" \
  rsa.private-key-pem="$(cat new-private.pem)"

# 3. Restart application (existing tokens signed with old key will be invalid)
# Users will need to re-authenticate
```

### Emergency: Disable a Feature

```bash
# Disable code execution (if Docker is compromised)
docker compose exec app env CODE_EXECUTION_ENABLED=false

# Or restart with override
docker compose up -d -e CODE_EXECUTION_ENABLED=false app
```
