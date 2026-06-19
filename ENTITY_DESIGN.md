# Entity Relationship & UML Design

This document provides the complete entity relationship diagram and UML class design for the Interview Platform Backend.

---

## 📐 Entity Relationship Diagram (ERD)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              USER DOMAIN                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐                │
│  │     User     │────▶│   UserRole   │◀────│     Role     │                │
│  │──────────────│     │──────────────│     │──────────────│                │
│  │ id: UUID     │     │ id: UUID     │     │ id: UUID     │                │
│  │ firstName    │     │ user_id (FK) │     │ name         │                │
│  │ lastName     │     │ role_id (FK) │     │ description  │                │
│  │ email (UQ)   │     │ assignedAt   │     │ createdAt    │                │
│  │ password     │     └──────────────┘     └──────┬───────┘                │
│  │ status       │                                  │                        │
│  │ authProvider  │     ┌──────────────┐     ┌──────┴───────┐               │
│  │ phoneNumber  │     │  Permission  │◀────│RolePermission│               │
│  │ createdAt    │     │──────────────│     │──────────────│               │
│  │ updatedAt    │     │ id: UUID     │     │ id: UUID     │               │
│  │ lastLoginAt  │     │ name         │     │ role_id (FK) │               │
│  └──────┬───────┘     │ description  │     │perm_id (FK)  │               │
│         │              └──────────────┘     │ createdAt    │               │
│         │                                    └──────────────┘               │
│         │                                                                    │
│         ▼                                                                    │
│  ┌──────────────┐                                                           │
│  │ UserProfile  │                                                           │
│  │──────────────│                                                           │
│  │ id: UUID     │                                                           │
│  │ user_id (FK) │                                                           │
│  │ bio          │                                                           │
│  │ designation  │                                                           │
│  │ company      │                                                           │
│  │ experience   │                                                           │
│  │ skills       │                                                           │
│  │ linkedinUrl  │                                                           │
│  │ githubUrl    │                                                           │
│  │ resumeUrl    │                                                           │
│  └──────────────┘                                                           │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                           INTERVIEW DOMAIN                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────┐        ┌─────────────────────────┐                  │
│  │     Interview      │───────▶│  InterviewInterviewer   │                  │
│  │────────────────────│        │─────────────────────────│                  │
│  │ id: UUID           │        │ id: UUID                │                  │
│  │ title              │        │ interview_id (FK)       │                  │
│  │ description        │        │ interviewer_id (FK→User)│                  │
│  │ candidate_id (FK)  │        │ isPrimaryInterviewer    │                  │
│  │ scheduledBy_id(FK) │        │ assignedAt              │                  │
│  │ startTime          │        └─────────────────────────┘                  │
│  │ endTime            │                                                      │
│  │ timeZone           │        ┌─────────────────────────┐                  │
│  │ status (ENUM)      │───────▶│   InterviewFeedBack     │                  │
│  │ type (ENUM)        │        │─────────────────────────│                  │
│  │ mode (ENUM)        │        │ id: UUID                │                  │
│  │ meetingLink        │        │ interview_id (FK)       │                  │
│  │ location           │        │ interviewer_id (FK→User)│                  │
│  │ cancelReason       │        │ rating (1-5)            │                  │
│  │ rescheduleReason   │        │ recommendation (ENUM)   │                  │
│  │ createdAt          │        │ strengths               │                  │
│  │ updatedAt          │        │ weaknesses              │                  │
│  └────────────────────┘        │ comments                │                  │
│                                 │ submittedAt             │                  │
│                                 └─────────────────────────┘                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                          TEMPLATE DOMAIN                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────┐        ┌─────────────────────────┐                  │
│  │ InterviewTemplate  │───────▶│    TemplateQuestion     │                  │
│  │────────────────────│        │─────────────────────────│                  │
│  │ id: UUID           │        │ id: UUID                │                  │
│  │ title              │        │ template_id (FK)        │                  │
│  │ description        │        │ question_id (FK)        │                  │
│  │ type (ENUM)        │        │ orderIndex              │                  │
│  │ mode (ENUM)        │        │ isMandatory             │                  │
│  │ durationMinutes    │        │ timeAllocationMinutes   │                  │
│  │ evaluationCriteria │        │ notes                   │                  │
│  │ instructions       │        └───────────┬─────────────┘                  │
│  │ tags               │                    │                                 │
│  │ isActive           │                    ▼                                 │
│  │ created_by (FK)    │        ┌─────────────────────────┐                  │
│  │ createdAt          │        │       Question          │                  │
│  │ updatedAt          │        │  (from Question Bank)   │                  │
│  └────────────────────┘        └─────────────────────────┘                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                        QUESTION BANK DOMAIN                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────┐        ┌─────────────────────────┐                  │
│  │  QuestionCategory  │◀───────│       Question          │                  │
│  │────────────────────│        │─────────────────────────│                  │
│  │ id: UUID           │        │ id: UUID                │                  │
│  │ name (UQ)          │        │ title                   │                  │
│  │ description        │        │ description             │                  │
│  └────────────────────┘        │ category_id (FK)        │                  │
│                                 │ difficulty (ENUM)       │                  │
│                                 │ type (ENUM)             │                  │
│                                 │ expectedDurationMinutes │                  │
│                                 │ sampleAnswer            │                  │
│                                 │ hints                   │                  │
│                                 │ tags                    │                  │
│                                 │ isActive                │                  │
│                                 │ created_by (FK→User)    │                  │
│                                 │ createdAt               │                  │
│                                 └─────────────────────────┘                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                     EXECUTION & COLLABORATION DOMAIN                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────┐        ┌─────────────────────────┐                  │
│  │   CodingSession    │        │      MeetingLink        │                  │
│  │────────────────────│        │─────────────────────────│                  │
│  │ id: UUID           │        │ id: UUID                │                  │
│  │ interview_id (FK)  │        │ interview_id (FK)       │                  │
│  │ language           │        │ provider (ENUM)         │                  │
│  │ codeContent        │        │ meetingUrl              │                  │
│  │ lastEditedBy (FK)  │        │ hostUrl                 │                  │
│  │ startedAt          │        │ meetingId               │                  │
│  │ endedAt            │        │ passcode                │                  │
│  └────────────────────┘        │ expiresAt               │                  │
│                                 │ createdAt               │                  │
│                                 └─────────────────────────┘                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                        CALENDAR DOMAIN                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────────────┐                                                │
│  │ InterviewerAvailability  │                                                │
│  │──────────────────────────│                                                │
│  │ id: UUID                 │                                                │
│  │ interviewer_id (FK→User) │                                                │
│  │ dayOfWeek                │                                                │
│  │ startTime                │                                                │
│  │ endTime                  │                                                │
│  │ timeZone                 │                                                │
│  │ isRecurring              │                                                │
│  │ specificDate             │                                                │
│  └──────────────────────────┘                                                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                     NOTIFICATION & AUDIT DOMAIN                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────┐        ┌─────────────────────────┐                  │
│  │   Notification     │        │       AuditLog          │                  │
│  │────────────────────│        │─────────────────────────│                  │
│  │ id: UUID           │        │ id: UUID                │                  │
│  │ user_id (FK)       │        │ entityType              │                  │
│  │ title              │        │ entityId                │                  │
│  │ message            │        │ action (ENUM)           │                  │
│  │ type               │        │ performedBy             │                  │
│  │ isRead             │        │ details                 │                  │
│  │ createdAt          │        │ timestamp               │                  │
│  └────────────────────┘        └─────────────────────────┘                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         PIPELINE DOMAIN                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────┐        ┌─────────────────────────┐                  │
│  │ InterviewPipeline  │───────▶│     PipelineStage       │                  │
│  │────────────────────│        │─────────────────────────│                  │
│  │ id: UUID           │        │ id: UUID                │                  │
│  │ name               │        │ pipeline_id (FK)        │                  │
│  │ description        │        │ name                    │                  │
│  │ department         │        │ description             │                  │
│  │ isActive           │        │ orderIndex              │                  │
│  │ created_by (FK)    │        │ interviewType (ENUM)    │                  │
│  │ createdAt          │        │ template_id (FK)        │                  │
│  │ updatedAt          │        │ durationMinutes         │                  │
│  └────────────────────┘        │ isOptional              │                  │
│                                 └─────────────────────────┘                  │
│                                                                              │
│  ┌────────────────────────┐    ┌─────────────────────────────┐              │
│  │  CandidatePipeline     │───▶│  CandidateStageProgress     │              │
│  │────────────────────────│    │─────────────────────────────│              │
│  │ id: UUID               │    │ id: UUID                    │              │
│  │ pipeline_id (FK)       │    │ candidatePipeline_id (FK)   │              │
│  │ candidate_id (FK→User) │    │ stage_id (FK→PipelineStage) │              │
│  │ currentStage_id (FK)   │    │ status (ENUM: StageStatus)  │              │
│  │ status (ENUM)          │    │ feedback                    │              │
│  │ notes                  │    │ startedAt                   │              │
│  │ startedAt              │    │ completedAt                 │              │
│  │ completedAt            │    └─────────────────────────────┘              │
│  │ updatedAt              │                                                  │
│  └────────────────────────┘                                                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                       SCORECARD DOMAIN                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────┐    ┌─────────────────────────────┐              │
│  │  EvaluationCriteria    │◀───│      ScorecardEntry         │              │
│  │────────────────────────│    │─────────────────────────────│              │
│  │ id: UUID               │    │ id: UUID                    │              │
│  │ name                   │    │ scorecard_id (FK)           │              │
│  │ description            │    │ criteria_id (FK)            │              │
│  │ interviewType (ENUM)   │    │ score: Integer              │              │
│  │ maxScore (default: 5)  │    │ comments                    │              │
│  │ weight (default: 1.0)  │    └──────────────┬──────────────┘              │
│  │ orderIndex             │                   │                              │
│  │ isActive               │                   │ belongs to                   │
│  │ created_by (FK→User)   │                   ▼                              │
│  │ createdAt              │    ┌─────────────────────────────┐              │
│  └────────────────────────┘    │   EvaluationScorecard       │              │
│                                 │─────────────────────────────│              │
│                                 │ id: UUID                    │              │
│                                 │ interview_id (FK)           │              │
│                                 │ interviewer_id (FK→User)    │              │
│                                 │ overallScore: Double        │              │
│                                 │ recommendation (ENUM)       │              │
│                                 │ overallComments             │              │
│                                 │ strengths                   │              │
│                                 │ weaknesses                  │              │
│                                 │ submittedAt                 │              │
│                                 │ updatedAt                   │              │
│                                 │ UQ(interview_id+interviewer)│              │
│                                 └─────────────────────────────┘              │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                       DOCUMENT DOMAIN (AWS S3)                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────┐                                              │
│  │        Document            │                                              │
│  │────────────────────────────│                                              │
│  │ id: UUID                   │                                              │
│  │ uploadedBy (FK→User)       │                                              │
│  │ interview_id (FK, nullable)│                                              │
│  │ candidate_id (FK, nullable)│                                              │
│  │ fileName                   │                                              │
│  │ s3Key                      │  ← S3 object key                             │
│  │ s3Url                      │  ← Full S3 URL                               │
│  │ contentType                │  ← MIME type                                 │
│  │ fileSize                   │  ← Bytes                                     │
│  │ type (ENUM: DocumentType)  │                                              │
│  │ description                │                                              │
│  │ isActive                   │  ← Soft delete                               │
│  │ uploadedAt                 │                                              │
│  └────────────────────────────┘                                              │
│                                                                              │
│  Note: Actual file content stored in AWS S3 bucket.                          │
│  PostgreSQL only stores metadata + S3 reference URL.                         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         SECURITY TOKENS                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────┐  ┌──────────────────────────┐                  │
│  │     RefreshToken        │  │  EmailVerificationToken  │                  │
│  │─────────────────────────│  │──────────────────────────│                  │
│  │ id: UUID                │  │ id: UUID                 │                  │
│  │ token                   │  │ token                    │                  │
│  │ user_id (FK)            │  │ user_id (FK)             │                  │
│  │ tokenFamily             │  │ expiryTime               │                  │
│  │ revoked                 │  └──────────────────────────┘                  │
│  │ expiryTime              │                                                 │
│  │ createdAt               │  ┌──────────────────────────┐                  │
│  └─────────────────────────┘  │   PasswordResetToken     │                  │
│                                │──────────────────────────│                  │
│                                │ id: UUID                 │                  │
│                                │ token                    │                  │
│                                │ user_id (FK)             │                  │
│                                │ expiryTime               │                  │
│                                └──────────────────────────┘                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 Enum Types

