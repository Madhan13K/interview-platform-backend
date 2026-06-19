# Interview Platform Backend

A comprehensive interview management platform built with **Spring Boot 4.0**, providing end-to-end interview lifecycle management including scheduling, execution, feedback, evaluation scorecards, hiring pipelines, document management, and analytics.

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENT APPLICATIONS                           │
│              (React Frontend / Mobile App / Admin Panel)              │
└─────────────────────────┬───────────────────────────────────────────┘
                          │ HTTPS / WebSocket
                          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      API GATEWAY / LOAD BALANCER                      │
└─────────────────────────┬───────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   SPRING BOOT APPLICATION                             │
│                                                                       │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────────┐    │
│  │  Security   │  │   REST API   │  │     WebSocket Layer      │    │
│  │  (JWT/OAuth)│  │  Controllers │  │  (Real-time Code Editor) │    │
│  └──────┬──────┘  └──────┬───────┘  └────────────┬────────────┘    │
│         │                 │                        │                  │
│         ▼                 ▼                        ▼                  │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                    SERVICE LAYER                              │    │
│  │  Auth │ User │ Interview │ Template │ Question │ Dashboard   │    │
│  │  Pipeline │ Scorecard │ Document │ BulkOps │ Notification   │    │
│  │  Calendar │ Meeting │ CodeEditor │ Audit                     │    │
│  │  AI │ Video │ Whiteboard │ Webhook │ Tenant │ ExportImport  │    │
│  │  CandidateFeedback │ Activity                                │    │
│  └──────────────────────────┬──────────────────────────────────┘    │
│                              │                                        │
│  ┌───────────────────────────┼──────────────────────────────────┐   │
│  │                REPOSITORY LAYER (Spring Data JPA)             │   │
│  └───────────────────────────┼──────────────────────────────────┘   │
│                              │                                        │
└──────────────────────────────┼───────────────────────────────────────┘
                               │
     ┌─────────────────────────┼─────────────────────────────┐
     ▼              ▼          ▼          ▼                   ▼
┌──────────────┐ ┌─────────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ PostgreSQL   │ │     Kafka       │ │    Redis     │ │   AWS S3     │ │    OTel      │
│ (Primary DB) │ │ (Event Stream)  │ │  (Cache/RL)  │ │ (Documents)  │ │ (Telemetry)  │
└──────────────┘ └─────────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 4.0.6, Java 21 |
| Security | JWT (RSA), OAuth2 (Google, GitHub, Microsoft), RBAC, MFA/TOTP, API Keys |
| Database | PostgreSQL 16, Flyway Migrations |
| Caching | Redis 7 (distributed) + Caffeine (L1 local) |
| Messaging | Apache Kafka |
| Real-time | WebSocket (STOMP) |
| File Storage | AWS S3 / LocalStack (free local emulation) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Resilience | Resilience4j (circuit breakers, retry) |
| Scheduling | ShedLock (distributed locks) |
| Build | Maven |
| Containerization | Docker + Docker Compose (PostgreSQL, Redis, Kafka, LocalStack, OTel Collector, Jaeger) |
| Observability | OpenTelemetry Java Agent (auto-instrumentation) + OTel Collector + Jaeger + Prometheus |
| Email | Spring Mail + Thymeleaf HTML templates |
| i18n | MessageSource (EN, ES, FR) |
| Compliance | GDPR (consent, data export, right-to-erasure) |

---

## 📁 Project Module Structure

```
src/main/java/com/interview_platform_backend/
├── activity/               # Activity feed & timeline tracking
├── ai/                     # AI-powered features (suggestions, parsing, summaries)
├── audit/                  # Audit logging
├── bulkops/                # Bulk operations (schedule, invite, export)
├── calendar/               # Interviewer availability & scheduling
├── candidate/              # Interview management & feedback
├── candidatefeedback/      # Candidate reverse feedback (interview experience)
├── codeeditor/             # Real-time collaborative code editor
├── config/                 # App configuration (Async, Jackson, Dotenv)
├── dashboard/              # Admin/Interviewer/Candidate dashboards
├── document/               # File/Document management (AWS S3)
├── event/                  # Domain events (Spring Events)
├── exception/              # Global exception handling
├── exportimport/           # Export/Import jobs (CSV, JSON)
├── meeting/                # Meeting link generation (Zoom, Google Meet)
├── notification/           # Email, SMS, In-App, Kafka notifications
├── pipeline/               # Hiring pipeline & candidate progression
├── questionbank/           # Question bank with categories
├── scorecard/              # Evaluation scorecards & criteria
├── security/               # Auth, JWT, OAuth2, RBAC
├── template/               # Interview templates
├── tenant/                 # Multi-tenant / Organization management
├── user/                   # User management, roles, permissions
├── video/                  # Video recording integration (S3 storage)
├── webhook/                # Webhook integrations (event notifications)
├── whiteboard/             # Whiteboard collaboration (real-time drawing)
└── websocket/              # WebSocket configuration
```

---

## 🔐 Authentication & Authorization

### Authentication Flows
- **Local Registration** → Email + Password → Email Verification → Login → JWT
- **OAuth2 Login** → Google/GitHub/Microsoft → Auto-registration → JWT
- **Token Refresh** → Rotation with replay detection
- **Password Reset** → Forgot password → Email link → Reset

### Authorization (RBAC)
| Role | Access |
|------|--------|
| ADMIN | Full system access |
| RECRUITER | Manage interviews, templates, users |
| INTERVIEWER | Conduct interviews, submit feedback |
| CANDIDATE | View own interviews, profile |

---

## 📡 API Endpoints

### Authentication (`/api/v1/auth`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/register` | Register new user (candidate) |
| POST | `/register/interviewer` | Register as interviewer |
| POST | `/admin/create-user` | Admin creates user with roles |
| POST | `/login` | Login with credentials |
| POST | `/forgot-password` | Request password reset email |
| POST | `/reset-password` | Reset password with token |
| POST | `/refresh` | Refresh access token |
| POST | `/logout` | Revoke refresh token |
| GET | `/verify-email` | Verify email with token |
| POST | `/resend-verification` | Resend verification email |
| GET | `/oauth2/providers` | List available OAuth2 providers |

### Users (`/api/v1/users`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create user |
| GET | `/me` | Get current user |
| GET | `/` | Get all users |
| GET | `/{userId}` | Get user by ID |
| PUT | `/{userId}` | Update user |
| DELETE | `/{userId}` | Soft-delete user |
| GET | `/{userId}/profile` | Get user profile |
| PUT | `/{userId}/profile` | Update user profile |
| PUT | `/{userId}/change-password` | Change password |
| PATCH | `/{userId}/status` | Update user status |
| GET | `/search` | Search users (keyword, status, pagination) |
| POST | `/{userId}/roles` | Assign role to user |
| GET | `/{userId}/roles` | Get user roles |
| DELETE | `/{userId}/roles/{roleId}` | Remove role from user |
| GET | `/{userId}/permissions` | Get user permissions |
| GET | `/roles` | Get all roles |

### Roles (`/api/v1/roles`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create role |
| GET | `/` | Get all roles |
| GET | `/{roleId}` | Get role by ID |
| PUT | `/{roleId}` | Update role |
| DELETE | `/{roleId}` | Delete role |
| POST | `/{roleId}/permissions` | Assign permission to role |
| GET | `/{roleId}/permissions` | Get role permissions |
| DELETE | `/permissions/{id}` | Remove permission from role |

### Permissions (`/api/v1/permissions`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create permission |
| GET | `/` | Get all permissions |
| GET | `/{permissionId}` | Get permission by ID |
| PUT | `/{permissionId}` | Update permission |
| DELETE | `/{permissionId}` | Delete permission |

### Interviews (`/api/v1/interviews`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Schedule interview |
| GET | `/` | Get all interviews |
| GET | `/paginated` | Get interviews (paginated) |
| GET | `/{interviewId}` | Get interview details |
| PUT | `/{interviewId}` | Update/reschedule interview |
| DELETE | `/{interviewId}` | Delete interview |
| PATCH | `/{interviewId}/cancel` | Cancel interview |
| PATCH | `/{interviewId}/status` | Update interview status |
| GET | `/my/candidate` | My interviews as candidate |
| GET | `/my/candidate/paginated` | My interviews as candidate (paginated) |
| GET | `/my/interviewer` | My interviews as interviewer |
| GET | `/my/interviewer/paginated` | My interviews as interviewer (paginated) |
| POST | `/{interviewId}/interviewers/{id}` | Add interviewer |
| DELETE | `/{interviewId}/interviewers/{id}` | Remove interviewer |
| POST | `/{interviewId}/feedback` | Submit feedback |
| GET | `/{interviewId}/feedback` | Get interview feedback |
| GET | `/{interviewId}/feedback/interviewer/{id}` | Get feedback by interviewer |
| GET | `/feedback/interviewer/{id}` | Get all feedback by interviewer |
| GET | `/filter/status` | Filter by status |
| GET | `/filter/status/paginated` | Filter by status (paginated) |
| GET | `/filter/date-range` | Filter by date range |
| GET | `/filter/date-range/paginated` | Filter by date range (paginated) |

### Interview Templates (`/api/v1/templates`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create template |
| GET | `/` | Get all active templates |
| GET | `/paginated` | Get templates (paginated) |
| GET | `/{templateId}` | Get template with questions |
| PUT | `/{templateId}` | Update template |
| DELETE | `/{templateId}` | Delete template |
| GET | `/filter/type` | Filter by interview type |
| GET | `/search` | Search templates |
| POST | `/{templateId}/questions` | Add question to template |
| DELETE | `/{templateId}/questions/{id}` | Remove question from template |
| POST | `/create-interview` | Create interview from template |

### Question Bank (`/api/v1/questions`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/categories` | Create category |
| GET | `/categories` | Get all categories |
| POST | `/` | Create question |
| GET | `/{questionId}` | Get question |
| PUT | `/{questionId}` | Update question |
| DELETE | `/{questionId}` | Delete question |
| GET | `/search` | Search questions |
| GET | `/category/{categoryId}` | Get questions by category |

### Code Editor (`/api/v1/interviews/{interviewId}/code`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/start` | Start coding session |
| GET | `/` | Get active session |
| PUT | `/save` | Save code |
| POST | `/end` | End session |
| GET | `/history` | Get session history |

### Meeting (`/api/v1/interviews/{interviewId}/meeting`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Generate meeting link |
| GET | `/` | Get meeting link |

### Calendar (`/api/v1/calendar`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/interviewers/{id}/availability` | Add availability |
| GET | `/interviewers/{id}/availability` | Get availability |
| GET | `/interviewers/{id}/availability/check` | Check availability |
| DELETE | `/interviewers/{id}/availability/{slotId}` | Remove availability |

### Dashboard (`/api/v1/dashboard`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin` | Admin dashboard stats |
| GET | `/interviewer` | Current interviewer dashboard |
| GET | `/interviewer/{id}` | Specific interviewer dashboard |
| GET | `/candidate` | Candidate dashboard |

### Notifications (`/api/v1/notifications`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get all notifications |
| GET | `/unread` | Get unread notifications |
| GET | `/count` | Get unread count |
| PATCH | `/{notificationId}/read` | Mark as read |
| PATCH | `/read-all` | Mark all as read |

### Audit (`/api/v1/audit`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get all audit logs |
| GET | `/entity/{type}/{id}` | Get audit by entity |
| GET | `/user/{email}` | Get audit by user |

