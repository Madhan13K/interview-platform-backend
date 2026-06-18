# System Architecture

## Interview Platform Backend

---

## 1. System Overview

The Interview Platform is a comprehensive Applicant Tracking System (ATS) and interview management platform built with Spring Boot 4.0.6 on Java 21. It provides end-to-end hiring lifecycle management: from public job postings and candidate applications, through interview scheduling, real-time collaborative coding assessments, evaluation scorecards, and hiring pipeline progression, to offer letter management with e-signature integration. The platform supports multi-tenancy, enterprise SSO (SAML), role-based access control, and real-time collaboration via WebSocket.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CLIENT APPLICATIONS                                │
│        React SPA  │  Mobile App  │  Admin Panel  │  Public Job Board         │
└────────────┬────────────────┬───────────────┬───────────────────────────────┘
             │ HTTPS           │ WebSocket      │ Public (no auth)
             ▼                 ▼                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SPRING BOOT APPLICATION                              │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                      SECURITY FILTER CHAIN                             │ │
│  │  XSS Filter → Rate Limiter → API Key Auth → JWT Auth → Controllers   │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌──────────────┐  ┌───────────────┐  ┌────────────────┐  ┌────────────┐  │
│  │  REST API    │  │  WebSocket    │  │  SAML2 SSO     │  │  OAuth2    │  │
│  │  Controllers │  │  (STOMP)      │  │  Endpoints     │  │  Handlers  │  │
│  └──────┬───────┘  └──────┬────────┘  └───────┬────────┘  └─────┬──────┘  │
│         │                  │                    │                  │         │
│  ┌──────▼──────────────────▼────────────────────▼──────────────────▼──────┐ │
│  │                        SERVICE LAYER (50+ Services)                     │ │
│  │  Auth │ User │ Interview │ Pipeline │ Code Exec │ Workflow Engine      │ │
│  │  Offer │ Calendar │ Notification │ Approval │ Referral │ Analytics    │ │
│  └──────────────────────────┬─────────────────────────────────────────────┘ │
│                              │                                               │
│  ┌──────────────────────────▼─────────────────────────────────────────────┐ │
│  │                   REPOSITORY LAYER (Spring Data JPA)                    │ │
│  └──────────────────────────┬─────────────────────────────────────────────┘ │
│                              │                                               │
└──────────────────────────────┼───────────────────────────────────────────────┘
                               │
     ┌───────────┬─────────────┼──────────────┬──────────────┬────────────┐
     ▼           ▼             ▼              ▼              ▼            ▼
┌─────────┐ ┌────────┐ ┌───────────┐ ┌───────────┐ ┌──────────┐ ┌──────────┐
│PostgreSQL│ │ Redis  │ │   Kafka   │ │  AWS S3   │ │  Vault   │ │   OTel   │
│  (DB)   │ │(Cache) │ │ (Events)  │ │  (Files)  │ │(Secrets) │ │(Tracing) │
│ :5433   │ │ :6379  │ │  :9092    │ │  :4566    │ │  :8200   │ │  :4318   │
└─────────┘ └────────┘ └───────────┘ └───────────┘ └──────────┘ └────┬─────┘
                                                                       │
                                                                       ▼
                                                                 ┌──────────┐
                                                                 │  Jaeger  │
                                                                 │ (Traces) │
                                                                 │  :16686  │
                                                                 └──────────┘