### UserStatus
```
ACTIVE | PENDING_VERIFICATION | SUSPENDED | INACTIVE | DELETED
```

### AuthProvider
```
LOCAL | GOOGLE | GITHUB | MICROSOFT
```

### InterviewStatus
```
DRAFT | SCHEDULED | RESCHEDULED | IN_PROGRESS | COMPLETED | CANCELLED | NO_SHOW
```

### InterviewType
```
SCREENING | TECHNICAL | HR | MANAGERIAL | FINAL
```

### InterviewMode
```
ONLINE | OFFLINE | PHONE
```

### FeedbackRecommendation
```
HIRE | HOLD | NO_HIRE | STRONG_NO_HIRE
```

### QuestionDifficulty
```
EASY | MEDIUM | HARD | EXPERT
```

### QuestionType
```
CODING | THEORETICAL | SYSTEM_DESIGN | BEHAVIORAL | MCQ
```

### MeetingProvider
```
ZOOM | GOOGLE_MEET | INTERNAL
```

### AuditAction
```
CREATE | UPDATE | DELETE | STATUS_CHANGE | ASSIGN_ROLE | REMOVE_ROLE |
PASSWORD_CHANGE | SUBMIT_FEEDBACK
```

### CandidatePipelineStatus
```
ACTIVE | HIRED | REJECTED | WITHDRAWN | ON_HOLD
```

### StageStatus
```
PENDING | IN_PROGRESS | COMPLETED | SKIPPED | REJECTED
```

### DocumentType
```
RESUME | JOB_DESCRIPTION | INTERVIEW_NOTES | ATTACHMENT | OFFER_LETTER | OTHER
```

---

## 🔗 Entity Relationships Summary

| Relationship | Type | Description |
|---|---|---|
| User → UserRole | 1:N | A user can have multiple roles |
| Role → UserRole | 1:N | A role can be assigned to multiple users |
| Role → RolePermission | 1:N | A role can have multiple permissions |
| Permission → RolePermission | 1:N | A permission can belong to multiple roles |
| User → UserProfile | 1:1 | Each user has one profile |
| User → RefreshToken | 1:N | User can have multiple refresh tokens |
| User → Interview (candidate) | 1:N | User as candidate in interviews |
| User → Interview (scheduledBy) | 1:N | User who scheduled |
| Interview → InterviewInterviewer | 1:N | Multiple interviewers per interview |
| User → InterviewInterviewer | 1:N | Interviewer assignments |
| Interview → InterviewFeedBack | 1:N | Multiple feedback per interview |
| Interview → CodingSession | 1:N | Multiple coding sessions per interview |
| Interview → MeetingLink | 1:1 | One meeting link per interview |
| InterviewTemplate → TemplateQuestion | 1:N | Template has multiple questions |
| Question → TemplateQuestion | 1:N | Question can be in multiple templates |
| QuestionCategory → Question | 1:N | Category has multiple questions |
| User → InterviewerAvailability | 1:N | Interviewer has multiple slots |
| User → Notification | 1:N | User receives notifications |
| User → EmailVerificationToken | 1:N | Verification tokens |
| User → PasswordResetToken | 1:N | Reset tokens |
| InterviewPipeline → PipelineStage | 1:N | Pipeline has ordered stages |
| PipelineStage → InterviewTemplate | N:1 | Stage can use a template |
| InterviewPipeline → CandidatePipeline | 1:N | Pipeline tracks multiple candidates |
| User → CandidatePipeline | 1:N | Candidate can be in multiple pipelines |
| CandidatePipeline → CandidateStageProgress | 1:N | Tracks progress per stage |
| EvaluationScorecard → Interview | N:1 | Multiple scorecards per interview |
| EvaluationScorecard → User (interviewer) | N:1 | Interviewer submits scorecard |
| EvaluationScorecard → ScorecardEntry | 1:N | Scorecard has multiple entries |
| ScorecardEntry → EvaluationCriteria | N:1 | Entry scores one criteria |
| User → Document (uploadedBy) | 1:N | User uploads documents |
| Interview → Document | 1:N | Interview can have attachments |
| User → Document (candidate) | 1:N | Candidate has resumes/docs |

---

## 🧩 UML Class Diagram (Simplified)

