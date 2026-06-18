# CRUD Scenario Checklist

## Users (`/api/v1/users`)

- [x] `POST /api/v1/users` success (default role assignment + profile creation)
- [x] `POST /api/v1/users` duplicate email -> `409`
- [x] `GET /api/v1/users` returns list
- [x] `GET /api/v1/users/{userId}` not found -> `404`
- [x] `PUT /api/v1/users/{userId}` partial update fields
- [x] `PUT /api/v1/users/{userId}` invalid status payload -> `400`
- [x] `DELETE /api/v1/users/{userId}` success -> `204`
- [x] `DELETE /api/v1/users/{userId}` not found -> `404`

## Profiles (`/api/v1/users/{userId}/profile`)

- [x] `GET /profile` existing profile -> `200`
- [x] `GET /profile` missing profile -> `404` (or define auto-create behavior)
- [x] `PUT /profile` create when missing
- [x] `PUT /profile` update existing profile

## User Roles (`/api/v1/users/{userId}/roles`)

- [x] `POST /roles` assign role success
- [x] `POST /roles` duplicate assignment -> `409`
- [x] `POST /roles` user not found -> `404`
- [x] `POST /roles` role not found -> `404`
- [x] `GET /roles` returns user roles list
- [x] `DELETE /roles/{roleId}` assignment removed -> `204`
- [x] `DELETE /roles/{roleId}` assignment not found -> `404`

## Permissions (`/api/v1/permissions`)

- [x] `POST` success
- [x] `POST` invalid request body -> `400`
- [x] `POST` duplicate permission -> `409`
- [x] `GET` all permissions -> `200`
- [x] `GET /{permissionId}` not found -> `404`
- [x] `PUT /{permissionId}` success -> `200`
- [x] `DELETE /{permissionId}` success -> `204`
- [x] `PUT /{permissionId}` duplicate name -> `409`
- [x] `DELETE /{permissionId}` not found -> `404`

## Roles (`/api/v1/roles`)

- [x] `POST` success
- [x] `POST` duplicate role -> `409`
- [x] `GET` all roles -> `200`
- [x] `GET /{roleId}` not found -> `404`
- [x] `PUT /{roleId}` success
- [x] `PUT /{roleId}` duplicate role name -> `409`
- [x] `DELETE /{roleId}` success -> `204`
- [x] `DELETE /{roleId}` not found -> `404`

## Role Permissions (`/api/v1/roles/{roleId}/permissions`)

- [x] `POST` assign permission success
- [x] `POST` duplicate assignment -> `409`
- [x] `POST` role not found -> `404`
- [x] `POST` permission not found -> `404`
- [x] `GET` list by role -> `200`
- [x] `DELETE /api/v1/roles/permissions/{rolePermissionId}` success -> `204`
- [x] `DELETE` mapping not found -> `404`

## Auth / Session

- [x] Register success — returns access + refresh tokens
- [x] Register duplicate email -> `409`
- [x] Register invalid body (missing fields / short password) -> `400`
- [x] Register as interviewer success
- [x] Login success — returns access + refresh tokens
- [x] Login invalid credentials -> `401`
- [x] Login unverified/suspended account -> `401`
- [x] Login invalid body (missing email / bad format) -> `400`
- [x] Refresh valid token — returns new tokens
- [x] Refresh invalid/expired token -> `401`
- [x] Refresh token reuse detection -> `401` (family revoked)
- [x] Refresh token not found in DB -> `404`
- [x] Logout success -> `204`
- [x] Logout invalid token -> `404`
- [x] Forgot password -> `204`
- [x] Reset password success -> `204`
- [x] Reset password invalid/expired token -> `400`
- [x] Email verification success -> `200`
- [x] Email verification invalid/expired/used token -> `400`
- [x] Resend verification email -> `204`

## Interviews (`/api/v1/interviews`)

### CRUD
- [x] `POST` create interview success -> `200`
- [x] `POST` candidate not found -> `404`
- [x] `POST` end time before start -> `400`
- [x] `GET /{interviewId}` success -> `200`
- [x] `GET /{interviewId}` not found -> `404`
- [x] `GET` all interviews -> `200`
- [x] `GET /paginated` paginated interviews -> `200`
- [x] `PUT /{interviewId}` update success -> `200`
- [x] `PUT /{interviewId}` not found -> `404`
- [x] `PUT /{interviewId}` cancelled interview -> `400`
- [x] `DELETE /{interviewId}` success -> `204`
- [x] `DELETE /{interviewId}` not found -> `404`