```

---

## 2. Module Map

### Core Platform

| Module | Package | Description |
|--------|---------|-------------|
| **User Management** | `user` | User CRUD, profiles, status management, search |
| **Authentication** | `security/auth` | Registration, login, password reset, email verification |
| **JWT** | `security/jwt` | RSA-256 access tokens, HMAC refresh tokens, JWKS endpoint |
| **OAuth2** | `security/oauth2` | Google, GitHub, Microsoft social login with PKCE |
| **SAML/SSO** | `sso` | Enterprise SSO for Okta, OneLogin, Azure AD |
| **MFA** | `security/mfa` | TOTP-based two-factor authentication |
| **API Keys** | `security/apikey` | Service-to-service authentication |
| **Account Lockout** | `accountlockout` | Brute-force protection, IP blocking, alerts |
| **Interviews** | `candidate` | Interview scheduling, lifecycle, feedback collection |
| **Templates** | `template` | Reusable interview templates with questions |
| **Question Bank** | `questionbank` | Categorized question library |

### Hiring Pipeline

| Module | Package | Description |
|--------|---------|-------------|
| **Pipeline** | `pipeline` | Multi-stage hiring pipelines with candidate progression |
| **Job Positions** | `jobposition` | Internal job position management |
| **Job Board** | `jobboard` | Public job listings, candidate applications, status tracking |
| **Offer Letters** | `offer` | Offer creation, approval workflow, e-signature (DocuSign/HelloSign) |
| **Approvals** | `approval` | Generic approval chains (sequential/parallel/any-one) |
| **Scorecards** | `scorecard` | Weighted evaluation criteria and scoring |

### Communication

| Module | Package | Description |
|--------|---------|-------------|
| **Notifications** | `notification` | Multi-channel: Email, SMS, In-App, Kafka-driven |
| **Meeting Links** | `meeting` | Zoom, Google Meet, internal meeting generation |
| **Reminders** | `reminder` | Automated 24h/1h/15min interview reminders |
| **Webhooks** | `webhook` | External system notifications with HMAC signatures |
| **Calendar** | `calendar` | Interviewer availability management |
| **Calendar Sync** | `calendarsync` | Bidirectional Google Calendar / Outlook sync |

### Collaboration

| Module | Package | Description |
|--------|---------|-------------|
| **Code Editor** | `codeeditor` | Real-time collaborative code editor via WebSocket |
| **Code Execution** | `codeexecution` | Docker-sandboxed code runner (10 languages) |
| **Whiteboard** | `whiteboard` | Real-time drawing/diagramming via WebSocket |
| **Video** | `video` | Interview recording management (S3 storage) |
| **WebSocket** | `websocket` | STOMP configuration and JWT auth for WS |

### Automation

| Module | Package | Description |
|--------|---------|-------------|
| **Workflow Engine** | `workflow` | Rule-based automation (trigger → condition → action) |
| **Scheduling** | `scheduling` | Auto-suggest interview slots based on availability |
| **Self-Service** | `selfservice` | Candidate preferred time slot submission |
| **Bulk Operations** | `bulk` | Batch scheduling, invitations, exports |
| **Export/Import** | `exportimport` | Async CSV/JSON data export and import |

### Analytics & Tracking

| Module | Package | Description |
|--------|---------|-------------|
| **Dashboard** | `dashboard` | Role-based dashboards (Admin, Interviewer, Candidate) |
| **Reports** | `report` | JSON + PDF analytics reports |
| **Activity Feed** | `activity` | Chronological timeline per entity |
| **Audit** | `audit` | Immutable audit trail for all operations |
| **DEI Analytics** | `dei` | Opt-in diversity metrics, aggregated funnel analysis |
| **Source Tracking** | `sourcetracking` | Candidate source ROI and effectiveness |
| **Referrals** | `referral` | Employee referral program with bonus tracking |

### Compliance & Security

| Module | Package | Description |
|--------|---------|-------------|
| **GDPR** | `gdpr` | Consent tracking, data export, right-to-erasure |
| **Encryption** | `encryption` | AES-256-GCM field-level PII encryption |
| **Account Lockout** | `accountlockout` | Failed login tracking, IP blocking |
| **SSO** | `sso` | SAML2 IdP management per tenant |

### Infrastructure

| Module | Package | Description |
|--------|---------|-------------|
| **Vault Config** | `config/vault` | RSA key loading from HashiCorp Vault |
| **Logging** | `config/logging` | Correlation ID filter, MDC propagation, JSON logging |
| **Tenant** | `tenant` | Multi-organization support |
| **Tags** | `tag` | Flexible entity tagging system |
| **Teams** | `team` | Team/department management |
| **Events** | `event` | Spring domain event publishing |

---

## 3. Data Flow Diagrams

### User Registration & Login

```
┌──────────┐    POST /register    ┌───────────────┐    BCrypt     ┌──────────┐
│  Client  │ ──────────────────▶  │ AuthController │ ──────────▶  │   DB     │
└──────────┘                      └───────┬───────┘               └──────────┘
                                          │
                                          ▼
                                  ┌───────────────┐    Kafka      ┌──────────┐
                                  │  Email        │ ◀──────────── │ Notif    │
                                  │  Verification │               │ Service  │
                                  └───────┬───────┘               └──────────┘
                                          │
                                          ▼