```
┌─────────────────────┐
│     <<Entity>>      │
│        User         │
├─────────────────────┤
│ - id: UUID          │
│ - firstName: String │
│ - lastName: String  │
│ - email: String     │
│ - password: String  │
│ - status: UserStatus│
│ - authProvider      │
├─────────────────────┤
│ + getUserRoles()    │
│ + getProfile()      │
└─────────┬───────────┘
          │ 1
          │
          │ *
┌─────────┴───────────┐         ┌─────────────────────┐
│     <<Entity>>      │    *    │     <<Entity>>      │
│      UserRole       │─────────│        Role         │
├─────────────────────┤    1    ├─────────────────────┤
│ - user: User        │         │ - id: UUID          │
│ - role: Role        │         │ - name: String      │
│ - assignedAt        │         │ - description       │
└─────────────────────┘         └─────────┬───────────┘
                                          │ 1
                                          │
                                          │ *
                                ┌─────────┴───────────┐         ┌──────────────────┐
                                │     <<Entity>>      │    *    │   <<Entity>>     │
                                │  RolePermission     │─────────│   Permission     │
                                ├─────────────────────┤    1    ├──────────────────┤
                                │ - role: Role        │         │ - id: UUID       │
                                │ - permission        │         │ - name: String   │
                                │ - createdAt         │         │ - description    │
                                └─────────────────────┘         └──────────────────┘

┌─────────────────────────┐
│       <<Entity>>        │
│       Interview         │
├─────────────────────────┤
│ - id: UUID              │
│ - title: String         │
│ - candidate: User       │
│ - scheduledBy: User     │
│ - startTime: Instant    │
│ - endTime: Instant      │
│ - status: InterviewStatus│
│ - type: InterviewType   │
│ - mode: InterviewMode   │
├─────────────────────────┤       ┌───────────────────────┐
│ + getInterviewers()     │──────▶│ InterviewInterviewer  │
│ + getFeedbackList()     │       │ - interviewer: User   │
└─────────────┬───────────┘       │ - isPrimary: boolean  │
              │                    └───────────────────────┘
              │
              │ *
┌─────────────┴───────────┐
│       <<Entity>>        │
│   InterviewFeedBack     │
├─────────────────────────┤
│ - interviewer: User     │
│ - rating: Integer       │
│ - recommendation        │
│ - strengths: String     │
│ - weaknesses: String    │
│ - comments: String      │
└─────────────────────────┘

┌─────────────────────────┐       ┌───────────────────────┐
│       <<Entity>>        │──────▶│   TemplateQuestion    │
│   InterviewTemplate     │       │ - question: Question  │
├─────────────────────────┤       │ - orderIndex: int     │
│ - title: String         │       │ - isMandatory: bool   │
│ - type: InterviewType   │       │ - timeAllocation: int │
│ - mode: InterviewMode   │       └───────────┬───────────┘
│ - durationMinutes: int  │                   │
│ - evaluationCriteria    │                   ▼
│ - instructions          │       ┌───────────────────────┐
│ - isActive: boolean     │       │     <<Entity>>        │
└─────────────────────────┘       │      Question         │
                                   ├───────────────────────┤
                                   │ - title: String       │
                                   │ - category            │
                                   │ - difficulty: ENUM    │
                                   │ - type: ENUM          │
                                   │ - sampleAnswer        │
                                   └───────────────────────┘
```

---

## 🔄 Service Layer Class Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                     <<Interface>>                                │
│                  AuthenticationService                           │
├────────────────────────────────────────────────────────────────┤
│ + register(RegisterRequest): AuthResponse                       │
│ + registerWithRole(RegisterRequest, String): AuthResponse       │
│ + adminCreateUser(AdminCreateUserRequest): AuthResponse         │
│ + login(LoginRequest): AuthResponse                             │
│ + logout(String): void                                          │
│ + refreshToken(String): AuthResponse                            │
│ + forgotPassword(String): void                                  │
│ + resetPassword(String, String): void                           │
│ + verifyEmail(String): void                                     │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                     <<Interface>>                                │
│                     InterviewService                             │
├────────────────────────────────────────────────────────────────┤
│ + createInterview(Request, UUID): InterviewResponse             │
│ + getInterview(UUID): InterviewResponse                         │
│ + updateInterview(UUID, Request): InterviewResponse             │
│ + cancelInterview(UUID, Request): InterviewResponse             │
│ + updateStatus(UUID, Status): InterviewResponse                 │
│ + deleteInterview(UUID): void                                   │
│ + addInterviewer(UUID, UUID, boolean): InterviewResponse        │
│ + removeInterviewer(UUID, UUID): InterviewResponse              │
│ + submitFeedback(UUID, UUID, Request): FeedbackResponse         │
│ + getInterviewFeedback(UUID): List<FeedbackResponse>            │
│ + getMyInterviewsAsCandidate(UUID): List<InterviewResponse>     │
│ + getMyInterviewsAsInterviewer(UUID): List<InterviewResponse>   │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                     <<Service>>                                  │
│               InterviewTemplateService                           │
├────────────────────────────────────────────────────────────────┤
│ + createTemplate(Request, UUID): TemplateResponse               │
│ + getTemplate(UUID): TemplateResponse                           │
│ + getAllTemplates(): List<TemplateResponse>                      │
│ + updateTemplate(UUID, Request): TemplateResponse               │
│ + deleteTemplate(UUID): void                                    │
│ + addQuestionToTemplate(UUID, Request): TemplateResponse        │
│ + removeQuestionFromTemplate(UUID, UUID): TemplateResponse      │
│ + createInterviewFromTemplate(Request, UUID): InterviewResponse │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                     <<Interface>>                                │
│                      UserService                                 │
├────────────────────────────────────────────────────────────────┤
│ + createUser(Request): UserResponse                             │
│ + getUsers(): List<UserResponse>                                │
│ + getCurrentUser(UUID): UserResponse                            │
│ + updateUser(UUID, Request): UserResponse                       │
│ + deleteUser(UUID): void                                        │
│ + getProfile(UUID): UserProfileResponse                         │
│ + updateProfile(UUID, Request): UserProfileResponse             │
│ + assignRoleToUser(UUID, UUID): RoleResponse                    │
│ + removeRoleFromUser(UUID, UUID): void                          │
│ + changePassword(UUID, Request): void                           │
│ + searchUsers(Request): PaginatedResponse<UserResponse>         │
│ + updateUserStatus(UUID, Status): UserResponse                  │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                     <<Interface>>                                │
│                    PipelineService                               │
├────────────────────────────────────────────────────────────────┤
│ + createPipeline(Request, UUID): PipelineResponse               │
│ + getPipeline(UUID): PipelineResponse                           │
│ + getAllPipelines(): List<PipelineResponse>                      │
│ + getPipelinesByDepartment(String): List<PipelineResponse>      │
│ + updatePipeline(UUID, Request): PipelineResponse               │
│ + deletePipeline(UUID): void                                    │
│ + addCandidateToPipeline(Request): CandidatePipelineResponse    │
│ + getCandidatePipeline(UUID): CandidatePipelineResponse         │
│ + getCandidatesInPipeline(UUID): List<CandidatePipelineResponse>│
│ + advanceToNextStage(UUID, String): CandidatePipelineResponse   │
│ + rejectCandidate(UUID, String): CandidatePipelineResponse      │
│ + updateStageProgress(UUID, UUID, Request): Response             │
│ + updateCandidatePipelineStatus(UUID, Status): Response          │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                     <<Interface>>                                │
│              EvaluationScorecardService                          │
├────────────────────────────────────────────────────────────────┤
│ + createCriteria(Request, UUID): CriteriaResponse               │
│ + getAllCriteria(): List<CriteriaResponse>                       │
│ + getCriteriaByType(InterviewType): List<CriteriaResponse>      │
│ + getCriteriaById(UUID): CriteriaResponse                       │
│ + updateCriteria(UUID, Request): CriteriaResponse               │
│ + deleteCriteria(UUID): void                                    │
│ + submitScorecard(Request, UUID): ScorecardResponse             │
│ + getScorecard(UUID): ScorecardResponse                         │
│ + getScorecardsByInterview(UUID): List<ScorecardResponse>       │
│ + getScorecardsByInterviewer(UUID): List<ScorecardResponse>     │
│ + getScorecardsByCandidate(UUID): List<ScorecardResponse>       │
│ + getCandidateSummary(UUID): CandidateScorecardSummary          │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                     <<Interface>>                                │
│                   DocumentService                                │
├────────────────────────────────────────────────────────────────┤
│ + uploadDocument(MultipartFile, Request): DocumentResponse      │
│ + uploadMultiple(List<MultipartFile>, Request): List<Response>  │
│ + getDocument(UUID): DocumentResponse                           │
│ + getDownloadUrl(UUID): PreSignedUrlResponse                    │
│ + getDocumentsByUser(UUID): List<DocumentResponse>              │
│ + getDocumentsByInterview(UUID): List<DocumentResponse>         │
│ + deleteDocument(UUID): void                                    │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                     <<Interface>>                                │
│                 BulkOperationsService                            │
├────────────────────────────────────────────────────────────────┤
│ + bulkScheduleInterviews(Request): BulkResult                   │
│ + bulkInviteCandidates(Request): BulkResult                     │
│ + bulkExportInterviews(Request): ExportResult                   │
│ + bulkExportFeedback(Request): ExportResult                     │
│ + bulkExportScorecards(Request): ExportResult                   │
│ + getBulkJobStatus(UUID): BulkJobStatus                         │
└────────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Database Schema (Flyway Migrations)

