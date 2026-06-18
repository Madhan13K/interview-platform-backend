# Interview Platform Backend — API Reference & End-to-End Testing Guide

## Table of Contents

- [Getting Started](#getting-started)
- [Authentication](#authentication)
- [API Endpoints](#api-endpoints)
  - [Auth](#auth-apiv1auth)
  - [Users](#users-apiv1users)
  - [Roles](#roles-apiv1roles)
  - [Permissions](#permissions-apiv1permissions)
  - [Interviews](#interviews-apiv1interviews)
  - [Meeting Links](#meeting-links-apiv1interviewsidmeeting)
  - [Calendar / Availability](#calendar-apiv1calendar)
  - [Code Editor Sessions](#code-editor-apiv1interviewsidcode)
  - [Question Bank](#question-bank-apiv1questions)
  - [WebSocket (Real-Time)](#websocket-real-time)
- [End-to-End Test Scripts](#end-to-end-test-scripts)
- [Notification System](#notification-system)
- [Swagger UI](#swagger-ui)

---

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven (or use included `./mvnw`)

### 1. Start Infrastructure

```bash
docker compose up -d
```

This starts:
- **PostgreSQL** (port 5433)
- **Zookeeper** (port 2181)
- **Kafka** (port 9092)

### 2. Run the Application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

App runs at **http://localhost:8080**

### 3. Seeded Users (ready to use)

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@interview.local` | `ChangeMe123!` |
| Recruiter | `recruiter@interview.local` | `ChangeMe123!` |
| Interviewer | `interviewer@interview.local` | `ChangeMe123!` |
| Candidate | `candidate@interview.local` | `ChangeMe123!` |

---

## Authentication

All protected endpoints require a `Bearer` token in the `Authorization` header.

```bash
# Get a token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@interview.local","password":"ChangeMe123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# Use it
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users
```

---

## API Endpoints

### Auth (`/api/v1/auth`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/register` | Register as candidate | Public |
| POST | `/register/interviewer` | Register as interviewer | Public |
| POST | `/admin/create-user` | Admin creates user with roles | ADMIN, RECRUITER |
| POST | `/login` | Login (returns JWT) | Public |
| POST | `/refresh` | Refresh access token | Public |
| POST | `/logout` | Invalidate refresh token | Authenticated |
| POST | `/forgot-password` | Request password reset email | Public |
| POST | `/reset-password` | Reset password with token | Public |
| GET | `/verify-email?token=` | Verify email address | Public |
| POST | `/resend-verification` | Resend verification email | Public |

---

### Users (`/api/v1/users`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/` | Create user (assigns CANDIDATE role) | ADMIN |
| GET | `/` | Get all users | ADMIN |
| GET | `/me` | Get current authenticated user | Authenticated |
| GET | `/{userId}` | Get user by ID | ADMIN |
| PUT | `/{userId}` | Update user | ADMIN |
| DELETE | `/{userId}` | Soft-delete user | ADMIN |
| GET | `/{userId}/profile` | Get user profile | ADMIN, CANDIDATE |
| PUT | `/{userId}/profile` | Update user profile | ADMIN, CANDIDATE |
| POST | `/{userId}/roles` | Assign role to user | ADMIN |
| GET | `/{userId}/roles` | Get user's roles | ADMIN |
| DELETE | `/{userId}/roles/{roleId}` | Remove role from user | ADMIN |
| GET | `/{userId}/permissions` | Get user's permissions | ADMIN |
| PUT | `/{userId}/change-password` | Change password | Owner, ADMIN |
| GET | `/search` | Search users (paginated) | ADMIN, RECRUITER |
| PATCH | `/{userId}/status` | Update account status | ADMIN |

---

### Roles (`/api/v1/roles`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/` | Create role | ADMIN |
| GET | `/` | Get all roles | ADMIN |
| GET | `/{roleId}` | Get role by ID | ADMIN |
| PUT | `/{roleId}` | Update role | ADMIN |
| DELETE | `/{roleId}` | Delete role | ADMIN |
| POST | `/{roleId}/permissions` | Assign permission to role | ADMIN |
| GET | `/{roleId}/permissions` | Get role's permissions | ADMIN |
| DELETE | `/permissions/{rolePermissionId}` | Remove permission from role | ADMIN |

---

### Permissions (`/api/v1/permissions`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/` | Create permission | ADMIN |
| GET | `/` | Get all permissions | ADMIN |
| GET | `/{permissionId}` | Get permission by ID | ADMIN |
| PUT | `/{permissionId}` | Update permission | ADMIN |
| DELETE | `/{permissionId}` | Delete permission | ADMIN |

---

### Interviews (`/api/v1/interviews`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/` | Create interview | ADMIN, RECRUITER |
| GET | `/` | Get all interviews | ADMIN, RECRUITER |
| GET | `/paginated` | Get interviews (paginated) | ADMIN, RECRUITER |
| GET | `/{interviewId}` | Get interview by ID | All roles |
| PUT | `/{interviewId}` | Update interview | ADMIN, RECRUITER |
| DELETE | `/{interviewId}` | Delete interview | ADMIN |
| PATCH | `/{interviewId}/cancel` | Cancel interview | ADMIN, RECRUITER |
| PATCH | `/{interviewId}/status` | Update status | ADMIN, RECRUITER |
| GET | `/my/candidate` | My interviews (as candidate) | CANDIDATE |
| GET | `/my/candidate/paginated` | My interviews paginated | CANDIDATE |
| GET | `/my/interviewer` | My interviews (as interviewer) | INTERVIEWER |
| GET | `/my/interviewer/paginated` | My interviews paginated | INTERVIEWER |
| POST | `/{id}/interviewers/{interviewerId}` | Add interviewer | ADMIN, RECRUITER |
| DELETE | `/{id}/interviewers/{interviewerId}` | Remove interviewer | ADMIN, RECRUITER |
| POST | `/{id}/feedback` | Submit feedback | INTERVIEWER |
| GET | `/{id}/feedback` | Get all feedback | ADMIN, RECRUITER |
| GET | `/{id}/feedback/interviewer/{iid}` | Get feedback by interviewer | ADMIN, RECRUITER |
| GET | `/feedback/interviewer/{iid}` | All feedback by interviewer | ADMIN, RECRUITER |
| GET | `/filter/status` | Filter by status | ADMIN, RECRUITER |
| GET | `/filter/status/paginated` | Filter by status (paginated) | ADMIN, RECRUITER |
| GET | `/filter/date-range` | Filter by date range | ADMIN, RECRUITER |
| GET | `/filter/date-range/paginated` | Filter by date range (paginated) | ADMIN, RECRUITER |

---

### Meeting Links (`/api/v1/interviews/{id}/meeting`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/` | Generate meeting link (ZOOM, GOOGLE_MEET, INTERNAL) | ADMIN, RECRUITER |
| GET | `/` | Get meeting link for interview | All roles |

---

### Calendar (`/api/v1/calendar`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/interviewers/{id}/availability` | Add availability slot | ADMIN, INTERVIEWER, RECRUITER |
| GET | `/interviewers/{id}/availability` | Get all availability | ADMIN, INTERVIEWER, RECRUITER |
| GET | `/interviewers/{id}/availability/check?date=` | Check availability (with conflicts) | ADMIN, INTERVIEWER, RECRUITER |
| DELETE | `/interviewers/{id}/availability/{slotId}` | Delete availability slot | ADMIN, INTERVIEWER |

---

### Code Editor (`/api/v1/interviews/{id}/code`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/start?language=java` | Start/join coding session | ADMIN, INTERVIEWER, CANDIDATE |
| GET | `/` | Get active coding session | ADMIN, INTERVIEWER, CANDIDATE |
| PUT | `/save` | Save code snapshot | ADMIN, INTERVIEWER, CANDIDATE |
| POST | `/end` | End coding session | ADMIN, INTERVIEWER |
| GET | `/history` | Get session history | ADMIN, INTERVIEWER, RECRUITER |

---

### Question Bank (`/api/v1/questions`)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/categories` | Create category | ADMIN, INTERVIEWER |
| GET | `/categories` | List all categories | ADMIN, INTERVIEWER, RECRUITER |
| POST | `/` | Create question | ADMIN, INTERVIEWER |
| GET | `/{questionId}` | Get question by ID | ADMIN, INTERVIEWER, RECRUITER |
| PUT | `/{questionId}` | Update question | ADMIN, INTERVIEWER |
| DELETE | `/{questionId}` | Soft-delete question | ADMIN |
| GET | `/search` | Search (filters + pagination) | ADMIN, INTERVIEWER, RECRUITER |
| GET | `/category/{categoryId}` | Get questions by category | ADMIN, INTERVIEWER, RECRUITER |

**Search parameters:** `?categoryId=&difficulty=EASY|MEDIUM|HARD&type=CODING|SYSTEM_DESIGN|BEHAVIORAL|THEORETICAL|MCQ&keyword=&page=0&size=20`

---

### WebSocket (Real-Time)

**Connect:** `ws://localhost:8080/ws` (STOMP over SockJS)

| Direction | Destination | Description |
|-----------|-------------|-------------|
| SUBSCRIBE | `/topic/interview/{id}` | General interview messages (join/leave/chat/status) |
| SUBSCRIBE | `/topic/interview/{id}/code` | Code editor updates |
| SUBSCRIBE | `/topic/interview/{id}/signal` | WebRTC signaling |
| SEND | `/app/interview/{id}/join` | Join session |
| SEND | `/app/interview/{id}/leave` | Leave session |
| SEND | `/app/interview/{id}/chat` | Send chat message |
| SEND | `/app/interview/{id}/code` | Send code update |
| SEND | `/app/interview/{id}/signal` | WebRTC signal |
| SEND | `/app/interview/{id}/status` | Status update |

**Message format:**
```json
{
  "senderId": "uuid",
  "senderName": "John",
  "type": "CODE",
  "content": "public class Solution { ... }"
}
```

---

## End-to-End Test Scripts

### Setup Variables

```bash
BASE=http://localhost:8080/api/v1

# Login as Admin
TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@interview.local","password":"ChangeMe123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# Get user IDs
USERS=$(curl -s $BASE/users -H "Authorization: Bearer $TOKEN")
ADMIN_ID=$(echo $USERS | python3 -c "import sys,json; u=json.load(sys.stdin); print(next(x['id'] for x in u if x['email']=='admin@interview.local'))")
INTERVIEWER_ID=$(echo $USERS | python3 -c "import sys,json; u=json.load(sys.stdin); print(next(x['id'] for x in u if x['email']=='interviewer@interview.local'))")
CANDIDATE_ID=$(echo $USERS | python3 -c "import sys,json; u=json.load(sys.stdin); print(next(x['id'] for x in u if x['email']=='candidate@interview.local'))")

echo "Admin: $ADMIN_ID"
echo "Interviewer: $INTERVIEWER_ID"
echo "Candidate: $CANDIDATE_ID"
```

---

### Test 1: Complete Registration & Login Flow

```bash
echo "=== REGISTER ==="
REG=$(curl -s -X POST $BASE/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName":"Test","lastName":"User",
    "email":"test@example.com","password":"Password@123"
  }')
echo $REG | python3 -m json.tool

echo "=== LOGIN (will fail - not verified) ==="
curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Password@123"}' | python3 -m json.tool
```

---

### Test 2: Schedule Interview + Notifications

```bash
echo "=== CREATE INTERVIEW ==="
START=$(date -u -v+3d '+%Y-%m-%dT10:00:00Z')
END=$(date -u -v+3d '+%Y-%m-%dT11:00:00Z')

INTERVIEW=$(curl -s -X POST $BASE/interviews \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\":\"Java Backend - Technical Round\",
    \"description\":\"DSA + System Design\",
    \"candidateId\":\"$CANDIDATE_ID\",
    \"startTime\":\"$START\",
    \"endTime\":\"$END\",
    \"timeZone\":\"Asia/Kolkata\",
    \"type\":\"TECHNICAL\",
    \"mode\":\"ONLINE\",
    \"interviewerIds\":[\"$INTERVIEWER_ID\"]
  }")
echo $INTERVIEW | python3 -m json.tool
INTERVIEW_ID=$(echo $INTERVIEW | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Interview ID: $INTERVIEW_ID"
# ✅ Check logs for: "Interview scheduled event: interviewId=..."
```

---

### Test 3: Generate Meeting Link

```bash
echo "=== GENERATE MEETING ==="
curl -s -X POST "$BASE/interviews/$INTERVIEW_ID/meeting" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"provider":"INTERNAL","durationMinutes":60}' | python3 -m json.tool

echo "=== GET MEETING ==="
curl -s "$BASE/interviews/$INTERVIEW_ID/meeting" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

echo "=== DUPLICATE (expect 409) ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST "$BASE/interviews/$INTERVIEW_ID/meeting" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"provider":"INTERNAL"}'
```

---

### Test 4: Calendar — Interviewer Availability

```bash
echo "=== ADD MONDAY SLOT (9-12) ==="
curl -s -X POST "$BASE/calendar/interviewers/$INTERVIEWER_ID/availability" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"dayOfWeek":1,"startTime":"09:00","endTime":"12:00","timeZone":"Asia/Kolkata","isRecurring":true}' \
  | python3 -m json.tool

echo "=== ADD MONDAY SLOT (14-17) ==="
curl -s -X POST "$BASE/calendar/interviewers/$INTERVIEWER_ID/availability" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"dayOfWeek":1,"startTime":"14:00","endTime":"17:00","timeZone":"Asia/Kolkata","isRecurring":true}' \
  | python3 -m json.tool

echo "=== GET ALL AVAILABILITY ==="
curl -s "$BASE/calendar/interviewers/$INTERVIEWER_ID/availability" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

echo "=== CHECK DATE (with conflict detection) ==="
curl -s "$BASE/calendar/interviewers/$INTERVIEWER_ID/availability/check?date=2026-06-02" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

---

### Test 5: Question Bank

```bash
echo "=== LIST CATEGORIES ==="
CATS=$(curl -s "$BASE/questions/categories" -H "Authorization: Bearer $TOKEN")
echo $CATS | python3 -m json.tool
DSA_ID=$(echo $CATS | python3 -c "import sys,json; c=json.load(sys.stdin); print(next(x['id'] for x in c if 'Algorithm' in x['name']))")

echo "=== CREATE QUESTION ==="
curl -s -X POST "$BASE/questions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\":\"Reverse a Linked List\",
    \"description\":\"Given the head of a singly linked list, reverse the list and return the reversed list.\",
    \"categoryId\":\"$DSA_ID\",
    \"difficulty\":\"EASY\",
    \"type\":\"CODING\",
    \"expectedDurationMinutes\":15,
    \"sampleAnswer\":\"Iterative: use three pointers (prev, curr, next). O(n) time, O(1) space.\",
    \"hints\":\"Think about what pointers you need to track.\",
    \"tags\":\"linked-list,pointers,recursion\"
  }" | python3 -m json.tool

echo "=== SEARCH (CODING + EASY) ==="
curl -s "$BASE/questions/search?type=CODING&difficulty=EASY" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

echo "=== SEARCH BY KEYWORD ==="
curl -s "$BASE/questions/search?keyword=linked" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

---

### Test 6: Coding Session (Collaborative Editor)

```bash
echo "=== START SESSION ==="
curl -s -X POST "$BASE/interviews/$INTERVIEW_ID/code/start?language=java" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

echo "=== SAVE CODE ==="
curl -s -X PUT "$BASE/interviews/$INTERVIEW_ID/code/save" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"code\":\"public class Solution {\\n    public ListNode reverseList(ListNode head) {\\n        ListNode prev = null;\\n        ListNode curr = head;\\n        while (curr != null) {\\n            ListNode next = curr.next;\\n            curr.next = prev;\\n            prev = curr;\\n            curr = next;\\n        }\\n        return prev;\\n    }\\n}\",
    \"language\":\"java\",
    \"userId\":\"$CANDIDATE_ID\"
  }" | python3 -m json.tool

echo "=== GET ACTIVE SESSION ==="
curl -s "$BASE/interviews/$INTERVIEW_ID/code" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

echo "=== END SESSION ==="
curl -s -X POST "$BASE/interviews/$INTERVIEW_ID/code/end" \
  -H "Authorization: Bearer $TOKEN"
echo "Done"

echo "=== SESSION HISTORY ==="
curl -s "$BASE/interviews/$INTERVIEW_ID/code/history" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

---

### Test 7: Full Interview Lifecycle

```bash
echo "============================================"
echo "  FULL E2E: Schedule → Meet → Code → Feedback"
echo "============================================"

# 1. Check availability
echo "\n[1/7] Checking interviewer availability..."
curl -s "$BASE/calendar/interviewers/$INTERVIEWER_ID/availability/check?date=2026-06-05" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# 2. Schedule interview
echo "\n[2/7] Scheduling interview..."
IV=$(curl -s -X POST $BASE/interviews \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\":\"Full Stack Assessment\",
    \"candidateId\":\"$CANDIDATE_ID\",
    \"startTime\":\"2026-06-05T09:00:00Z\",
    \"endTime\":\"2026-06-05T10:00:00Z\",
    \"timeZone\":\"Asia/Kolkata\",
    \"type\":\"TECHNICAL\",\"mode\":\"ONLINE\",
    \"interviewerIds\":[\"$INTERVIEWER_ID\"]
  }")
IV_ID=$(echo $IV | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Created: $IV_ID"

# 3. Generate meeting link
echo "\n[3/7] Generating meeting link..."
curl -s -X POST "$BASE/interviews/$IV_ID/meeting" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"provider":"INTERNAL"}' | python3 -m json.tool

# 4. Start coding session
echo "\n[4/7] Starting code session..."
curl -s -X POST "$BASE/interviews/$IV_ID/code/start?language=python" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# 5. Candidate writes code
echo "\n[5/7] Saving code..."
curl -s -X PUT "$BASE/interviews/$IV_ID/code/save" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"code\":\"def two_sum(nums, target):\\n    seen = {}\\n    for i, n in enumerate(nums):\\n        if target - n in seen:\\n            return [seen[target-n], i]\\n        seen[n] = i\",\"language\":\"python\"}" \
  | python3 -m json.tool

# 6. Complete interview
echo "\n[6/7] Completing interview..."
curl -s -X POST "$BASE/interviews/$IV_ID/code/end" -H "Authorization: Bearer $TOKEN"
curl -s -X PATCH "$BASE/interviews/$IV_ID/status?status=COMPLETED" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# 7. Submit feedback (as interviewer)
echo "\n[7/7] Submitting feedback..."
INT_TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"interviewer@interview.local","password":"ChangeMe123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

curl -s -X POST "$BASE/interviews/$IV_ID/feedback" \
  -H "Authorization: Bearer $INT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rating":4,"recommendation":"HIRE",
    "strengths":"Clean code, good problem solving",
    "weaknesses":"Could optimize space complexity",
    "comments":"Strong hire recommendation"
  }' | python3 -m json.tool

echo "\n✅ Full lifecycle complete!"
```

---

## Notification System

Notifications are triggered automatically when:

| Event | Trigger | Recipients | Channels |
|-------|---------|------------|----------|
| Interview Scheduled | `POST /interviews` | Candidate + Interviewers | Email, SMS, Kafka |
| Interview Rescheduled | `PUT /interviews/{id}` (time change) | Candidate + Interviewers | Email, SMS, Kafka |
| Interview Cancelled | `PATCH /interviews/{id}/cancel` | Candidate + Interviewers | Email, SMS, Kafka |
| Feedback Submitted | `POST /interviews/{id}/feedback` | Admin/Recruiter | Kafka |

**Dev mode:** Emails are logged (not sent). Check application console output:
```
INFO  - Email notifications disabled. Would send to=candidate@interview.local, subject=Interview Scheduled: ...
INFO  - Interview scheduled event: interviewId=xxx, candidate=candidate@interview.local
```

**View Kafka messages:**
```bash
docker exec -it $(docker ps --filter "ancestor=confluentinc/cp-kafka:7.6.0" -q) \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic interview-notifications --from-beginning --max-messages 5
```

---

## Swagger UI

Interactive API documentation available at:

```
http://localhost:8080/swagger-ui/index.html
```

Tags available:
- Authentication
- Users
- Roles
- Permissions
- Interviews
- Meeting
- Calendar
- Code Editor
- Question Bank

---

## Error Responses

All errors follow this format:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: xxx",
  "path": "/api/v1/users/xxx",
  "timestamp": "2026-05-31T..."
}
```

| Status | Meaning |
|--------|---------|
| 400 | Bad Request — validation failed or invalid data |
| 401 | Unauthorized — invalid/expired token or bad credentials |
| 403 | Forbidden — insufficient permissions |
| 404 | Not Found — resource doesn't exist |
| 409 | Conflict — duplicate resource |

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Framework | Spring Boot 3.x |
| Language | Java 21 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Auth | JWT (access + refresh tokens with rotation) |
| OAuth2 | Google, GitHub, Microsoft |
| Real-time | WebSocket (STOMP + SockJS) |
| Messaging | Apache Kafka |
| API Docs | SpringDoc OpenAPI (Swagger) |
| Testing | JUnit 5, MockMvc, Mockito |

---

## Phase 7 — New API Endpoints

### AI-Powered Features (`/api/v1/ai`)

**Prerequisites**: Authenticated user, Bearer token

```bash
# Suggest interview questions
curl -X POST http://localhost:8080/api/v1/ai/suggest-questions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "jobTitle": "Senior Java Developer",
    "difficulty": "MEDIUM",
    "category": "CODING",
    "skills": ["Java", "Spring Boot", "Microservices"],
    "count": 5
  }'

# Parse resume
curl -X POST http://localhost:8080/api/v1/ai/parse-resume \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"documentId": "uuid-of-uploaded-resume"}'

# Generate interview summary
curl -X POST http://localhost:8080/api/v1/ai/interview-summary \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"interviewId": "uuid-of-completed-interview"}'

# Get suggestion history
curl http://localhost:8080/api/v1/ai/suggestions?page=0&size=10 \
  -H "Authorization: Bearer $TOKEN"

# Accept/Reject a suggestion
curl -X PATCH http://localhost:8080/api/v1/ai/suggestions/{id}/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "ACCEPTED"}'
```

### Video Recording (`/api/v1/video-recordings`)

```bash
# Start recording
curl -X POST http://localhost:8080/api/v1/video-recordings/start \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"interviewId": "uuid-of-interview"}'

# Complete recording
curl -X PATCH http://localhost:8080/api/v1/video-recordings/{id}/complete \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fileSizeBytes": 52428800, "durationSeconds": 3600}'

# Get recordings for interview
curl http://localhost:8080/api/v1/video-recordings/interview/{interviewId} \
  -H "Authorization: Bearer $TOKEN"

# Get single recording (includes presigned download URL)
curl http://localhost:8080/api/v1/video-recordings/{id} \
  -H "Authorization: Bearer $TOKEN"

# Delete recording (soft)
curl -X DELETE http://localhost:8080/api/v1/video-recordings/{id} \
  -H "Authorization: Bearer $TOKEN"
```

### Whiteboard Collaboration (`/api/v1/whiteboards`)

```bash
# Create whiteboard session
curl -X POST http://localhost:8080/api/v1/whiteboards \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"interviewId": "uuid", "title": "System Design"}'

# Add stroke
curl -X POST http://localhost:8080/api/v1/whiteboards/{sessionId}/strokes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "strokeData": "{\"points\": [[0,0],[50,50],[100,100]]}",
    "tool": "PEN",
    "color": "#FF0000",
    "strokeWidth": 3.0
  }'

# Get all strokes (ordered)
curl http://localhost:8080/api/v1/whiteboards/{sessionId}/strokes \
  -H "Authorization: Bearer $TOKEN"

# Save snapshot
curl -X PUT http://localhost:8080/api/v1/whiteboards/{sessionId}/snapshot \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"snapshotData": "<base64-encoded-canvas-state>"}'

# Close session
curl -X PATCH http://localhost:8080/api/v1/whiteboards/{sessionId}/close \
  -H "Authorization: Bearer $TOKEN"
```

**WebSocket (Real-Time Strokes)**: Subscribe to `/topic/whiteboard/{sessionId}` for live updates.

### Export/Import (`/api/v1/export-import`)

```bash
# Start export
curl -X POST http://localhost:8080/api/v1/export-import/export \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "entityType": "INTERVIEWS",
    "format": "CSV",
    "filters": {"status": "COMPLETED"}
  }'

# Start import (from uploaded CSV)
curl -X POST http://localhost:8080/api/v1/export-import/import \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"entityType": "CANDIDATES", "fileDocumentId": "uuid-of-csv-file"}'

# Check job status
curl http://localhost:8080/api/v1/export-import/jobs/{jobId} \
  -H "Authorization: Bearer $TOKEN"

# List all jobs
curl http://localhost:8080/api/v1/export-import/jobs?page=0&size=10 \
  -H "Authorization: Bearer $TOKEN"
```

### Webhook Integrations (`/api/v1/webhooks`)

```bash
# Register webhook
curl -X POST http://localhost:8080/api/v1/webhooks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://your-server.com/webhook",
    "description": "Interview events",
    "events": ["INTERVIEW_SCHEDULED", "INTERVIEW_COMPLETED", "FEEDBACK_SUBMITTED"]
  }'

# Get my webhooks
curl http://localhost:8080/api/v1/webhooks \
  -H "Authorization: Bearer $TOKEN"

# Regenerate secret
curl -X POST http://localhost:8080/api/v1/webhooks/{id}/regenerate-secret \
  -H "Authorization: Bearer $TOKEN"

# Get delivery history
curl http://localhost:8080/api/v1/webhooks/{id}/deliveries?page=0&size=20 \
  -H "Authorization: Bearer $TOKEN"

# Retry failed delivery
curl -X POST http://localhost:8080/api/v1/webhooks/deliveries/{deliveryId}/retry \
  -H "Authorization: Bearer $TOKEN"
```

**Webhook Payload Format**:
```json
{
  "event": "INTERVIEW_SCHEDULED",
  "timestamp": "2024-01-15T10:30:00Z",
  "data": { ... }
}
```
**Signature Header**: `X-Webhook-Signature: sha256=<hmac-hex>`

### Multi-Tenant / Organizations (`/api/v1/organizations`)

```bash
# Create organization
curl -X POST http://localhost:8080/api/v1/organizations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Acme Corp", "slug": "acme-corp", "domain": "acme.com", "plan": "PROFESSIONAL"}'

# Get my organizations
curl http://localhost:8080/api/v1/organizations/my \
  -H "Authorization: Bearer $TOKEN"

# Add member
curl -X POST http://localhost:8080/api/v1/organizations/{orgId}/members \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userId": "uuid-of-user", "role": "MEMBER"}'

# Update member role
curl -X PATCH http://localhost:8080/api/v1/organizations/{orgId}/members/{userId}/role \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"role": "ADMIN"}'

# List members
curl http://localhost:8080/api/v1/organizations/{orgId}/members \
  -H "Authorization: Bearer $TOKEN"
```

### Candidate Feedback — Reverse (`/api/v1/candidate-feedback`)

```bash
# Submit feedback (as candidate)
curl -X POST http://localhost:8080/api/v1/candidate-feedback \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "interviewId": "uuid-of-interview",
    "overallRating": 4,
    "communicationRating": 5,
    "professionalismRating": 4,
    "technicalClarityRating": 3,
    "timelinessRating": 5,
    "comments": "Great experience overall",
    "wouldRecommend": true,
    "isAnonymous": false
  }'

# Get feedback for interview
curl http://localhost:8080/api/v1/candidate-feedback/interview/{interviewId} \
  -H "Authorization: Bearer $TOKEN"

# Get aggregate summary (admin/recruiter)
curl http://localhost:8080/api/v1/candidate-feedback/summary \
  -H "Authorization: Bearer $TOKEN"

# Get my submitted feedback
curl http://localhost:8080/api/v1/candidate-feedback/my?page=0&size=10 \
  -H "Authorization: Bearer $TOKEN"
```

### Activity Feed / Timeline (`/api/v1/activities`)

```bash
# Global activity feed
curl http://localhost:8080/api/v1/activities?page=0&size=20 \
  -H "Authorization: Bearer $TOKEN"

# Entity timeline
curl http://localhost:8080/api/v1/activities/entity/INTERVIEW/{interviewId} \
  -H "Authorization: Bearer $TOKEN"

# User activity
curl http://localhost:8080/api/v1/activities/user/{userId} \
  -H "Authorization: Bearer $TOKEN"

# My activity
curl http://localhost:8080/api/v1/activities/my?page=0&size=20 \
  -H "Authorization: Bearer $TOKEN"

# Filtered activity
curl -X POST http://localhost:8080/api/v1/activities/filter \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "entityType": "INTERVIEW",
    "action": "SCHEDULED",
    "startDate": "2024-01-01T00:00:00Z",
    "endDate": "2024-12-31T23:59:59Z"
  }'
```

---

## End-to-End Test: Phase 7 Features

### Test 8: AI Question Suggestions + Accept Flow
```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"Password@123"}' | jq -r '.accessToken')

# 2. Get AI question suggestions
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/ai/suggest-questions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"jobTitle":"Backend Developer","difficulty":"MEDIUM","category":"CODING","count":3}')
echo $RESPONSE | jq .
SUGGESTION_ID=$(echo $RESPONSE | jq -r '.id')

# 3. Accept suggestion
curl -X PATCH http://localhost:8080/api/v1/ai/suggestions/$SUGGESTION_ID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "ACCEPTED"}'
```

### Test 9: Full Webhook Lifecycle
```bash
# 1. Register webhook
WH=$(curl -s -X POST http://localhost:8080/api/v1/webhooks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://webhook.site/your-id","events":["INTERVIEW_SCHEDULED"],"description":"Test"}')
WH_ID=$(echo $WH | jq -r '.id')

# 2. Schedule an interview (triggers webhook)
# ... (use interview create endpoint)

# 3. Check deliveries
curl http://localhost:8080/api/v1/webhooks/$WH_ID/deliveries?page=0&size=10 \
  -H "Authorization: Bearer $TOKEN" | jq .
```