┌──────────┐    GET /verify-email ┌───────────────┐  Status=ACTIVE ┌──────────┐
│  Client  │ ──────────────────▶  │ AuthController │ ──────────────▶│   DB     │
└──────────┘                      └───────────────┘                 └──────────┘

LOGIN:
┌──────────┐    POST /login       ┌───────────────┐
│  Client  │ ──────────────────▶  │ AuthController │
└──────────┘                      └───────┬───────┘
                                          │
                              ┌───────────▼───────────┐
                              │  AccountLockoutService │
                              │  (check IP + lockout)  │
                              └───────────┬───────────┘
                                          │ PASS
                              ┌───────────▼───────────┐
                              │ AuthenticationManager  │
                              │  (verify credentials)  │
                              └───────────┬───────────┘
                                          │ SUCCESS
                              ┌───────────▼───────────┐
                              │     JwtService         │
                              │ (RSA access + HMAC     │
                              │  refresh token)        │
                              └───────────┬───────────┘
                                          │
                                          ▼
┌──────────┐   {accessToken, refreshToken} 
│  Client  │ ◀────────────────────────────
└──────────┘
```

### Interview Lifecycle

```
SCHEDULE                    CONDUCT                     EVALUATE
────────────────────────────────────────────────────────────────────────

┌──────────┐  Create    ┌──────────┐  Start     ┌──────────────┐
│RECRUITER │──────────▶ │ SCHEDULED│──────────▶ │ IN_PROGRESS  │
└──────────┘            └──────────┘            └──────┬───────┘
     │                       │                         │
     │ Assign                │ Reschedule              │ Code Editor
     │ Interviewers          │                         │ Whiteboard
     │                       ▼                         │ Meeting Link
     │                  ┌──────────┐                   │
     │                  │RESCHEDULED│                   │
     │                  └──────────┘                   │
     │                       │                         │
     │                       │ Cancel                  ▼
     │                       ▼                   ┌──────────┐
     │                  ┌──────────┐             │COMPLETED │
     │                  │CANCELLED │             └──────┬───┘
     │                  └──────────┘                    │
     │                                                  │ Submit Feedback
     │                                                  ▼
     │                                          ┌──────────────┐
     │                                          │  FEEDBACK    │
     │                                          │  (per        │
     │                                          │  interviewer)│
     │                                          └──────┬───────┘
     │                                                 │
     │                         ┌───────────────────────┤
     │                         ▼                       ▼
     │                  ┌──────────────┐       ┌──────────────┐
     │                  │  SCORECARD   │       │  WORKFLOW     │
     │                  │  (weighted)  │       │  ENGINE       │
     │                  └──────────────┘       │  (auto-rules) │
     │                                         └──────┬───────┘
     │                                                │
     ▼                                                ▼
┌──────────────────────────────────────────────────────────────┐
│                    PIPELINE ADVANCEMENT                        │
│  Screening → Technical → HR → Final → Offer → Hired          │
└──────────────────────────────────────────────────────────────┘
```

### Code Execution Flow

```
┌──────────┐  POST /code-execution/run  ┌──────────────────┐
│  Client  │ ─────────────────────────▶ │ CodeExecController│
└──────────┘                            └────────┬─────────┘
                                                 │
                                    ┌────────────▼────────────┐
                                    │  CodeExecutionService    │
                                    │  (validate, queue)       │
                                    └────────────┬────────────┘
                                                 │ @Async
                                    ┌────────────▼────────────┐
                                    │  Docker Client           │
                                    │                          │
                                    │  1. Create container     │
                                    │     - Memory: 256MB      │
                                    │     - CPU: 50%           │
                                    │     - Network: none      │
                                    │     - User: nobody       │
                                    │     - CAP_DROP: ALL      │
                                    │                          │
                                    │  2. Copy source (TAR)    │
                                    │  3. Start container      │
                                    │  4. Wait (with timeout)  │
                                    │  5. Capture stdout/err   │
                                    │  6. Remove container     │
                                    └────────────┬────────────┘
                                                 │
                                    ┌────────────▼────────────┐
                                    │  Store result in DB      │
                                    │  (status, output, time)  │
                                    └─────────────────────────┘