### Pipelines (`/api/v1/pipelines`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create hiring pipeline |
| GET | `/` | Get all pipelines |
| GET | `/{pipelineId}` | Get pipeline by ID |
| GET | `/department/{department}` | Get pipelines by department |
| PUT | `/{pipelineId}` | Update pipeline |
| DELETE | `/{pipelineId}` | Delete pipeline |
| POST | `/candidates` | Add candidate to pipeline |
| GET | `/candidates/{candidatePipelineId}` | Get candidate pipeline progress |
| GET | `/{pipelineId}/candidates` | Get all candidates in pipeline |
| GET | `/candidates/user/{candidateId}` | Get candidate's pipelines |
| POST | `/candidates/{candidatePipelineId}/advance` | Advance candidate to next stage |
| POST | `/candidates/{candidatePipelineId}/reject` | Reject candidate |
| PATCH | `/candidates/{candidatePipelineId}/stages/{stageId}` | Update stage progress |
| PATCH | `/candidates/{candidatePipelineId}/status` | Update candidate pipeline status |

### Evaluation Scorecards (`/api/v1/scorecards`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/criteria` | Create evaluation criteria |
| GET | `/criteria` | Get all criteria |
| GET | `/criteria/type/{type}` | Get criteria by interview type |
| GET | `/criteria/{criteriaId}` | Get criteria by ID |
| PUT | `/criteria/{criteriaId}` | Update criteria |
| DELETE | `/criteria/{criteriaId}` | Delete criteria |
| POST | `/` | Submit scorecard |
| GET | `/{scorecardId}` | Get scorecard |
| GET | `/interview/{interviewId}` | Get scorecards by interview |
| GET | `/interview/{interviewId}/interviewer/{interviewerId}` | Get scorecard by interview & interviewer |
| GET | `/interviewer/{interviewerId}` | Get scorecards by interviewer |
| GET | `/candidate/{candidateId}` | Get scorecards by candidate |
| GET | `/interview/{interviewId}/summary` | Get candidate scorecard summary |

### Bulk Operations (`/api/v1/bulk`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/interviews/schedule` | Bulk schedule multiple interviews (up to 100) |
| POST | `/candidates/invite` | Bulk invite candidates (email invitations) |
| GET | `/export?exportType=INTERVIEWS&format=CSV` | Bulk export interview data (CSV/JSON) |
| GET | `/export?exportType=CANDIDATES` | Bulk export candidate data |
| GET | `/export?exportType=FEEDBACK` | Bulk export feedback data |
| GET | `/export?exportType=SCORECARDS` | Bulk export scorecard data |

### Documents (`/api/v1/documents`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Upload document to S3 (multipart/form-data) |
| GET | `/{documentId}` | Get document metadata |
| GET | `/{documentId}/download-url` | Get presigned download URL (time-limited) |
| GET | `/entity/{entityType}/{entityId}` | Get documents linked to an entity |
| GET | `/my` | Get current user's documents |
| GET | `/my/paginated` | Get current user's documents (paginated) |
| GET | `/type/{documentType}` | Get documents by type (ADMIN/RECRUITER) |
| POST | `/presigned-upload` | Get presigned upload URL for client-side upload |
| PATCH | `/{documentId}` | Update document metadata |
| DELETE | `/{documentId}` | Delete document from S3 and database |

### Job Positions (`/api/v1/job-positions`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create a new job position/opening |
| GET | `/` | Get all job positions |
| GET | `/{id}` | Get job position by ID |
| GET | `/paginated` | Get job positions (paginated) |
| GET | `/search?keyword=` | Search job positions by keyword |
| GET | `/filter/status?status=` | Filter by status (OPEN, CLOSED, FILLED, etc.) |
| GET | `/my` | Get current user's created positions |
| PUT | `/{id}` | Update a job position |
| PATCH | `/{id}/status?status=` | Update job position status |
| DELETE | `/{id}` | Delete a job position |
| POST | `/{positionId}/interviews/{interviewId}` | Link interview to position |
| DELETE | `/interviews/{interviewId}` | Unlink interview from position |

### Reports & Analytics (`/api/v1/reports`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/analytics` | Get overall analytics report (JSON) |
| GET | `/analytics/interviewer/{id}` | Get interviewer performance (JSON) |
| GET | `/pdf/analytics` | Generate overall analytics PDF report |
| GET | `/pdf/interviewer/{id}` | Generate interviewer performance PDF |
| GET | `/pdf/job-position/{id}` | Generate job position PDF report |
| GET | `/metrics/conversion` | Get conversion funnel metrics |
| GET | `/metrics/time-to-hire` | Get time-to-hire metrics |

### Automated Scheduling (`/api/v1/scheduling`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/availability` | Add availability slot for current user |
| GET | `/availability/my` | Get my availability slots |
| GET | `/availability/user/{userId}` | Get user's availability (admin/recruiter) |
| DELETE | `/availability/{slotId}` | Delete an availability slot |
| POST | `/suggest` | Auto-suggest time slots based on interviewer availability |

### Interview Reminders (`/api/v1/reminders`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/interview/{interviewId}` | Create 24h/1h/15min reminders for interview |
| DELETE | `/interview/{interviewId}` | Cancel all pending reminders |
| GET | `/interview/{interviewId}` | Get reminders for an interview |
| GET | `/my` | Get my reminders |

### Candidate Self-Service (`/api/v1/self-service`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/preferred-slots` | Submit preferred time slots |
| GET | `/preferred-slots/my` | Get my preferred slots |
| GET | `/preferred-slots/interview/{id}` | Get slots for interview (recruiter) |
| GET | `/preferred-slots/job-position/{id}` | Get slots for job position |
| PATCH | `/preferred-slots/{id}/status` | Accept/reject a slot |
| DELETE | `/preferred-slots/{id}` | Delete a preferred slot |

### Teams & Departments (`/api/v1/teams`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create a team |
| GET | `/` | Get all active teams |
| GET | `/{teamId}` | Get team with members |
| GET | `/department/{dept}` | Get teams by department |
| GET | `/my` | Get my teams |
| PUT | `/{teamId}` | Update a team |
| DELETE | `/{teamId}` | Deactivate a team |
| POST | `/{teamId}/members/{userId}` | Add member to team |
| DELETE | `/{teamId}/members/{userId}` | Remove member |
| PATCH | `/{teamId}/members/{userId}/role` | Update member role |

### Tags & Labels (`/api/v1/tags`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create a tag |
| GET | `/` | Get all tags |
| GET | `/category/{category}` | Get tags by category |
| GET | `/search?query=` | Search tags by name |
| DELETE | `/{tagId}` | Delete a tag |
| POST | `/{tagId}/entities/{type}/{id}` | Tag an entity |
| DELETE | `/{tagId}/entities/{type}/{id}` | Remove tag from entity |
| GET | `/entities/{type}/{id}` | Get tags for an entity |
| GET | `/{tagId}/entities/{type}` | Get entities by tag |

---

## Phase 7 — Advanced Features

### 13. AI-Powered Features
AI-driven question suggestions, resume parsing, and interview summary generation.

**API Endpoints** (`/api/v1/ai`):
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/suggest-questions` | Generate question suggestions for a role | Authenticated |
| POST | `/parse-resume` | Parse uploaded resume using AI | Authenticated |
| POST | `/interview-summary` | Generate interview summary from feedback | Authenticated |
| GET | `/suggestions` | Get AI suggestion history (paginated) | Authenticated |
| GET | `/suggestions/interview/{interviewId}` | Get suggestions for interview | Authenticated |
| PATCH | `/suggestions/{id}/status` | Accept/reject suggestion | Authenticated |

### 14. Video Recording Integration
Record interview sessions for later review with S3 storage and presigned download URLs.

**API Endpoints** (`/api/v1/video-recordings`):
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/start` | Start recording for an interview | Authenticated |
| PATCH | `/{id}/complete` | Mark recording as complete | Authenticated |
| PATCH | `/{id}/fail` | Mark recording as failed | Authenticated |
| GET | `/interview/{interviewId}` | Get recordings for an interview | Authenticated |
| GET | `/{id}` | Get recording with presigned download URL | Authenticated |
| DELETE | `/{id}` | Soft-delete a recording | Authenticated |
| GET | `/my` | Get my recordings (paginated) | Authenticated |

### 15. Whiteboard Collaboration
Shared drawing/diagramming tool during interviews with real-time WebSocket synchronization.

**API Endpoints** (`/api/v1/whiteboards`):
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/` | Create whiteboard session | Authenticated |
| GET | `/{id}` | Get session details | Authenticated |
| GET | `/interview/{interviewId}` | Get sessions for interview | Authenticated |
| POST | `/{id}/strokes` | Add a stroke (broadcast via WebSocket) | Authenticated |
| GET | `/{id}/strokes` | Get all strokes | Authenticated |
| PUT | `/{id}/snapshot` | Save full board snapshot | Authenticated |
| PATCH | `/{id}/close` | Close session | Authenticated |
| DELETE | `/{id}` | Delete session | Authenticated |

**WebSocket**: Real-time strokes broadcast to `/topic/whiteboard/{sessionId}`

### 16. Export/Import
Export interview data to CSV/JSON, import candidates from ATS systems.

**API Endpoints** (`/api/v1/export-import`):
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/export` | Start async export job (CSV/JSON) | Authenticated |
| POST | `/import` | Start async import job | Authenticated |
| GET | `/jobs` | Get my export/import jobs (paginated) | Authenticated |
| GET | `/jobs/{id}` | Get job status with download URL | Authenticated |
| DELETE | `/jobs/{id}` | Cancel pending job | Authenticated |

Supported entity types: `INTERVIEWS`, `CANDIDATES`, `FEEDBACK`, `QUESTIONS`
Supported formats: `CSV`, `JSON` (Excel planned)

### 17. Webhook Integrations
Notify external systems (Slack, Teams, ATS) on platform events with HMAC-SHA256 signed payloads.

**API Endpoints** (`/api/v1/webhooks`):
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/` | Register webhook endpoint | Authenticated |
| GET | `/` | Get my webhooks | Authenticated |
| GET | `/{id}` | Get webhook details (secret masked) | Authenticated |
| PUT | `/{id}` | Update webhook | Authenticated |
| DELETE | `/{id}` | Delete webhook | Authenticated |
| POST | `/{id}/regenerate-secret` | Regenerate signing secret | Authenticated |
| GET | `/{id}/deliveries` | Get delivery history (paginated) | Authenticated |
| POST | `/deliveries/{deliveryId}/retry` | Retry failed delivery | Authenticated |

**Supported Events**: `INTERVIEW_SCHEDULED`, `INTERVIEW_COMPLETED`, `INTERVIEW_CANCELLED`, `FEEDBACK_SUBMITTED`, `CANDIDATE_HIRED`, `CANDIDATE_REJECTED`

**Delivery**: Async HTTP POST with `X-Webhook-Signature` (HMAC-SHA256), automatic retries with exponential backoff (max 5 attempts).

### 18. Multi-Tenant Support
Support multiple companies/organizations with member management and role-based access.

**API Endpoints** (`/api/v1/organizations`):
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/` | Create organization | ADMIN |
| GET | `/{id}` | Get organization | Authenticated |
| PUT | `/{id}` | Update organization | Authenticated |
| DELETE | `/{id}` | Delete organization | ADMIN |
| GET | `/my` | Get my organizations | Authenticated |
| POST | `/{id}/members` | Add member | Authenticated |
| DELETE | `/{id}/members/{userId}` | Remove member | Authenticated |
| GET | `/{id}/members` | List members | Authenticated |
| PATCH | `/{id}/members/{userId}/role` | Update member role | Authenticated |

**Plans**: FREE, STARTER, PROFESSIONAL, ENTERPRISE
**Roles**: OWNER, ADMIN, MEMBER, VIEWER