### Status Management
- [x] `PATCH /{interviewId}/cancel` success -> `200`
- [x] `PATCH /{interviewId}/cancel` already cancelled -> `400`
- [x] `PATCH /{interviewId}/cancel` missing reason -> `400`
- [x] `PATCH /{interviewId}/status` success -> `200`
- [x] `PATCH /{interviewId}/status` cancelled interview -> `400`

### My Interviews
- [x] `GET /my/candidate` -> `200`
- [x] `GET /my/interviewer` -> `200`
- [x] `GET /my/candidate/paginated` -> `200`
- [x] `GET /my/interviewer/paginated` -> `200`

### Interviewer Management
- [x] `POST /{interviewId}/interviewers/{interviewerId}` success -> `200`
- [x] `POST /{interviewId}/interviewers/{interviewerId}` duplicate -> `409`
- [x] `POST /{interviewId}/interviewers/{interviewerId}` not found -> `404`
- [x] `DELETE /{interviewId}/interviewers/{interviewerId}` success -> `200`
- [x] `DELETE /{interviewId}/interviewers/{interviewerId}` not assigned -> `404`

### Feedback
- [x] `POST /{interviewId}/feedback` success -> `200`
- [x] `POST /{interviewId}/feedback` duplicate -> `409`
- [x] `POST /{interviewId}/feedback` interview not completed -> `400`
- [x] `POST /{interviewId}/feedback` invalid rating -> `400`
- [x] `GET /{interviewId}/feedback` all feedback -> `200`
- [x] `GET /{interviewId}/feedback/interviewer/{id}` success -> `200`
- [x] `GET /{interviewId}/feedback/interviewer/{id}` not found -> `404`
- [x] `GET /feedback/interviewer/{id}` all by interviewer -> `200`

### Filters
- [x] `GET /filter/status` -> `200`
- [x] `GET /filter/status/paginated` -> `200`
- [x] `GET /filter/date-range` -> `200`
- [x] `GET /filter/date-range/paginated` -> `200`

## Phase 5: Interview Execution

### Video Meeting Integration (`/api/v1/interviews/{interviewId}/meeting`)
- [x] `POST` generate meeting link (ZOOM / GOOGLE_MEET / INTERNAL) -> `200`
- [x] `POST` duplicate meeting link -> `409`
- [x] `GET` get meeting link for interview -> `200`
- [x] `GET` meeting not found -> `404`
- [x] Provider strategy pattern (pluggable Zoom, Google Meet, Internal)

### Calendar Integration (`/api/v1/calendar`)
- [x] `POST /interviewers/{id}/availability` add availability slot -> `200`
- [x] `GET /interviewers/{id}/availability` get all slots -> `200`
- [x] `GET /interviewers/{id}/availability/check?date=` check availability with conflict detection -> `200`
- [x] `DELETE /interviewers/{id}/availability/{slotId}` remove slot -> `204`

### Real-Time Collaborative Code Editor (`/api/v1/interviews/{interviewId}/code`)
- [x] `POST /start` start or join coding session -> `200`
- [x] `GET` get active session -> `200`
- [x] `PUT /save` save code snapshot -> `200`
- [x] `POST /end` end session -> `204`
- [x] `GET /history` session history -> `200`
- [x] WebSocket `/app/interview/{id}/code` — real-time code sync (FULL_SYNC, INSERT, DELETE, CURSOR_MOVE, LANGUAGE_CHANGE)

### Question Bank (`/api/v1/questions`)
- [x] `POST /categories` create category -> `200`
- [x] `GET /categories` list categories -> `200`
- [x] `POST` create question -> `200`
- [x] `GET /{questionId}` get question -> `200`
- [x] `PUT /{questionId}` update question -> `200`
- [x] `DELETE /{questionId}` soft delete -> `204`
- [x] `GET /search` filter by category/difficulty/type/keyword (paginated) -> `200`
- [x] `GET /category/{categoryId}` questions by category -> `200`
- [x] Seeded categories: DSA, System Design, OOD, Behavioral, Database, Concurrency, Web Dev, DevOps

## Dashboard & Analytics (`/api/v1/dashboard`)

- [x] `GET /admin` — admin/recruiter stats (total interviews, users, questions, by-status breakdown, upcoming, today, feedback metrics)
- [x] `GET /interviewer` — current interviewer dashboard (assigned, completed, pending feedback, upcoming list)
- [x] `GET /interviewer/{id}` — admin view of specific interviewer
- [x] `GET /candidate` — current candidate dashboard (upcoming, completed, meeting links)

## In-App Notifications (`/api/v1/notifications`)