┌──────────┐  GET /code-execution/{id}   ┌──────────────┐
│  Client  │ ──────────────────────────▶ │ Result       │
│  (poll)  │ ◀────────────────────────── │ {stdout,     │
└──────────┘                             │  stderr,     │
                                         │  exitCode,   │
                                         │  timeMs}     │
                                         └──────────────┘
```

### Notification Flow (Event-Driven)

```
┌──────────────┐      Spring Event       ┌──────────────────┐
│ Any Service  │ ──────────────────────▶  │  Event Publisher  │
│ (interview   │                          └────────┬─────────┘
│  scheduled)  │                                   │
└──────────────┘                                   ▼
                                          ┌──────────────────┐
                                          │   Kafka Producer  │
                                          │  topic: notif-    │
                                          │  events           │
                                          └────────┬─────────┘
                                                   │
                              ┌─────────────────────┼──────────────────────┐
                              ▼                     ▼                      ▼
                     ┌──────────────┐     ┌──────────────┐      ┌──────────────┐
                     │ Email Worker │     │  SMS Worker  │      │ Slack/Teams  │
                     │ (Thymeleaf   │     │  (Twilio)    │      │ (Webhook)    │
                     │  templates)  │     │              │      │              │
                     └──────────────┘     └──────────────┘      └──────────────┘
                              │                     │                      │
                              ▼                     ▼                      ▼
                     ┌──────────────┐     ┌──────────────┐      ┌──────────────┐
                     │  Gmail SMTP  │     │   Twilio     │      │ Slack/Teams  │
                     │              │     │   API        │      │ API          │
                     └──────────────┘     └──────────────┘      └──────────────┘
```

### Calendar Sync Flow

```
┌──────────┐  POST /calendar-sync/connect  ┌─────────────────────┐
│  Client  │ ────────────────────────────▶ │ CalendarSyncController│
│  (with   │                               └──────────┬──────────┘
│  OAuth   │                                          │
│  code)   │                               ┌──────────▼──────────┐
└──────────┘                               │ CalendarSyncService  │
                                           └──────────┬──────────┘
                                                      │
                              ┌────────────────────────┤
                              ▼                        ▼
                     ┌──────────────────┐    ┌──────────────────┐
                     │GoogleCalProvider  │    │OutlookCalProvider │
                     │                  │    │                  │
                     │Exchange code     │    │Exchange code     │
                     │for tokens at     │    │for tokens at     │
                     │googleapis.com    │    │login.microsoft   │
                     └────────┬─────────┘    └────────┬─────────┘
                              │                        │
                              ▼                        ▼
                     ┌──────────────────────────────────────────┐
                     │  Store CalendarConnection (tokens, etc.)  │
                     └──────────────────────────────────────────┘

BIDIRECTIONAL SYNC:
┌─────────────────────────────────────────────────────────────────┐
│                                                                   │
│  OUTBOUND (Local → External):                                    │
│  Find interviews for user → For each without synced event →      │
│  Create event via Google/Outlook API → Store CalendarEvent       │
│                                                                   │
│  INBOUND (External → Local):                                     │
│  Fetch events from Google/Outlook API (timeMin/timeMax) →        │
│  Compare with local events → Update/import changes               │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### Workflow Engine Flow