### 19. Candidate Feedback (Reverse)
Let candidates rate their interview experience after completion.

**API Endpoints** (`/api/v1/candidate-feedback`):
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/` | Submit feedback | CANDIDATE |
| GET | `/interview/{interviewId}` | Get feedback for interview | Authenticated |
| GET | `/summary` | Get aggregate feedback summary | ADMIN/RECRUITER |
| GET | `/my` | Get my submitted feedback (paginated) | CANDIDATE |

**Rating dimensions**: Overall (required 1-5), Communication, Professionalism, Technical Clarity, Timeliness
**Features**: Anonymous feedback option, "would recommend" flag, aggregate statistics

### 20. Activity Feed / Timeline
Chronological activity log per candidate showing all interactions across the platform.

**API Endpoints** (`/api/v1/activities`):
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/` | Global activity feed (paginated) | ADMIN/RECRUITER |
| GET | `/entity/{entityType}/{entityId}` | Timeline for specific entity | Authenticated |
| GET | `/user/{userId}` | Activity by specific user | Authenticated |
| GET | `/my` | Current user's activity | Authenticated |
| POST | `/filter` | Filtered activity feed | Authenticated |

**Entity Types**: INTERVIEW, CANDIDATE, FEEDBACK, JOB_POSITION, PIPELINE, DOCUMENT
**Actions**: CREATED, UPDATED, DELETED, SCHEDULED, CANCELLED, COMPLETED, SUBMITTED, ASSIGNED, ADVANCED, REJECTED

---

## 🔄 Application Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────────┐
│   Register   │────▶│ Verify Email │────▶│      Login       │
└──────────────┘     └──────────────┘     └────────┬─────────┘
                                                    │
                                                    ▼
┌────────────────────────────────────────────────────────────────┐
│                        JWT Access Token                          │
└────────────────────────────┬───────────────────────────────────┘
                             │
          ┌──────────────────┼──────────────────────┐
          ▼                  ▼                      ▼
┌──────────────┐   ┌──────────────────┐   ┌──────────────────┐
│  RECRUITER   │   │   INTERVIEWER    │   │    CANDIDATE     │
└──────┬───────┘   └────────┬─────────┘   └────────┬─────────┘
       │                     │                      │
       ▼                     ▼                      ▼
┌──────────────┐   ┌──────────────────┐   ┌──────────────────┐
│Create Template│   │View Assigned     │   │View My Interviews│
│Schedule      │   │Interviews        │   │Join Meeting      │
│Interview     │   │Conduct Interview │   │View Feedback     │
│Assign        │   │Submit Feedback   │   │                  │
│Interviewers  │   │Use Code Editor   │   │                  │
└──────────────┘   └──────────────────┘   └──────────────────┘
       │                     │
       ▼                     ▼
┌─────────────────────────────────────────┐
│        NOTIFICATION ENGINE              │
│  ┌────────┐ ┌─────┐ ┌───────┐ ┌─────┐ │
│  │ Email  │ │ SMS │ │In-App │ │Kafka│ │
│  └────────┘ └─────┘ └───────┘ └─────┘ │
└─────────────────────────────────────────┘
```

### Interview Lifecycle

```
  TEMPLATE ──────▶ CREATE INTERVIEW ──▶ SCHEDULED
                                            │
                                  ┌─────────┴──────────┐
                                  ▼                    ▼
                            RESCHEDULED           CANCELLED
                                  │
                                  ▼
                            IN_PROGRESS
                                  │
                          ┌───────┴────────┐
                          ▼                ▼
                     COMPLETED          NO_SHOW
                          │
                          ▼
                   SUBMIT FEEDBACK
                          │
                          ▼
                   ANALYTICS/REPORTS
```

---

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven

### Run Locally (with Docker)

```bash
# Clone the repository
git clone <repo-url>
cd interview-platform-backend

# Copy env file and update values
cp .env.example .env

# Start everything (app + infrastructure + OTel Agent)
docker compose up --build

# The app starts at http://localhost:8080 with OTel Java Agent attached
# Traces are visible at http://localhost:16686 (Jaeger UI)
```

### Run Locally (without Docker for the app)

```bash
# Start infrastructure only
docker compose up -d postgres kafka redis localstack otel-collector jaeger

# Download the OpenTelemetry Java Agent
curl -L -o opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.12.0/opentelemetry-javaagent.jar

# Run with agent attached
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=interview-platform-backend \
     -Dotel.exporter.otlp.endpoint=http://localhost:4318 \
     -Dotel.exporter.otlp.protocol=http/protobuf \
     -Dotel.metrics.exporter=otlp \
     -Dotel.logs.exporter=otlp \
     -jar target/interview-platform-backend-0.0.1-SNAPSHOT.jar

# Or run without the agent (no tracing)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run Tests
```bash
# Unit tests only (no Docker services needed - default)
./mvnw clean install

# Same as above (explicit)
./mvnw test

# Integration tests (requires: docker compose up -d)
./mvnw verify -PintegrationTests

# Skip ALL tests (fastest build)
./mvnw clean install -DskipTests
```

> **Note:** Integration tests (`*IntegrationTest.java`) are excluded from the default build 
> because they require PostgreSQL, Redis, and Kafka. They run automatically in CI via 
> Testcontainers, or locally after starting infrastructure with `docker compose up -d`.

### API Documentation
Once running, visit: `http://localhost:8080/swagger-ui.html`

---

## 📊 Services Overview

| Service | Responsibility |
|---------|---------------|
| `AuthenticationService` | Register, login, token refresh, password reset, email verification |
| `UserService` | User CRUD, profiles, role assignment, search |
| `RoleService` | Role CRUD |
| `PermissionService` | Permission CRUD |
| `RolePermissionService` | Role-permission mapping |
| `InterviewService` | Interview lifecycle, feedback, filtering |
| `InterviewTemplateService` | Template CRUD, create interview from template |
| `QuestionBankService` | Questions and categories management |
| `PipelineService` | Hiring pipeline CRUD, candidate progression, stage management |
| `EvaluationScorecardService` | Scorecard submission, criteria management, weighted scoring |
| `BulkOperationsService` | Bulk scheduling, bulk invitations, bulk data export |
| `DocumentService` | File upload/download via AWS S3, metadata management |
| `CalendarService` | Interviewer availability management |
| `MeetingService` | Meeting link generation (Zoom, Google Meet, Internal) |
| `CodingSessionService` | Real-time code editor sessions |
| `DashboardService` | Stats & analytics for Admin/Interviewer/Candidate |
| `EmailNotificationService` | Email delivery |
| `SmsNotificationService` | SMS delivery |
| `InAppNotificationService` | In-app notification management |
| `AuditService` | Audit trail logging |
| `JwtService` | JWT token generation & validation (RSA) |
| `RefreshTokenService` | Refresh token management with rotation |
| `CustomUserDetailsService` | Spring Security user loading |
| `AiService` | AI question suggestions, resume parsing, interview summaries |
| `VideoRecordingService` | Video recording lifecycle and S3 storage |
| `WhiteboardService` | Whiteboard session management and real-time strokes |
| `WebhookService` | Webhook registration, delivery, and retry management |
| `OrganizationService` | Multi-tenant organization and member management |
| `CandidateFeedbackService` | Candidate reverse feedback and aggregate statistics |
| `ActivityService` | Activity feed tracking and timeline queries |
| `ExportImportService` | Async export/import job management (CSV/JSON) |

---

## 🔒 Security Features

- **JWT with RSA keys** (asymmetric signing)
- **Refresh token rotation** with replay detection
- **OAuth2** (Google, GitHub, Microsoft) with PKCE
- **RBAC** (Role-Based Access Control)
- **Rate limiting** filter
- **CORS** configuration
- **Email verification** for new accounts
- **Password reset** via secure tokens
- **Audit logging** for sensitive operations

---

## 📦 Environment Variables

All sensitive values are externalized via `.env` file. See `.env.example` for the full list of required variables.

Key categories:
- Database (PostgreSQL)
- JWT secrets
- OAuth2 client credentials (Google, GitHub, Microsoft)
- SMTP mail credentials
- Kafka configuration
- OpenTelemetry Java Agent (`OTEL_*` env vars — service name, endpoint, sampling, resource attributes)
- Meeting provider API keys
- AWS S3 (bucket name, region, access key, secret key)

---

## 📡 OpenTelemetry (OTel) Observability

This application uses the **OpenTelemetry Java Agent** for automatic distributed tracing, metrics collection, and log correlation. The agent attaches to the JVM at startup and instruments all supported libraries at the bytecode level — no application code changes required.

### What is OpenTelemetry?

OpenTelemetry is a collection of APIs, SDKs, and tools for instrumenting, generating, collecting, and exporting telemetry data (traces, metrics, logs). It enables you to understand the behavior and performance of your application without vendor lock-in.

**Key Concepts:**
| Concept | Description |
|---------|-------------|
| **Traces** | End-to-end request flows across services. Each trace consists of spans (units of work) |
| **Spans** | Individual operations within a trace (e.g., HTTP request, DB query, Kafka publish) |
| **Metrics** | Quantitative measurements (request count, latency histograms, JVM memory) |
| **Logs** | Structured event records, correlated with trace context (traceId, spanId) |
| **OTLP** | OpenTelemetry Protocol — the standard wire format for exporting all telemetry |
| **Collector** | A vendor-agnostic proxy that receives, processes, and exports telemetry data |
| **Java Agent** | A JAR attached via `-javaagent` that auto-instruments libraries at the bytecode level |

### Architecture

```
┌──────────────────────────────────────┐
│     Spring Boot Application          │
│                                      │
│  ┌──────────────────────────────┐   │
│  │  OpenTelemetry Java Agent    │   │  Automatic instrumentation:
│  │  (attached via -javaagent)   │   │  - Spring MVC (HTTP server)
│  │                              │   │  - JDBC / Hibernate / JPA
│  │  Instruments at bytecode     │   │  - Apache Kafka (produce/consume)
│  │  level — zero code changes   │   │  - Redis (Lettuce)
│  └──────────┬───────────────────┘   │  - HTTP clients (RestTemplate, WebClient)
│             │                        │  - Spring Security
│             │ OTLP (HTTP/gRPC)       │  - WebSocket (STOMP)
│             │                        │  - AWS SDK (S3)
└─────────────┼────────────────────────┘  - Scheduled tasks
              │
              ▼
┌──────────────────────────────────────┐
│     OpenTelemetry Collector          │
│                                      │
│  Receivers → Processors → Exporters  │
│  (OTLP)     (batch,       (Jaeger,  │
│              memory_limit)  Prometheus│
│                             debug)   │
└──────────┬──────────┬────────────────┘
           │          │
           ▼          ▼
┌──────────────┐  ┌───────────────────┐
│   Jaeger     │  │   Prometheus      │
│  (Traces UI) │  │ (Metrics scrape)  │
│  :16686      │  │  :8889            │
└──────────────┘  └───────────────────┘
```

### How the Java Agent Works

1. The agent JAR is attached at JVM startup: `-javaagent:/app/opentelemetry-javaagent.jar`
2. It uses bytecode manipulation to intercept calls to supported libraries
3. Spans are created automatically for each instrumented operation
4. Trace context is propagated via W3C TraceContext headers (`traceparent`, `tracestate`)
5. All telemetry (traces, metrics, logs) is exported to the OTel Collector via OTLP
6. **No SDK dependencies are needed** in the application classpath for basic instrumentation

### Configuration (Environment Variables)

All OpenTelemetry configuration is done via **environment variables** passed to the JVM. No `application.yml` properties are needed for tracing.