| Version | Description |
|---------|-------------|
| V1 | Create auth and RBAC tables (users, roles, permissions, user_roles, role_permissions, user_profiles, refresh_tokens) |
| V2 | Create interview tables (interviews, interview_interviewers, interview_feedback) |
| V3 | Add token_family to refresh_tokens (rotation support) |
| V4 | Add auth_provider, drop sessions table |
| V5 | Seed default RBAC data (ADMIN, RECRUITER, INTERVIEWER, CANDIDATE roles) |
| V6 | Create password_reset_tokens table |
| V7 | Create email_verification_tokens table |
| V8 | Alter refresh_tokens token column to TEXT |
| V9 | Phase 5 - Interview execution (availability, questions, categories, coding_sessions, meeting_links) |
| V10 | Create notifications table |
| V11 | Create interview_templates and template_questions tables |
| V12 | Create pipeline tables (interview_pipelines, pipeline_stages, candidate_pipelines, candidate_stage_progress) |
| V13 | Create evaluation scorecard tables (evaluation_criteria, evaluation_scorecards, scorecard_entries) |
| V14 | Create documents table (S3 metadata storage) |

---

## 🌊 Data Flow Diagrams

### Interview Scheduling Flow
```
Recruiter                    System                         Database
    │                          │                              │
    │  POST /interviews        │                              │
    │─────────────────────────▶│                              │
    │                          │  Validate candidate exists   │
    │                          │─────────────────────────────▶│
    │                          │◀─────────────────────────────│
    │                          │  Validate interviewers exist │
    │                          │─────────────────────────────▶│
    │                          │◀─────────────────────────────│
    │                          │  Save Interview + Assignments│
    │                          │─────────────────────────────▶│
    │                          │◀─────────────────────────────│
    │                          │                              │
    │                          │  Publish InterviewScheduledEvent
    │                          │─────────┐                    │
    │                          │         ▼                    │
    │                          │  ┌──────────────┐            │
    │                          │  │ Notification │            │
    │                          │  │   Engine     │            │
    │                          │  └──────┬───────┘            │
    │                          │         │ Email/SMS/InApp    │
    │                          │         ▼                    │
    │  Response (201)          │  Candidate + Interviewers    │
    │◀─────────────────────────│  get notified               │
    │                          │                              │
```

### Authentication Flow
```
Client                       Server                        Database
   │                           │                              │
   │  POST /auth/register      │                              │
   │──────────────────────────▶│                              │
   │                           │  Hash password               │
   │                           │  Save user (PENDING)         │
   │                           │─────────────────────────────▶│
   │                           │  Generate JWT + Refresh       │
   │                           │  Send verification email      │
   │  { accessToken, refresh } │                              │
   │◀──────────────────────────│                              │
   │                           │                              │
   │  GET /verify-email?token  │                              │
   │──────────────────────────▶│                              │
   │                           │  Validate token              │
   │                           │  Set status = ACTIVE         │
   │                           │─────────────────────────────▶│
   │  { verified: true }       │                              │
   │◀──────────────────────────│                              │
   │                           │                              │
   │  POST /auth/login         │                              │
   │──────────────────────────▶│                              │
   │                           │  Authenticate credentials    │
   │                           │  Check status = ACTIVE       │
   │                           │  Generate new JWT + Refresh  │
   │  { accessToken, refresh } │                              │
   │◀──────────────────────────│                              │
```

---

## 📈 Key Design Patterns Used

| Pattern | Usage |
|---------|-------|
| **Repository Pattern** | Spring Data JPA repositories for data access |
| **Service Layer** | Business logic separation from controllers |
| **DTO Pattern** | Request/Response objects separate from entities |
| **Builder Pattern** | Lombok @Builder for entity and DTO construction |
| **Strategy Pattern** | Meeting providers (Zoom, Google Meet, Internal) |
| **Observer/Event Pattern** | Spring ApplicationEvents for notifications |
| **Filter Chain** | JWT authentication filter, Rate limiting filter |
| **Token Rotation** | Refresh token rotation with family tracking |
| **Soft Delete** | Users marked as DELETED, not removed from DB |
| **Mapper Pattern** | Entity-to-DTO conversion via mapper classes |
| **Weighted Scoring** | Evaluation scorecards with configurable criteria weights |
| **State Machine** | Pipeline stage progression (PENDING → IN_PROGRESS → COMPLETED) |
| **Bulk/Batch Pattern** | Async bulk operations with job status tracking |
| **External Storage** | AWS S3 for files, PostgreSQL for metadata only |
| **Pre-signed URL** | Secure time-limited direct download URLs from S3 |

---

## Phase 7 — New Entities (V17 Migration)

### Multi-Tenant Domain

```
┌─────────────────────────┐       ┌──────────────────────────────┐
│     Organization        │       │     OrganizationMember       │
├─────────────────────────┤       ├──────────────────────────────┤
│ id: UUID (PK)           │       │ id: UUID (PK)                │
│ name: VARCHAR(200)      │◄──────│ organization_id: UUID (FK)   │
│ slug: VARCHAR(100) UQ   │       │ user_id: UUID (FK)           │
│ domain: VARCHAR(200)    │       │ role: OWNER|ADMIN|MEMBER|    │
│ logo_url: VARCHAR(500)  │       │       VIEWER                 │
│ plan: FREE|STARTER|     │       │ joined_at: TIMESTAMPTZ       │
│       PROFESSIONAL|     │       └──────────────────────────────┘
│       ENTERPRISE        │
│ max_users: INTEGER      │
│ is_active: BOOLEAN      │
│ created_at: TIMESTAMPTZ │
│ updated_at: TIMESTAMPTZ │
└─────────────────────────┘
```

### AI Suggestion Domain

```
┌─────────────────────────────┐
│       AiSuggestion          │
├─────────────────────────────┤
│ id: UUID (PK)               │
│ organization_id: UUID (FK)  │
│ user_id: UUID (FK)          │
│ type: QUESTION_SUGGESTION|  │
│       RESUME_PARSE|         │
│       INTERVIEW_SUMMARY|    │
│       CANDIDATE_ASSESSMENT  │
│ input_context: TEXT         │
│ output_content: TEXT        │
│ model: VARCHAR(100)         │
│ tokens_used: INTEGER        │
│ confidence_score: DOUBLE    │
│ status: GENERATED|ACCEPTED| │
│         REJECTED            │
│ interview_id: UUID (FK)     │
│ created_at: TIMESTAMPTZ     │
└─────────────────────────────┘
```

### Video Recording Domain

```
┌─────────────────────────────┐
│      VideoRecording         │
├─────────────────────────────┤
│ id: UUID (PK)               │
│ organization_id: UUID (FK)  │
│ interview_id: UUID (FK)     │
│ recorded_by: UUID (FK)      │
│ file_name: VARCHAR(300)     │
│ s3_key: VARCHAR(500)        │
│ s3_bucket: VARCHAR(200)     │
│ file_size_bytes: BIGINT     │
│ duration_seconds: INTEGER   │
│ mime_type: VARCHAR(100)     │
│ status: PROCESSING|READY|  │
│         FAILED|DELETED      │
│ thumbnail_url: VARCHAR(500) │
│ started_at: TIMESTAMPTZ     │
│ ended_at: TIMESTAMPTZ       │
│ created_at: TIMESTAMPTZ     │
└─────────────────────────────┘
```

### Whiteboard Domain

```
┌──────────────────────────┐       ┌──────────────────────────┐
│    WhiteboardSession     │       │    WhiteboardStroke      │
├──────────────────────────┤       ├──────────────────────────┤
│ id: UUID (PK)            │       │ id: UUID (PK)            │
│ organization_id: UUID    │       │ session_id: UUID (FK)    │
│ interview_id: UUID (FK)  │◄──────│ user_id: UUID (FK)       │
│ created_by: UUID (FK)    │       │ stroke_data: JSONB       │
│ title: VARCHAR(200)      │       │ tool: PEN|ERASER|LINE|   │
│ snapshot_data: TEXT      │       │       RECT|CIRCLE|TEXT|  │
│ thumbnail_url: VARCHAR   │       │       ARROW              │
│ is_active: BOOLEAN       │       │ color: VARCHAR(20)       │
│ created_at: TIMESTAMPTZ  │       │ stroke_width: DOUBLE     │
│ updated_at: TIMESTAMPTZ  │       │ sequence_number: INT     │
└──────────────────────────┘       │ created_at: TIMESTAMPTZ  │
                                   └──────────────────────────┘
```