```
┌──────────────────┐
│  Trigger Event   │  (e.g., FEEDBACK_SUBMITTED)
│  from any service│
└────────┬─────────┘
         │
         ▼
┌────────────────────────────────────────────┐
│          WorkflowEngineService              │
│                                            │
│  1. Find rules where:                      │
│     trigger_event = event AND enabled=true │
│     ORDER BY priority ASC                  │
│                                            │
│  2. For each rule:                         │
│     ┌──────────────────────────────┐       │
│     │ ConditionEvaluator           │       │
│     │                              │       │
│     │ SCORE_ABOVE: avg >= value?   │       │
│     │ ALL_FEEDBACK_IN: all done?   │       │
│     │ RECOMMENDATION_COUNT:        │       │
│     │   count(HIRE) >= N?          │       │
│     └──────────────┬───────────────┘       │
│                    │ true                   │
│     ┌──────────────▼───────────────┐       │
│     │ ActionExecutor               │       │
│     │                              │       │
│     │ ADVANCE_PIPELINE_STAGE       │       │
│     │ SEND_EMAIL (with template)   │       │
│     │ CHANGE_INTERVIEW_STATUS      │       │
│     │ REJECT_CANDIDATE             │       │
│     │ NOTIFY_RECRUITER             │       │
│     │ WEBHOOK_CALL                 │       │
│     └──────────────────────────────┘       │
│                                            │
│  3. Record WorkflowExecution (audit)       │
└────────────────────────────────────────────┘
```

---

## 4. Authentication Flows

### Local Email/Password

```
Register → BCrypt hash → Save (PENDING_VERIFICATION) → Send email → Verify → ACTIVE → Login → JWT
```

### OAuth2 with PKCE

```
Client                     Backend                    Provider (Google/GitHub/MS)
  │                           │                              │
  │  GET /oauth2/auth/google  │                              │
  │ ─────────────────────────▶│                              │
  │                           │  302 Redirect + code_challenge
  │                           │ ────────────────────────────▶│
  │  ◀─────────────────────────────────────────────────────  │
  │  (User logs in at provider)                              │
  │  ─────────────────────────────────────────────────────▶  │
  │                           │  Callback + auth_code        │
  │                           │ ◀────────────────────────────│
  │                           │                              │
  │                           │  Exchange code + verifier    │
  │                           │ ────────────────────────────▶│
  │                           │  {access_token, id_token}    │
  │                           │ ◀────────────────────────────│
  │                           │                              │
  │                           │  Find/Create user            │
  │                           │  Generate JWT                │
  │  302 → frontend/callback?token=xxx                       │
  │ ◀────────────────────────│                              │
```

### SAML/SSO

```
Client                    Backend (SP)                 IdP (Okta/Azure AD)
  │                          │                              │
  │ GET /saml2/authenticate/ │                              │
  │    {registrationId}      │                              │
  │ ────────────────────────▶│                              │
  │                          │  AuthnRequest (signed)       │
  │                          │ ────────────────────────────▶│
  │  ◀──────────────────────────────────────────────────── │
  │  (User authenticates at IdP)                            │
  │  ─────────────────────────────────────────────────────▶│
  │                          │  SAML Response (assertions)  │
  │                          │ POST /login/saml2/sso/{id}   │
  │                          │ ◀────────────────────────────│
  │                          │                              │
  │                          │  Validate signature          │
  │                          │  Extract attributes          │
  │                          │  Find/Create user            │
  │                          │  Generate JWT                │
  │  302 → frontend?token=xxx                               │
  │ ◀───────────────────────│                              │
```

### Refresh Token Rotation

```
┌──────────┐  POST /refresh {refreshToken}  ┌──────────────────────┐
│  Client  │ ─────────────────────────────▶ │ AuthController        │
└──────────┘                                └──────────┬───────────┘
                                                       │
                                           ┌───────────▼───────────┐
                                           │ Validate HMAC token   │
                                           │ Find in DB            │
                                           └───────────┬───────────┘
                                                       │
                              ┌─────────────────────────┤
                              │                         │
                     ┌────────▼────────┐      ┌────────▼────────┐
                     │ Token REVOKED?   │      │ Token VALID?    │
                     │ → REPLAY ATTACK! │      │                 │
                     │ Revoke ENTIRE    │      │ Revoke old      │
                     │ token family     │      │ Generate new    │
                     │ → 401            │      │ (same family)   │
                     └─────────────────┘      │ → 200 {new      │
                                              │   tokens}        │
                                              └─────────────────┘
```

