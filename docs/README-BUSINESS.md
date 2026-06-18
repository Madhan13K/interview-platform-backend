# Interview Platform - Product Overview

## What Is This?

The Interview Platform is a complete **Applicant Tracking System (ATS)** and **Interview Management Solution** that handles the entire hiring process from job posting to offer acceptance. It replaces the need for multiple disconnected tools by providing a single, integrated platform for recruiters, hiring managers, interviewers, and candidates.

---

## Who Is It For?

| User | What They Do |
|------|-------------|
| **Recruiters** | Post jobs, manage applications, schedule interviews, send offers |
| **Hiring Managers** | Define job requirements, approve offers, track pipeline progress |
| **Interviewers** | Conduct interviews, submit feedback, use code editors and whiteboards |
| **Candidates** | Browse jobs, apply, track application status, attend interviews |
| **HR/Admin** | Configure system settings, manage users, view analytics and compliance |

---

## Key Capabilities

### 1. Job Board & Applications

- Public-facing job listings website (no login required to browse)
- Candidates apply with cover letter and resume
- Application status tracking (Applied → Under Review → Shortlisted → Interview → Offer → Hired)
- Duplicate application prevention
- Source tracking (LinkedIn, referral, job board, etc.)

### 2. Interview Management

- Schedule interviews with one click
- Multiple interview types: Screening, Technical, HR, Managerial, Final
- Multiple modes: Online (video), In-person, Phone
- Automatic meeting link generation (Zoom, Google Meet)
- Interviewer assignment with primary/secondary roles
- Interview templates (reusable question sets for consistency)

### 3. Real-Time Collaboration

- **Live Code Editor**: Candidates write code while interviewers observe in real-time
- **Code Execution**: Run candidate code in 10 programming languages (Java, Python, JavaScript, etc.) with instant results
- **Whiteboard**: Shared drawing tool for system design and diagramming
- **Video Recording**: Record interviews for later review

### 4. Evaluation & Scoring

- Structured scorecards with weighted criteria
- Standardized rating scales (1-5) across interviewers
- Recommendation options: Hire, Hold, No Hire, Strong No Hire
- Side-by-side comparison of multiple interviewer assessments
- Candidate experience feedback (reverse reviews)

### 5. Hiring Pipeline

- Visual pipeline stages (Screening → Technical → HR → Final → Offer)
- Drag-and-drop candidate progression
- Automated advancement based on rules (e.g., "score above 4 = advance automatically")
- Rejection with reasons, hold status, withdrawal tracking
- Pipeline analytics (conversion rates, bottleneck identification)

### 6. Offer Management

- Create offer letters with salary, start date, and benefits
- Multi-level approval workflows (e.g., Hiring Manager → VP → HR Director)
- Electronic signature integration (DocuSign, HelloSign)
- Offer tracking: Sent → Viewed → Accepted/Declined
- Offer expiration with automatic reminders

### 7. Automation & Workflows

- **Rule-based automation**: "If average score > 4, auto-advance to next stage"
- **Configurable approval chains**: Sequential, parallel, or any-one-of modes
- **Automated notifications**: Email, SMS, Slack, Microsoft Teams
- **Interview reminders**: 24 hours, 1 hour, and 15 minutes before
- **Bulk operations**: Schedule 100 interviews at once, bulk invite candidates

### 8. Calendar & Scheduling

- Interviewer availability management (recurring + specific dates)
- Smart scheduling suggestions (finds overlapping free time)
- Candidate preferred time slot submission
- **Google Calendar sync** (bidirectional - changes reflect both ways)
- **Outlook/Microsoft 365 sync** (bidirectional)
- Conflict detection and prevention

### 9. Analytics & Reporting

- **Recruiter Dashboard**: Open positions, pending interviews, pipeline health
- **Interviewer Dashboard**: Upcoming interviews, feedback pending, workload
- **Candidate Dashboard**: Application status, upcoming interviews
- **Diversity Analytics** (DEI): Opt-in demographic tracking, funnel analysis by diversity categories
- **Source Effectiveness**: Which job boards/sources produce the best hires? ROI by channel
- **Time-to-Hire Metrics**: Average days from application to offer
- **PDF Reports**: Downloadable analytics reports for stakeholders
- **Employee Referral Analytics**: Referral conversion rates, bonus tracking, leaderboard

### 10. Compliance & Security

- **GDPR Compliant**: Consent tracking, data export, right-to-erasure
- **SOC2 Ready**: Encrypted data at rest, audit trails, access controls
- **Enterprise SSO**: Okta, OneLogin, Azure AD (SAML 2.0)
- **Multi-Factor Authentication**: TOTP-based (Google Authenticator, Authy)
- **Account Protection**: Auto-lock after failed login attempts, IP blocking
- **Audit Trail**: Every action logged with who, what, when, and from where
- **Role-Based Access**: Fine-grained permissions (Admin, Recruiter, Interviewer, Candidate)

### 11. Integrations

| Category | Integrations |
|----------|-------------|
| **Authentication** | Google, GitHub, Microsoft (single sign-on) |
| **Enterprise SSO** | Okta, OneLogin, Azure AD (SAML) |
| **Video Conferencing** | Zoom, Google Meet |
| **Calendar** | Google Calendar, Outlook 365 |
| **Messaging** | Slack, Microsoft Teams |
| **E-Signature** | DocuSign, HelloSign/Dropbox Sign |
| **Email** | Any SMTP (Gmail, SendGrid, Mailgun) |
| **SMS** | Twilio |
| **File Storage** | AWS S3 |
| **Webhooks** | Any external system (signed HTTP callbacks) |

### 12. Employee Referral Program