### Webhook Domain

```
┌─────────────────────────────┐       ┌─────────────────────────────┐
│     WebhookEndpoint         │       │     WebhookDelivery         │
├─────────────────────────────┤       ├─────────────────────────────┤
│ id: UUID (PK)               │       │ id: UUID (PK)               │
│ organization_id: UUID (FK)  │       │ endpoint_id: UUID (FK)      │
│ user_id: UUID (FK)          │◄──────│ event_type: VARCHAR(100)    │
│ url: VARCHAR(500)           │       │ payload: JSONB              │
│ secret: VARCHAR(200)        │       │ response_status: INTEGER    │
│ description: VARCHAR(300)   │       │ response_body: TEXT         │
│ events: TEXT[]              │       │ attempt: INTEGER            │
│ is_active: BOOLEAN          │       │ max_attempts: INTEGER       │
│ created_at: TIMESTAMPTZ     │       │ status: PENDING|DELIVERED|  │
│ updated_at: TIMESTAMPTZ     │       │         FAILED|RETRYING     │
└─────────────────────────────┘       │ next_retry_at: TIMESTAMPTZ  │
                                      │ delivered_at: TIMESTAMPTZ   │
                                      │ created_at: TIMESTAMPTZ     │
                                      └─────────────────────────────┘
```

### Candidate Feedback (Reverse) Domain

```
┌──────────────────────────────────┐
│       CandidateFeedback          │
├──────────────────────────────────┤
│ id: UUID (PK)                    │
│ organization_id: UUID (FK)       │
│ interview_id: UUID (FK)          │
│ candidate_id: UUID (FK)          │
│ overall_rating: INT (1-5)        │
│ communication_rating: INT (1-5)  │
│ professionalism_rating: INT(1-5) │
│ technical_clarity_rating: INT    │
│ timeliness_rating: INT (1-5)     │
│ comments: TEXT                   │
│ would_recommend: BOOLEAN         │
│ is_anonymous: BOOLEAN            │
│ created_at: TIMESTAMPTZ          │
│ UNIQUE(interview_id,candidate_id)│
└──────────────────────────────────┘
```

### Activity Event Domain

```
┌─────────────────────────────┐
│       ActivityEvent         │
├─────────────────────────────┤
│ id: UUID (PK)               │
│ organization_id: UUID (FK)  │
│ actor_id: UUID (FK)         │
│ action: VARCHAR(100)        │
│ entity_type: VARCHAR(50)    │
│ entity_id: UUID             │
│ target_type: VARCHAR(50)    │
│ target_id: UUID             │
│ metadata: JSONB             │
│ created_at: TIMESTAMPTZ     │
└─────────────────────────────┘
```

### Export/Import Job Domain

```
┌─────────────────────────────┐
│     ExportImportJob         │
├─────────────────────────────┤
│ id: UUID (PK)               │
│ organization_id: UUID (FK)  │
│ user_id: UUID (FK)          │
│ type: EXPORT|IMPORT         │
│ format: CSV|EXCEL|JSON      │
│ status: PENDING|PROCESSING| │
│         COMPLETED|FAILED    │
│ entity_type: VARCHAR(50)    │
│ filters: JSONB              │
│ file_name: VARCHAR(300)     │
│ s3_key: VARCHAR(500)        │
│ total_records: INTEGER      │
│ processed_records: INTEGER  │
│ error_message: TEXT         │
│ started_at: TIMESTAMPTZ     │
│ completed_at: TIMESTAMPTZ   │
│ created_at: TIMESTAMPTZ     │
└─────────────────────────────┘
```

### New Enum Types (Phase 7)

| Enum | Values |
|------|--------|
| OrganizationPlan | FREE, STARTER, PROFESSIONAL, ENTERPRISE |
| OrganizationMemberRole | OWNER, ADMIN, MEMBER, VIEWER |
| AiSuggestionType | QUESTION_SUGGESTION, RESUME_PARSE, INTERVIEW_SUMMARY, CANDIDATE_ASSESSMENT |
| AiSuggestionStatus | GENERATED, ACCEPTED, REJECTED |
| RecordingStatus | PROCESSING, READY, FAILED, DELETED |
| StrokeTool | PEN, ERASER, LINE, RECTANGLE, CIRCLE, TEXT, ARROW |
| DeliveryStatus | PENDING, DELIVERED, FAILED, RETRYING |
| JobType | EXPORT, IMPORT |
| JobFormat | CSV, EXCEL, JSON |
| JobStatus | PENDING, PROCESSING, COMPLETED, FAILED |

### New Entity Relationships

| From | To | Type | FK Column |
|------|----|------|-----------|
| OrganizationMember | Organization | ManyToOne | organization_id |
| OrganizationMember | User | ManyToOne | user_id |
| AiSuggestion | User | ManyToOne | user_id |
| AiSuggestion | Interview | ManyToOne | interview_id |
| VideoRecording | Interview | ManyToOne | interview_id |
| VideoRecording | User | ManyToOne | recorded_by |
| WhiteboardSession | Interview | ManyToOne | interview_id |
| WhiteboardSession | User | ManyToOne | created_by |
| WhiteboardStroke | WhiteboardSession | ManyToOne | session_id |
| WhiteboardStroke | User | ManyToOne | user_id |
| WebhookEndpoint | User | ManyToOne | user_id |
| WebhookDelivery | WebhookEndpoint | ManyToOne | endpoint_id |
| CandidateFeedback | Interview | ManyToOne | interview_id |
| CandidateFeedback | User | ManyToOne | candidate_id |
| ActivityEvent | User | ManyToOne | actor_id |
| ExportImportJob | User | ManyToOne | user_id |

### Database Migration V17

File: `V17__ai_video_whiteboard_webhook_tenant_feedback_activity.sql`

Tables created:
- `organizations`
- `organization_members`
- `ai_suggestions`
- `video_recordings`
- `whiteboard_sessions`
- `whiteboard_strokes`
- `webhook_endpoints`
- `webhook_deliveries`
- `candidate_feedback`
- `activity_events`
- `export_import_jobs`

Indexes: 14 new indexes for performance on common query patterns.

---

## Phase 5-6 — Execution & Scheduling Entities

### Job Position Domain (V15)

```
┌─────────────────────────────────┐
│         JobPosition             │
├─────────────────────────────────┤
│ id: UUID (PK)                   │
│ title: VARCHAR(300)             │
│ department: VARCHAR(200)        │
│ location: VARCHAR(300)          │
│ employment_type: ENUM           │
│ experience_level: ENUM          │
│ status: ENUM                    │
│ description: TEXT               │
│ requirements: TEXT              │
│ responsibilities: TEXT          │
│ salary_min: DECIMAL(12,2)       │
│ salary_max: DECIMAL(12,2)       │
│ salary_currency: VARCHAR(10)    │
│ number_of_openings: INTEGER     │
│ number_hired: INTEGER           │
│ pipeline_id: UUID (FK)          │
│ created_by: UUID (FK→User)      │
│ hiring_manager_id: UUID (FK)    │
│ skills: TEXT                    │
│ posted_at: TIMESTAMPTZ          │
│ closed_at: TIMESTAMPTZ          │
│ deadline: TIMESTAMPTZ           │
│ created_at: TIMESTAMPTZ         │
│ updated_at: TIMESTAMPTZ         │
└─────────────────────────────────┘
```

### Scheduling Domain (V16)

```
┌──────────────────────────┐       ┌──────────────────────────────┐
│    AvailabilitySlot      │       │    InterviewReminder         │
├──────────────────────────┤       ├──────────────────────────────┤
│ id: UUID (PK)            │       │ id: UUID (PK)                │
│ user_id: UUID (FK→User)  │       │ interview_id: UUID (FK)      │
│ day_of_week: INTEGER     │       │ recipient_id: UUID (FK→User) │
│ start_time: TIME         │       │ type: BEFORE_24H|BEFORE_1H|  │
│ end_time: TIME           │       │       BEFORE_15MIN           │
│ time_zone: VARCHAR(50)   │       │ status: PENDING|SENT|FAILED| │
│ is_recurring: BOOLEAN    │       │         CANCELLED            │
│ specific_date: DATE      │       │ scheduled_for: TIMESTAMPTZ   │
│ created_at: TIMESTAMPTZ  │       │ sent_at: TIMESTAMPTZ         │
└──────────────────────────┘       │ created_at: TIMESTAMPTZ      │
                                   └──────────────────────────────┘

┌──────────────────────────────────┐
│     CandidatePreferredSlot       │
├──────────────────────────────────┤
│ id: UUID (PK)                    │
│ candidate_id: UUID (FK→User)     │
│ interview_id: UUID (FK, nullable)│
│ job_position_id: UUID (FK, null) │
│ preferred_date: DATE             │
│ start_time: TIME                 │
│ end_time: TIME                   │
│ time_zone: VARCHAR(50)           │
│ priority: INTEGER                │
│ status: PENDING|ACCEPTED|REJECTED│
│ notes: TEXT                      │
│ created_at: TIMESTAMPTZ          │
└──────────────────────────────────┘
```

