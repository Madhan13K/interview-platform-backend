# Project Roadmap & Feature Audit

## Interview Platform Backend

**Repository:** https://github.com/Madhan13K/interview-platform-backend  
**Last Updated:** 2026-06-18

---

## Feature Status Legend

| Status | Meaning |
|--------|---------|
| DONE | Fully implemented, compiles, has migrations |
| IN PROGRESS | Package created, partially implemented |
| PLANNED | Scoped, not yet started |

---

## Implemented Features (DONE)

### Core Platform (Phase 1-4)

| # | Feature | Package | Migrations | Status |
|---|---------|---------|-----------|--------|
| 1 | Authentication (Local + JWT) | `security/auth` | V1 | DONE |
| 2 | OAuth2 (Google, GitHub, Microsoft) | `security/oauth2` | V4 | DONE |
| 3 | RBAC (Roles, Permissions) | `user`, `security` | V1, V5 | DONE |
| 4 | User Management & Profiles | `user` | V1 | DONE |
| 5 | Interview CRUD & Lifecycle | `candidate` | V2 | DONE |
| 6 | Interview Feedback | `candidate` | V2 | DONE |
| 7 | Refresh Token Rotation + Replay Detection | `security/token` | V3, V8 | DONE |
| 8 | Email Verification | `security/auth` | V7 | DONE |
| 9 | Password Reset | `security/auth` | V6 | DONE |
| 10 | Collaborative Code Editor (WebSocket) | `codeeditor` | V9 | DONE |
| 11 | Question Bank | `questionbank` | V9 | DONE |
| 12 | Notifications (Email, SMS, In-App, Kafka) | `notification` | V10 | DONE |
| 13 | Interview Templates | `template` | V11 | DONE |
| 14 | Evaluation Scorecards | `scorecard` | V12 | DONE |
| 15 | Hiring Pipelines & Candidate Progression | `pipeline` | V13 | DONE |
| 16 | Document/File Management (S3) | `document` | V14 | DONE |
| 17 | Job Positions | `jobposition` | V15 | DONE |
| 18 | Automated Scheduling (Availability + Suggest) | `scheduling` | V16 | DONE |
| 19 | Interview Reminders | `reminder` | V16 | DONE |
| 20 | Candidate Self-Service (Preferred Slots) | `selfservice` | V16 | DONE |
| 21 | Teams & Departments | `team` | V16 | DONE |
| 22 | Tags & Labels | `tag` | V16 | DONE |
| 23 | AI Features (Questions, Resume Parse, Summary) | `ai` | V17 | DONE |
| 24 | Video Recording Integration | `video` | V17 | DONE |
| 25 | Whiteboard Collaboration | `whiteboard` | V17 | DONE |
| 26 | Webhook Integrations | `webhook` | V17 | DONE |
| 27 | Multi-Tenant Support | `tenant` | V17 | DONE |
| 28 | Candidate Reverse Feedback | `candidatefeedback` | V17 | DONE |
| 29 | Activity Feed / Timeline | `activity` | V17 | DONE |
| 30 | MFA/TOTP | `security/mfa` | V18 | DONE |
| 31 | GDPR Compliance (Consent, Erasure, Export) | `gdpr` | V18 | DONE |
| 32 | API Key Authentication | `security/apikey` | V18 | DONE |
| 33 | Bulk Operations (Schedule, Invite, Export) | `bulk` | - | DONE |
| 34 | Reports & Analytics (JSON + PDF) | `report` | - | DONE |
| 35 | Export/Import (CSV, JSON) | `exportimport` | - | DONE |
| 36 | Meeting Link Generation (Zoom, Google Meet) | `meeting` | V9 | DONE |
| 37 | Dashboard (Admin, Interviewer, Candidate) | `dashboard` | - | DONE |
| 38 | Audit Logging | `audit` | V17 | DONE |
| 39 | Calendar (Interviewer Availability) | `calendar` | V16 | DONE |

### New Features (Phase 5-7)