| Variable | Default | Description |
|----------|---------|-------------|
| `OTEL_SERVICE_NAME` | `interview-platform-backend` | Logical service name in traces |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4318` | OTLP collector endpoint (HTTP) |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | `http/protobuf` | Transport protocol (`http/protobuf` or `grpc`) |
| `OTEL_TRACES_SAMPLER` | `parentbased_traceidratio` | Sampling strategy |
| `OTEL_TRACES_SAMPLER_ARG` | `1.0` | Sampling rate (0.0-1.0). Use 0.1 in production |
| `OTEL_METRICS_EXPORTER` | `otlp` | Metrics exporter (`otlp`, `prometheus`, `none`) |
| `OTEL_LOGS_EXPORTER` | `otlp` | Logs exporter (`otlp`, `none`) |
| `OTEL_RESOURCE_ATTRIBUTES` | (see below) | Comma-separated key=value resource attributes |
| `OTEL_INSTRUMENTATION_COMMON_DB_STATEMENT_SANITIZER_ENABLED` | `true` | Sanitize SQL in spans (remove literals) |
| `OTEL_INSTRUMENTATION_KAFKA_EXPERIMENTAL_SPAN_ATTRIBUTES` | `true` | Add extra Kafka span attributes |
| `OTEL_INSTRUMENTATION_[NAME]_ENABLED` | `true` | Enable/disable specific instrumentations |

### Resource Attributes

Resource attributes provide metadata about the service producing telemetry:

```
service.namespace=interview-platform    # Logical grouping of services
deployment.environment=dev              # Environment (dev, staging, production)
service.version=0.0.1-SNAPSHOT          # Application version
```

### Dependencies (pom.xml)

With the Java Agent approach, only lightweight annotation/API dependencies are needed:

```xml
<!-- Annotations for custom spans (optional) -->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-instrumentation-annotations</artifactId>
    <version>2.12.0</version>
</dependency>

<!-- OTel API for programmatic span/metric creation (optional) -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
</dependency>
```

The agent itself provides all SDK, exporter, and instrumentation logic. These dependencies are only needed if you want to add **custom spans** or **manual metrics** beyond the automatic instrumentation.

### Dockerfile

The application is containerized with the OTel Java Agent included:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
# Download OpenTelemetry Java Agent
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.12.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]
```

### Docker Compose Services

The `compose.yaml` includes the full observability stack:

| Service | Port | Purpose |
|---------|------|---------|
| `app` | `8080` | Application with OTel Java Agent attached |
| `otel-collector` | `4317` (gRPC), `4318` (HTTP), `8889` (Prometheus) | Receives, processes, and routes telemetry |
| `jaeger` | `16686` (UI), `14250` (gRPC) | Trace visualization and analysis |

### OTel Collector Configuration (otel-collector-config.yaml)

The collector pipeline is configured as:

```
Receivers (OTLP) → Processors (batch, memory_limiter) → Exporters (Jaeger, Prometheus, debug)
```

| Component | Configuration | Purpose |
|-----------|--------------|---------|
| **OTLP Receiver** | gRPC `:4317`, HTTP `:4318` | Accepts traces/metrics/logs from the agent |
| **Batch Processor** | timeout=5s, batch_size=1024 | Groups telemetry before export (reduces overhead) |
| **Memory Limiter** | limit=512MiB, spike=128MiB | Prevents OOM by backpressuring when memory is high |
| **Jaeger Exporter** | `jaeger:4317` (gRPC) | Sends traces to Jaeger for visualization |
| **Prometheus Exporter** | `:8889` | Exposes metrics in Prometheus format for scraping |
| **Debug Exporter** | stdout | Logs telemetry to console (development only) |

### What Gets Traced Automatically

The Java Agent auto-instruments the following with **zero code changes**:

| Library | What's Traced |
|---------|--------------|
| Spring MVC | All incoming HTTP requests (path, method, status, duration) |
| RestTemplate / WebClient | Outgoing HTTP calls with context propagation |
| JDBC / Hibernate | Database queries (with sanitized SQL statements) |
| Apache Kafka | Message production and consumption (topic, partition, offset) |
| Redis (Lettuce) | Cache operations (command, key) |
| AWS SDK | S3 operations (bucket, key) |
| Spring Security | Authentication events |
| `@Scheduled` | Scheduled task executions |
| `@Async` | Async operations with context propagation |
| Logback | Log correlation with traceId/spanId |

### Custom Spans (Optional)

For business-critical operations, add custom spans using annotations:

```java
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;

@Service
public class PipelineService {

    @WithSpan("advance-candidate-stage")
    public void advanceCandidate(
            @SpanAttribute("pipeline.id") UUID pipelineId,
            @SpanAttribute("candidate.id") UUID candidateId) {
        // This method gets its own span in the trace
    }
}
```

Or programmatically using the OTel API:

```java
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;

@Service
public class ScorecardService {
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("scorecard-service");

    public void calculateWeightedScore(UUID interviewId) {
        Span span = tracer.spanBuilder("calculate-weighted-score")
            .setAttribute("interview.id", interviewId.toString())
            .startSpan();
        try (var scope = span.makeCurrent()) {
            // ... business logic
        } finally {
            span.end();
        }
    }
}
```

### Trace Context in Logs

The Java Agent automatically injects trace context into log MDC. Log lines include `trace_id` and `span_id`:

```
2026-06-16 10:30:00 INFO [trace_id=abc123def456, span_id=789012] c.i.security.JwtAuthFilter : Authenticated user: admin@example.com
```

This allows correlating logs with distributed traces in Jaeger.

### Running & Viewing Traces

```bash
# Start everything
docker compose up --build

# View traces in Jaeger UI
open http://localhost:16686

# View Prometheus metrics from OTel Collector
curl http://localhost:8889/metrics

# Check application health
curl http://localhost:8080/actuator/health
```

### Environment-Specific Configuration

| Environment | Sampling Rate | Endpoint | Notes |
|-------------|--------------|----------|-------|
| **dev** | 100% (`1.0`) | `otel-collector:4318` | All traces captured for debugging |
| **test** | 0% (`0.0`) | disabled | No tracing overhead in tests |
| **production** | 10% (`0.1`) | `otel-collector:4318` | Reduced sampling to limit volume/cost |

### Production Deployment Notes

1. **Sampling**: Set `OTEL_TRACES_SAMPLER_ARG=0.1` (10%) to reduce data volume
2. **Collector**: Deploy OTel Collector as a sidecar or DaemonSet in Kubernetes
3. **Backend**: Replace Jaeger with your preferred backend (Grafana Tempo, Datadog, New Relic, etc.)
4. **Security**: Use TLS for OTLP endpoints; set `OTEL_EXPORTER_OTLP_HEADERS` for auth tokens
5. **Resource Attributes**: Set `OTEL_RESOURCE_ATTRIBUTES=deployment.environment=production,service.version=x.y.z`
6. **Disable instrumentations**: If specific auto-instrumentation causes issues:
   ```
   OTEL_INSTRUMENTATION_SPRING_WEBMVC_ENABLED=false
   ```
7. **Agent version**: Pin the agent version in the Dockerfile `ARG OTEL_AGENT_VERSION=2.12.0`

---

## 🔄 Hiring Pipeline Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        INTERVIEW PIPELINE                                  │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  RECRUITER creates Pipeline (e.g., "Backend Engineer Pipeline")           │
│                                                                            │
│  Pipeline Stages (ordered):                                                │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │  Screening  │─▶│  Technical   │─▶│  HR Round   │─▶│   Final     │   │
│  │   Round     │  │   Round      │  │             │  │   Offer     │   │
│  └─────────────┘  └──────────────┘  └─────────────┘  └─────────────┘   │
│                                                                            │
│  Candidate Progression:                                                    │
│  ┌─────────┐  advance   ┌─────────────┐  advance   ┌───────────┐        │
│  │ PENDING │──────────▶│ IN_PROGRESS  │──────────▶│ COMPLETED │        │
│  └─────────┘            └─────────────┘            └───────────┘        │
│       │                        │                                          │
│       │ reject                 │ reject                                    │
│       ▼                        ▼                                          │
│  ┌──────────┐           ┌──────────┐                                     │
│  │ REJECTED │           │ REJECTED │                                     │
│  └──────────┘           └──────────┘                                     │
│                                                                            │
│  CandidatePipelineStatus: ACTIVE → HIRED / REJECTED / WITHDRAWN / ON_HOLD│
└──────────────────────────────────────────────────────────────────────────┘
```

### Pipeline Entities

| Entity | Description |
|--------|-------------|
| `InterviewPipeline` | Defines a hiring pipeline (name, department, stages) |
| `PipelineStage` | A single stage within a pipeline (ordered, linked to template) |
| `CandidatePipeline` | Tracks a candidate's overall progress through a pipeline |
| `CandidateStageProgress` | Tracks progress per-stage (status, feedback, timestamps) |

---

## 📋 Evaluation Scorecard Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    EVALUATION SCORECARD SYSTEM                             │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  1. ADMIN/RECRUITER defines Evaluation Criteria                           │
│     ┌─────────────────────────────────────────────────────────┐          │
│     │ Criteria: Problem Solving (weight: 2.0, maxScore: 5)    │          │
│     │ Criteria: Communication (weight: 1.5, maxScore: 5)      │          │
│     │ Criteria: System Design (weight: 2.0, maxScore: 5)      │          │
│     │ Criteria: Culture Fit (weight: 1.0, maxScore: 5)        │          │
│     └─────────────────────────────────────────────────────────┘          │
│                                                                            │
│  2. INTERVIEWER submits Scorecard after interview                         │
│     ┌─────────────────────────────────────────────────────────┐          │
│     │ Scorecard (interview + interviewer → unique)             │          │
│     │  ├── Entry: Problem Solving → score: 4, comments: "..." │          │
│     │  ├── Entry: Communication → score: 5, comments: "..."   │          │
│     │  ├── Entry: System Design → score: 3, comments: "..."   │          │
│     │  └── Entry: Culture Fit → score: 4, comments: "..."     │          │
│     │  Overall: recommendation, strengths, weaknesses          │          │
│     └─────────────────────────────────────────────────────────┘          │
│                                                                            │
│  3. System calculates WEIGHTED SCORE automatically                        │
│     weightedScore = Σ(normalizedScore × weight) / Σ(weight)              │
│                                                                            │
│  4. SUMMARY aggregated across all interviewers for a candidate            │
└──────────────────────────────────────────────────────────────────────────┘
```

### Scorecard Entities

| Entity | Description |
|--------|-------------|
| `EvaluationCriteria` | Defines scoring criteria (name, weight, maxScore, interviewType) |
| `EvaluationScorecard` | A scorecard per interview+interviewer pair |
| `ScorecardEntry` | Individual score per criteria within a scorecard |

---

## 📦 Bulk Operations

Bulk operations allow recruiters and admins to perform high-volume actions efficiently. All bulk operations support **partial success** — individual failures don't prevent the rest from completing.

### Bulk Schedule Interviews

```
POST /api/v1/bulk/interviews/schedule
Authorization: Bearer <token> (ADMIN/RECRUITER)

Request Body:
{
  "interviews": [
    {
      "title": "Technical Round - Candidate A",
      "candidateId": "uuid-1",
      "interviewerIds": ["uuid-2", "uuid-3"],
      "startTime": "2026-06-01T10:00:00Z",
      "endTime": "2026-06-01T11:00:00Z",
      "type": "TECHNICAL",
      "mode": "ONLINE",
      "meetingLink": "https://meet.google.com/abc",
      "timeZone": "America/New_York"
    },
    { ... }
  ]
}

Response (200 OK):
{
  "totalRequested": 50,
  "successCount": 48,
  "failureCount": 2,
  "successResults": [ { "id": "uuid", "title": "...", "status": "SCHEDULED", ... } ],
  "errors": [ { "index": 3, "identifier": "Title - candidateId", "errorMessage": "Candidate not found" } ]
}
```