### Team Domain (V16)

```
┌──────────────────────────┐       ┌──────────────────────────┐
│          Team            │       │       TeamMember         │
├──────────────────────────┤       ├──────────────────────────┤
│ id: UUID (PK)            │       │ id: UUID (PK)            │
│ name: VARCHAR(200)       │◀──────│ team_id: UUID (FK)       │
│ description: TEXT        │       │ user_id: UUID (FK→User)  │
│ department: VARCHAR(100) │       │ role: LEAD|MEMBER|       │
│ manager_id: UUID (FK)    │       │       OBSERVER           │
│ is_active: BOOLEAN       │       │ joined_at: TIMESTAMPTZ   │
│ created_at: TIMESTAMPTZ  │       └──────────────────────────┘
│ updated_at: TIMESTAMPTZ  │
└──────────────────────────┘
```

### Tag Domain (V16)

```
┌──────────────────────────┐       ┌──────────────────────────┐
│          Tag             │       │       EntityTag          │
├──────────────────────────┤       ├──────────────────────────┤
│ id: UUID (PK)            │       │ id: UUID (PK)            │
│ name: VARCHAR(100) UQ    │◀──────│ tag_id: UUID (FK)        │
│ color: VARCHAR(20)       │       │ entity_type: VARCHAR(50) │
│ category: VARCHAR(50)    │       │ entity_id: UUID          │
│ created_by: UUID (FK)    │       │ created_at: TIMESTAMPTZ  │
│ created_at: TIMESTAMPTZ  │       │ UQ(tag_id, entity_type,  │
└──────────────────────────┘       │    entity_id)            │
                                   └──────────────────────────┘
```

---

## Phase 8 — Security & Enterprise Entities

### Code Execution Domain (V19)

```
┌─────────────────────────────────┐
│        CodeExecution            │
├─────────────────────────────────┤
│ id: UUID (PK)                   │
│ coding_session_id: UUID (FK)    │
│ user_id: UUID (FK→User)         │
│ language: ENUM (SupportedLang)  │
│ source_code: TEXT               │
│ stdin: TEXT                     │
│ stdout: TEXT                    │
│ stderr: TEXT                    │
│ exit_code: INTEGER              │
│ status: ENUM (ExecutionStatus)  │
│ timeout_ms: INTEGER             │
│ execution_time_ms: LONG         │
│ memory_used_bytes: LONG         │
│ container_id: VARCHAR(100)      │
│ error_message: TEXT             │
│ created_at: TIMESTAMPTZ         │
│ completed_at: TIMESTAMPTZ       │
└─────────────────────────────────┘
```

### SSO/SAML Domain (V20)

```
┌─────────────────────────────────┐
│       SsoConfiguration          │
├─────────────────────────────────┤
│ id: UUID (PK)                   │
│ tenant_id: UUID (FK→Org)        │
│ provider_type: ENUM             │
│ registration_id: VARCHAR(100) UQ│
│ entity_id: VARCHAR(500)         │
│ sso_url: VARCHAR(500)           │
│ certificate: TEXT               │
│ metadata_url: VARCHAR(500)      │
│ sp_entity_id: VARCHAR(200)      │
│ acs_url: VARCHAR(500)           │
│ sign_requests: BOOLEAN          │
│ enabled: BOOLEAN                │
│ created_by: UUID (FK→User)      │
│ created_at: TIMESTAMPTZ         │
│ updated_at: TIMESTAMPTZ         │
└─────────────────────────────────┘
```

### Account Lockout Domain (V21)

```
┌──────────────────────────────┐  ┌──────────────────────────────┐
│      AccountLockout          │  │       LoginAttempt           │
├──────────────────────────────┤  ├──────────────────────────────┤
│ id: UUID (PK)                │  │ id: UUID (PK)                │
│ email: VARCHAR(255) UQ       │  │ email: VARCHAR(255)          │
│ failed_attempts: INTEGER     │  │ ip_address: VARCHAR(45)      │
│ locked: BOOLEAN              │  │ user_agent: TEXT             │
│ locked_at: TIMESTAMPTZ       │  │ success: BOOLEAN             │
│ lock_expires_at: TIMESTAMPTZ │  │ failure_reason: VARCHAR(200) │
│ last_failed_at: TIMESTAMPTZ  │  │ attempted_at: TIMESTAMPTZ    │
│ created_at: TIMESTAMPTZ      │  └──────────────────────────────┘
│ updated_at: TIMESTAMPTZ      │
└──────────────────────────────┘  ┌──────────────────────────────┐
                                  │       IpBlocklist            │
                                  ├──────────────────────────────┤
                                  │ id: UUID (PK)                │
                                  │ ip_address: VARCHAR(45) UQ   │
                                  │ reason: VARCHAR(500)         │
                                  │ blocked_at: TIMESTAMPTZ      │
                                  │ expires_at: TIMESTAMPTZ      │
                                  │ blocked_by: VARCHAR(255)     │
                                  └──────────────────────────────┘
```

### MFA Domain (V18)

```
┌─────────────────────────────────┐
│           UserMfa               │
├─────────────────────────────────┤
│ id: UUID (PK)                   │
│ user_id: UUID (FK→User) UQ     │
│ secret: VARCHAR(200)            │
│ enabled: BOOLEAN                │
│ verified: BOOLEAN               │
│ recovery_codes: TEXT            │
│ created_at: TIMESTAMPTZ         │
│ updated_at: TIMESTAMPTZ         │
└─────────────────────────────────┘
```

### API Key Domain (V18)

```
┌─────────────────────────────────┐
│           ApiKey                │
├─────────────────────────────────┤
│ id: UUID (PK)                   │
│ user_id: UUID (FK→User)         │
│ name: VARCHAR(200)              │
│ key_hash: VARCHAR(500) UQ       │
│ key_prefix: VARCHAR(20)         │
│ scopes: TEXT[]                  │
│ is_active: BOOLEAN              │
│ expires_at: TIMESTAMPTZ         │
│ last_used_at: TIMESTAMPTZ       │
│ created_at: TIMESTAMPTZ         │
└─────────────────────────────────┘
```

### GDPR Domain (V18)

```
┌──────────────────────────────┐  ┌──────────────────────────────┐
│       UserConsent            │  │    DataErasureRequest        │
├──────────────────────────────┤  ├──────────────────────────────┤
│ id: UUID (PK)                │  │ id: UUID (PK)                │
│ user_id: UUID (FK→User)      │  │ user_id: UUID (FK→User)      │
│ consent_type: VARCHAR(100)   │  │ reason: TEXT                 │
│ granted: BOOLEAN             │  │ status: PENDING|PROCESSING|  │
│ details: TEXT                │  │         COMPLETED|REJECTED   │
│ ip_address: VARCHAR(45)      │  │ requested_at: TIMESTAMPTZ    │
│ granted_at: TIMESTAMPTZ      │  │ processed_at: TIMESTAMPTZ    │
│ revoked_at: TIMESTAMPTZ      │  │ processed_by: UUID (FK)      │
│ created_at: TIMESTAMPTZ      │  │ notes: TEXT                  │
└──────────────────────────────┘  └──────────────────────────────┘
```

---

## Phase 9 — Job Board, Offers, Calendar Sync

### Job Board / Application Domain (V23)

```
┌─────────────────────────────────┐
│        JobApplication           │
├─────────────────────────────────┤
│ id: UUID (PK)                   │
│ job_position_id: UUID (FK)      │
│ candidate_id: UUID (FK→User)    │
│ status: ENUM (ApplicationStatus)│
│ source: ENUM (ApplicationSource)│
│ cover_letter: TEXT              │
│ resume_document_id: UUID (FK)   │
│ notes: TEXT                     │
│ applied_at: TIMESTAMPTZ         │
│ reviewed_at: TIMESTAMPTZ        │
│ updated_at: TIMESTAMPTZ         │
│ UQ(job_position_id, candidate)  │
└─────────────────────────────────┘
```