---

## 5. Security Filter Chain

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────────────────────┐
│  CorrelationIdFilter (HIGHEST_PRECEDENCE)                │
│  - Generate/extract X-Correlation-ID                     │
│  - Populate MDC (traceId, userId, clientIp, etc.)        │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│  XssSanitizingFilter                                     │
│  - Strip malicious scripts from params/headers           │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│  RateLimitingFilter (Redis-backed)                        │
│  - Login: 5 req/min                                      │
│  - Authenticated: 60 req/min                             │
│  - Anonymous: 30 req/min                                 │
│  - Returns 429 if exceeded                               │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│  ApiKeyAuthenticationFilter                              │
│  - Check X-API-Key header                                │
│  - Validate against DB                                   │
│  - Set SecurityContext if valid                          │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│  JwtAuthenticationFilter                                 │
│  - Extract Bearer token from Authorization header        │
│  - Validate RSA signature + expiry                       │
│  - Load UserDetails                                      │
│  - Set SecurityContext                                   │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │  @PreAuthorize Check  │
              │  (Method Security)    │
              └───────────┬───────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │     Controller        │
              └───────────────────────┘
```

---

## 6. Database Schema Overview

### 30 Flyway Migrations

| Version | Tables Created/Modified |
|---------|----------------------|
| V1 | `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `refresh_tokens` |
| V2 | `interviews`, `interview_interviewers`, `interview_feedback` |
| V3 | Add `token_family` to refresh_tokens |
| V4 | Add `auth_provider` to users |
| V5 | Seed RBAC data (ADMIN, RECRUITER, INTERVIEWER, CANDIDATE roles) |
| V6 | `password_reset_tokens` |
| V7 | `email_verification_tokens` |
| V8 | Alter refresh_tokens.token to TEXT |
| V9 | `coding_sessions`, `question_categories`, `questions`, `meeting_links` |
| V10 | `notifications` |
| V11 | `interview_templates`, `template_questions` |
| V12 | `evaluation_criteria`, `evaluation_scorecards`, `scorecard_entries` |
| V13 | `interview_pipelines`, `pipeline_stages`, `candidate_pipelines`, `candidate_stage_progress` |
| V14 | `documents` |
| V15 | `job_positions` |
| V16 | `availability_slots`, `interview_reminders`, `candidate_preferred_slots`, `teams`, `team_members`, `tags`, `entity_tags` |
| V17 | `ai_suggestions`, `video_recordings`, `whiteboard_sessions`, `whiteboard_strokes`, `webhooks`, `webhook_deliveries`, `organizations`, `organization_members`, `candidate_feedback_surveys`, `activity_logs` |
| V18 | `mfa_secrets`, `user_consents`, `data_erasure_requests`, `api_keys` |
| V19 | `code_executions` |
| V20 | `sso_configurations` |
| V21 | `login_attempts`, `account_lockouts`, `ip_blocklist` |
| V22 | Alter salary/phone columns for encryption |
| V23 | `job_applications` |
| V24 | `offer_letters`, `offer_approvals` |
| V25 | `calendar_connections`, `calendar_events` |
| V26 | `workflow_rules`, `workflow_executions` |
| V27 | `approval_chains`, `approval_steps`, `approval_requests`, `approval_decisions` |
| V28 | `referrals` |
| V29 | `demographic_profiles` |
| V30 | `candidate_sources` |

### Key Entity Relationships

```
User ─┬─── UserRole ───── Role ───── RolePermission ───── Permission
      │
      ├─── Interview (as candidate)
      │       ├─── InterviewInterviewer (assigned interviewers)
      │       ├─── InterviewFeedBack (per interviewer)
      │       ├─── CodingSession ───── CodeExecution
      │       ├─── MeetingLink
      │       ├─── CalendarEvent (synced)
      │       └─── EvaluationScorecard
      │
      ├─── JobApplication ───── JobPosition
      │                              └─── OfferLetter ───── OfferApproval
      │
      ├─── CandidatePipeline ───── InterviewPipeline ───── PipelineStage
      │
      ├─── Referral (as referrer)
      ├─── DemographicProfile (opt-in)
      ├─── CalendarConnection
      └─── Notification
```