### Bulk Invite Candidates

```
POST /api/v1/bulk/candidates/invite
Authorization: Bearer <token> (ADMIN/RECRUITER)

Request Body:
{
  "interviewId": "uuid",
  "candidates": [
    { "candidateId": "uuid-1", "customMessage": "Looking forward to meeting you!" },
    { "email": "external@example.com", "firstName": "Bob", "lastName": "Jones", "customMessage": "Please prepare..." }
  ]
}

Response (200 OK):
{
  "totalRequested": 2,
  "successCount": 2,
  "failureCount": 0,
  "successResults": [
    { "email": "candidate@example.com", "candidateName": "Alice Smith", "sent": true, "message": "Invitation sent successfully" }
  ],
  "errors": []
}
```

### Bulk Export Data

```
GET /api/v1/bulk/export?exportType=INTERVIEWS&format=CSV&statusFilter=COMPLETED&fromDate=2026-01-01T00:00:00Z&toDate=2026-05-31T23:59:59Z
Authorization: Bearer <token> (ADMIN/RECRUITER)

Response: Binary file download (Content-Disposition: attachment)

Supported export types: INTERVIEWS, CANDIDATES, FEEDBACK, SCORECARDS
Supported formats: CSV, JSON
Optional filters: statusFilter, fromDate, toDate
```

---

## 📄 File/Document Management (AWS S3)

Documents (resumes, job descriptions, interview notes, attachments) are stored in **AWS S3** for scalability, while metadata and S3 URLs are persisted in **PostgreSQL**.

### Architecture Decision

```
┌─────────────┐         ┌──────────────────┐         ┌──────────────┐
│   Client    │────────▶│  Spring Boot API  │────────▶│   AWS S3     │
│  (Upload)   │         │  /api/v1/documents │         │   Bucket     │
└─────────────┘         └────────┬─────────┘         └──────────────┘
                                 │                          │
                                 │  Save metadata           │ Store file
                                 ▼                          │
                        ┌──────────────────┐               │
                        │   PostgreSQL     │               │
                        │  (documents      │◀──────────────┘
                        │   table with     │   Return S3 URL
                        │   s3_url, size,  │
                        │   content_type)  │
                        └──────────────────┘
```

**Why S3 over PostgreSQL for file storage?**
- Documents (PDFs, images) can be **10MB–50MB+** — too large for database BLOBs
- S3 provides **unlimited scalable storage** with 99.999999999% durability
- Presigned URLs allow **direct client downloads** without server bottleneck
- PostgreSQL only stores lightweight metadata (~200 bytes per record)
- Cost-effective: S3 storage is **~$0.023/GB/month** vs expensive DB storage

### Document Entity

```java
@Entity
@Table(name = "documents")
public class Document {
    UUID id;
    String fileName;            // UUID-based S3 filename
    String originalFileName;    // Original uploaded filename
    String s3Key;               // Full S3 object key (e.g., resume/user-id/uuid.pdf)
    String s3Url;               // Constructed S3 URL
    String s3Bucket;            // Bucket name
    String contentType;         // MIME type (application/pdf, image/png, etc.)
    Long fileSize;              // Size in bytes
    DocumentType documentType;  // RESUME, JOB_DESCRIPTION, INTERVIEW_NOTES, etc.
    String entityType;          // Linked entity type (interview, candidate, etc.)
    UUID entityId;              // Linked entity ID
    User uploadedBy;            // FK → User
    String description;         // Optional description
    Instant createdAt;
    Instant updatedAt;
}
```

### DocumentType Enum
```
RESUME | JOB_DESCRIPTION | INTERVIEW_NOTES | COVER_LETTER | PORTFOLIO | ASSESSMENT | OFFER_LETTER | OTHER
```

### Allowed File Types
```
PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, TXT, CSV, PNG, JPEG, GIF, WEBP, ZIP, JSON
Max size: 50MB
```

### Upload Flow (Server-Side)

```
Client                       Server                      AWS S3
   │                           │                           │
   │  POST /api/v1/documents   │                           │
   │  (multipart/form-data)    │                           │
   │──────────────────────────▶│                           │
   │                           │  Validate type/size       │
   │                           │  Generate unique S3 key   │
   │                           │                           │
   │                           │  PUT object               │
   │                           │──────────────────────────▶│
   │                           │         200 OK            │
   │                           │◀──────────────────────────│
   │                           │                           │
   │                           │  Save metadata + S3 URL   │
   │                           │  to PostgreSQL             │
   │                           │                           │
   │  { id, fileName, s3Url,  │                           │
   │    contentType, size }    │                           │
   │◀──────────────────────────│                           │
```

### Download Flow (Presigned URL)

```
Client                       Server                      AWS S3
   │                           │                           │
   │  GET /api/v1/documents/   │                           │
   │       {id}/download-url   │                           │
   │──────────────────────────▶│                           │
   │                           │  Generate presigned URL   │
   │                           │  (expires in 60 minutes)  │
   │                           │                           │
   │  { downloadUrl,           │                           │
   │    expiresInSeconds }     │                           │
   │◀──────────────────────────│                           │
   │                           │                           │
   │  GET (presigned URL)      │                           │
   │──────────────────────────────────────────────────────▶│
   │              File content (direct from S3)            │
   │◀──────────────────────────────────────────────────────│
```

### Client-Side Upload Flow (Presigned URL)

```
Client                       Server                      AWS S3
   │                           │                           │
   │  POST /api/v1/documents/  │                           │
   │   presigned-upload        │                           │
   │──────────────────────────▶│                           │
   │                           │  Generate presigned       │
   │                           │  PUT URL                  │
   │  { uploadUrl,             │                           │
   │    expiresInSeconds }     │                           │
   │◀──────────────────────────│                           │
   │                           │                           │
   │  PUT (presigned URL)      │                           │
   │  + file body              │                           │
   │──────────────────────────────────────────────────────▶│
   │              200 OK                                   │
   │◀──────────────────────────────────────────────────────│
```

### Local Development (LocalStack)

S3 is emulated locally using **LocalStack** (included in `compose.yaml`):

```bash
docker compose up -d
./scripts/init_localstack_s3.sh
```

Environment variables for local dev:
```
AWS_S3_ACCESS_KEY=test
AWS_S3_SECRET_KEY=test
AWS_S3_ENDPOINT=http://localhost:4566
AWS_S3_BUCKET_NAME=interview-platform-documents
AWS_S3_REGION=us-east-1
```

---

## ⚙️ Bulk & Document Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `AWS_S3_BUCKET_NAME` | `interview-platform-documents` | S3 bucket name |
| `AWS_S3_REGION` | `us-east-1` | AWS region |
| `AWS_S3_ACCESS_KEY` | (empty) | AWS access key ID |
| `AWS_S3_SECRET_KEY` | (empty) | AWS secret access key |
| `AWS_S3_ENDPOINT` | (empty) | Custom endpoint (for LocalStack/MinIO) |
| `AWS_S3_PRESIGNED_URL_EXPIRY` | `60` | Presigned URL expiry in minutes |

### Production S3 Setup

**1. Create S3 Bucket:**
```bash
aws s3 mb s3://interview-platform-documents --region us-east-1
```

**2. Create IAM Policy:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::interview-platform-documents",
        "arn:aws:s3:::interview-platform-documents/*"
      ]
    }
  ]
}
```

**3. Configure CORS on the bucket (for client-side uploads):**
```json
{
  "CORSRules": [
    {
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST"],
      "AllowedOrigins": ["https://your-frontend-domain.com"],
      "ExposeHeaders": ["ETag"],
      "MaxAgeSeconds": 3600
    }
  ]
}
```

**4. Set environment variables:**
```bash
export AWS_S3_BUCKET_NAME=interview-platform-documents
export AWS_S3_REGION=us-east-1
export AWS_S3_ACCESS_KEY=AKIA...
export AWS_S3_SECRET_KEY=secret...
export AWS_S3_PRESIGNED_URL_EXPIRY=60
```

---

## 🗄️ Documents Database Schema (V14 Migration)

```sql
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name VARCHAR(500) NOT NULL,           -- S3 file name (UUID-based)
    original_file_name VARCHAR(500) NOT NULL,   -- Original uploaded file name
    content_type VARCHAR(255) NOT NULL,         -- MIME type
    file_size BIGINT NOT NULL,                  -- Size in bytes
    s3_bucket VARCHAR(255) NOT NULL,            -- S3 bucket name
    s3_key VARCHAR(1000) NOT NULL UNIQUE,       -- Full S3 object key
    s3_url TEXT NOT NULL,                       -- Constructed S3 URL
    document_type VARCHAR(50) NOT NULL,         -- RESUME, JOB_DESCRIPTION, etc.
    entity_type VARCHAR(100),                   -- Optional: linked entity type
    entity_id UUID,                             -- Optional: linked entity ID
    uploaded_by UUID NOT NULL REFERENCES users(id),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);
```

**Indexes:**
- `idx_documents_uploaded_by` – Fast lookup by uploader
- `idx_documents_entity` – Fast lookup by linked entity
- `idx_documents_type` – Filter by document type
- `idx_documents_s3_key` – Unique S3 key lookup

### S3 Key Structure

```
{document_type}/{user_id}/{uuid}.{extension}
```

Example: `resume/550e8400-e29b-41d4-a716-446655440000/a1b2c3d4-e5f6-7890-abcd-ef1234567890.pdf`

---

## 🔒 Security & Authorization (Bulk & Documents)

### Bulk Operations

| Endpoint | Required |
|----------|----------|
| Bulk Schedule | `INTERVIEW_CREATE` permission OR `ADMIN`/`RECRUITER` role |
| Bulk Invite | `INTERVIEW_CREATE` permission OR `ADMIN`/`RECRUITER` role |
| Bulk Export | `ADMIN` or `RECRUITER` role |

### Document Management

| Endpoint | Required |
|----------|----------|
| Upload | Any authenticated user |
| View metadata | Any authenticated user |
| Get by type | `ADMIN` or `RECRUITER` role |
| Download URL | Any authenticated user |
| Delete | Document owner only |
| Update metadata | Any authenticated user |

### Rate Limits

Bulk operations are limited to **100 items per request** to prevent abuse and server overload.

### Allowed File Types

- PDF (`.pdf`)
- Word (`.doc`, `.docx`)
- Excel (`.xls`, `.xlsx`)
- PowerPoint (`.ppt`, `.pptx`)
- Text (`.txt`, `.csv`)
- Images (`.png`, `.jpeg`, `.gif`, `.webp`)
- Archives (`.zip`)
- JSON (`.json`)

**Max file size:** 50MB

---

## ❌ Error Handling (Bulk & Documents)

All errors follow the standard `ErrorResponse` format:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "File size exceeds maximum limit of 50MB",
  "path": "/api/v1/documents",
  "timestamp": "2026-05-31T10:30:00Z"
}
```

### Common Error Scenarios

| Scenario | Status | Message |
|----------|--------|---------|
| File too large | 400 | File size exceeds maximum limit of 50MB |
| Invalid file type | 400 | File type not allowed |
| Empty file | 400 | File cannot be empty |
| Document not found | 404 | Document not found with id: ... |
| Delete other's document | 403 | You can only delete your own documents |
| Bulk limit exceeded | 400 | Cannot schedule more than 100 interviews at once |
| S3 upload failure | 400 | Failed to upload file: ... |
| S3 not configured | 500 | S3 storage is not configured |