### Offer Letter Domain (V24)

```
┌─────────────────────────────────┐       ┌─────────────────────────────┐
│        OfferLetter              │       │      OfferApproval          │
├─────────────────────────────────┤       ├─────────────────────────────┤
│ id: UUID (PK)                   │       │ id: UUID (PK)               │
│ candidate_id: UUID (FK→User)    │       │ offer_id: UUID (FK)         │
│ job_position_id: UUID (FK)      │◀──────│ approver_id: UUID (FK→User) │
│ created_by: UUID (FK→User)      │       │ status: ENUM                │
│ salary: DECIMAL(12,2)           │       │ comments: TEXT              │
│ currency: VARCHAR(10)           │       │ order_index: INTEGER        │
│ start_date: DATE                │       │ decided_at: TIMESTAMPTZ     │
│ expiry_date: DATE               │       │ created_at: TIMESTAMPTZ     │
│ benefits: TEXT                  │       └─────────────────────────────┘
│ notes: TEXT                     │
│ status: ENUM (OfferStatus)      │
│ esignature_status: ENUM         │
│ esignature_provider: ENUM       │
│ esignature_request_id: VARCHAR  │
│ sent_at: TIMESTAMPTZ            │
│ viewed_at: TIMESTAMPTZ          │
│ responded_at: TIMESTAMPTZ       │
│ candidate_comments: TEXT        │
│ created_at: TIMESTAMPTZ         │
│ updated_at: TIMESTAMPTZ         │
└─────────────────────────────────┘
```

### Calendar Sync Domain (V25)

```
┌──────────────────────────────┐       ┌──────────────────────────────┐
│    CalendarConnection        │       │      CalendarEvent           │
├──────────────────────────────┤       ├──────────────────────────────┤
│ id: UUID (PK)                │       │ id: UUID (PK)                │
│ user_id: UUID (FK→User)      │       │ connection_id: UUID (FK)     │
│ provider: ENUM (CalProvider) │◀──────│ interview_id: UUID (FK)      │
│ access_token: TEXT           │       │ external_event_id: VARCHAR   │
│ refresh_token: TEXT          │       │ title: VARCHAR(300)          │
│ token_expires_at: TIMESTAMPTZ│       │ start_time: TIMESTAMPTZ      │
│ calendar_id: VARCHAR(200)    │       │ end_time: TIMESTAMPTZ        │
│ sync_direction: ENUM         │       │ sync_direction: ENUM         │
│ is_active: BOOLEAN           │       │ last_synced_at: TIMESTAMPTZ  │
│ last_synced_at: TIMESTAMPTZ  │       │ created_at: TIMESTAMPTZ      │
│ created_at: TIMESTAMPTZ      │       └──────────────────────────────┘
└──────────────────────────────┘
```

---

## Phase 10 — Workflow, Approvals, Referrals, DEI, Source Tracking

### Workflow Domain (V26+)

```
┌──────────────────────────────────┐       ┌──────────────────────────────┐
│        WorkflowRule              │       │     WorkflowExecution        │
├──────────────────────────────────┤       ├──────────────────────────────┤
│ id: UUID (PK)                    │       │ id: UUID (PK)                │
│ organization_id: UUID (FK)       │       │ rule_id: UUID (FK)           │
│ name: VARCHAR(200)               │◀──────│ trigger_entity_id: UUID      │
│ description: TEXT                │       │ trigger_entity_type: VARCHAR  │
│ trigger_event: ENUM              │       │ status: SUCCESS|FAILED|      │
│ condition_type: ENUM             │       │         SKIPPED              │
│ condition_value: VARCHAR(500)    │       │ result_message: TEXT         │
│ action_type: ENUM                │       │ executed_at: TIMESTAMPTZ     │
│ action_config: JSONB             │       │ created_at: TIMESTAMPTZ      │
│ is_active: BOOLEAN               │       └──────────────────────────────┘
│ priority: INTEGER                │
│ created_by: UUID (FK→User)       │
│ created_at: TIMESTAMPTZ          │
│ updated_at: TIMESTAMPTZ          │
└──────────────────────────────────┘
```

### Approval Domain (V26+)

```
┌───────────────────────────┐     ┌─────────────────────────┐
│     ApprovalChain         │     │     ApprovalStep        │
├───────────────────────────┤     ├─────────────────────────┤
│ id: UUID (PK)             │     │ id: UUID (PK)           │
│ organization_id: UUID(FK) │     │ chain_id: UUID (FK)     │
│ name: VARCHAR(200)        │◀────│ approver_id: UUID (FK)  │
│ entity_type: ENUM         │     │ order_index: INTEGER    │
│ mode: ENUM (ApprovalMode) │     │ created_at: TIMESTAMPTZ │
│ is_active: BOOLEAN        │     └─────────────────────────┘
│ created_by: UUID (FK)     │
│ created_at: TIMESTAMPTZ   │     ┌─────────────────────────────┐
└───────────────────────────┘     │     ApprovalRequest         │
                                  ├─────────────────────────────┤
┌─────────────────────────────┐   │ id: UUID (PK)               │
│     ApprovalDecision        │   │ chain_id: UUID (FK)         │
├─────────────────────────────┤   │ entity_type: ENUM           │
│ id: UUID (PK)               │   │ entity_id: UUID             │
│ request_id: UUID (FK)       │   │ requester_id: UUID (FK)     │
│ step_id: UUID (FK)          │   │ status: ENUM (ReqStatus)    │
│ approver_id: UUID (FK)      │   │ notes: TEXT                 │
│ approved: BOOLEAN           │   │ created_at: TIMESTAMPTZ     │
│ comments: TEXT              │   │ completed_at: TIMESTAMPTZ   │
│ decided_at: TIMESTAMPTZ     │   └─────────────────────────────┘
└─────────────────────────────┘
```

### Referral Domain (V26+)

```
┌─────────────────────────────────┐
│          Referral               │
├─────────────────────────────────┤
│ id: UUID (PK)                   │
│ referrer_id: UUID (FK→User)     │
│ candidate_email: VARCHAR(255)   │
│ candidate_first_name: VARCHAR   │
│ candidate_last_name: VARCHAR    │
│ job_position_id: UUID (FK)      │
│ relationship: VARCHAR(200)      │
│ notes: TEXT                     │
│ status: ENUM (ReferralStatus)   │
│ bonus_amount: DECIMAL(10,2)     │
│ bonus_paid: BOOLEAN             │
│ created_at: TIMESTAMPTZ         │
│ updated_at: TIMESTAMPTZ         │
└─────────────────────────────────┘
```

### DEI Domain (V26+)

```
┌─────────────────────────────────┐
│      DemographicProfile         │
├─────────────────────────────────┤
│ id: UUID (PK)                   │
│ user_id: UUID (FK→User) UQ     │
│ gender: ENUM (Gender)           │
│ ethnicity: ENUM (Ethnicity)     │
│ age_range: ENUM (AgeRange)      │
│ veteran_status: BOOLEAN         │
│ disability_status: BOOLEAN      │
│ opt_in: BOOLEAN                 │
│ created_at: TIMESTAMPTZ         │
│ updated_at: TIMESTAMPTZ         │
└─────────────────────────────────┘
```

### Source Tracking Domain (V26+)

```
┌─────────────────────────────────┐
│       CandidateSource           │
├─────────────────────────────────┤
│ id: UUID (PK)                   │
│ candidate_id: UUID (FK→User)    │
│ source_type: ENUM (SourceType)  │
│ source_name: VARCHAR(200)       │
│ campaign: VARCHAR(200)          │
│ cost: DECIMAL(10,2)             │
│ referrer_id: UUID (FK, nullable)│
│ created_at: TIMESTAMPTZ         │
└─────────────────────────────────┘
```

---

## Additional Enum Types (Phase 8-10)

