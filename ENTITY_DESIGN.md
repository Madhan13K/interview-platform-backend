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