---

## 🧪 Testing (Bulk & Documents)

### cURL Examples

**Bulk Schedule:**
```bash
curl -X POST http://localhost:8080/api/v1/bulk/interviews/schedule \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "interviews": [
      {
        "title": "Tech Interview",
        "candidateId": "uuid",
        "startTime": "2026-07-01T10:00:00Z",
        "endTime": "2026-07-01T11:00:00Z",
        "type": "TECHNICAL",
        "mode": "ONLINE",
        "interviewerIds": ["uuid"]
      }
    ]
  }'
```

**Bulk Invite Candidates:**
```bash
curl -X POST http://localhost:8080/api/v1/bulk/candidates/invite \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "interviewId": "uuid-interview",
    "candidates": [
      { "candidateId": "uuid-1", "customMessage": "Looking forward to meeting you!" },
      { "email": "external@example.com", "firstName": "Bob", "lastName": "Jones" }
    ]
  }'
```

**Upload Document:**
```bash
curl -X POST http://localhost:8080/api/v1/documents \
  -H "Authorization: Bearer <token>" \
  -F "file=@resume.pdf" \
  -F "documentType=RESUME" \
  -F "entityType=candidate" \
  -F "entityId=<candidate-uuid>"
```

**Export Interviews:**
```bash
curl -X GET "http://localhost:8080/api/v1/bulk/export?exportType=INTERVIEWS&format=CSV" \
  -H "Authorization: Bearer <token>" \
  --output interviews.csv
```

**Get Download URL:**
```bash
curl -X GET http://localhost:8080/api/v1/documents/<doc-id>/download-url \
  -H "Authorization: Bearer <token>"
```

**Get Presigned Upload URL (Client-Side Upload):**
```bash
curl -X POST "http://localhost:8080/api/v1/documents/presigned-upload?fileName=report.pdf&contentType=application/pdf&documentType=ASSESSMENT" \
  -H "Authorization: Bearer <token>"
```

### Using LocalStack for Testing

```bash
# List files in bucket
aws --endpoint-url=http://localhost:4566 s3 ls s3://interview-platform-documents/ --recursive

# Download a file directly from LocalStack
aws --endpoint-url=http://localhost:4566 s3 cp s3://interview-platform-documents/resume/uuid/file.pdf ./downloaded.pdf
```

---

## 📁 Bulk & Document Project Structure

```
src/main/java/.../
├── bulk/
│   ├── controller/
│   │   └── BulkOperationController.java
│   ├── dto/
│   │   ├── BulkScheduleInterviewsRequest.java
│   │   ├── BulkInviteCandidatesRequest.java
│   │   ├── BulkExportRequest.java
│   │   ├── BulkOperationResponse.java
│   │   └── BulkInviteResult.java
│   └── service/
│       ├── BulkOperationService.java
│       └── BulkOperationServiceImpl.java
├── document/
│   ├── controller/
│   │   └── DocumentController.java
│   ├── dto/
│   │   ├── DocumentResponse.java
│   │   ├── PresignedUrlResponse.java
│   │   ├── UpdateDocumentRequest.java
│   │   └── UploadDocumentRequest.java
│   ├── entity/
│   │   ├── Document.java
│   │   └── DocumentType.java
│   ├── repository/
│   │   └── DocumentRepository.java
│   └── service/
│       ├── DocumentService.java
│       ├── DocumentServiceImpl.java
│       └── S3StorageService.java
└── config/
    └── FileUploadConfig.java

src/main/resources/
├── application-s3.yml          # S3 configuration
└── db/migration/
    └── V14__create_documents_table.sql

scripts/
└── init_localstack_s3.sh       # LocalStack bucket initialization

compose.yaml                    # Includes LocalStack service
```

---

## 📦 Dependencies Added (Bulk & Documents)

```xml
<!-- AWS S3 SDK -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.60</version>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sts</artifactId>
    <version>2.25.60</version>
</dependency>

<!-- Apache Commons CSV (for bulk export) -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.11.0</version>
</dependency>
```

---

## 📋 Audit Trail (Bulk & Documents)

All bulk operations and document management actions are logged:

| Action | Logged When |
|--------|-------------|
| `BULK_SCHEDULE` | Bulk interview scheduling completed |
| `BULK_INVITE` | Bulk candidate invitation completed |
| `BULK_EXPORT` | Data export requested |
| `DOCUMENT_UPLOAD` | File uploaded to S3 |
| `DOCUMENT_DELETE` | File deleted from S3 |

---

## 💼 Job Positions/Openings

Job Positions represent open roles that interviews can be linked to. This enables tracking the full hiring funnel per position.

### Entity Model

```java
@Entity
@Table(name = "job_positions")
public class JobPosition {
    UUID id;
    String title;                    // e.g., "Senior Backend Engineer"
    String department;               // e.g., "Engineering"
    String location;                 // e.g., "New York, NY (Remote)"
    EmploymentType employmentType;   // FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP, FREELANCE, TEMPORARY
    ExperienceLevel experienceLevel; // ENTRY, JUNIOR, MID, SENIOR, LEAD, PRINCIPAL, EXECUTIVE
    JobPositionStatus status;        // DRAFT, OPEN, ON_HOLD, CLOSED, FILLED, CANCELLED
    String description;              // Detailed job description
    String requirements;             // Required qualifications
    String responsibilities;         // Key responsibilities
    BigDecimal salaryMin;            // Salary range min
    BigDecimal salaryMax;            // Salary range max
    String salaryCurrency;           // USD, EUR, etc.
    Integer numberOfOpenings;        // Positions available
    Integer numberHired;             // Positions filled so far
    InterviewPipeline pipeline;      // Optional linked pipeline
    User createdBy;                  // Recruiter who created
    User hiringManager;              // Hiring manager for this role
    String skills;                   // Required skills (comma-separated)
    Instant postedAt;                // When the position was published
    Instant closedAt;                // When it was closed/filled
    Instant deadline;                // Application deadline
    List<Interview> interviews;      // All interviews for this position
}
```

### Interview-to-Position Linking

Interviews have an optional `jobPositionId` FK. When an interview is linked to a position:
- You can view all interviews for a specific opening
- Track how many candidates are in the pipeline per position
- Generate per-position reports and analytics

```
POST /api/v1/job-positions/{positionId}/interviews/{interviewId}
DELETE /api/v1/job-positions/interviews/{interviewId}
```

### CRUD Examples

**Create a job position:**
```bash
curl -X POST http://localhost:8080/api/v1/job-positions \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Senior Backend Engineer",
    "department": "Engineering",
    "location": "Remote (US)",
    "employmentType": "FULL_TIME",
    "experienceLevel": "SENIOR",
    "description": "We are looking for a Senior Backend Engineer...",
    "requirements": "5+ years Java/Spring Boot experience...",
    "responsibilities": "Design and implement microservices...",
    "salaryMin": 150000,
    "salaryMax": 200000,
    "salaryCurrency": "USD",
    "numberOfOpenings": 2,
    "skills": "Java, Spring Boot, PostgreSQL, Kafka, AWS",
    "hiringManagerId": "uuid-manager",
    "pipelineId": "uuid-pipeline"
  }'
```

**Response:**
```json
{
  "id": "uuid",
  "title": "Senior Backend Engineer",
  "department": "Engineering",
  "status": "OPEN",
  "employmentType": "FULL_TIME",
  "experienceLevel": "SENIOR",
  "numberOfOpenings": 2,
  "numberHired": 0,
  "totalInterviews": 0,
  "totalCandidates": 0,
  "createdByName": "Jane Recruiter",
  "hiringManagerName": "John Manager",
  ...
}
```

**Search positions:**
```bash
GET /api/v1/job-positions/search?keyword=backend&page=0&size=10
```

**Filter by status:**
```bash
GET /api/v1/job-positions/filter/status?status=OPEN
```

### Database Schema (V15 Migration)

```sql
CREATE TABLE job_positions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(300) NOT NULL,
    department VARCHAR(200),
    location VARCHAR(300),
    employment_type VARCHAR(50) NOT NULL DEFAULT 'FULL_TIME',
    experience_level VARCHAR(50) NOT NULL DEFAULT 'MID',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    description TEXT,
    requirements TEXT,
    responsibilities TEXT,
    salary_min DECIMAL(12,2),
    salary_max DECIMAL(12,2),
    salary_currency VARCHAR(10) DEFAULT 'USD',
    number_of_openings INTEGER NOT NULL DEFAULT 1,
    number_hired INTEGER NOT NULL DEFAULT 0,
    pipeline_id UUID REFERENCES interview_pipelines(id),
    created_by UUID NOT NULL REFERENCES users(id),
    hiring_manager_id UUID REFERENCES users(id),
    skills TEXT,
    posted_at TIMESTAMP,
    closed_at TIMESTAMP,
    deadline TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Link from interviews table
ALTER TABLE interviews ADD COLUMN job_position_id UUID REFERENCES job_positions(id);
```

---

## 📊 Interview Reports & Analytics

Comprehensive analytics with PDF report generation for hiring insights.

### Analytics Report (JSON)

**Endpoint:** `GET /api/v1/reports/analytics`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `fromDate` | Instant | Filter from date |
| `toDate` | Instant | Filter to date |
| `statusFilter` | Enum | Filter by interview status |
| `department` | String | Filter by department |

**Response:**
```json
{
  "totalInterviews": 250,
  "completedInterviews": 180,
  "cancelledInterviews": 15,
  "totalCandidates": 120,
  "totalJobPositions": 12,
  "openJobPositions": 5,
  "conversionMetrics": {
    "totalCandidatesScreened": 120,
    "passedScreening": 80,
    "passedTechnical": 45,
    "offersExtended": 20,
    "hired": 15,
    "screeningToTechnicalRate": 66.7,
    "technicalToOfferRate": 44.4,
    "offerAcceptanceRate": 75.0,
    "overallConversionRate": 12.5
  },
  "timeToHireMetrics": {
    "averageDaysToHire": 28.5,
    "medianDaysToHire": 25.0,
    "averageDaysPerStage": 7.1,
    "averageInterviewsPerCandidate": 3.2,
    "fastestHireDays": 10,
    "slowestHireDays": 65
  },
  "interviewerPerformances": [
    {
      "interviewerId": "uuid",
      "interviewerName": "Alice Smith",
      "totalInterviewsConducted": 45,
      "feedbackSubmitted": 42,
      "feedbackSubmissionRate": 93.3,
      "averageRatingGiven": 3.8,
      "hireRecommendations": 15,
      "noHireRecommendations": 20,
      "averageInterviewDurationMinutes": 52.5
    }
  ],
  "interviewsByStatus": { "COMPLETED": 180, "SCHEDULED": 40, "CANCELLED": 15 },
  "interviewsByType": { "TECHNICAL": 100, "SCREENING": 80, "HR": 50 },
  "interviewsByMonth": { "JANUARY 2026": 20, "FEBRUARY 2026": 35 },
  "candidatesByRecommendation": { "HIRE": 50, "NO_HIRE": 40, "HOLD": 30 }
}
```

### Conversion Funnel

**Endpoint:** `GET /api/v1/reports/metrics/conversion`

Tracks candidate progression through interview stages:
```
Screening → Technical → Offer → Hire
   120    →    80     →  45   →  20   →  15
            66.7%        44.4%    75.0%
```

### Time-to-Hire Metrics

**Endpoint:** `GET /api/v1/reports/metrics/time-to-hire`

