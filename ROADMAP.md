# Project Roadmap, Bugs & Deployment Checklist

## Interview Platform Backend

**Repository:** https://github.com/Madhan13K/interview-platform-backend  
**Last Updated:** 2026-06-19

---

## Table of Contents

- [Feature Status](#feature-status)
- [Known Bugs & Issues](#known-bugs--issues)
- [Placeholder / Mock Implementations](#placeholder--mock-implementations)
- [Missing Functionalities](#missing-functionalities)
- [Test Coverage Gaps](#test-coverage-gaps)
- [Deployment Requirements](#deployment-requirements)
- [Production Checklist](#production-checklist)
- [Infrastructure Needed for Production](#infrastructure-needed-for-production)
- [Future Roadmap](#future-roadmap)
- [Architecture & Statistics](#architecture--statistics)

---

## Feature Status

### Legend

| Status | Meaning |
|--------|---------|
| DONE | Fully implemented, compiles, has migrations |
| MOCK | Implemented but uses placeholder/simulated responses |
| BUG | Has known issues that need fixing |
| MISSING | Feature gap that needs implementation |
| PLANNED | Scoped, not yet started |

---

### Core Platform (Phase 1-4) - All DONE

| # | Feature | Package | Status |
|---|---------|---------|--------|
| 1 | Authentication (Local + JWT RS256) | `security/auth` | DONE |
| 2 | OAuth2 (Google, GitHub, Microsoft) with PKCE | `security/oauth2` | DONE |
| 3 | RBAC (Roles, Permissions, Dynamic Assignment) | `user`, `security` | DONE |
| 4 | User Management & Profiles | `user` | DONE |
| 5 | Interview CRUD & Lifecycle | `candidate` | DONE |
| 6 | Interview Feedback | `candidate` | DONE |
| 7 | Refresh Token Rotation + Replay Detection | `security/token` | DONE |
| 8 | Email Verification | `security/auth` | DONE |
| 9 | Password Reset | `security/auth` | DONE |
| 10 | Collaborative Code Editor (WebSocket) | `codeeditor` | DONE |
| 11 | Question Bank | `questionbank` | DONE |
| 12 | Notifications (Email, SMS, In-App, Kafka) | `notification` | DONE (SMS=MOCK) |
| 13 | Interview Templates | `template` | DONE |
| 14 | Evaluation Scorecards (Weighted) | `scorecard` | DONE |
| 15 | Hiring Pipelines & Candidate Progression | `pipeline` | DONE |
| 16 | Document/File Management (S3) | `document` | DONE |
| 17 | Job Positions | `jobposition` | DONE |
| 18 | Automated Scheduling (Availability + Suggest) | `scheduling` | DONE |
| 19 | Interview Reminders (24h/1h/15min) | `reminder` | DONE |
| 20 | Candidate Self-Service (Preferred Slots) | `selfservice` | DONE |
| 21 | Teams & Departments | `team` | DONE |
| 22 | Tags & Labels | `tag` | DONE |
| 23 | AI Features (Questions, Resume Parse, Summary) | `ai` | MOCK |
| 24 | Video Recording Integration | `video` | DONE |
| 25 | Whiteboard Collaboration (WebSocket) | `whiteboard` | DONE |
| 26 | Webhook Integrations (HMAC-SHA256) | `webhook` | DONE |
| 27 | Multi-Tenant Support | `tenant` | DONE |
| 28 | Candidate Reverse Feedback | `candidatefeedback` | DONE |
| 29 | Activity Feed / Timeline | `activity` | DONE |
| 30 | MFA/TOTP | `security/mfa` | DONE |
| 31 | GDPR Compliance (Consent, Erasure, Export) | `gdpr` | DONE |
| 32 | API Key Authentication | `security/apikey` | DONE |
| 33 | Bulk Operations (Schedule, Invite, Export) | `bulk` | DONE |
| 34 | Reports & Analytics (JSON + PDF) | `report` | DONE |
| 35 | Export/Import (CSV, JSON) | `exportimport` | DONE (Excel=MISSING) |
| 36 | Meeting Link Generation | `meeting` | MOCK (Zoom/Google Meet) |
| 37 | Dashboard (Admin, Interviewer, Candidate) | `dashboard` | DONE |
| 38 | Audit Logging | `audit` | DONE |
| 39 | Calendar (Interviewer Availability) | `calendar` | DONE |

### Advanced Features (Phase 5-8) - All DONE

| # | Feature | Package | Status |
|---|---------|---------|--------|
| 40 | Code Execution Engine (Docker Sandbox) | `codeexecution` | DONE |
| 41 | SSO/SAML Integration | `sso` | DONE |
| 42 | Account Lockout & IP Blocking | `accountlockout` | DONE |
| 43 | Data Encryption at Rest (AES-256-GCM) | `encryption` | DONE |
| 44 | Candidate Portal / Job Board | `jobboard` | DONE |
| 45 | Offer Letter Management | `offer` | DONE (E-Sign=MOCK) |
| 46 | Calendar Sync (Google + Outlook) | `calendarsync` | DONE |
| 47 | Configurable Workflow Engine | `workflow` | DONE |
| 48 | Approval Workflows | `approval` | DONE |
| 49 | Referral Program | `referral` | DONE |
| 50 | DEI/Diversity Analytics | `dei` | DONE |
| 51 | Source Effectiveness Tracking | `sourcetracking` | DONE |

### Infrastructure & DevOps - All DONE

| # | Feature | Status |
|---|---------|--------|
| 52 | HashiCorp Vault Secret Management | DONE |
| 53 | Structured JSON Logging + Correlation IDs | DONE |
| 54 | CI/CD Pipeline (GitHub Actions, 7 stages) | DONE |
| 55 | SAST Scanning (SonarCloud) | DONE |
| 56 | OWASP Dependency Check (CVE Scanning) | DONE |
| 57 | Docker Image Scanning (Trivy) | DONE |
| 58 | Integration Tests (Testcontainers) | DONE |
| 59 | Database User Separation (DDL vs DML) | DONE |
| 60 | OpenTelemetry (Traces + Metrics + Logs) | DONE |
| 61 | Rate Limiting (Redis-backed) | DONE |
| 62 | XSS Sanitization Filter | DONE |
| 63 | Security Headers (HSTS, CSP, X-Frame-Options) | DONE |
| 64 | Branch Protection (master - PR only) | DONE |

---

## Known Bugs & Issues

### Critical (Must Fix Before Production)

| # | Bug | Location | Impact | Fix |
|---|-----|----------|--------|-----|
| 1 | AI service returns hardcoded mock responses | `ai/service/AiService.java:165` | AI features non-functional | Integrate OpenAI/Anthropic API |
| 2 | Zoom meeting provider generates fake URLs | `meeting/provider/ZoomMeetingProvider.java:38` | Meetings don't work with Zoom | Implement Zoom REST API |
| 3 | Google Meet provider generates fake URLs | `meeting/provider/GoogleMeetProvider.java:36` | Meetings don't work with Google Meet | Implement Google Calendar API |
| 4 | DocuSign service is fully simulated | `offer/esignature/DocuSignService.java:22` | E-signatures don't work | Integrate DocuSign SDK |
| 5 | HelloSign service is fully simulated | `offer/esignature/HelloSignService.java:22` | E-signatures don't work | Integrate Dropbox Sign API |
| 6 | SMS notification only logs (no Twilio) | `notification/sms/SmsNotificationService.java:46` | SMS not sent | Implement Twilio SDK |
| 7 | Export/Import uses userId as org placeholder | `exportimport/service/ExportService.java:72` | Wrong organization_id | Pass actual org context |

### High (Should Fix)

| # | Bug | Location | Impact | Fix |
|---|-----|----------|--------|-----|
| 8 | Excel export falls back to CSV silently | `exportimport/service/ExportService.java:124` | Users expecting .xlsx get .csv | Add Apache POI dependency |
| 9 | Excel import not supported | `exportimport/service/ImportService.java:170` | Cannot import .xlsx files | Add Apache POI dependency |
| 10 | Some POST endpoints return 200 instead of 201 | Various controllers | Non-standard REST responses | Audit all POST returns |
| 11 | No pagination consistency across all endpoints | Various | Some paginated, some not | Standardize PageRequest/PageResponse |
| 12 | `private.pem` in source tree (dev profile) | `src/main/resources/` | Key exposure risk if leaked | Load exclusively from Vault in prod |
| 13 | `ddl-auto: update` in dev profile | `application-dev.yml` | Schema drift risk if accidentally used in prod | Ensure prod profile uses `validate` |

### Medium (Should Fix Before Scale)

| # | Bug | Location | Impact | Fix |
|---|-----|----------|--------|-----|
| 14 | N+1 query potential in pipeline candidate listing | `pipeline/service/` | Slow at scale | Add `@EntityGraph` or fetch joins |
| 15 | No retry mechanism for email sending failures | `notification/email/` | Lost notifications on SMTP failure | Add Spring Retry / dead letter queue |
| 16 | Calendar sync token refresh may race condition | `calendarsync/service/` | Token expiry edge case | Add synchronized refresh with lock |
| 17 | Webhook delivery retries are in-memory | `webhook/service/` | Lost retries on app restart | Move to persistent queue (Kafka/DB) |
| 18 | Code execution container cleanup on crash | `codeexecution/service/` | Orphaned Docker containers | Add startup cleanup job |
| 19 | No request body size limit configuration | Security config | Potential DoS via large payloads | Add `spring.servlet.multipart.max-file-size` enforcement |

### Low (Polish)

| # | Bug | Location | Impact | Fix |
|---|-----|----------|--------|-----|
| 20 | jjwt version 0.11.5 outdated | `pom.xml` | No security vuln but missing features | Upgrade to 0.12.6 |
| 21 | Error codes are generic strings | `exception/` | Hard to programmatically handle | Add structured error codes (ERR_AUTH_001) |
| 22 | Audit logs stored in mutable DB | `audit/` | Compliance concern for immutability | Export to append-only store (S3/CloudWatch) |
| 23 | No graceful handling of Kafka broker down | Kafka config | App startup fails if Kafka unavailable | Add fallback / circuit breaker |

---

## Placeholder / Mock Implementations

These features are coded but use fake/simulated responses. They need real API integration.

| Module | Current State | Required Integration | Effort | Priority |
|--------|--------------|---------------------|--------|----------|
| **AI Service** | Returns hardcoded mock questions/summaries | OpenAI / Anthropic API (GPT-4, Claude) | Medium | HIGH |
| **Zoom Provider** | Generates fake `zoom.us` style URLs | Zoom REST API (Server-to-Server OAuth) | Medium | HIGH |
| **Google Meet Provider** | Generates fake `meet.google.com` URLs | Google Calendar API (create event with conferenceData) | Medium | HIGH |
| **DocuSign** | Returns fake envelope IDs | DocuSign eSign REST API + JWT auth | Large | HIGH |
| **HelloSign** | Returns fake signature request IDs | Dropbox Sign API | Medium | MEDIUM |
| **SMS (Twilio)** | Only logs messages to console | Twilio REST API (Messages.create) | Small | MEDIUM |
| **SMS (AWS SNS)** | Not implemented at all | AWS SNS SDK | Small | LOW |
| **Excel Export** | Falls back to CSV | Apache POI library | Small | MEDIUM |
| **Push Notifications** | Enum exists, no implementation | Firebase Cloud Messaging (FCM) / APNS | Medium | LOW |

---

## Missing Functionalities

### Critical (Production Blockers)

| # | Feature | Description | Effort | Priority |
|---|---------|-------------|--------|----------|
| 1 | **Real AI Integration** | Replace mock AI with OpenAI/Anthropic API calls. Config for API keys, model selection, token limits, rate limiting. | Medium | P0 |
| 2 | **Real Zoom Integration** | Implement actual Zoom meeting creation via REST API. Requires Zoom Marketplace app. | Medium | P0 |
| 3 | **Real Google Meet Integration** | Create Google Calendar events with Meet conferenceData. Reuses existing Google OAuth2 creds. | Medium | P0 |
| 4 | **Real E-Signature Integration** | DocuSign/HelloSign SDK integration for actual envelope creation, signing, and status polling. | Large | P0 |
| 5 | **SMS Delivery** | Twilio SDK integration for actual SMS sending (interview reminders, notifications). | Small | P1 |
| 6 | **Email Delivery Verification** | Implement bounce/complaint handling, delivery status tracking. | Small | P1 |
| 7 | **File Virus Scanning** | Scan uploaded documents (resumes, attachments) for malware before S3 storage. | Medium | P1 |

### High (Competitive Parity)

| # | Feature | Description | Effort | Priority |
|---|---------|-------------|--------|----------|
| 8 | **Excel Export/Import** | Apache POI integration for .xlsx support in export/import. | Small | P1 |
| 9 | **In-App Messaging / Chat** | Recruiter-to-candidate and recruiter-to-interviewer messaging. WebSocket infra exists. | Medium | P1 |
| 10 | **Push Notifications (Mobile)** | Firebase Cloud Messaging for Android/iOS apps. | Medium | P2 |
| 11 | **Data Retention Policies** | Configurable auto-purge (e.g., delete candidate data after 2 years). Scheduled cleanup. | Small | P2 |
| 12 | **Structured Interview Kits** | Downloadable interview guides with rubrics, questions, scoring criteria per role. | Small | P2 |
| 13 | **Interview Debrief/Calibration** | Structured debrief workflow for hiring committees after interviews. | Medium | P2 |
| 14 | **@Mentions & Threaded Comments** | Comment threads on candidate profiles with @mention notifications. | Medium | P2 |
| 15 | **Custom Report Builder** | User-configurable report templates with saved filters. Current reports are fixed-format. | Large | P2 |
| 16 | **IP Whitelisting per Org** | Organization-level IP restrictions for sensitive operations. | Small | P2 |

### Medium (Enterprise-Ready)

| # | Feature | Description | Effort | Priority |
|---|---------|-------------|--------|----------|
| 17 | **Background Check Integration** | Checkr/Sterling API for automated background checks post-offer. | Medium | P3 |
| 18 | **ATS Integration Connectors** | Pre-built bidirectional sync with Greenhouse, Lever, Workday APIs. | Large | P3 |
| 19 | **Job Board Auto-Posting** | Automated distribution to LinkedIn, Indeed, Glassdoor via APIs. | Medium | P3 |
| 20 | **Talent Pool / CRM** | Candidate relationship management for passive candidates. Nurture campaigns. | Large | P3 |
| 21 | **Recruiter SLA Tracking** | Response time SLA, workload balancing, bottleneck identification. | Medium | P3 |
| 22 | **Feature Flags** | LaunchDarkly/Flagsmith integration for gradual rollouts. | Small | P3 |
| 23 | **Billing / Subscriptions** | Stripe integration for organization plan management and payments. | Medium | P3 |
| 24 | **Audit Log Immutability** | Export audit logs to append-only store (S3/CloudWatch). | Small | P3 |

### Low (Future Differentiators)

| # | Feature | Description | Effort | Priority |
|---|---------|-------------|--------|----------|
| 25 | **GraphQL API** | Alongside REST for complex frontend queries (nested candidate data). | Large | P4 |
| 26 | **Predictive Analytics** | ML models for candidate success prediction, interviewer bias detection. | Large | P4 |
| 27 | **Chatbot / Conversational AI** | Automated candidate Q&A about process, timeline, company. | Large | P4 |
| 28 | **Native Video (WebRTC)** | Built-in video interviewing without Zoom/Meet dependency. | Large | P4 |
| 29 | **Plagiarism Detection** | Code similarity analysis for take-home coding assessments. | Medium | P4 |
| 30 | **Test Case Validation** | Automated test case execution for coding problems (HackerRank-style). | Large | P4 |
| 31 | **Multi-Region Data Residency** | EU data stays in EU (GDPR Article 44+). | Large | P4 |
| 32 | **Mobile SDK** | React Native / Flutter SDK for mobile interview experience. | Large | P4 |
| 33 | **AI Interview Scoring** | Video analysis for body language, confidence, engagement scoring. | Large | P4 |
| 34 | **Skills Assessment Marketplace** | Third-party assessment integration marketplace. | Large | P4 |

---

## Test Coverage Gaps

### Missing Unit Tests (High Priority)

| Module | What's Missing | Risk |
|--------|----------------|------|
| `JwtService` | Token generation, validation, expiry, RSA signing | Auth bypass if broken |
| `Document/S3` | Upload, download, presigned URL generation | File handling failures |
| `MFA Service` | TOTP generation, verification, backup codes | Auth flow broken |
| `GDPR Service` | Consent recording, data anonymization, export | Compliance violations |
| `API Key Service` | Key generation, hashing, validation, scope checks | Security bypass |
| `RateLimitingFilter` | Sliding window, Redis fallback, per-endpoint limits | DoS vulnerability |
| `Notification/Email` | Template rendering, async delivery, retry | Lost notifications |
| `BulkOperations` | Partial failure handling, validation, limits | Data corruption |

### Missing Integration Tests

| Module | What's Missing |
|--------|----------------|
| Pipeline advancement | End-to-end pipeline + scorecard + workflow trigger |
| Offer letter flow | Create → approve → send → respond |
| Calendar sync | OAuth token exchange → event creation → sync |
| Code execution | Submit → container spin-up → execution → result |
| Webhook delivery | Event → delivery → retry → failure handling |

### Missing Performance/Load Tests

| Scenario | Tool Needed | Why |
|----------|-------------|-----|
| 100 concurrent interviews | k6 / Gatling | WebSocket connection limits |
| 1000 bulk schedule | k6 | Database connection pool exhaustion |
| High-frequency code saves | k6 | WebSocket message throughput |
| 50 concurrent code executions | Custom | Docker container limits |
| Rate limiter under load | k6 | Redis INCR race conditions |

---

## Deployment Requirements

### Required External Services for Production

| Service | Required? | Purpose | Free Tier | Monthly Cost (Estimate) |
|---------|-----------|---------|-----------|------------------------|
| **PostgreSQL** (managed) | YES | Primary database | - | $50-200 (RDS/Cloud SQL) |
| **Redis** (managed) | YES | Cache + rate limiting | - | $15-50 (ElastiCache) |
| **Kafka** (managed) | YES | Event streaming | - | $100-300 (MSK/Confluent) |
| **AWS S3** | YES | File storage | 5GB free | $5-50 |
| **SMTP Provider** | YES | Email sending | Limited free | $20-100 (SendGrid/SES) |
| **Docker Host** | YES* | Code execution engine | - | Included in compute |
| **Domain + SSL** | YES | HTTPS termination | - | $10-50/year |
| **HashiCorp Vault** | RECOMMENDED | Secret management | Self-hosted free | $0-100 |
| **OpenAI/Anthropic** | NO** | AI features | $5 credit | $50-500 (usage-based) |
| **Zoom** | NO** | Meeting generation | Free tier | $0-15/month |
| **Google Workspace** | NO** | OAuth2 + Calendar + Meet | Free tier | $0 |
| **Twilio** | NO** | SMS notifications | Trial credit | $20-100 |
| **DocuSign/HelloSign** | NO** | E-signatures | Sandbox free | $25-100 |
| **SonarCloud** | NO | SAST scanning | Free (public) | $0 |

*Required only if Code Execution Engine is enabled  
**Required only if respective feature is enabled

### Minimum Infrastructure for Production

| Component | Minimum Spec | Recommended |
|-----------|-------------|-------------|
| Application Server | 2 vCPU, 4GB RAM | 4 vCPU, 8GB RAM (x2 for HA) |
| PostgreSQL | 2 vCPU, 4GB RAM, 50GB SSD | 4 vCPU, 8GB RAM, 100GB SSD |
| Redis | 1 vCPU, 2GB RAM | 2 vCPU, 4GB RAM (cluster mode) |
| Kafka | 2 vCPU, 4GB RAM | 3 brokers, 4GB each |
| Docker Host (Code Exec) | 2 vCPU, 4GB RAM | Separate node, 4 vCPU, 8GB |
| Load Balancer | - | AWS ALB / nginx |
| Object Storage (S3) | 10GB | 100GB+ |

---

## Production Checklist

### Pre-Deployment (Must Do)

- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Configure HashiCorp Vault with ALL secrets
- [ ] Generate and store `ENCRYPTION_SECRET_KEY` (AES-256): `openssl rand -base64 32`
- [ ] Generate and store `JWT_SECRET` and `JWT_REFRESH_SECRET`: `openssl rand -base64 48`
- [ ] Generate RSA key pair and store in Vault (NOT classpath)
- [ ] Configure real OAuth2 client IDs/secrets (Google, GitHub, Microsoft)
- [ ] Configure SMTP credentials (SendGrid/SES recommended over Gmail)
- [ ] Set `DB_SEPARATE_USERS=true` and create DDL/DML users
- [ ] Set `OTEL_TRACES_SAMPLER_ARG=0.1` (10% sampling)
- [ ] Set proper `CORS_ALLOWED_ORIGINS` (frontend domain only)
- [ ] Disable `spring.jpa.show-sql`
- [ ] Configure SSL/TLS termination at load balancer
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate`
- [ ] Verify all health checks pass (`/actuator/health/liveness`, `/actuator/health/readiness`)
- [ ] Remove or restrict `/actuator` endpoints (only health public)
- [ ] Set `LOCKOUT_ALERTS_ENABLED=true` for security emails
- [ ] Configure S3 bucket with proper IAM policy (least privilege)
- [ ] Set S3 bucket CORS policy for client-side uploads
- [ ] Run `./mvnw dependency-check:check` and resolve HIGH/CRITICAL CVEs

### Security Hardening

- [ ] Ensure no hardcoded secrets in code or config files
- [ ] Enable HSTS with `includeSubDomains` and `preload`
- [ ] Set `Content-Security-Policy` header appropriate for your frontend
- [ ] Configure Redis password authentication (not open in prod)
- [ ] Configure Kafka SSL + SASL authentication
- [ ] Enable PostgreSQL SSL (`sslmode=verify-full`)
- [ ] Set up database backup schedule (pg_dump daily, WAL archiving)
- [ ] Configure Docker socket access securely (if code execution enabled)
- [ ] Set file upload limits (`spring.servlet.multipart.max-file-size=50MB`)
- [ ] Enable IP-based rate limiting for login endpoint
- [ ] Configure firewall rules (only ALB can reach app port)

### Monitoring & Alerting

- [ ] Configure OTel Collector to export to production backend (Datadog/Grafana/New Relic)
- [ ] Set up alerting for:
  - Error rate > 1% (5xx responses)
  - Response time P95 > 2s
  - Database connection pool exhaustion
  - Redis connection failures
  - Kafka consumer lag > 1000
  - Account lockout events (suspicious activity)
  - Disk space > 80%
  - Container restart loops
- [ ] Configure log aggregation (ELK/Loki/CloudWatch)
- [ ] Set up uptime monitoring (external health check)
- [ ] Configure PagerDuty/OpsGenie for critical alerts

### Performance

- [ ] Run load test with k6/Gatling (establish baselines)
- [ ] Verify database indexes are adequate (EXPLAIN ANALYZE on slow queries)
- [ ] Configure HikariCP pool size based on expected load
- [ ] Enable Redis connection pooling
- [ ] Configure Kafka consumer concurrency appropriately
- [ ] Set appropriate JVM heap size (`-Xms512m -Xmx1024m`)
- [ ] Enable GC logging for production debugging

### Backup & Disaster Recovery

- [ ] Automated daily PostgreSQL backups (pg_dump + WAL)
- [ ] S3 versioning enabled on document bucket
- [ ] Define RTO (Recovery Time Objective) — target: 1 hour
- [ ] Define RPO (Recovery Point Objective) — target: 1 hour
- [ ] Test restore procedure from backup
- [ ] Document rollback procedure for failed deployments
- [ ] Configure multi-AZ for database (if cloud-managed)

---

## Infrastructure Needed for Production

### Cloud Services Setup (AWS Example)

```
┌──────────────────────────────────────────────────────────────────┐
│                        AWS VPC                                     │
│                                                                    │
│  ┌─────────────┐    ┌──────────────────────────────────────────┐ │
│  │    ALB      │    │          Private Subnets                  │ │
│  │ (HTTPS/443) │───▶│                                          │ │
│  └─────────────┘    │  ┌────────────┐  ┌────────────┐         │ │
│                      │  │   ECS/EKS  │  │   ECS/EKS  │         │ │
│                      │  │   App (1)  │  │   App (2)  │  (HA)   │ │
│                      │  └──────┬─────┘  └──────┬─────┘         │ │
│                      │         │               │                │ │
│                      │         ▼               ▼                │ │
│                      │  ┌──────────────────────────────┐        │ │
│                      │  │        Internal Services      │        │ │
│                      │  │                              │        │ │
│                      │  │  RDS PostgreSQL (Multi-AZ)   │        │ │
│                      │  │  ElastiCache Redis (Cluster) │        │ │
│                      │  │  MSK Kafka (3 brokers)       │        │ │
│                      │  │  S3 Bucket (documents)       │        │ │
│                      │  │  Vault (ECS/EC2)             │        │ │
│                      │  └──────────────────────────────┘        │ │
│                      └──────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

### Required AWS Services

| Service | AWS Equivalent | Configuration |
|---------|---------------|--------------|
| Compute | ECS Fargate / EKS | 2+ tasks, auto-scaling |
| Database | RDS PostgreSQL 16 | Multi-AZ, encrypted, automated backups |
| Cache | ElastiCache Redis 7 | Cluster mode, encrypted in-transit |
| Messaging | Amazon MSK (Kafka) | 3 brokers, 100GB each |
| Storage | S3 Standard | Versioning, lifecycle rules |
| Secrets | AWS Secrets Manager OR Vault on EC2 | Rotation enabled |
| Load Balancer | Application Load Balancer | SSL termination, health checks |
| DNS | Route 53 | A/AAAA records, health checks |
| SSL | ACM (Certificate Manager) | Auto-renewal |
| Monitoring | CloudWatch + X-Ray | Logs, metrics, traces |
| Container Registry | ECR | Image scanning enabled |
| CI/CD | GitHub Actions → ECR → ECS | Blue/green deployments |

### Required API Keys / Credentials

| Service | Credentials Needed | Where to Get |
|---------|-------------------|--------------|
| Google OAuth2 | Client ID + Client Secret | console.cloud.google.com |
| GitHub OAuth2 | Client ID + Client Secret | github.com/settings/developers |
| Microsoft OAuth2 | Client ID + Client Secret | portal.azure.com |
| SMTP (SendGrid) | API Key | sendgrid.com |
| Twilio (SMS) | Account SID + Auth Token + Phone | twilio.com |
| OpenAI (AI) | API Key | platform.openai.com |
| Zoom | Client ID + Client Secret | marketplace.zoom.us |
| DocuSign | Integration Key + Account ID | developers.docusign.com |
| HelloSign | API Key | hellosign.com/api |
| Slack | Webhook URL | api.slack.com |
| Teams | Webhook URL | Teams channel settings |
| SonarCloud | Token | sonarcloud.io |
| AWS | Access Key + Secret Key + Region | aws.amazon.com/iam |

---

## Future Roadmap

### Phase 9: Integration & Polish (Next 1-2 months)

| # | Feature | Size | Priority | Description |
|---|---------|------|----------|-------------|
| 1 | Real AI Integration (OpenAI) | Medium | P0 | Replace mock with actual GPT-4/Claude calls. Rate limiting, token tracking, model selection. |
| 2 | Real Zoom Integration | Medium | P0 | Zoom Server-to-Server OAuth app, meeting creation, join URLs. |
| 3 | Real Google Meet Integration | Medium | P0 | Google Calendar API, create events with conferenceData. |
| 4 | Real DocuSign Integration | Large | P0 | Envelope creation, embedded signing, webhook status updates. |
| 5 | Real Twilio SMS | Small | P1 | Messages.create with delivery status callbacks. |
| 6 | Excel Export (Apache POI) | Small | P1 | .xlsx generation for export/import module. |
| 7 | Unit Tests for Security | Medium | P1 | JwtService, MFA, API Keys, RateLimiter, GDPR tests. |
| 8 | Load Testing Suite | Medium | P1 | k6 scripts for critical paths, establish performance baselines. |

### Phase 10: Scale & Enterprise (2-4 months)

| # | Feature | Size | Priority | Description |
|---|---------|------|----------|-------------|
| 9 | Read Replicas (PostgreSQL) | Medium | P2 | `@Transactional(readOnly=true)` routing for analytics/reports. |
| 10 | In-App Messaging | Medium | P2 | WebSocket-based recruiter↔candidate chat. |
| 11 | Push Notifications (FCM) | Medium | P2 | Firebase for mobile apps. |
| 12 | Data Retention Policies | Small | P2 | Scheduled auto-purge with configurable TTL per entity type. |
| 13 | Background Check (Checkr) | Medium | P2 | Post-offer automated background verification. |
| 14 | ATS Connectors (Greenhouse) | Large | P3 | Bidirectional sync with major ATS platforms. |
| 15 | Custom Report Builder | Large | P3 | Drag-and-drop report configuration with saved templates. |
| 16 | API Gateway (Kong) | Medium | P3 | Rate limiting offloading, auth caching, request routing. |

### Phase 11: Differentiation (4-6 months)

| # | Feature | Size | Priority | Description |
|---|---------|------|----------|-------------|
| 17 | GraphQL Layer | Large | P3 | Alongside REST for complex nested queries. |
| 18 | CQRS + Elasticsearch | Large | P3 | Materialized views for dashboard/analytics read path. |
| 19 | Native WebRTC Video | Large | P4 | Built-in video without Zoom/Meet dependency. |
| 20 | AI Interview Scoring | Large | P4 | Video analysis for engagement, confidence metrics. |
| 21 | Plagiarism Detection | Medium | P4 | Code similarity for take-home assessments. |
| 22 | Test Case Validation | Large | P4 | HackerRank-style automated test execution. |
| 23 | Multi-Region (Data Residency) | Large | P4 | EU data stays in EU (GDPR Article 44). |
| 24 | Microservices Extraction | Large | P4 | Notification, AI, Document services as separate deployables. |

---

## Architecture & Statistics

### Current Metrics

| Metric | Count |
|--------|-------|
| Java source files | ~460+ |
| JPA Entities | 55+ |
| REST Controllers | 52 |
| Services | 40+ |
| Repositories | 40+ |
| Flyway Migrations | 30 (V1-V30) |
| API Endpoints | ~280+ |
| Supported Languages (Code Exec) | 10 |
| OAuth2 Providers | 3 |
| SSO/SAML Providers | 4 |
| Notification Channels | 5 (Email, SMS, In-App, Slack, Teams) |
| Docker Compose Services | 8 |
| CI/CD Pipeline Stages | 7 |
| Test Methods | 559 |
| Total LOC (Java) | ~25,000+ |

### Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 4.0.6 |
| Security | Spring Security + JWT (RS256) + OAuth2 + SAML2 | 6.x |
| ORM | Spring Data JPA / Hibernate | 6.x |
| Database | PostgreSQL | 16 |
| Migrations | Flyway | 10.x |
| Cache | Redis + Caffeine (L1/L2) | 7 / 3.x |
| Messaging | Apache Kafka | 7.6.0 |
| Real-time | WebSocket (STOMP) | - |
| File Storage | AWS S3 (LocalStack for dev) | SDK 2.25 |
| Secrets | HashiCorp Vault | 1.15 |
| Containers | Docker + Docker Compose | 24+ |
| CI/CD | GitHub Actions | - |
| SAST | SonarCloud | - |
| Dep Scan | OWASP Dependency-Check | 9.0.9 |
| Container Scan | Trivy | latest |
| Observability | OpenTelemetry + Jaeger + Prometheus | 2.12.0 |
| Logging | Logstash JSON Encoder + MDC | 7.4 |
| API Docs | SpringDoc OpenAPI (Swagger UI) | 2.8.6 |
| Resilience | Resilience4j | 2.2.0 |
| Scheduling | ShedLock (distributed) | 6.0.2 |
| E-Signature | DocuSign + HelloSign (simulated) | - |
| Code Execution | Docker Java Client | 3.3.6 |
| SAML | OpenSAML (via Spring Security) | 5.1.6 |
| PDF | OpenPDF | 2.0.2 |
| CSV | Apache Commons CSV | 1.11.0 |
| MFA | TOTP (dev.samstevens) | 1.7.1 |
| Google API | google-api-client | 2.2.0 |

---

## Dependency Upgrade Schedule

| Dependency | Current | Latest | Action | Risk |
|-----------|---------|--------|--------|------|
| Spring Boot | 4.0.6 | 4.0.x | Current | - |
| Java | 21 | 21 (LTS) | Current | - |
| jjwt | 0.11.5 | 0.12.6 | **Upgrade recommended** | Low (API changes) |
| PostgreSQL Driver | 42.x | 42.x | Current | - |
| Flyway | (managed) | 11.x | Current | - |
| SpringDoc OpenAPI | 2.8.6 | 2.8.x | Current | - |
| Resilience4j | 2.2.0 | 2.2.x | Current | - |
| ShedLock | 6.0.2 | 6.x | Current | - |
| AWS SDK | 2.25.60 | 2.x | Current | - |
| Docker Java | 3.3.6 | 3.x | Current | - |
| Nimbus JOSE JWT | 9.37.3 | 9.x | Current | - |
| OpenSAML | 5.1.6 | 5.x | Current | - |