| # | Feature | Size | Package | Migrations | Status |
|---|---------|------|---------|-----------|--------|
| 40 | Code Execution Engine (Docker Sandbox) | Large | `codeexecution` | V19 | DONE |
| 41 | SSO/SAML Integration (Okta, OneLogin, Azure AD) | Medium | `sso` | V20 | DONE |
| 42 | Account Lockout & IP Blocking | Medium | `accountlockout` | V21 | DONE |
| 43 | Data Encryption at Rest (AES-256-GCM) | Medium | `encryption` | V22 | DONE |
| 44 | Candidate Portal / Job Board | Large | `jobboard` | V23 | DONE |
| 45 | Offer Letter Management + E-Signature | Large | `offer` | V24 | DONE |
| 46 | Calendar Sync (Google + Outlook Bidirectional) | Medium | `calendarsync` | V25 | DONE |

### Infrastructure & DevOps

| # | Feature | Status |
|---|---------|--------|
| 47 | HashiCorp Vault Secret Management | DONE |
| 48 | Structured JSON Logging + Correlation IDs (MDC) | DONE |
| 49 | CI/CD Pipeline (GitHub Actions) | DONE |
| 50 | SAST Scanning (SonarCloud) | DONE |
| 51 | OWASP Dependency Check (CVE Scanning) | DONE |
| 52 | Docker Image Scanning (Trivy) | DONE |
| 53 | Integration Tests (Testcontainers) | DONE |
| 54 | Database User Separation (DDL vs DML) | DONE |
| 55 | OpenTelemetry Observability (Traces, Metrics) | DONE |
| 56 | Rate Limiting (Redis-backed) | DONE |
| 57 | XSS Sanitization Filter | DONE |
| 58 | CORS Configuration | DONE |
| 59 | Security Headers (HSTS, CSP, X-Frame-Options) | DONE |
| 60 | Branch Protection (master - PR only) | DONE |

---

## Planned Features (IN PROGRESS / PLANNED)

### Phase 8: Workflow & Automation

| # | Feature | Size | Package | Status | Description |
|---|---------|------|---------|--------|-------------|
| 61 | Configurable Workflow Engine | Large | `workflow` | DONE | Rule-based automation (e.g., "if avg score > 4, auto-advance to next stage"). Supports triggers, conditions, and actions. |
| 62 | Approval Workflows | Medium | `approval` | DONE | Configurable approval chains for offers, job requisitions, job postings. Sequential/parallel/any-one approvers. |
| 63 | Referral Program | Medium | `referral` | DONE | Employee referral tracking, bonus workflows, referral source analytics, leaderboards. |
| 64 | DEI/Diversity Analytics | Medium | `dei` | DONE | Opt-in demographic tracking, funnel analysis by diversity categories, aggregated-only stats (privacy-first). |
| 65 | Source Effectiveness | Small | `sourcetracking` | DONE | Track candidate sources (LinkedIn, referral, job board), calculate source ROI, cost-per-hire. |

---

## Architecture Summary

### Packages (47 total)