Measures efficiency of the hiring process:
- **Average days to hire** – Mean time from first interview to final decision
- **Median days to hire** – Midpoint (less affected by outliers)
- **Average days per stage** – How long each stage takes
- **Average interviews per candidate** – Number of rounds

### Interviewer Performance

**Endpoint:** `GET /api/v1/reports/analytics/interviewer/{interviewerId}`

Per-interviewer metrics:
- Total interviews conducted
- Feedback submission rate (%)
- Average rating given
- Hire vs. no-hire recommendation ratio
- Average interview duration

### PDF Report Generation

Generate professional PDF reports for offline sharing and stakeholder presentations.

**Overall Analytics PDF:**
```bash
curl -X GET "http://localhost:8080/api/v1/reports/pdf/analytics?fromDate=2026-01-01T00:00:00Z&toDate=2026-06-01T00:00:00Z" \
  -H "Authorization: Bearer <token>" \
  --output analytics_report.pdf
```

**Interviewer Performance PDF:**
```bash
curl -X GET "http://localhost:8080/api/v1/reports/pdf/interviewer/{interviewerId}" \
  -H "Authorization: Bearer <token>" \
  --output interviewer_report.pdf
```

**Job Position PDF:**
```bash
curl -X GET "http://localhost:8080/api/v1/reports/pdf/job-position/{jobPositionId}" \
  -H "Authorization: Bearer <token>" \
  --output position_report.pdf
```

PDF reports include:
- Overview metrics table
- Conversion funnel data
- Time-to-hire statistics
- Interviewer performance matrix
- Interview status breakdown
- Job position details with linked interviews

### Project Structure

```
src/main/java/.../
├── jobposition/
│   ├── controller/
│   │   └── JobPositionController.java
│   ├── dto/
│   │   ├── CreateJobPositionRequest.java
│   │   ├── UpdateJobPositionRequest.java
│   │   └── JobPositionResponse.java
│   ├── entity/
│   │   ├── JobPosition.java
│   │   ├── JobPositionStatus.java
│   │   ├── EmploymentType.java
│   │   └── ExperienceLevel.java
│   ├── repository/
│   │   └── JobPositionRepository.java
│   └── service/
│       ├── JobPositionService.java
│       └── JobPositionServiceImpl.java
├── report/
│   ├── controller/
│   │   └── ReportController.java
│   ├── dto/
│   │   ├── AnalyticsReport.java
│   │   └── ReportRequest.java
│   └── service/
│       ├── ReportService.java
│       └── ReportServiceImpl.java

src/main/resources/db/migration/
└── V15__create_job_positions_table.sql
```

### Dependencies Added

```xml
<!-- OpenPDF for PDF report generation -->
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>2.0.2</version>
</dependency>
```


---

## 🗓️ Automated Scheduling

Auto-suggest optimal interview time slots based on interviewer availability and candidate preferences.

### How It Works

1. **Interviewers set their availability** – recurring weekly slots or specific dates
2. **Recruiter requests suggestions** – specifies interviewers, date range, and duration
3. **System finds overlapping slots** – checks against existing interview conflicts
4. **Returns ranked suggestions** – scored by how many interviewers are available

### Set Availability

```bash
POST /api/v1/scheduling/availability
{
  "dayOfWeek": 1,          // 0=Monday, 6=Sunday (Tuesday)
  "startTime": "09:00",
  "endTime": "12:00",
  "timeZone": "America/New_York",
  "isRecurring": true
}
```

### Request Time Suggestions

```bash
POST /api/v1/scheduling/suggest
{
  "interviewerIds": ["uuid-1", "uuid-2", "uuid-3"],
  "candidateId": "uuid-candidate",
  "fromDate": "2026-06-15",
  "toDate": "2026-06-20",
  "durationMinutes": 60,
  "preferredTimeZone": "America/New_York"
}
```

**Response:**
```json
[
  {
    "startTime": "2026-06-16T14:00:00Z",
    "endTime": "2026-06-16T15:00:00Z",
    "availableInterviewerIds": ["uuid-1", "uuid-2", "uuid-3"],
    "availableInterviewerNames": ["Alice Smith", "Bob Jones", "Charlie Brown"],
    "score": 100.0
  },
  {
    "startTime": "2026-06-17T10:00:00Z",
    "endTime": "2026-06-17T11:00:00Z",
    "availableInterviewerIds": ["uuid-1", "uuid-3"],
    "availableInterviewerNames": ["Alice Smith", "Charlie Brown"],
    "score": 66.7
  }
]
```

Score represents percentage of requested interviewers who are available for that slot.

---

## ⏰ Interview Reminders

Automated scheduled reminders sent via email (24h, 1h, and 15min before) to all interview participants.

### How It Works

1. **Reminders auto-created** when an interview is scheduled
2. **Scheduler runs every minute** checking for due reminders
3. **Sends via configured channel** (email, SMS, push)
4. **Auto-cancelled** when an interview is cancelled

### Reminder Types

| Type | When Sent | Purpose |
|------|-----------|---------|
| `BEFORE_24H` | 24 hours before | Day-before preparation reminder |
| `BEFORE_1H` | 1 hour before | Final preparation reminder |
| `BEFORE_15MIN` | 15 minutes before | Join meeting reminder |

### Manual Trigger

```bash
# Create reminders for an interview
POST /api/v1/reminders/interview/{interviewId}

# Cancel all pending reminders
DELETE /api/v1/reminders/interview/{interviewId}
```

### Reminder Status Flow

```
PENDING → SENT (success)
PENDING → FAILED (error)
PENDING → CANCELLED (interview cancelled)
```

---

## 🙋 Candidate Self-Service Portal

Candidates can submit their preferred time slots and availability for interview scheduling.

### Workflow

1. Candidate receives interview invitation
2. Candidate submits preferred time slots (ranked by priority)
3. Recruiter views candidate preferences
4. Recruiter accepts/rejects slots and schedules accordingly

### Submit Preferences

```bash
POST /api/v1/self-service/preferred-slots
{
  "preferredDate": "2026-06-18",
  "startTime": "10:00",
  "endTime": "11:00",
  "timeZone": "America/New_York",
  "priority": 1,
  "notes": "Morning works best for me",
  "jobPositionId": "uuid-position"
}
```

### Recruiter Actions

```bash
# View candidate preferences for a position
GET /api/v1/self-service/preferred-slots/job-position/{jobPositionId}

# Accept a slot
PATCH /api/v1/self-service/preferred-slots/{slotId}/status?status=ACCEPTED

# Reject a slot
PATCH /api/v1/self-service/preferred-slots/{slotId}/status?status=REJECTED
```

---

## 👥 Team/Department Management

Organize interviewers into teams and departments for better assignment and scheduling.

### Team Structure

```
Team
├── name, description, department
├── manager (User)
└── members[]
    ├── user
    ├── role: LEAD | MEMBER | OBSERVER
    └── joinedAt
```

### Create a Team

```bash
POST /api/v1/teams
{
  "name": "Backend Engineering",
  "description": "Backend interview panel",
  "department": "Engineering",
  "managerId": "uuid-manager"
}
```

### Manage Members

```bash
# Add member with role
POST /api/v1/teams/{teamId}/members/{userId}?role=LEAD

# Update role
PATCH /api/v1/teams/{teamId}/members/{userId}/role?role=MEMBER

# Remove member
DELETE /api/v1/teams/{teamId}/members/{userId}
```

### Query Teams

```bash
# Get teams by department
GET /api/v1/teams/department/Engineering

# Get my teams
GET /api/v1/teams/my
```

---

## 🏷️ Tags & Labels

Flexible tagging system to categorize and filter interviews, candidates, questions, and job positions.

### Tag Structure

| Field | Description |
|-------|-------------|
| `name` | Unique tag name (e.g., "urgent", "senior-level") |
| `color` | Hex color code for UI display |
| `category` | INTERVIEW, CANDIDATE, QUESTION, GENERAL |

### Create Tags

```bash
POST /api/v1/tags
{
  "name": "senior-level",
  "color": "#28a745",
  "category": "INTERVIEW"
}
```

### Tag Entities

```bash
# Tag an interview
POST /api/v1/tags/{tagId}/entities/INTERVIEW/{interviewId}

# Tag a candidate
POST /api/v1/tags/{tagId}/entities/USER/{userId}

# Tag a question
POST /api/v1/tags/{tagId}/entities/QUESTION/{questionId}

# Tag a job position
POST /api/v1/tags/{tagId}/entities/JOB_POSITION/{positionId}
```

### Filter by Tags

```bash
# Get all tags for an interview
GET /api/v1/tags/entities/INTERVIEW/{interviewId}

# Get all interviews with a specific tag
GET /api/v1/tags/{tagId}/entities/INTERVIEW

# Search tags
GET /api/v1/tags/search?query=senior
```

### Remove Tags

```bash
DELETE /api/v1/tags/{tagId}/entities/INTERVIEW/{interviewId}
```

---

## 📁 New Features Project Structure

```
src/main/java/.../
├── scheduling/
│   ├── controller/SchedulingController.java
│   ├── dto/
│   │   ├── AvailabilitySlotResponse.java
│   │   ├── CreateAvailabilitySlotRequest.java
│   │   ├── SuggestedTimeSlot.java
│   │   └── SuggestTimeSlotsRequest.java
│   ├── entity/AvailabilitySlot.java
│   ├── repository/AvailabilitySlotRepository.java
│   └── service/SchedulingService.java
├── reminder/
│   ├── controller/ReminderController.java
│   ├── entity/InterviewReminder.java
│   ├── repository/InterviewReminderRepository.java
│   └── service/ReminderService.java
├── selfservice/
│   ├── controller/CandidateSelfServiceController.java
│   ├── dto/
│   │   ├── PreferredSlotResponse.java
│   │   └── SubmitPreferredSlotRequest.java
│   ├── entity/CandidatePreferredSlot.java
│   ├── repository/CandidatePreferredSlotRepository.java
│   └── service/CandidateSelfServiceImpl.java
├── team/
│   ├── controller/TeamController.java
│   ├── dto/
│   │   ├── CreateTeamRequest.java
│   │   └── TeamResponse.java
│   ├── entity/
│   │   ├── Team.java
│   │   └── TeamMember.java
│   ├── repository/
│   │   ├── TeamRepository.java
│   │   └── TeamMemberRepository.java
│   └── service/TeamService.java
├── tag/
│   ├── controller/TagController.java
│   ├── dto/
│   │   ├── CreateTagRequest.java
│   │   └── TagResponse.java
│   ├── entity/
│   │   ├── Tag.java
│   │   └── EntityTag.java
│   ├── repository/
│   │   ├── TagRepository.java
│   │   └── EntityTagRepository.java
│   └── service/TagService.java

src/main/resources/db/migration/
└── V16__scheduling_reminders_selfservice_teams_tags.sql
```

### Database Tables Added (V16)

| Table | Purpose |
|-------|---------|
| `availability_slots` | Interviewer recurring/specific availability |
| `interview_reminders` | Scheduled reminder records |
| `candidate_preferred_slots` | Candidate preferred time submissions |
| `teams` | Team/department definitions |
| `team_members` | Team membership with roles |
| `tags` | Tag definitions with colors |
| `entity_tags` | Many-to-many tag assignments |

---

## New Features (Latest)

### Code Execution Engine (Docker Sandbox)

Sandboxed code execution for coding assessments. Candidates can write and run code in 10 supported languages during interviews.

**Supported Languages:** Java, Python, JavaScript, TypeScript, C++, C, Go, Rust, Ruby, PHP