---

## 7. Event-Driven Architecture

### Kafka Topics

| Topic | Producer | Consumer | Events |
|-------|----------|----------|--------|
| `notification-events` | All services | NotificationConsumer | INTERVIEW_SCHEDULED, FEEDBACK_SUBMITTED, OFFER_SENT, etc. |
| `interview-events` | InterviewService | WorkflowEngine, ActivityService | Status changes, assignments |
| `audit-events` | AuditService | AuditLogConsumer | All auditable operations |

### Workflow Engine Trigger Points

| Event | Triggered When | Possible Actions |
|-------|---------------|-----------------|
| INTERVIEW_COMPLETED | Interview status → COMPLETED | Advance pipeline, notify |
| FEEDBACK_SUBMITTED | Interviewer submits feedback | Check all feedback in, auto-score |
| SCORE_THRESHOLD_MET | Avg score exceeds threshold | Advance candidate, send offer |
| ALL_FEEDBACK_RECEIVED | Last interviewer submits | Notify recruiter, generate summary |
| CANDIDATE_APPLIED | New job application | Auto-screen, assign recruiter |
| OFFER_ACCEPTED | Candidate accepts offer | Mark hired, notify HR, close position |
| OFFER_DECLINED | Candidate declines | Re-open position, notify |

### WebSocket Channels

| Channel | Purpose | Message Type |
|---------|---------|-------------|
| `/topic/interview/{id}/code` | Real-time code collaboration | CodeChangeMessage (INSERT, DELETE, CURSOR) |
| `/topic/whiteboard/{id}` | Real-time drawing strokes | WhiteboardStrokeMessage |
| `/topic/notifications/{userId}` | Live notifications | NotificationMessage |

---

## 8. API URL Structure

All APIs follow: `http://localhost:8080/api/v1/{domain}/{resource}`

| Domain | Base Path | Auth Required |
|--------|-----------|---------------|
| Authentication | `/api/v1/auth` | No (public) |
| Public Job Board | `/api/v1/jobs` | No (public) |
| SSO Login URLs | `/api/v1/sso/tenant/*/login-urls` | No (public) |
| Users | `/api/v1/users` | Yes |
| Interviews | `/api/v1/interviews` | Yes |
| Code Execution | `/api/v1/code-execution` | Yes |
| Job Positions | `/api/v1/job-positions` | Yes |
| Candidate Portal | `/api/v1/portal` | Yes |
| Offer Letters | `/api/v1/offers` | Yes |
| Approvals | `/api/v1/approvals` | Yes |
| Pipelines | `/api/v1/pipelines` | Yes |
| Scorecards | `/api/v1/scorecards` | Yes |
| Calendar | `/api/v1/calendar` | Yes |
| Calendar Sync | `/api/v1/calendar-sync` | Yes |
| Scheduling | `/api/v1/scheduling` | Yes |
| Workflows | `/api/v1/workflows` | Yes (ADMIN/RECRUITER) |
| Referrals | `/api/v1/referrals` | Yes |
| DEI Analytics | `/api/v1/dei` | Yes |
| Source Tracking | `/api/v1/sources` | Yes |
| Notifications | `/api/v1/notifications` | Yes |
| Documents | `/api/v1/documents` | Yes |
| Webhooks | `/api/v1/webhooks` | Yes |
| Reports | `/api/v1/reports` | Yes |
| Dashboard | `/api/v1/dashboard` | Yes |
| Audit | `/api/v1/audit` | Yes (ADMIN) |
| Security (Lockout) | `/api/v1/security` | Yes (ADMIN) |
| SSO Management | `/api/v1/sso` | Yes (ADMIN) |
| Bulk Operations | `/api/v1/bulk` | Yes |
| Teams | `/api/v1/teams` | Yes |
| Tags | `/api/v1/tags` | Yes |
| Templates | `/api/v1/templates` | Yes |
| Questions | `/api/v1/questions` | Yes |
| Swagger UI | `/swagger-ui.html` | No |
| JWKS | `/.well-known/jwks.json` | No |
| Health | `/actuator/health` | No |