```
src/main/java/com/interview_platform_backend/interview_platform_backend/
├── accountlockout/        # Account lockout & IP blocking [V21]
├── activity/              # Activity feed & timeline [V17]
├── ai/                    # AI features (questions, resume, summary) [V17]
├── approval/              # Approval workflows [PLANNED]
├── audit/                 # Audit logging [V17]
├── bulk/                  # Bulk operations
├── calendar/              # Interviewer availability [V16]
├── calendarsync/          # Google/Outlook calendar sync [V25]
├── candidate/             # Interview management & feedback [V2]
├── candidatefeedback/     # Candidate reverse feedback [V17]
├── codeeditor/            # Real-time collaborative code editor [V9]
├── codeexecution/         # Docker sandboxed code execution [V19]
├── config/                # App configuration
│   ├── logging/           # MDC correlation IDs, async MDC
│   └── vault/             # Vault RSA key loading
├── dashboard/             # Admin/Interviewer/Candidate dashboards
├── dei/                   # DEI/Diversity analytics [PLANNED]
├── document/              # File management (S3) [V14]
├── encryption/            # AES-256-GCM field encryption [V22]
├── event/                 # Domain events
├── exception/             # Global exception handling
├── exportimport/          # CSV/JSON export/import
├── gdpr/                  # GDPR compliance [V18]
├── jobboard/              # Public job board & applications [V23]
├── jobposition/           # Job position management [V15]
├── meeting/               # Meeting link generation [V9]
├── notification/          # Email, SMS, In-App, Kafka [V10]
├── offer/                 # Offer letters & e-signature [V24]
├── pipeline/              # Hiring pipeline [V13]
├── questionbank/          # Question bank [V9]
├── referral/              # Referral program [PLANNED]
├── reminder/              # Interview reminders [V16]
├── report/                # Reports & analytics (JSON + PDF)
├── scheduling/            # Automated scheduling [V16]
├── scorecard/             # Evaluation scorecards [V12]
├── security/              # Auth, JWT, OAuth2, RBAC, MFA, API keys
├── selfservice/           # Candidate self-service [V16]
├── sourcetracking/        # Source effectiveness [PLANNED]
├── sso/                   # SSO/SAML integration [V20]
├── tag/                   # Tags & labels [V16]
├── team/                  # Teams & departments [V16]
├── template/              # Interview templates [V11]
├── tenant/                # Multi-tenant [V17]
├── user/                  # User management [V1]
├── video/                 # Video recording [V17]
├── webhook/               # Webhook integrations [V17]
├── websocket/             # WebSocket configuration
├── whiteboard/            # Whiteboard collaboration [V17]
└── workflow/              # Workflow engine [PLANNED]
```

### Database Migrations (25 total)

| Migration | Description |
|-----------|-------------|
| V1 | Auth & RBAC tables (users, roles, permissions, user_roles, role_permissions) |
| V2 | Interview tables (interviews, interview_interviewers, interview_feedback) |
| V3 | Token family for refresh token replay detection |
| V4 | Auth provider enum, drop sessions table |
| V5 | Seed default RBAC data (roles, permissions) |
| V6 | Password reset tokens |
| V7 | Email verification tokens |
| V8 | Alter refresh_tokens.token to TEXT |
| V9 | Coding sessions, question bank, meeting links |
| V10 | Notifications table |
| V11 | Interview templates |
| V12 | Evaluation scorecards & criteria |
| V13 | Hiring pipelines, stages, candidate progress |
| V14 | Documents table |
| V15 | Job positions |
| V16 | Scheduling, reminders, self-service, teams, tags |
| V17 | AI, video, whiteboard, webhooks, tenant, feedback, activity |
| V18 | MFA, GDPR, API keys |
| V19 | Code executions (sandboxed Docker execution) |
| V20 | SSO/SAML configurations |
| V21 | Account lockout, login attempts, IP blocklist |
| V22 | Column alterations for field-level encryption |
| V23 | Job applications (candidate portal) |
| V24 | Offer letters & approval workflow |
| V25 | Calendar sync (connections + events) |

### API Endpoints Count

| Category | Endpoints |
|----------|-----------|
| Authentication | 11 |
| Users & Roles | 25 |
| Interviews | 20 |
| Templates & Questions | 15 |
| Code Editor & Execution | 9 |
| Job Positions | 12 |
| Job Board (Public) | 3 |
| Candidate Portal | 6 |
| Offer Letters | 11 |
| Calendar & Scheduling | 12 |
| Calendar Sync | 7 |
| Pipelines | 11 |
| Scorecards | 11 |
| Notifications | 5 |
| Documents | 10 |
| Meeting | 2 |
| Reports & Dashboard | 10 |
| Bulk Operations | 4 |
| Export/Import | 5 |
| Webhooks | 8 |
| Audit | 3 |
| Teams & Tags | 14 |
| Self-Service | 6 |
| Reminders | 4 |
| AI | 6 |
| Video | 7 |
| Whiteboard | 8 |
| SSO/SAML | 7 |
| Account Security | 6 |
| Multi-Tenant | 9 |
| GDPR | 5 |
| Candidate Feedback | 4 |
| Activity Feed | 5 |
| **Total** | **~280+** |

### Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 4.0.6 |
| Security | Spring Security + JWT (RS256) + OAuth2 + SAML2 | 6.x |
| ORM | Spring Data JPA / Hibernate | 6.x |
| Database | PostgreSQL | 16 |
| Migrations | Flyway | 10.x |
| Cache | Redis + Caffeine | 7 / 3.x |
| Messaging | Apache Kafka | 7.6.0 |
| Real-time | WebSocket (STOMP) | - |
| File Storage | AWS S3 (LocalStack for dev) | SDK 2.25 |
| Secrets | HashiCorp Vault | 1.15 |
| Containers | Docker + Docker Compose | 24+ |
| CI/CD | GitHub Actions | - |
| SAST | SonarCloud | - |
| Dependency Scan | OWASP Dependency-Check | 9.0.9 |
| Container Scan | Trivy | latest |
| Observability | OpenTelemetry + Jaeger + Prometheus | 2.12.0 |
| Logging | Logstash JSON Encoder + MDC | 7.4 |
| API Docs | SpringDoc OpenAPI (Swagger UI) | 2.8.6 |
| Resilience | Resilience4j | 2.2.0 |
| Scheduling | ShedLock | 6.0.2 |
| E-Signature | DocuSign + HelloSign (simulated) | - |
| Code Execution | Docker Java Client | 3.3.6 |
| SAML | OpenSAML (via Spring Security) | 5.1.6 |
| PDF | OpenPDF | 2.0.2 |
| CSV | Apache Commons CSV | 1.11.0 |
| MFA | TOTP (dev.samstevens) | 1.7.1 |

---

## Metrics & Statistics

| Metric | Count |
|--------|-------|
| Java source files | ~460+ |
| JPA Entities | ~55+ |
| REST Controllers | ~35+ |
| Services | ~40+ |
| Repositories | ~40+ |
| Flyway Migrations | 25 |
| API Endpoints | ~280+ |
| Supported Languages (Code Exec) | 10 |
| OAuth2 Providers | 3 (Google, GitHub, Microsoft) |
| SSO/SAML Providers | 4 (Okta, OneLogin, Azure AD, Generic) |
| Notification Channels | 5 (Email, SMS, In-App, Slack, Teams) |
| Docker Compose Services | 8 |
| CI/CD Pipeline Stages | 7 |

---

## Upcoming Priorities

### Phase 8 (Next)

1. **Configurable Workflow Engine** (Large)
   - Rule DSL: trigger + condition + action
   - Example: "When interview.completed AND avgScore > 4.0 THEN advance candidate to next pipeline stage"
   - Visual workflow builder support (API-driven)

2. **Approval Workflows** (Medium)
   - Generic approval chains (not just offers)
   - Apply to: job requisitions, job postings, budget approvals
   - Sequential and parallel approval modes

3. **Referral Program** (Medium)
   - Employee referral submission
   - Referral status tracking (applied, interviewed, hired)
   - Bonus eligibility and payout tracking
   - Referral leaderboard and analytics

4. **DEI/Diversity Analytics** (Medium)
   - Opt-in demographic data collection
   - Funnel analysis by demographic categories
   - Bias detection in scoring patterns
   - Compliance reporting (EEO-1)

5. **Source Effectiveness** (Small)
   - Track candidate acquisition source
   - Cost-per-hire by source
   - Source-to-hire conversion rates
   - ROI calculations

### Future Considerations

- GraphQL API layer
- Mobile push notifications (Firebase)
- AI interview scoring (video analysis)
- Automated reference checking
- Background check integration
- Compensation benchmarking
- Skills assessment marketplace
- Candidate relationship management (CRM)
- Internal mobility / career pathing