- [x] `GET /` — get my notifications (paginated)
- [x] `GET /unread` — get unread notifications
- [x] `GET /count` — unread count (for notification badge)
- [x] `PATCH /{id}/read` — mark single notification as read
- [x] `PATCH /read-all` — mark all as read
- [x] WebSocket push to `/user/{email}/queue/notifications` on new events
- [x] Auto-created on: interview scheduled, rescheduled, cancelled, interviewer assigned

---

## Phase 7 — Advanced Features

### AI Suggestions (`/api/v1/ai`)
- [x] POST `/suggest-questions` — Generate question suggestions (success, user not found)
- [x] POST `/parse-resume` — Parse resume (success)
- [x] POST `/interview-summary` — Generate summary (success)
- [x] GET `/suggestions` — Paginated suggestion history (results, empty)
- [x] GET `/suggestions/interview/{id}` — Suggestions by interview
- [x] PATCH `/suggestions/{id}/status` — Accept/reject (success, not found)

### Video Recording (`/api/v1/video-recordings`)
- [x] POST `/start` — Start recording (success, interview not found)
- [x] PATCH `/{id}/complete` — Complete recording (success, not found)
- [x] PATCH `/{id}/fail` — Fail recording (success)
- [x] GET `/interview/{id}` — Get by interview (results, empty)
- [x] GET `/{id}` — Get single with presigned URL (success, not found)
- [x] DELETE `/{id}` — Soft delete (success, not found)
- [x] GET `/my` — My recordings paginated (results, empty)

### Whiteboard Collaboration (`/api/v1/whiteboards`)
- [x] POST `/` — Create session (success, with title, interview not found)
- [x] GET `/{id}` — Get session (success, not found)
- [x] GET `/interview/{id}` — Get by interview (results, empty)
- [x] POST `/{id}/strokes` — Add stroke (success, auto-sequence, session not found, session closed)
- [x] GET `/{id}/strokes` — Get strokes ordered (results, empty)
- [x] PUT `/{id}/snapshot` — Save snapshot (success, not found)
- [x] PATCH `/{id}/close` — Close session (success, already closed)
- [x] DELETE `/{id}` — Delete session (success, not found)

### Export/Import (`/api/v1/export-import`)
- [x] POST `/export` — Start export (CSV/JSON for various entity types)
- [x] POST `/import` — Start import (success)
- [x] GET `/jobs` — My jobs paginated (results, empty)
- [x] GET `/jobs/{id}` — Job status (success, not found)
- [x] DELETE `/jobs/{id}` — Cancel job (PENDING only, not found, already processing)

### Webhook Integrations (`/api/v1/webhooks`)
- [x] POST `/` — Register webhook (success, secret generated)
- [x] GET `/` — My webhooks (results, empty)
- [x] GET `/{id}` — Webhook details (secret masked, not found)
- [x] PUT `/{id}` — Update webhook (success, not found)
- [x] DELETE `/{id}` — Delete webhook (success, not found)
- [x] POST `/{id}/regenerate-secret` — New secret generated
- [x] GET `/{id}/deliveries` — Delivery history (paginated)
- [x] POST `/deliveries/{id}/retry` — Retry delivery (success, not found)

### Multi-Tenant / Organizations (`/api/v1/organizations`)
- [x] POST `/` — Create organization (success, duplicate slug)
- [x] GET `/{id}` — Get organization (success, not found)
- [x] PUT `/{id}` — Update organization (success, not found)
- [x] DELETE `/{id}` — Delete organization (success, not found)
- [x] GET `/my` — User's organizations (results, empty)
- [x] POST `/{id}/members` — Add member (success, duplicate, max limit)
- [x] DELETE `/{id}/members/{userId}` — Remove member (success, owner protection)
- [x] GET `/{id}/members` — List members
- [x] PATCH `/{id}/members/{userId}/role` — Update role (success, not found)

### Candidate Feedback — Reverse (`/api/v1/candidate-feedback`)
- [x] POST `/` — Submit feedback (success full/minimal, duplicate, interview not found)
- [x] GET `/interview/{id}` — Feedback for interview (results, empty)
- [x] GET `/summary` — Aggregate statistics (with data, empty)
- [x] GET `/my` — My feedback paginated (results, empty)
- [x] Anonymous feedback hides candidate name

### Activity Feed / Timeline (`/api/v1/activities`)
- [x] GET `/` — Global feed paginated (results, page size)
- [x] GET `/entity/{type}/{id}` — Entity timeline (results, empty, filtering)
- [x] GET `/user/{userId}` — User activity (paginated, not found)
- [x] GET `/my` — My activity (results, empty)
- [x] POST `/filter` — Filtered by entityType, action, date range, actor
- [x] Convenience methods: logInterviewCreated, logInterviewScheduled, logFeedbackSubmitted, logCandidateAdvanced