- Employees submit referrals with unique tracking codes
- Track referral status through the entire hiring funnel
- Bonus eligibility and payment tracking
- Referral leaderboard (gamification)
- Analytics: which employees refer the best candidates?

---

## How It Works (Simplified)

```
        JOB POSTED                    CANDIDATE APPLIES
            │                               │
            ▼                               ▼
    ┌───────────────┐              ┌────────────────┐
    │  Job Board    │◄─────────────│  Application   │
    │  (public)     │              │  Submitted     │
    └───────┬───────┘              └────────┬───────┘
            │                               │
            └───────────────┬───────────────┘
                            ▼
                   ┌────────────────┐
                   │  SCREENING     │  Recruiter reviews application
                   └────────┬───────┘
                            │
                            ▼
                   ┌────────────────┐
                   │  INTERVIEW     │  Schedule, conduct, evaluate
                   │  (1-4 rounds)  │  Code editor + whiteboard
                   └────────┬───────┘
                            │
                            ▼
                   ┌────────────────┐
                   │  EVALUATION    │  Scorecards + team discussion
                   └────────┬───────┘
                            │
                   ┌────────┴────────┐
                   ▼                 ▼
          ┌──────────────┐  ┌──────────────┐
          │   REJECT     │  │    OFFER     │  Approval workflow
          │   (notify)   │  │   (e-sign)   │  + e-signature
          └──────────────┘  └──────┬───────┘
                                   │
                                   ▼
                          ┌──────────────┐
                          │    HIRED     │  Onboarding begins
                          └──────────────┘
```

---

## Business Value

| Benefit | Impact |
|---------|--------|
| **Reduce time-to-hire** | Automated scheduling, instant feedback collection, workflow rules |
| **Improve candidate experience** | Self-service portal, real-time status, modern interview tools |
| **Ensure consistency** | Templates, scorecards, standardized evaluation criteria |
| **Make data-driven decisions** | Analytics dashboards, source ROI, diversity metrics |
| **Reduce bias** | Structured interviews, standardized scoring, blind screening support |
| **Compliance** | GDPR, SOC2, audit trails, encrypted data, SSO |
| **Scale hiring** | Bulk operations, automation rules, multi-tenant support |
| **Cut tool costs** | Replaces: ATS + video platform + code assessment + scheduling + e-signature |

---

## Deployment Options

| Option | Best For | Requirements |
|--------|----------|-------------|
| **Docker (self-hosted)** | Small-medium companies | Any server with Docker |
| **Kubernetes** | Enterprise scale | K8s cluster |
| **Cloud (AWS/GCP/Azure)** | Managed infrastructure | Cloud account |

The platform runs on standard infrastructure:
- **Database**: PostgreSQL (any managed offering: RDS, Cloud SQL, Azure DB)
- **Cache**: Redis (ElastiCache, Memorystore, Azure Cache)
- **File Storage**: AWS S3 (or compatible: GCS, Azure Blob)
- **Email**: Any SMTP provider

---

## Pricing & Licensing

This is an **open-source** platform. No licensing fees. You pay only for:
- Infrastructure hosting (servers, database, storage)
- External service subscriptions you choose to enable (Zoom, Twilio, DocuSign, etc.)
- Optional: Support and customization services

---

## Competitive Comparison

| Feature | This Platform | Greenhouse | Lever | BambooHR |
|---------|:---:|:---:|:---:|:---:|
| Job Board | Yes | Yes | Yes | Yes |
| Live Code Editor | Yes | No | No | No |
| Code Execution (sandbox) | Yes | No | No | No |
| Whiteboard | Yes | No | No | No |
| Workflow Automation | Yes | Limited | Yes | No |
| E-Signature | Yes | Add-on | No | No |
| Calendar Sync | Yes | Yes | Yes | Yes |
| SAML SSO | Yes | Enterprise | Enterprise | Enterprise |
| Diversity Analytics | Yes | Add-on | Yes | No |
| Source ROI | Yes | Yes | Yes | No |
| Referral Program | Yes | Yes | Yes | No |
| Self-hosted option | Yes | No | No | No |
| Open source | Yes | No | No | No |
| Per-seat pricing | None | $$$  | $$$  | $$  |

---

## Getting Started

For technical setup instructions, see:
- [Deployment Guide](DEPLOYMENT.md) - How to install and run
- [Architecture](docs/README-ARCHITECTURE.md) - System design
- [Services & Credentials](docs/SERVICES-CREDENTIALS.md) - What accounts you need

For a product demo, start the platform locally:
```
docker compose up --build
```
Then open: http://localhost:8080/swagger-ui.html

---

## Feature Modules Summary

| Module | Business Purpose |
|--------|-----------------|
| Job Board | Attract candidates with public listings |
| Applications | Collect and organize candidate submissions |
| Interviews | Schedule, conduct, and record interviews |
| Code Assessment | Test technical skills in real-time |
| Scorecards | Standardize evaluation across interviewers |
| Pipeline | Visualize and manage candidate flow through stages |
| Offers | Create, approve, and send offer letters |
| Calendar Sync | Keep everyone's calendar up-to-date |
| Notifications | Keep all parties informed (email, SMS, Slack, Teams) |
| Referrals | Incentivize employee referrals |
| Analytics | Measure hiring effectiveness |
| Diversity (DEI) | Track and improve hiring diversity |
| Automation | Reduce manual work with smart rules |
| Compliance | Meet GDPR/SOC2 requirements |
| Security | Protect data and control access |

---

## Contact & Support

- **Repository**: https://github.com/Madhan13K/interview-platform-backend
- **API Documentation**: http://localhost:8080/swagger-ui.html (when running)
- **Issues**: https://github.com/Madhan13K/interview-platform-backend/issues