**Security:** Each execution runs in an isolated Docker container with:
- No network access
- Memory limit (256MB default)
- CPU throttling (50% of one core)
- PID limit (64 processes)
- Read-only filesystem, runs as `nobody`
- All capabilities dropped (`CAP_DROP ALL`)
- Automatic container cleanup

**API Endpoints** (`/api/v1/code-execution`):
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/run` | Submit code for execution |
| GET | `/{executionId}` | Get execution result |
| GET | `/session/{codingSessionId}` | Get all executions for a session |
| GET | `/languages` | List supported languages |

### SSO/SAML Integration

Enterprise SSO support for Okta, OneLogin, Azure AD, and generic SAML 2.0 providers. Configurable per-tenant.

**API Endpoints** (`/api/v1/sso`):
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create SSO/SAML configuration (ADMIN) |
| PUT | `/{configId}` | Update SSO configuration |
| GET | `/{configId}` | Get SSO configuration |
| GET | `/tenant/{tenantId}` | Get all SSO configs for tenant |
| PATCH | `/{configId}/toggle` | Enable/disable SSO config |
| DELETE | `/{configId}` | Delete SSO configuration |
| GET | `/tenant/{tenantId}/login-urls` | Get SSO login URLs (public) |

**SAML Flow:** SP-initiated SSO via `/saml2/authenticate/{registrationId}`

### Account Lockout & IP Blocking

Protects against brute-force attacks with configurable lockout thresholds and IP-based blocking.

**Features:**
- Lock accounts after N failed attempts (default: 5)
- Auto-unlock after configurable duration (default: 30 min)
- IP-based blocking after excessive failures (default: 20 from same IP)
- Security alert emails on suspicious activity
- Full login attempt audit trail
- Admin APIs for manual lock/unlock/block/unblock

**API Endpoints** (`/api/v1/security`):
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/lockout/{email}` | Get lockout status (ADMIN) |
| POST | `/lockout/{email}/unlock` | Unlock account (ADMIN) |
| GET | `/blocked-ips` | Get all blocked IPs (ADMIN) |
| POST | `/block-ip` | Manually block IP (ADMIN) |
| POST | `/unblock-ip/{ip}` | Unblock IP (ADMIN) |
| GET | `/login-attempts/{email}` | Get login history (ADMIN) |

### Data Encryption at Rest (SOC2/GDPR)

AES-256-GCM field-level encryption for PII data. Transparent encrypt/decrypt via JPA AttributeConverters.

**Encrypted Fields:**
- `User.phoneNumber`
- `UserProfile.contactNumber`, `linkedinUrl`, `githubUrl`
- `JobPosition.salaryMin`, `salaryMax`

**Features:**
- AES-256-GCM authenticated encryption (NIST recommended)
- Unique random IV per encryption operation
- `ENC:` prefix for identifying encrypted values
- Backward-compatible decryption (unencrypted values returned as-is)
- Migration tool for encrypting existing data (`--spring.profiles.active=dev,encrypt-migrate`)
- Configurable: disable for development, mandatory in production

### Candidate Portal / Job Board

Public-facing job listings and application management for candidates.

**Public Endpoints** (no auth, `/api/v1/jobs`):
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Paginated public job listings |
| GET | `/{id}` | Job detail view |
| GET | `/search` | Search with filters (keyword, department, location, type, level) |

**Candidate Portal** (authenticated, `/api/v1/portal`):
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/applications` | Submit job application |
| GET | `/applications` | Get my applications |
| GET | `/applications/{id}` | Application detail |
| DELETE | `/applications/{id}/withdraw` | Withdraw application |
| GET | `/admin/applications/position/{id}` | Applications for position (ADMIN/RECRUITER) |
| PATCH | `/admin/applications/{id}/status` | Update application status (ADMIN/RECRUITER) |

**Application Status Flow:** SUBMITTED -> UNDER_REVIEW -> SHORTLISTED -> INTERVIEW_SCHEDULED -> OFFERED -> HIRED/REJECTED/WITHDRAWN

### Offer Letter Management

Create, approve, send, and track offer letters with e-signature integration.

**API Endpoints** (`/api/v1/offers`):
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create offer (ADMIN/RECRUITER) |
| GET | `/{id}` | Get offer details |
| POST | `/{id}/submit-approval` | Submit for approval |
| POST | `/{id}/approve` | Process approval decision |
| POST | `/{id}/send` | Send offer to candidate |
| POST | `/{id}/view` | Mark as viewed (CANDIDATE) |
| POST | `/{id}/respond` | Accept/decline (CANDIDATE) |
| POST | `/{id}/revoke` | Revoke offer (ADMIN/RECRUITER) |
| GET | `/candidate/my` | My offers (CANDIDATE) |
| GET | `/position/{positionId}` | Offers for position |
| GET | `/{id}/esignature-status` | Check e-signature status |

**Approval Workflow:** Sequential multi-approver with email notifications
**E-Signature:** DocuSign and HelloSign/Dropbox Sign integration (simulated, production-ready interfaces)

### Calendar Sync (Bidirectional)

Bidirectional sync with Google Calendar and Outlook/Microsoft Graph.

**API Endpoints** (`/api/v1/calendar-sync`):
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/connect` | Connect external calendar (OAuth) |
| DELETE | `/connections/{id}` | Disconnect calendar |
| GET | `/connections` | List connected calendars |
| POST | `/sync/interview/{interviewId}` | Sync single interview |
| POST | `/sync/all` | Sync all upcoming interviews |
| POST | `/sync/bidirectional/{connectionId}` | Full bidirectional sync |
| GET | `/events` | List synced events |

**Features:**
- OAuth2 token exchange for Google and Microsoft
- Automatic token refresh on expiry
- Push interviews to external calendars
- Pull external changes back to platform
- Per-event tracking with external event IDs

### Secret Management (HashiCorp Vault)

Production-grade secret management using HashiCorp Vault (free/open-source).

**Managed Secrets:**
- Database credentials (DDL and DML users)
- JWT signing secrets
- RSA key pairs (loaded from Vault instead of classpath)
- OAuth2 client secrets
- Encryption keys
- API keys for external services

**Setup:**
```bash
# Start Vault (included in docker-compose)
docker compose up -d vault

# Initialize Vault with application secrets
./scripts/vault/init-vault.sh

# Run app with Vault profile
SPRING_PROFILES_ACTIVE=dev,vault VAULT_TOKEN=dev-root-token ./mvnw spring-boot:run
```

**Production:** Set `SPRING_PROFILES_ACTIVE=prod` which auto-enables Vault integration.

### Structured JSON Logging with Correlation IDs

Production-ready logging for ELK/Datadog/Loki log aggregation.

**Dev mode:** Human-readable console output with correlation ID
```
12:30:00.123 INFO  [async-mdc-1] [abc-123-def] c.i.s.AuthService - Login successful
```

**Production mode (JSON):**
```json
{
  "@timestamp": "2026-06-18T12:30:00.123Z",
  "level": "INFO",
  "service": "interview-platform-backend",
  "environment": "prod",
  "correlationId": "abc-123-def",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "userId": "admin@example.com",
  "requestMethod": "POST",
  "requestUri": "/api/v1/auth/login",
  "clientIp": "192.168.1.1",
  "message": "Login successful"
}
```

**MDC fields propagated to:** Async threads, Kafka consumers, WebSocket handlers

### CI/CD Pipeline (GitHub Actions)

7-stage pipeline with full security scanning (all free/open-source tools):

| Stage | Tool | Purpose |
|-------|------|---------|
| Build | Maven + JDK 21 | Compile + unit tests |
| Integration Tests | Testcontainers | PostgreSQL, Kafka, Vault |
| SAST | SonarCloud | Static security analysis |
| Dependency Scan | OWASP Dependency-Check | CVE scanning (fail on CVSS >= 7) |
| Docker Scan | Trivy (Aqua Security) | Container image vulnerabilities |
| Migration Check | Flyway | Validates DDL migrations |
| Security Gate | Composite | Blocks merge on critical issues |

**Run scans locally:**
```bash
# OWASP dependency check
./mvnw dependency-check:check

# SonarQube analysis (requires SONAR_TOKEN)
./mvnw sonar:sonar -Dsonar.host.url=https://sonarcloud.io
```

### Database User Separation (Least Privilege)

Separate database users for DDL (schema changes) and DML (application runtime).

| User | Privileges | Used By |
|------|-----------|---------|
| `ddl_admin` | CREATE, ALTER, DROP, ALL | Flyway migrations |
| `app_user` | SELECT, INSERT, UPDATE, DELETE | Application runtime |

**Setup:**
```bash
# Create separate users on PostgreSQL
PGPASSWORD=postgres psql -h localhost -p 5433 -U admin -d interview_platform -f scripts/db/setup-users.sql
```

**Enable in config:**
```yaml
app:
  database:
    separate-users: true
    ddl:
      username: ddl_admin
      password: ${DB_DDL_PASSWORD}
```

---

## Running All Features Locally

```bash
# 1. Start infrastructure (includes Vault)
docker compose up -d postgres kafka redis localstack otel-collector jaeger vault

# 2. (Optional) Initialize Vault with secrets
./scripts/vault/init-vault.sh

# 3. (Optional) Set up DB user separation
PGPASSWORD=postgres psql -h localhost -p 5433 -U admin -d interview_platform -f scripts/db/setup-users.sql

# 4. Run with all features enabled
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# All features are enabled by default in dev profile:
# - Code execution engine (requires Docker daemon)
# - Account lockout & IP blocking
# - Field-level encryption (dev key auto-generated)
# - Calendar sync
# - SSO/SAML (configure IdP in /api/v1/sso)
# - Offer management
# - Job board (public at /api/v1/jobs)
# - Structured logging (human-readable in dev)
```

### Environment Variables for All Features

See `.env.example` for the complete list. Key additions:

```bash
# Code Execution
CODE_EXECUTION_ENABLED=true
DOCKER_HOST=unix:///var/run/docker.sock

# Account Lockout
LOCKOUT_ENABLED=true
LOCKOUT_MAX_ATTEMPTS=5

# Encryption
ENCRYPTION_ENABLED=true
ENCRYPTION_SECRET_KEY=  # Generate with: openssl rand -base64 32

# Vault (optional for dev)
VAULT_ENABLED=false
VAULT_TOKEN=dev-root-token

# DB Separation (optional for dev)
DB_SEPARATE_USERS=false

# Calendar Sync
CALENDAR_SYNC_ENABLED=true

# E-Signature (optional)
DOCUSIGN_ENABLED=false
HELLOSIGN_ENABLED=false
```

---

## Documentation Index

| Document | Description |
|----------|-------------|
| [README.md](README.md) | Main project overview, API endpoints, architecture |
| [API_TESTING.md](API_TESTING.md) | Complete API testing guide with curl commands for ALL 280+ endpoints |
| [ENTITY_DESIGN.md](ENTITY_DESIGN.md) | Entity relationship diagrams, UML, all 55+ entities |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Deployment guide, environment variables, CI/CD, Kubernetes |
| [ROADMAP.md](ROADMAP.md) | Feature status, roadmap, planned features |
| [docs/README-ARCHITECTURE.md](docs/README-ARCHITECTURE.md) | System architecture, module map, data flow diagrams |
| [docs/README-TECHNICAL.md](docs/README-TECHNICAL.md) | Deep-dive technical reference (auth, SAML, Kafka, Redis, OTel testing) |
| [docs/README-BUSINESS.md](docs/README-BUSINESS.md) | Business overview, value proposition, executive summary |
| [docs/SERVICES-CREDENTIALS.md](docs/SERVICES-CREDENTIALS.md) | All services inventory, API keys, credentials setup guide |