| Enum | Values |
|------|--------|
| JobPositionStatus | DRAFT, OPEN, ON_HOLD, CLOSED, FILLED, CANCELLED |
| EmploymentType | FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP, FREELANCE, TEMPORARY |
| ExperienceLevel | ENTRY, JUNIOR, MID, SENIOR, LEAD, PRINCIPAL, EXECUTIVE |
| ExecutionStatus | PENDING, RUNNING, COMPLETED, TIMEOUT, ERROR |
| SupportedLanguage | JAVA, PYTHON, JAVASCRIPT, TYPESCRIPT, CPP, C, GO, RUST, RUBY, PHP |
| SsoProviderType | OKTA, ONELOGIN, AZURE_AD, GENERIC |
| ReminderType | BEFORE_24H, BEFORE_1H, BEFORE_15MIN |
| ReminderStatus | PENDING, SENT, FAILED, CANCELLED |
| TeamMemberRole | LEAD, MEMBER, OBSERVER |
| TagCategory | INTERVIEW, CANDIDATE, QUESTION, JOB_POSITION, GENERAL |
| ApplicationStatus | SUBMITTED, UNDER_REVIEW, SHORTLISTED, INTERVIEW_SCHEDULED, OFFERED, HIRED, REJECTED, WITHDRAWN |
| ApplicationSource | CAREER_PAGE, LINKEDIN, INDEED, REFERRAL, AGENCY, DIRECT, OTHER |
| OfferStatus | DRAFT, PENDING_APPROVAL, APPROVED, SENT, VIEWED, ACCEPTED, DECLINED, REVOKED, EXPIRED |
| ApprovalStatus | PENDING, APPROVED, REJECTED |
| ESignatureStatus | NOT_STARTED, SENT, VIEWED, SIGNED, DECLINED, EXPIRED |
| ESignatureProvider | DOCUSIGN, HELLOSIGN, NONE |
| CalendarProvider | GOOGLE, MICROSOFT |
| SyncDirection | PUSH, PULL, BIDIRECTIONAL |
| TriggerEvent | INTERVIEW_COMPLETED, FEEDBACK_SUBMITTED, SCORE_ABOVE_THRESHOLD, CANDIDATE_APPLIED, STAGE_COMPLETED |
| ConditionType | SCORE_THRESHOLD, STATUS_EQUALS, FIELD_CONTAINS, ALWAYS |
| ActionType | ADVANCE_PIPELINE, SEND_NOTIFICATION, UPDATE_STATUS, CREATE_TASK, WEBHOOK |
| ApprovalMode | SEQUENTIAL, PARALLEL, ANY_ONE |
| ApprovalEntityType | OFFER, JOB_REQUISITION, JOB_POSTING, BUDGET |
| ApprovalRequestStatus | PENDING, APPROVED, REJECTED, CANCELLED |
| ReferralStatus | SUBMITTED, CONTACTED, APPLIED, INTERVIEWING, HIRED, REJECTED, EXPIRED |
| Gender | MALE, FEMALE, NON_BINARY, OTHER, PREFER_NOT_TO_SAY |
| Ethnicity | WHITE, BLACK, HISPANIC, ASIAN, NATIVE_AMERICAN, PACIFIC_ISLANDER, MIXED, OTHER, PREFER_NOT_TO_SAY |
| AgeRange | RANGE_18_24, RANGE_25_34, RANGE_35_44, RANGE_45_54, RANGE_55_64, RANGE_65_PLUS, PREFER_NOT_TO_SAY |
| SourceType | LINKEDIN, INDEED, GLASSDOOR, REFERRAL, CAREER_PAGE, AGENCY, DIRECT, SOCIAL_MEDIA, JOB_FAIR, OTHER |

---

## Complete Entity Relationships (All Phases)

| From | To | Type | FK Column |
|------|----|------|-----------|
| User → UserRole | 1:N | user_id |
| Role → UserRole | 1:N | role_id |
| Role → RolePermission | 1:N | role_id |
| Permission → RolePermission | 1:N | permission_id |
| User → UserProfile | 1:1 | user_id |
| User → RefreshToken | 1:N | user_id |
| User → Interview (candidate) | 1:N | candidate_id |
| User → Interview (scheduledBy) | 1:N | scheduled_by_id |
| Interview → InterviewInterviewer | 1:N | interview_id |
| Interview → InterviewFeedBack | 1:N | interview_id |
| Interview → CodingSession | 1:N | interview_id |
| Interview → MeetingLink | 1:1 | interview_id |
| Interview → JobPosition | N:1 | job_position_id |
| InterviewTemplate → TemplateQuestion | 1:N | template_id |
| Question → TemplateQuestion | 1:N | question_id |
| QuestionCategory → Question | 1:N | category_id |
| User → InterviewerAvailability | 1:N | interviewer_id |
| User → Notification | 1:N | user_id |
| InterviewPipeline → PipelineStage | 1:N | pipeline_id |
| InterviewPipeline → CandidatePipeline | 1:N | pipeline_id |
| User → CandidatePipeline | 1:N | candidate_id |
| CandidatePipeline → CandidateStageProgress | 1:N | candidate_pipeline_id |
| EvaluationScorecard → ScorecardEntry | 1:N | scorecard_id |
| ScorecardEntry → EvaluationCriteria | N:1 | criteria_id |
| User → Document | 1:N | uploaded_by |
| User → AvailabilitySlot | 1:N | user_id |
| Interview → InterviewReminder | 1:N | interview_id |
| User → CandidatePreferredSlot | 1:N | candidate_id |
| Team → TeamMember | 1:N | team_id |
| TeamMember → User | N:1 | user_id |
| Tag → EntityTag | 1:N | tag_id |
| CodingSession → CodeExecution | 1:N | coding_session_id |
| Organization → SsoConfiguration | 1:N | tenant_id |
| User → AccountLockout | 1:1 | email |
| User → UserMfa | 1:1 | user_id |
| User → ApiKey | 1:N | user_id |
| User → UserConsent | 1:N | user_id |
| User → DataErasureRequest | 1:N | user_id |
| JobPosition → JobApplication | 1:N | job_position_id |
| User → JobApplication | 1:N | candidate_id |
| OfferLetter → OfferApproval | 1:N | offer_id |
| User → CalendarConnection | 1:N | user_id |
| CalendarConnection → CalendarEvent | 1:N | connection_id |
| OrganizationMember → Organization | N:1 | organization_id |
| OrganizationMember → User | N:1 | user_id |
| AiSuggestion → User | N:1 | user_id |
| AiSuggestion → Interview | N:1 | interview_id |
| VideoRecording → Interview | N:1 | interview_id |
| VideoRecording → User | N:1 | recorded_by |
| WhiteboardSession → Interview | N:1 | interview_id |
| WhiteboardStroke → WhiteboardSession | N:1 | session_id |
| WebhookEndpoint → User | N:1 | user_id |
| WebhookDelivery → WebhookEndpoint | N:1 | endpoint_id |
| CandidateFeedback → Interview | N:1 | interview_id |
| ActivityEvent → User | N:1 | actor_id |
| ExportImportJob → User | N:1 | user_id |
| WorkflowRule → WorkflowExecution | 1:N | rule_id |
| ApprovalChain → ApprovalStep | 1:N | chain_id |
| ApprovalChain → ApprovalRequest | 1:N | chain_id |
| ApprovalRequest → ApprovalDecision | 1:N | request_id |
| Referral → User (referrer) | N:1 | referrer_id |
| Referral → JobPosition | N:1 | job_position_id |
| DemographicProfile → User | 1:1 | user_id |
| CandidateSource → User | N:1 | candidate_id |

---

## Complete Database Migration History

| Version | Description |
|---------|-------------|
| V1 | Auth & RBAC tables (users, roles, permissions, user_roles, role_permissions, user_profiles, refresh_tokens) |
| V2 | Interview tables (interviews, interview_interviewers, interview_feedback) |
| V3 | Token family for refresh token rotation (replay detection) |
| V4 | Auth provider enum, drop sessions table |
| V5 | Seed default RBAC data (ADMIN, RECRUITER, INTERVIEWER, CANDIDATE) |
| V6 | Password reset tokens |
| V7 | Email verification tokens |
| V8 | Alter refresh_tokens.token to TEXT |
| V9 | Coding sessions, question bank, categories, meeting links |
| V10 | Notifications table |
| V11 | Interview templates & template_questions |
| V12 | Evaluation scorecards, criteria, entries |
| V13 | Hiring pipelines, stages, candidate progress |
| V14 | Documents table (S3 metadata storage) |
| V15 | Job positions, interview linkage |
| V16 | Scheduling, reminders, self-service, teams, tags |
| V17 | AI suggestions, video recordings, whiteboard, webhooks, tenant, candidate feedback, activity events, export/import |
| V18 | MFA (user_mfa), GDPR (user_consents, data_erasure_requests), API keys |
| V19 | Code executions (sandboxed Docker execution results) |
| V20 | SSO/SAML configurations |
| V21 | Account lockout, login attempts, IP blocklist |
| V22 | Column alterations for field-level encryption (AES-256-GCM) |
| V23 | Job applications (candidate portal/job board) |
| V24 | Offer letters & offer approvals |
| V25 | Calendar sync (connections + events) |
| V26 | Workflow rules & executions |
| V27 | Approval chains, steps, requests, decisions |
| V28 | Referrals |
| V29 | DEI demographic profiles |
| V30 | Candidate source tracking |

