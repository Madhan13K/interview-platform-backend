# Interview Platform Backend - Complete API Testing Guide

All APIs with curl commands, payloads, and expected responses.

**Base URL:** `http://localhost:8080`

---

## Table of Contents

- [Setup](#setup)
- [1. Authentication](#1-authentication)
- [2. Users](#2-users)
- [3. Roles](#3-roles)
- [4. Permissions](#4-permissions)
- [5. Interviews](#5-interviews)
- [6. Templates](#6-templates)
- [7. Question Bank](#7-question-bank)
- [8. Code Editor](#8-code-editor)
- [9. Code Execution](#9-code-execution)
- [10. Meeting](#10-meeting)
- [11. Calendar](#11-calendar)
- [12. Scheduling](#12-scheduling)
- [13. Reminders](#13-reminders)
- [14. Self-Service](#14-self-service)
- [15. Dashboard](#15-dashboard)
- [16. Notifications](#16-notifications)
- [17. Pipelines](#17-pipelines)
- [18. Scorecards](#18-scorecards)
- [19. Job Positions](#19-job-positions)
- [20. Documents](#20-documents)
- [21. Bulk Operations](#21-bulk-operations)
- [22. Reports & Analytics](#22-reports--analytics)
- [23. Teams](#23-teams)
- [24. Tags](#24-tags)
- [25. AI Features](#25-ai-features)
- [26. Video Recording](#26-video-recording)
- [27. Whiteboard](#27-whiteboard)
- [28. Webhooks](#28-webhooks)
- [29. Organizations](#29-organizations-multi-tenant)
- [30. Candidate Feedback](#30-candidate-feedback)
- [31. Activity Feed](#31-activity-feed)
- [32. Export/Import](#32-exportimport)
- [33. SSO/SAML](#33-ssosaml)
- [34. Account Security](#34-account-security)
- [35. MFA](#35-mfa)
- [36. API Keys](#36-api-keys)
- [37. GDPR](#37-gdpr)
- [38. Job Board (Public)](#38-job-board-public)
- [39. Candidate Portal](#39-candidate-portal)
- [40. Offer Letters](#40-offer-letters)
- [41. Calendar Sync](#41-calendar-sync)
- [42. Workflows](#42-workflows)
- [43. Approvals](#43-approvals)
- [44. Referrals](#44-referrals)
- [45. DEI Analytics](#45-dei-analytics)
- [46. Source Tracking](#46-source-tracking)
- [47. WebSocket (Real-Time)](#47-websocket-real-time)
- [48. End-to-End Flow](#48-end-to-end-flow)
- [Error Responses](#error-responses)

---

## Setup

### Prerequisites
- Java 21+, Docker, Maven
- Start infrastructure: `docker compose up -d`
- Run app: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

### Build & Test Commands

```bash
# Build (compiles + unit tests, no Docker needed)
./mvnw clean install

# Run app locally (requires docker compose up -d for DB/Redis/Kafka)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run integration tests (requires docker compose up -d)
./mvnw verify -PintegrationTests

# Skip all tests (fastest build)
./mvnw clean install -DskipTests
```

> Integration tests (`*IntegrationTest.java`) are excluded from the default build.
> They require PostgreSQL, Redis, and Kafka running via Docker Compose.

### Seeded Users

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@interview.local` | `ChangeMe123!` |
| Recruiter | `recruiter@interview.local` | `ChangeMe123!` |
| Interviewer | `interviewer@interview.local` | `ChangeMe123!` |
| Candidate | `candidate@interview.local` | `ChangeMe123!` |

### Get Tokens & IDs

```bash
BASE=http://localhost:8080/api/v1

# Login as Admin
TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@interview.local","password":"ChangeMe123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# Login as Interviewer
INT_TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"interviewer@interview.local","password":"ChangeMe123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# Login as Candidate
CAND_TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"candidate@interview.local","password":"ChangeMe123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# Get User IDs
USERS=$(curl -s $BASE/users -H "Authorization: Bearer $TOKEN")
ADMIN_ID=$(echo $USERS | python3 -c "import sys,json; u=json.load(sys.stdin); print(next(x['id'] for x in u if x['email']=='admin@interview.local'))")
INTERVIEWER_ID=$(echo $USERS | python3 -c "import sys,json; u=json.load(sys.stdin); print(next(x['id'] for x in u if x['email']=='interviewer@interview.local'))")
CANDIDATE_ID=$(echo $USERS | python3 -c "import sys,json; u=json.load(sys.stdin); print(next(x['id'] for x in u if x['email']=='candidate@interview.local'))")
RECRUITER_ID=$(echo $USERS | python3 -c "import sys,json; u=json.load(sys.stdin); print(next(x['id'] for x in u if x['email']=='recruiter@interview.local'))")

echo "Admin: $ADMIN_ID | Interviewer: $INTERVIEWER_ID | Candidate: $CANDIDATE_ID | Recruiter: $RECRUITER_ID"
```

---

## 1. Authentication

**Base:** `/api/v1/auth`

### Register (Candidate)
```bash
curl -X POST $BASE/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "password": "Password@123"
  }'
```

### Register (Interviewer)
```bash
curl -X POST $BASE/auth/register/interviewer \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane.smith@example.com",
    "password": "Password@123"
  }'
```

### Admin Create User
```bash
curl -X POST $BASE/auth/admin/create-user \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Mike",
    "lastName": "Johnson",
    "email": "mike@example.com",
    "password": "Password@123",
    "roles": ["RECRUITER"]
  }'
```

### Login
```bash
curl -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@interview.local",
    "password": "ChangeMe123!"
  }'
```

### Refresh Token
```bash
curl -X POST $BASE/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<refresh-token-from-login>"
  }'
```

### Logout
```bash
curl -X POST $BASE/auth/logout \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<refresh-token>"
  }'
```

### Forgot Password
```bash
curl -X POST $BASE/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com"
  }'
```

### Reset Password
```bash
curl -X POST $BASE/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "<reset-token-from-email>",
    "newPassword": "NewPassword@123"
  }'
```

### Verify Email
```bash
curl -X GET "$BASE/auth/verify-email?token=<verification-token>"
```

### Resend Verification
```bash
curl -X POST $BASE/auth/resend-verification \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com"
  }'
```

### OAuth2 Providers
```bash
curl -X GET $BASE/auth/oauth2/providers
```

---

## 2. Users

**Base:** `/api/v1/users`

### Create User
```bash
curl -X POST $BASE/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Alice",
    "lastName": "Brown",
    "email": "alice@example.com",
    "password": "Password@123"
  }'
```

### Get Current User
```bash
curl -X GET $BASE/users/me \
  -H "Authorization: Bearer $TOKEN"
```

### Get All Users
```bash
curl -X GET $BASE/users \
  -H "Authorization: Bearer $TOKEN"
```

### Get User by ID
```bash
curl -X GET $BASE/users/$CANDIDATE_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Update User
```bash
curl -X PUT $BASE/users/$CANDIDATE_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Updated",
    "lastName": "Name",
    "phoneNumber": "+1234567890"
  }'
```

### Delete User (Soft)
```bash
curl -X DELETE $BASE/users/$CANDIDATE_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Get User Profile
```bash
curl -X GET $BASE/users/$CANDIDATE_ID/profile \
  -H "Authorization: Bearer $TOKEN"
```

### Update User Profile
```bash
curl -X PUT $BASE/users/$CANDIDATE_ID/profile \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "bio": "Senior Java Developer with 8 years experience",
    "designation": "Senior Engineer",
    "company": "TechCorp",
    "experience": 8,
    "skills": "Java, Spring Boot, Microservices, AWS",
    "linkedinUrl": "https://linkedin.com/in/candidate",
    "githubUrl": "https://github.com/candidate"
  }'
```

### Change Password
```bash
curl -X PUT $BASE/users/$CANDIDATE_ID/change-password \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "ChangeMe123!",
    "newPassword": "NewPassword@123"
  }'
```

### Update User Status
```bash
curl -X PATCH $BASE/users/$CANDIDATE_ID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "SUSPENDED"
  }'
```

### Search Users
```bash
curl -X GET "$BASE/users/search?keyword=admin&status=ACTIVE&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

### Assign Role to User
```bash
curl -X POST $BASE/users/$CANDIDATE_ID/roles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "roleId": "<role-uuid>"
  }'
```

### Get User Roles
```bash
curl -X GET $BASE/users/$CANDIDATE_ID/roles \
  -H "Authorization: Bearer $TOKEN"
```

### Remove Role from User
```bash
curl -X DELETE $BASE/users/$CANDIDATE_ID/roles/<roleId> \
  -H "Authorization: Bearer $TOKEN"
```

### Get User Permissions
```bash
curl -X GET $BASE/users/$CANDIDATE_ID/permissions \
  -H "Authorization: Bearer $TOKEN"
```

---

## 3. Roles

**Base:** `/api/v1/roles`

### Create Role
```bash
curl -X POST $BASE/roles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "HIRING_MANAGER",
    "description": "Can manage hiring decisions and approvals"
  }'
```

### Get All Roles
```bash
curl -X GET $BASE/roles \
  -H "Authorization: Bearer $TOKEN"
```

### Get Role by ID
```bash
curl -X GET $BASE/roles/<roleId> \
  -H "Authorization: Bearer $TOKEN"
```

### Update Role
```bash
curl -X PUT $BASE/roles/<roleId> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "HIRING_MANAGER",
    "description": "Updated description for hiring manager role"
  }'
```

### Delete Role
```bash
curl -X DELETE $BASE/roles/<roleId> \
  -H "Authorization: Bearer $TOKEN"
```

### Assign Permission to Role
```bash
curl -X POST $BASE/roles/<roleId>/permissions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "permissionId": "<permission-uuid>"
  }'
```

### Get Role Permissions
```bash
curl -X GET $BASE/roles/<roleId>/permissions \
  -H "Authorization: Bearer $TOKEN"
```

### Remove Permission from Role
```bash
curl -X DELETE $BASE/roles/permissions/<rolePermissionId> \
  -H "Authorization: Bearer $TOKEN"
```

---

## 4. Permissions

**Base:** `/api/v1/permissions`

### Create Permission
```bash
curl -X POST $BASE/permissions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "INTERVIEW_CREATE",
    "description": "Can create and schedule interviews"
  }'
```

### Get All Permissions
```bash
curl -X GET $BASE/permissions \
  -H "Authorization: Bearer $TOKEN"
```

### Get Permission by ID
```bash
curl -X GET $BASE/permissions/<permissionId> \
  -H "Authorization: Bearer $TOKEN"
```

### Update Permission
```bash
curl -X PUT $BASE/permissions/<permissionId> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "INTERVIEW_CREATE",
    "description": "Updated - Can create and schedule interviews"
  }'
```

### Delete Permission
```bash
curl -X DELETE $BASE/permissions/<permissionId> \
  -H "Authorization: Bearer $TOKEN"
```

---

## 5. Interviews

**Base:** `/api/v1/interviews`

### Create Interview
```bash
curl -X POST $BASE/interviews \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Senior Java Developer - Technical Round\",
    \"description\": \"DSA + System Design assessment\",
    \"candidateId\": \"$CANDIDATE_ID\",
    \"startTime\": \"2026-07-01T10:00:00Z\",
    \"endTime\": \"2026-07-01T11:00:00Z\",
    \"timeZone\": \"Asia/Kolkata\",
    \"type\": \"TECHNICAL\",
    \"mode\": \"ONLINE\",
    \"interviewerIds\": [\"$INTERVIEWER_ID\"]
  }"
```

### Get All Interviews
```bash
curl -X GET $BASE/interviews \
  -H "Authorization: Bearer $TOKEN"
```

### Get Interviews (Paginated)
```bash
curl -X GET "$BASE/interviews/paginated?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

### Get Interview by ID
```bash
curl -X GET $BASE/interviews/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Update Interview
```bash
curl -X PUT $BASE/interviews/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated - Senior Java Developer Round",
    "startTime": "2026-07-02T14:00:00Z",
    "endTime": "2026-07-02T15:00:00Z",
    "rescheduleReason": "Interviewer unavailable on original date"
  }'
```

### Delete Interview
```bash
curl -X DELETE $BASE/interviews/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Cancel Interview
```bash
curl -X PATCH $BASE/interviews/$INTERVIEW_ID/cancel \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cancelReason": "Position filled internally"
  }'
```

### Update Interview Status
```bash
curl -X PATCH "$BASE/interviews/$INTERVIEW_ID/status?status=COMPLETED" \
  -H "Authorization: Bearer $TOKEN"
```

### My Interviews (as Candidate)
```bash
curl -X GET $BASE/interviews/my/candidate \
  -H "Authorization: Bearer $CAND_TOKEN"
```

### My Interviews (as Interviewer)
```bash
curl -X GET $BASE/interviews/my/interviewer \
  -H "Authorization: Bearer $INT_TOKEN"
```

### Add Interviewer
```bash
curl -X POST "$BASE/interviews/$INTERVIEW_ID/interviewers/$INTERVIEWER_ID?isPrimary=true" \
  -H "Authorization: Bearer $TOKEN"
```

### Remove Interviewer
```bash
curl -X DELETE $BASE/interviews/$INTERVIEW_ID/interviewers/$INTERVIEWER_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Submit Feedback
```bash
curl -X POST $BASE/interviews/$INTERVIEW_ID/feedback \
  -H "Authorization: Bearer $INT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 4,
    "recommendation": "HIRE",
    "strengths": "Strong problem solving, clean code architecture",
    "weaknesses": "Could improve on time complexity optimization",
    "comments": "Solid candidate, recommend for next round"
  }'
```

### Get Interview Feedback
```bash
curl -X GET $BASE/interviews/$INTERVIEW_ID/feedback \
  -H "Authorization: Bearer $TOKEN"
```

### Filter by Status
```bash
curl -X GET "$BASE/interviews/filter/status?status=SCHEDULED" \
  -H "Authorization: Bearer $TOKEN"
```

### Filter by Date Range
```bash
curl -X GET "$BASE/interviews/filter/date-range?startDate=2026-06-01T00:00:00Z&endDate=2026-07-31T23:59:59Z" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 6. Templates

**Base:** `/api/v1/templates`

### Create Template
```bash
curl -X POST $BASE/templates \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Backend Engineer - Technical Interview",
    "description": "Standard template for backend technical interviews",
    "type": "TECHNICAL",
    "mode": "ONLINE",
    "durationMinutes": 60,
    "evaluationCriteria": "Problem solving, code quality, system design",
    "instructions": "Start with warm-up, then DSA, then system design",
    "tags": "backend,java,senior"
  }'
```

### Get All Templates
```bash
curl -X GET $BASE/templates \
  -H "Authorization: Bearer $TOKEN"
```

### Get Template by ID
```bash
curl -X GET $BASE/templates/<templateId> \
  -H "Authorization: Bearer $TOKEN"
```

### Update Template
```bash
curl -X PUT $BASE/templates/<templateId> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Backend Engineer Template",
    "durationMinutes": 90
  }'
```

### Delete Template
```bash
curl -X DELETE $BASE/templates/<templateId> \
  -H "Authorization: Bearer $TOKEN"
```

### Add Question to Template
```bash
curl -X POST $BASE/templates/<templateId>/questions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "questionId": "<question-uuid>",
    "orderIndex": 1,
    "isMandatory": true,
    "timeAllocationMinutes": 20,
    "notes": "Start with this warm-up question"
  }'
```

### Remove Question from Template
```bash
curl -X DELETE $BASE/templates/<templateId>/questions/<templateQuestionId> \
  -H "Authorization: Bearer $TOKEN"
```

### Create Interview from Template
```bash
curl -X POST $BASE/templates/create-interview \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"templateId\": \"<template-uuid>\",
    \"candidateId\": \"$CANDIDATE_ID\",
    \"startTime\": \"2026-07-05T10:00:00Z\",
    \"endTime\": \"2026-07-05T11:00:00Z\",
    \"timeZone\": \"Asia/Kolkata\",
    \"interviewerIds\": [\"$INTERVIEWER_ID\"]
  }"
```

---

## 7. Question Bank

**Base:** `/api/v1/questions`

### Create Category
```bash
curl -X POST $BASE/questions/categories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Microservices",
    "description": "Questions about microservice architecture patterns"
  }'
```

### Get All Categories
```bash
curl -X GET $BASE/questions/categories \
  -H "Authorization: Bearer $TOKEN"
```

### Create Question
```bash
curl -X POST $BASE/questions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Reverse a Linked List",
    "description": "Given the head of a singly linked list, reverse the list and return it.",
    "categoryId": "<category-uuid>",
    "difficulty": "EASY",
    "type": "CODING",
    "expectedDurationMinutes": 15,
    "sampleAnswer": "Iterative: use three pointers. O(n) time, O(1) space.",
    "hints": "Think about what pointers you need to track",
    "tags": "linked-list,pointers,recursion"
  }'
```

### Get Question by ID
```bash
curl -X GET $BASE/questions/<questionId> \
  -H "Authorization: Bearer $TOKEN"
```

### Update Question
```bash
curl -X PUT $BASE/questions/<questionId> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Reverse a Singly Linked List",
    "difficulty": "MEDIUM"
  }'
```

### Delete Question
```bash
curl -X DELETE $BASE/questions/<questionId> \
  -H "Authorization: Bearer $TOKEN"
```

### Search Questions
```bash
curl -X GET "$BASE/questions/search?difficulty=MEDIUM&type=CODING&keyword=linked&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### Get Questions by Category
```bash
curl -X GET $BASE/questions/category/<categoryId> \
  -H "Authorization: Bearer $TOKEN"
```

---

## 8. Code Editor

**Base:** `/api/v1/interviews/{interviewId}/code`

### Start Coding Session
```bash
curl -X POST "$BASE/interviews/$INTERVIEW_ID/code/start?language=java" \
  -H "Authorization: Bearer $TOKEN"
```

### Get Active Session
```bash
curl -X GET $BASE/interviews/$INTERVIEW_ID/code \
  -H "Authorization: Bearer $TOKEN"
```

### Save Code
```bash
curl -X PUT $BASE/interviews/$INTERVIEW_ID/code/save \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"code\": \"public class Solution {\\n    public ListNode reverseList(ListNode head) {\\n        ListNode prev = null;\\n        ListNode curr = head;\\n        while (curr != null) {\\n            ListNode next = curr.next;\\n            curr.next = prev;\\n            prev = curr;\\n            curr = next;\\n        }\\n        return prev;\\n    }\\n}\",
    \"language\": \"java\",
    \"userId\": \"$CANDIDATE_ID\"
  }"
```

### End Session
```bash
curl -X POST $BASE/interviews/$INTERVIEW_ID/code/end \
  -H "Authorization: Bearer $TOKEN"
```

### Get Session History
```bash
curl -X GET $BASE/interviews/$INTERVIEW_ID/code/history \
  -H "Authorization: Bearer $TOKEN"
```

---

## 9. Code Execution

**Base:** `/api/v1/code-execution`

### Run Code
```bash
curl -X POST $BASE/code-execution/run \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "codingSessionId": "<session-uuid>",
    "language": "PYTHON",
    "sourceCode": "def two_sum(nums, target):\n    seen = {}\n    for i, n in enumerate(nums):\n        if target - n in seen:\n            return [seen[target-n], i]\n        seen[n] = i\n\nprint(two_sum([2,7,11,15], 9))",
    "stdin": "",
    "timeoutMs": 10000
  }'
```

### Get Execution Result
```bash
curl -X GET $BASE/code-execution/<executionId> \
  -H "Authorization: Bearer $TOKEN"
```

### Get Executions for Session
```bash
curl -X GET $BASE/code-execution/session/<codingSessionId> \
  -H "Authorization: Bearer $TOKEN"
```

### Get Supported Languages
```bash
curl -X GET $BASE/code-execution/languages \
  -H "Authorization: Bearer $TOKEN"
```

---

## 10. Meeting

**Base:** `/api/v1/interviews/{interviewId}/meeting`

### Generate Meeting Link
```bash
curl -X POST $BASE/interviews/$INTERVIEW_ID/meeting \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "INTERNAL",
    "durationMinutes": 60
  }'
```

### Get Meeting Link
```bash
curl -X GET $BASE/interviews/$INTERVIEW_ID/meeting \
  -H "Authorization: Bearer $TOKEN"
```

---

## 11. Calendar

**Base:** `/api/v1/calendar`

### Add Availability
```bash
curl -X POST $BASE/calendar/interviewers/$INTERVIEWER_ID/availability \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "dayOfWeek": 1,
    "startTime": "09:00",
    "endTime": "12:00",
    "timeZone": "Asia/Kolkata",
    "isRecurring": true
  }'
```

### Get Availability
```bash
curl -X GET $BASE/calendar/interviewers/$INTERVIEWER_ID/availability \
  -H "Authorization: Bearer $TOKEN"
```

### Check Availability (with conflicts)
```bash
curl -X GET "$BASE/calendar/interviewers/$INTERVIEWER_ID/availability/check?date=2026-07-01" \
  -H "Authorization: Bearer $TOKEN"
```

### Remove Availability
```bash
curl -X DELETE $BASE/calendar/interviewers/$INTERVIEWER_ID/availability/<slotId> \
  -H "Authorization: Bearer $TOKEN"
```

---

## 12. Scheduling

**Base:** `/api/v1/scheduling`

### Add Availability Slot
```bash
curl -X POST $BASE/scheduling/availability \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "dayOfWeek": 2,
    "startTime": "10:00",
    "endTime": "16:00",
    "timeZone": "America/New_York",
    "isRecurring": true
  }'
```

### Get My Availability
```bash
curl -X GET $BASE/scheduling/availability/my \
  -H "Authorization: Bearer $TOKEN"
```

### Get User Availability (Admin)
```bash
curl -X GET $BASE/scheduling/availability/user/$INTERVIEWER_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Delete Availability Slot
```bash
curl -X DELETE $BASE/scheduling/availability/<slotId> \
  -H "Authorization: Bearer $TOKEN"
```

### Auto-Suggest Time Slots
```bash
curl -X POST $BASE/scheduling/suggest \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"interviewerIds\": [\"$INTERVIEWER_ID\"],
    \"candidateId\": \"$CANDIDATE_ID\",
    \"fromDate\": \"2026-07-01\",
    \"toDate\": \"2026-07-07\",
    \"durationMinutes\": 60,
    \"preferredTimeZone\": \"Asia/Kolkata\"
  }"
```

---

## 13. Reminders

**Base:** `/api/v1/reminders`

### Create Reminders for Interview
```bash
curl -X POST $BASE/reminders/interview/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Get Reminders for Interview
```bash
curl -X GET $BASE/reminders/interview/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Get My Reminders
```bash
curl -X GET $BASE/reminders/my \
  -H "Authorization: Bearer $TOKEN"
```

### Cancel All Reminders
```bash
curl -X DELETE $BASE/reminders/interview/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

## 14. Self-Service

**Base:** `/api/v1/self-service`

### Submit Preferred Slots
```bash
curl -X POST $BASE/self-service/preferred-slots \
  -H "Authorization: Bearer $CAND_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "preferredDate": "2026-07-03",
    "startTime": "10:00",
    "endTime": "11:00",
    "timeZone": "Asia/Kolkata",
    "priority": 1,
    "notes": "Morning works best",
    "jobPositionId": "<position-uuid>"
  }'
```

### Get My Preferred Slots
```bash
curl -X GET $BASE/self-service/preferred-slots/my \
  -H "Authorization: Bearer $CAND_TOKEN"
```

### Get Slots for Interview (Recruiter)
```bash
curl -X GET $BASE/self-service/preferred-slots/interview/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Accept/Reject Slot
```bash
curl -X PATCH "$BASE/self-service/preferred-slots/<slotId>/status?status=ACCEPTED" \
  -H "Authorization: Bearer $TOKEN"
```

### Delete Preferred Slot
```bash
curl -X DELETE $BASE/self-service/preferred-slots/<slotId> \
  -H "Authorization: Bearer $CAND_TOKEN"
```

---

## 15. Dashboard

**Base:** `/api/v1/dashboard`

### Admin Dashboard
```bash
curl -X GET $BASE/dashboard/admin \
  -H "Authorization: Bearer $TOKEN"
```

### Interviewer Dashboard
```bash
curl -X GET $BASE/dashboard/interviewer \
  -H "Authorization: Bearer $INT_TOKEN"
```

### Specific Interviewer Dashboard (Admin)
```bash
curl -X GET $BASE/dashboard/interviewer/$INTERVIEWER_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Candidate Dashboard
```bash
curl -X GET $BASE/dashboard/candidate \
  -H "Authorization: Bearer $CAND_TOKEN"
```

---

## 16. Notifications

**Base:** `/api/v1/notifications`

### Get All Notifications
```bash
curl -X GET $BASE/notifications \
  -H "Authorization: Bearer $TOKEN"
```

### Get Unread Notifications
```bash
curl -X GET $BASE/notifications/unread \
  -H "Authorization: Bearer $TOKEN"
```

### Get Unread Count
```bash
curl -X GET $BASE/notifications/count \
  -H "Authorization: Bearer $TOKEN"
```

### Mark as Read
```bash
curl -X PATCH $BASE/notifications/<notificationId>/read \
  -H "Authorization: Bearer $TOKEN"
```

### Mark All as Read
```bash
curl -X PATCH $BASE/notifications/read-all \
  -H "Authorization: Bearer $TOKEN"
```

---

## 17. Pipelines

**Base:** `/api/v1/pipelines`

### Create Pipeline
```bash
curl -X POST $BASE/pipelines \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Backend Engineer Pipeline",
    "description": "Standard hiring pipeline for backend roles",
    "department": "Engineering",
    "stages": [
      {"name": "Phone Screening", "orderIndex": 1, "interviewType": "SCREENING", "durationMinutes": 30},
      {"name": "Technical Round", "orderIndex": 2, "interviewType": "TECHNICAL", "durationMinutes": 60},
      {"name": "System Design", "orderIndex": 3, "interviewType": "TECHNICAL", "durationMinutes": 60},
      {"name": "HR Round", "orderIndex": 4, "interviewType": "HR", "durationMinutes": 45},
      {"name": "Final Decision", "orderIndex": 5, "interviewType": "FINAL", "durationMinutes": 30}
    ]
  }'
```

### Get All Pipelines
```bash
curl -X GET $BASE/pipelines \
  -H "Authorization: Bearer $TOKEN"
```

### Get Pipeline by ID
```bash
curl -X GET $BASE/pipelines/<pipelineId> \
  -H "Authorization: Bearer $TOKEN"
```

### Get Pipelines by Department
```bash
curl -X GET $BASE/pipelines/department/Engineering \
  -H "Authorization: Bearer $TOKEN"
```

### Add Candidate to Pipeline
```bash
curl -X POST $BASE/pipelines/candidates \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"pipelineId\": \"<pipeline-uuid>\",
    \"candidateId\": \"$CANDIDATE_ID\",
    \"notes\": \"Strong referral from team lead\"
  }"
```

### Advance Candidate
```bash
curl -X POST "$BASE/pipelines/candidates/<candidatePipelineId>/advance" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "feedback": "Passed technical round with flying colors"
  }'
```

### Reject Candidate
```bash
curl -X POST "$BASE/pipelines/candidates/<candidatePipelineId>/reject" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Did not meet minimum technical bar"
  }'
```

### Update Stage Progress
```bash
curl -X PATCH $BASE/pipelines/candidates/<candidatePipelineId>/stages/<stageId> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "COMPLETED",
    "feedback": "Excellent system design skills"
  }'
```

---

## 18. Scorecards

**Base:** `/api/v1/scorecards`

### Create Evaluation Criteria
```bash
curl -X POST $BASE/scorecards/criteria \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Problem Solving",
    "description": "Ability to break down complex problems",
    "interviewType": "TECHNICAL",
    "maxScore": 5,
    "weight": 2.0,
    "orderIndex": 1
  }'
```

### Get All Criteria
```bash
curl -X GET $BASE/scorecards/criteria \
  -H "Authorization: Bearer $TOKEN"
```

### Submit Scorecard
```bash
curl -X POST $BASE/scorecards \
  -H "Authorization: Bearer $INT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"interviewId\": \"$INTERVIEW_ID\",
    \"recommendation\": \"HIRE\",
    \"overallComments\": \"Strong candidate\",
    \"strengths\": \"Excellent coding skills\",
    \"weaknesses\": \"Could improve communication\",
    \"entries\": [
      {\"criteriaId\": \"<criteria-uuid>\", \"score\": 4, \"comments\": \"Good approach\"},
      {\"criteriaId\": \"<criteria-uuid-2>\", \"score\": 5, \"comments\": \"Excellent\"}
    ]
  }"
```

### Get Scorecards by Interview
```bash
curl -X GET $BASE/scorecards/interview/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Get Candidate Summary
```bash
curl -X GET $BASE/scorecards/interview/$INTERVIEW_ID/summary \
  -H "Authorization: Bearer $TOKEN"
```

---

## 19. Job Positions

**Base:** `/api/v1/job-positions`

### Create Job Position
```bash
curl -X POST $BASE/job-positions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Senior Backend Engineer",
    "department": "Engineering",
    "location": "Remote (US)",
    "employmentType": "FULL_TIME",
    "experienceLevel": "SENIOR",
    "description": "We are looking for a Senior Backend Engineer to lead API development",
    "requirements": "5+ years Java/Spring Boot, microservices, AWS",
    "responsibilities": "Design and implement scalable backend services",
    "salaryMin": 150000,
    "salaryMax": 200000,
    "salaryCurrency": "USD",
    "numberOfOpenings": 2,
    "skills": "Java, Spring Boot, PostgreSQL, Kafka, AWS"
  }'
```

### Get All Job Positions
```bash
curl -X GET $BASE/job-positions \
  -H "Authorization: Bearer $TOKEN"
```

### Search Job Positions
```bash
curl -X GET "$BASE/job-positions/search?keyword=backend&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

### Filter by Status
```bash
curl -X GET "$BASE/job-positions/filter/status?status=OPEN" \
  -H "Authorization: Bearer $TOKEN"
```

### Update Status
```bash
curl -X PATCH "$BASE/job-positions/<positionId>/status?status=CLOSED" \
  -H "Authorization: Bearer $TOKEN"
```

### Link Interview to Position
```bash
curl -X POST $BASE/job-positions/<positionId>/interviews/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

## 20. Documents

**Base:** `/api/v1/documents`

### Upload Document
```bash
curl -X POST $BASE/documents \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@resume.pdf" \
  -F "documentType=RESUME" \
  -F "entityType=candidate" \
  -F "entityId=$CANDIDATE_ID"
```

### Get Document Metadata
```bash
curl -X GET $BASE/documents/<documentId> \
  -H "Authorization: Bearer $TOKEN"
```

### Get Download URL (Presigned)
```bash
curl -X GET $BASE/documents/<documentId>/download-url \
  -H "Authorization: Bearer $TOKEN"
```

### Get My Documents
```bash
curl -X GET $BASE/documents/my \
  -H "Authorization: Bearer $TOKEN"
```

### Get Presigned Upload URL
```bash
curl -X POST "$BASE/documents/presigned-upload?fileName=report.pdf&contentType=application/pdf&documentType=ASSESSMENT" \
  -H "Authorization: Bearer $TOKEN"
```

### Delete Document
```bash
curl -X DELETE $BASE/documents/<documentId> \
  -H "Authorization: Bearer $TOKEN"
```

---

## 21. Bulk Operations

**Base:** `/api/v1/bulk`

### Bulk Schedule Interviews
```bash
curl -X POST $BASE/bulk/interviews/schedule \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"interviews\": [
      {
        \"title\": \"Tech Interview - Candidate A\",
        \"candidateId\": \"$CANDIDATE_ID\",
        \"interviewerIds\": [\"$INTERVIEWER_ID\"],
        \"startTime\": \"2026-07-01T10:00:00Z\",
        \"endTime\": \"2026-07-01T11:00:00Z\",
        \"type\": \"TECHNICAL\",
        \"mode\": \"ONLINE\",
        \"timeZone\": \"Asia/Kolkata\"
      }
    ]
  }"
```

### Bulk Invite Candidates
```bash
curl -X POST $BASE/bulk/candidates/invite \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"interviewId\": \"$INTERVIEW_ID\",
    \"candidates\": [
      {\"candidateId\": \"$CANDIDATE_ID\", \"customMessage\": \"Looking forward to meeting you!\"},
      {\"email\": \"external@example.com\", \"firstName\": \"Bob\", \"lastName\": \"Jones\"}
    ]
  }"
```

### Bulk Export
```bash
curl -X GET "$BASE/bulk/export?exportType=INTERVIEWS&format=CSV&statusFilter=COMPLETED" \
  -H "Authorization: Bearer $TOKEN" \
  --output interviews.csv
```

---

## 22. Reports & Analytics

**Base:** `/api/v1/reports`

### Get Analytics Report
```bash
curl -X GET "$BASE/reports/analytics?fromDate=2026-01-01T00:00:00Z&toDate=2026-12-31T23:59:59Z" \
  -H "Authorization: Bearer $TOKEN"
```

### Interviewer Performance
```bash
curl -X GET $BASE/reports/analytics/interviewer/$INTERVIEWER_ID \
  -H "Authorization: Bearer $TOKEN"
```

### PDF Report - Analytics
```bash
curl -X GET "$BASE/reports/pdf/analytics?fromDate=2026-01-01T00:00:00Z" \
  -H "Authorization: Bearer $TOKEN" \
  --output analytics_report.pdf
```

### PDF Report - Interviewer
```bash
curl -X GET $BASE/reports/pdf/interviewer/$INTERVIEWER_ID \
  -H "Authorization: Bearer $TOKEN" \
  --output interviewer_report.pdf
```

### Conversion Metrics
```bash
curl -X GET $BASE/reports/metrics/conversion \
  -H "Authorization: Bearer $TOKEN"
```

### Time-to-Hire Metrics
```bash
curl -X GET $BASE/reports/metrics/time-to-hire \
  -H "Authorization: Bearer $TOKEN"
```

---

## 23. Teams

**Base:** `/api/v1/teams`

### Create Team
```bash
curl -X POST $BASE/teams \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"Backend Engineering\",
    \"description\": \"Backend interview panel\",
    \"department\": \"Engineering\",
    \"managerId\": \"$ADMIN_ID\"
  }"
```

### Get All Teams
```bash
curl -X GET $BASE/teams \
  -H "Authorization: Bearer $TOKEN"
```

### Get Team by ID
```bash
curl -X GET $BASE/teams/<teamId> \
  -H "Authorization: Bearer $TOKEN"
```

### Get Teams by Department
```bash
curl -X GET $BASE/teams/department/Engineering \
  -H "Authorization: Bearer $TOKEN"
```

### Add Member
```bash
curl -X POST "$BASE/teams/<teamId>/members/$INTERVIEWER_ID?role=LEAD" \
  -H "Authorization: Bearer $TOKEN"
```

### Update Member Role
```bash
curl -X PATCH "$BASE/teams/<teamId>/members/$INTERVIEWER_ID/role?role=MEMBER" \
  -H "Authorization: Bearer $TOKEN"
```

### Remove Member
```bash
curl -X DELETE $BASE/teams/<teamId>/members/$INTERVIEWER_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

## 24. Tags

**Base:** `/api/v1/tags`

### Create Tag
```bash
curl -X POST $BASE/tags \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "urgent-hire",
    "color": "#FF0000",
    "category": "INTERVIEW"
  }'
```

### Get All Tags
```bash
curl -X GET $BASE/tags \
  -H "Authorization: Bearer $TOKEN"
```

### Search Tags
```bash
curl -X GET "$BASE/tags/search?query=urgent" \
  -H "Authorization: Bearer $TOKEN"
```

### Tag an Entity
```bash
curl -X POST $BASE/tags/<tagId>/entities/INTERVIEW/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Get Tags for Entity
```bash
curl -X GET $BASE/tags/entities/INTERVIEW/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Remove Tag from Entity
```bash
curl -X DELETE $BASE/tags/<tagId>/entities/INTERVIEW/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

## 25. AI Features

**Base:** `/api/v1/ai`

### Suggest Questions
```bash
curl -X POST $BASE/ai/suggest-questions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "jobTitle": "Senior Java Developer",
    "difficulty": "MEDIUM",
    "category": "CODING",
    "skills": ["Java", "Spring Boot", "Microservices"],
    "count": 5
  }'
```

### Parse Resume
```bash
curl -X POST $BASE/ai/parse-resume \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "<uploaded-resume-uuid>"
  }'
```

### Generate Interview Summary
```bash
curl -X POST $BASE/ai/interview-summary \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"interviewId\": \"$INTERVIEW_ID\"
  }"
```

### Get Suggestion History
```bash
curl -X GET "$BASE/ai/suggestions?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

### Accept/Reject Suggestion
```bash
curl -X PATCH $BASE/ai/suggestions/<suggestionId>/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "ACCEPTED"}'
```

---

## 26. Video Recording

**Base:** `/api/v1/video-recordings`

### Start Recording
```bash
curl -X POST $BASE/video-recordings/start \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"interviewId\": \"$INTERVIEW_ID\"
  }"
```

### Complete Recording
```bash
curl -X PATCH $BASE/video-recordings/<recordingId>/complete \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fileSizeBytes": 52428800,
    "durationSeconds": 3600
  }'
```

### Mark Recording Failed
```bash
curl -X PATCH $BASE/video-recordings/<recordingId>/fail \
  -H "Authorization: Bearer $TOKEN"
```

### Get Recordings for Interview
```bash
curl -X GET $BASE/video-recordings/interview/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Get Single Recording (with download URL)
```bash
curl -X GET $BASE/video-recordings/<recordingId> \
  -H "Authorization: Bearer $TOKEN"
```

### Delete Recording
```bash
curl -X DELETE $BASE/video-recordings/<recordingId> \
  -H "Authorization: Bearer $TOKEN"
```

### My Recordings
```bash
curl -X GET "$BASE/video-recordings/my?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 27. Whiteboard

**Base:** `/api/v1/whiteboards`

### Create Session
```bash
curl -X POST $BASE/whiteboards \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"interviewId\": \"$INTERVIEW_ID\",
    \"title\": \"System Design - URL Shortener\"
  }"
```

### Get Session
```bash
curl -X GET $BASE/whiteboards/<sessionId> \
  -H "Authorization: Bearer $TOKEN"
```

### Get Sessions for Interview
```bash
curl -X GET $BASE/whiteboards/interview/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Add Stroke
```bash
curl -X POST $BASE/whiteboards/<sessionId>/strokes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "strokeData": "{\"points\": [[0,0],[50,50],[100,100]]}",
    "tool": "PEN",
    "color": "#FF0000",
    "strokeWidth": 3.0
  }'
```

### Get All Strokes
```bash
curl -X GET $BASE/whiteboards/<sessionId>/strokes \
  -H "Authorization: Bearer $TOKEN"
```

### Save Snapshot
```bash
curl -X PUT $BASE/whiteboards/<sessionId>/snapshot \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "snapshotData": "<base64-encoded-canvas-state>"
  }'
```

### Close Session
```bash
curl -X PATCH $BASE/whiteboards/<sessionId>/close \
  -H "Authorization: Bearer $TOKEN"
```

### Delete Session
```bash
curl -X DELETE $BASE/whiteboards/<sessionId> \
  -H "Authorization: Bearer $TOKEN"
```

---

## 28. Webhooks

**Base:** `/api/v1/webhooks`

### Register Webhook
```bash
curl -X POST $BASE/webhooks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://your-server.com/webhook",
    "description": "Interview event notifications",
    "events": ["INTERVIEW_SCHEDULED", "INTERVIEW_COMPLETED", "FEEDBACK_SUBMITTED", "CANDIDATE_HIRED"]
  }'
```

### Get My Webhooks
```bash
curl -X GET $BASE/webhooks \
  -H "Authorization: Bearer $TOKEN"
```

### Get Webhook Details
```bash
curl -X GET $BASE/webhooks/<webhookId> \
  -H "Authorization: Bearer $TOKEN"
```

### Update Webhook
```bash
curl -X PUT $BASE/webhooks/<webhookId> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://your-server.com/webhook/v2",
    "events": ["INTERVIEW_SCHEDULED", "INTERVIEW_COMPLETED"]
  }'
```

### Regenerate Secret
```bash
curl -X POST $BASE/webhooks/<webhookId>/regenerate-secret \
  -H "Authorization: Bearer $TOKEN"
```

### Get Delivery History
```bash
curl -X GET "$BASE/webhooks/<webhookId>/deliveries?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### Retry Failed Delivery
```bash
curl -X POST $BASE/webhooks/deliveries/<deliveryId>/retry \
  -H "Authorization: Bearer $TOKEN"
```

### Delete Webhook
```bash
curl -X DELETE $BASE/webhooks/<webhookId> \
  -H "Authorization: Bearer $TOKEN"
```

---

## 29. Organizations (Multi-Tenant)

**Base:** `/api/v1/organizations`

### Create Organization
```bash
curl -X POST $BASE/organizations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corp",
    "slug": "acme-corp",
    "domain": "acme.com",
    "plan": "PROFESSIONAL"
  }'
```

### Get Organization
```bash
curl -X GET $BASE/organizations/<orgId> \
  -H "Authorization: Bearer $TOKEN"
```

### Update Organization
```bash
curl -X PUT $BASE/organizations/<orgId> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corporation",
    "plan": "ENTERPRISE"
  }'
```

### My Organizations
```bash
curl -X GET $BASE/organizations/my \
  -H "Authorization: Bearer $TOKEN"
```

### Add Member
```bash
curl -X POST $BASE/organizations/<orgId>/members \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$INTERVIEWER_ID\",
    \"role\": \"MEMBER\"
  }"
```

### List Members
```bash
curl -X GET $BASE/organizations/<orgId>/members \
  -H "Authorization: Bearer $TOKEN"
```

### Update Member Role
```bash
curl -X PATCH $BASE/organizations/<orgId>/members/$INTERVIEWER_ID/role \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"role": "ADMIN"}'
```

### Remove Member
```bash
curl -X DELETE $BASE/organizations/<orgId>/members/$INTERVIEWER_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Delete Organization
```bash
curl -X DELETE $BASE/organizations/<orgId> \
  -H "Authorization: Bearer $TOKEN"
```

---

## 30. Candidate Feedback

**Base:** `/api/v1/candidate-feedback`

### Submit Feedback (as Candidate)
```bash
curl -X POST $BASE/candidate-feedback \
  -H "Authorization: Bearer $CAND_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"interviewId\": \"$INTERVIEW_ID\",
    \"overallRating\": 4,
    \"communicationRating\": 5,
    \"professionalismRating\": 4,
    \"technicalClarityRating\": 3,
    \"timelinessRating\": 5,
    \"comments\": \"Great interview experience, well-structured questions\",
    \"wouldRecommend\": true,
    \"isAnonymous\": false
  }"
```

### Get Feedback for Interview
```bash
curl -X GET $BASE/candidate-feedback/interview/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Get Aggregate Summary
```bash
curl -X GET $BASE/candidate-feedback/summary \
  -H "Authorization: Bearer $TOKEN"
```

### Get My Feedback
```bash
curl -X GET "$BASE/candidate-feedback/my?page=0&size=10" \
  -H "Authorization: Bearer $CAND_TOKEN"
```

---

## 31. Activity Feed

**Base:** `/api/v1/activities`

### Global Activity Feed
```bash
curl -X GET "$BASE/activities?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### Entity Timeline
```bash
curl -X GET $BASE/activities/entity/INTERVIEW/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

### User Activity
```bash
curl -X GET $BASE/activities/user/$INTERVIEWER_ID \
  -H "Authorization: Bearer $TOKEN"
```

### My Activity
```bash
curl -X GET "$BASE/activities/my?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### Filtered Activity
```bash
curl -X POST $BASE/activities/filter \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "entityType": "INTERVIEW",
    "action": "SCHEDULED",
    "startDate": "2026-01-01T00:00:00Z",
    "endDate": "2026-12-31T23:59:59Z"
  }'
```

---

## 32. Export/Import

**Base:** `/api/v1/export-import`

### Start Export
```bash
curl -X POST $BASE/export-import/export \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "entityType": "INTERVIEWS",
    "format": "CSV",
    "filters": {"status": "COMPLETED"}
  }'
```

### Start Import
```bash
curl -X POST $BASE/export-import/import \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "entityType": "CANDIDATES",
    "fileDocumentId": "<uploaded-csv-uuid>"
  }'
```

### Get My Jobs
```bash
curl -X GET "$BASE/export-import/jobs?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

### Get Job Status
```bash
curl -X GET $BASE/export-import/jobs/<jobId> \
  -H "Authorization: Bearer $TOKEN"
```

### Cancel Job
```bash
curl -X DELETE $BASE/export-import/jobs/<jobId> \
  -H "Authorization: Bearer $TOKEN"
```

---

## 33. SSO/SAML

**Base:** `/api/v1/sso`

### Create SSO Configuration
```bash
curl -X POST $BASE/sso \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "<org-uuid>",
    "providerType": "OKTA",
    "entityId": "https://your-okta.okta.com/app/exk123",
    "ssoUrl": "https://your-okta.okta.com/app/exk123/sso/saml",
    "certificate": "<base64-x509-cert>",
    "metadataUrl": "https://your-okta.okta.com/app/exk123/sso/saml/metadata",
    "enabled": true
  }'
```

### Get SSO Configuration
```bash
curl -X GET $BASE/sso/<configId> \
  -H "Authorization: Bearer $TOKEN"
```

### Get All SSO for Tenant
```bash
curl -X GET $BASE/sso/tenant/<tenantId> \
  -H "Authorization: Bearer $TOKEN"
```

### Toggle SSO Config
```bash
curl -X PATCH $BASE/sso/<configId>/toggle \
  -H "Authorization: Bearer $TOKEN"
```

### Get Login URLs (Public)
```bash
curl -X GET $BASE/sso/tenant/<tenantId>/login-urls
```

### Delete SSO Configuration
```bash
curl -X DELETE $BASE/sso/<configId> \
  -H "Authorization: Bearer $TOKEN"
```

---

## 34. Account Security

**Base:** `/api/v1/security`

### Get Lockout Status
```bash
curl -X GET $BASE/security/lockout/user@example.com \
  -H "Authorization: Bearer $TOKEN"
```

### Unlock Account
```bash
curl -X POST $BASE/security/lockout/user@example.com/unlock \
  -H "Authorization: Bearer $TOKEN"
```

### Get Blocked IPs
```bash
curl -X GET $BASE/security/blocked-ips \
  -H "Authorization: Bearer $TOKEN"
```

### Block IP
```bash
curl -X POST $BASE/security/block-ip \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "ipAddress": "192.168.1.100",
    "reason": "Brute force attempt detected"
  }'
```

### Unblock IP
```bash
curl -X POST $BASE/security/unblock-ip/192.168.1.100 \
  -H "Authorization: Bearer $TOKEN"
```

### Get Login Attempts
```bash
curl -X GET $BASE/security/login-attempts/user@example.com \
  -H "Authorization: Bearer $TOKEN"
```

---

## 35. MFA

**Base:** `/api/v1/mfa`

### Enable MFA
```bash
curl -X POST $BASE/mfa/enable \
  -H "Authorization: Bearer $TOKEN"
```

### Verify MFA Setup
```bash
curl -X POST $BASE/mfa/verify \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "123456"
  }'
```

### Disable MFA
```bash
curl -X POST $BASE/mfa/disable \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "123456"
  }'
```

### Get MFA Status
```bash
curl -X GET $BASE/mfa/status \
  -H "Authorization: Bearer $TOKEN"
```

---

## 36. API Keys

**Base:** `/api/v1/api-keys`

### Create API Key
```bash
curl -X POST $BASE/api-keys \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CI/CD Pipeline",
    "scopes": ["READ_INTERVIEWS", "READ_USERS"],
    "expiresInDays": 90
  }'
```

### List API Keys
```bash
curl -X GET $BASE/api-keys \
  -H "Authorization: Bearer $TOKEN"
```

### Revoke API Key
```bash
curl -X DELETE $BASE/api-keys/<keyId> \
  -H "Authorization: Bearer $TOKEN"
```

---

## 37. GDPR

**Base:** `/api/v1/gdpr`

### Record Consent
```bash
curl -X POST $BASE/gdpr/consent \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "consentType": "DATA_PROCESSING",
    "granted": true,
    "details": "Consent for interview data processing"
  }'
```

### Get My Consents
```bash
curl -X GET $BASE/gdpr/consent \
  -H "Authorization: Bearer $TOKEN"
```

### Request Data Export
```bash
curl -X POST $BASE/gdpr/data-export \
  -H "Authorization: Bearer $TOKEN"
```

### Request Data Erasure
```bash
curl -X POST $BASE/gdpr/erasure \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "No longer using the platform"
  }'
```

### Get Erasure Requests (Admin)
```bash
curl -X GET $BASE/gdpr/erasure/requests \
  -H "Authorization: Bearer $TOKEN"
```

---

## 38. Job Board (Public)

**Base:** `/api/v1/jobs` (No auth required)

### List Public Jobs
```bash
curl -X GET "$BASE/jobs?page=0&size=10"
```

### Get Job Detail
```bash
curl -X GET $BASE/jobs/<jobId>
```

### Search Jobs
```bash
curl -X GET "$BASE/jobs/search?keyword=backend&department=Engineering&location=Remote&employmentType=FULL_TIME&experienceLevel=SENIOR"
```

---

## 39. Candidate Portal

**Base:** `/api/v1/portal`

### Submit Application
```bash
curl -X POST $BASE/portal/applications \
  -H "Authorization: Bearer $CAND_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "jobPositionId": "<position-uuid>",
    "coverLetter": "I am excited to apply for this role...",
    "resumeDocumentId": "<uploaded-resume-uuid>",
    "source": "CAREER_PAGE"
  }'
```

### Get My Applications
```bash
curl -X GET $BASE/portal/applications \
  -H "Authorization: Bearer $CAND_TOKEN"
```

### Get Application Detail
```bash
curl -X GET $BASE/portal/applications/<applicationId> \
  -H "Authorization: Bearer $CAND_TOKEN"
```

### Withdraw Application
```bash
curl -X DELETE $BASE/portal/applications/<applicationId>/withdraw \
  -H "Authorization: Bearer $CAND_TOKEN"
```

### Get Applications for Position (Admin)
```bash
curl -X GET $BASE/portal/admin/applications/position/<positionId> \
  -H "Authorization: Bearer $TOKEN"
```

### Update Application Status (Admin)
```bash
curl -X PATCH $BASE/portal/admin/applications/<applicationId>/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "SHORTLISTED"}'
```

---

## 40. Offer Letters

**Base:** `/api/v1/offers`

### Create Offer
```bash
curl -X POST $BASE/offers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"candidateId\": \"$CANDIDATE_ID\",
    \"jobPositionId\": \"<position-uuid>\",
    \"salary\": 175000,
    \"currency\": \"USD\",
    \"startDate\": \"2026-08-01\",
    \"expiryDate\": \"2026-07-15\",
    \"benefits\": \"Health, dental, 401k, unlimited PTO\",
    \"notes\": \"Senior Backend Engineer offer\",
    \"approverIds\": [\"$ADMIN_ID\"]
  }"
```

### Get Offer
```bash
curl -X GET $BASE/offers/<offerId> \
  -H "Authorization: Bearer $TOKEN"
```

### Submit for Approval
```bash
curl -X POST $BASE/offers/<offerId>/submit-approval \
  -H "Authorization: Bearer $TOKEN"
```

### Approve Offer
```bash
curl -X POST $BASE/offers/<offerId>/approve \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "approved": true,
    "comments": "Approved - within budget"
  }'
```

### Send Offer to Candidate
```bash
curl -X POST $BASE/offers/<offerId>/send \
  -H "Authorization: Bearer $TOKEN"
```

### Candidate Responds
```bash
curl -X POST $BASE/offers/<offerId>/respond \
  -H "Authorization: Bearer $CAND_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accepted": true,
    "comments": "Happy to accept!"
  }'
```

### Revoke Offer
```bash
curl -X POST $BASE/offers/<offerId>/revoke \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Position eliminated"
  }'
```

### My Offers (Candidate)
```bash
curl -X GET $BASE/offers/candidate/my \
  -H "Authorization: Bearer $CAND_TOKEN"
```

---

## 41. Calendar Sync

**Base:** `/api/v1/calendar-sync`

### Connect Calendar
```bash
curl -X POST $BASE/calendar-sync/connect \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "GOOGLE",
    "authorizationCode": "<oauth2-auth-code>",
    "redirectUri": "http://localhost:5173/callback"
  }'
```

### List Connections
```bash
curl -X GET $BASE/calendar-sync/connections \
  -H "Authorization: Bearer $TOKEN"
```

### Disconnect Calendar
```bash
curl -X DELETE $BASE/calendar-sync/connections/<connectionId> \
  -H "Authorization: Bearer $TOKEN"
```

### Sync Interview to Calendar
```bash
curl -X POST $BASE/calendar-sync/sync/interview/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Sync All Upcoming
```bash
curl -X POST $BASE/calendar-sync/sync/all \
  -H "Authorization: Bearer $TOKEN"
```

### Bidirectional Sync
```bash
curl -X POST $BASE/calendar-sync/sync/bidirectional/<connectionId> \
  -H "Authorization: Bearer $TOKEN"
```

### List Synced Events
```bash
curl -X GET $BASE/calendar-sync/events \
  -H "Authorization: Bearer $TOKEN"
```

---

## 42. Workflows

**Base:** `/api/v1/workflows`

### Create Workflow Rule
```bash
curl -X POST $BASE/workflows \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Auto-advance high scorers",
    "description": "Automatically advance candidates with avg score > 4",
    "triggerEvent": "INTERVIEW_COMPLETED",
    "conditionType": "SCORE_THRESHOLD",
    "conditionValue": "4.0",
    "actionType": "ADVANCE_PIPELINE",
    "actionConfig": "{\"notifyRecruiter\": true}",
    "isActive": true
  }'
```

### Get All Workflows
```bash
curl -X GET $BASE/workflows \
  -H "Authorization: Bearer $TOKEN"
```

### Get Workflow
```bash
curl -X GET $BASE/workflows/<workflowId> \
  -H "Authorization: Bearer $TOKEN"
```

### Update Workflow
```bash
curl -X PUT $BASE/workflows/<workflowId> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "conditionValue": "4.5",
    "isActive": true
  }'
```

### Delete Workflow
```bash
curl -X DELETE $BASE/workflows/<workflowId> \
  -H "Authorization: Bearer $TOKEN"
```

---

## 43. Approvals

**Base:** `/api/v1/approvals`

### Create Approval Chain
```bash
curl -X POST $BASE/approvals/chains \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"Offer Approval Chain\",
    \"entityType\": \"OFFER\",
    \"mode\": \"SEQUENTIAL\",
    \"steps\": [
      {\"approverId\": \"$RECRUITER_ID\", \"orderIndex\": 1},
      {\"approverId\": \"$ADMIN_ID\", \"orderIndex\": 2}
    ]
  }"
```

### Get Approval Chains
```bash
curl -X GET $BASE/approvals/chains \
  -H "Authorization: Bearer $TOKEN"
```

### Submit Approval Request
```bash
curl -X POST $BASE/approvals/requests \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "chainId": "<chain-uuid>",
    "entityType": "OFFER",
    "entityId": "<offer-uuid>",
    "notes": "Please approve this senior offer"
  }'
```

### Make Approval Decision
```bash
curl -X POST $BASE/approvals/requests/<requestId>/decide \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "approved": true,
    "comments": "Approved - excellent candidate"
  }'
```

### Get Pending Approvals
```bash
curl -X GET $BASE/approvals/requests/pending \
  -H "Authorization: Bearer $TOKEN"
```

---

## 44. Referrals

**Base:** `/api/v1/referrals`

### Create Referral
```bash
curl -X POST $BASE/referrals \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "candidateEmail": "referred@example.com",
    "candidateFirstName": "Bob",
    "candidateLastName": "Williams",
    "jobPositionId": "<position-uuid>",
    "relationship": "Former colleague",
    "notes": "Excellent engineer, worked together at TechCorp"
  }'
```

### Get My Referrals
```bash
curl -X GET $BASE/referrals/my \
  -H "Authorization: Bearer $TOKEN"
```

### Get All Referrals (Admin)
```bash
curl -X GET $BASE/referrals \
  -H "Authorization: Bearer $TOKEN"
```

### Update Referral Status
```bash
curl -X PATCH $BASE/referrals/<referralId>/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "HIRED"}'
```

---

## 45. DEI Analytics

**Base:** `/api/v1/dei`

### Submit Demographics (Opt-In)
```bash
curl -X POST $BASE/dei/demographics \
  -H "Authorization: Bearer $CAND_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "gender": "PREFER_NOT_TO_SAY",
    "ethnicity": "PREFER_NOT_TO_SAY",
    "ageRange": "RANGE_25_34",
    "veteranStatus": false,
    "disabilityStatus": false
  }'
```

### Get DEI Analytics (Admin)
```bash
curl -X GET $BASE/dei/analytics \
  -H "Authorization: Bearer $TOKEN"
```

### Get Funnel Analysis
```bash
curl -X GET "$BASE/dei/analytics/funnel?fromDate=2026-01-01&toDate=2026-12-31" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 46. Source Tracking

**Base:** `/api/v1/source-tracking`

### Create Source
```bash
curl -X POST $BASE/source-tracking \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"candidateId\": \"$CANDIDATE_ID\",
    \"sourceType\": \"REFERRAL\",
    \"sourceName\": \"Employee Referral - John Smith\",
    \"campaign\": \"Q3 2026 Hiring Drive\",
    \"cost\": 500.00
  }"
```

### Get Sources for Candidate
```bash
curl -X GET $BASE/source-tracking/candidate/$CANDIDATE_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Get Source Analytics
```bash
curl -X GET $BASE/source-tracking/analytics \
  -H "Authorization: Bearer $TOKEN"
```

### Get Source ROI
```bash
curl -X GET "$BASE/source-tracking/analytics/roi?fromDate=2026-01-01&toDate=2026-12-31" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 47. WebSocket (Real-Time)

### Connection
```
URL: ws://localhost:8080/ws (STOMP over SockJS)
```

### Subscribe Destinations
| Destination | Description |
|-------------|-------------|
| `/topic/interview/{id}` | Interview messages (join/leave/chat/status) |
| `/topic/interview/{id}/code` | Code editor updates |
| `/topic/interview/{id}/signal` | WebRTC signaling |
| `/topic/whiteboard/{sessionId}` | Whiteboard strokes |
| `/user/{email}/queue/notifications` | User notifications |

### Send Destinations
| Destination | Description |
|-------------|-------------|
| `/app/interview/{id}/join` | Join interview session |
| `/app/interview/{id}/leave` | Leave session |
| `/app/interview/{id}/chat` | Send chat message |
| `/app/interview/{id}/code` | Send code update |
| `/app/interview/{id}/signal` | WebRTC signal |
| `/app/interview/{id}/status` | Status update |

### Message Format
```json
{
  "senderId": "uuid",
  "senderName": "John Doe",
  "type": "CODE",
  "content": "public class Solution { ... }"
}
```

---

## 48. End-to-End Flow

### Complete Hiring Lifecycle
```bash
echo "=== FULL HIRING LIFECYCLE ==="

# 1. Create Job Position
POS=$(curl -s -X POST $BASE/job-positions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Backend Engineer","department":"Engineering","employmentType":"FULL_TIME","experienceLevel":"SENIOR","numberOfOpenings":1}')
POS_ID=$(echo $POS | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Position: $POS_ID"

# 2. Create Pipeline
PIPE=$(curl -s -X POST $BASE/pipelines \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Backend Pipeline","department":"Engineering","stages":[{"name":"Screening","orderIndex":1,"interviewType":"SCREENING","durationMinutes":30},{"name":"Technical","orderIndex":2,"interviewType":"TECHNICAL","durationMinutes":60}]}')
PIPE_ID=$(echo $PIPE | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Pipeline: $PIPE_ID"

# 3. Schedule Interview
IV=$(curl -s -X POST $BASE/interviews \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Tech Screen\",\"candidateId\":\"$CANDIDATE_ID\",\"startTime\":\"2026-07-10T10:00:00Z\",\"endTime\":\"2026-07-10T11:00:00Z\",\"timeZone\":\"UTC\",\"type\":\"TECHNICAL\",\"mode\":\"ONLINE\",\"interviewerIds\":[\"$INTERVIEWER_ID\"]}")
IV_ID=$(echo $IV | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Interview: $IV_ID"

# 4. Generate Meeting Link
curl -s -X POST "$BASE/interviews/$IV_ID/meeting" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"provider":"INTERNAL"}' > /dev/null

# 5. Start Coding Session
curl -s -X POST "$BASE/interviews/$IV_ID/code/start?language=python" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

# 6. Save Code
curl -s -X PUT "$BASE/interviews/$IV_ID/code/save" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"code":"def solve(): return 42","language":"python"}' > /dev/null

# 7. Complete Interview
curl -s -X POST "$BASE/interviews/$IV_ID/code/end" -H "Authorization: Bearer $TOKEN" > /dev/null
curl -s -X PATCH "$BASE/interviews/$IV_ID/status?status=COMPLETED" -H "Authorization: Bearer $TOKEN" > /dev/null

# 8. Submit Feedback (as interviewer)
curl -s -X POST "$BASE/interviews/$IV_ID/feedback" \
  -H "Authorization: Bearer $INT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"rating":5,"recommendation":"HIRE","strengths":"Excellent","weaknesses":"None","comments":"Strong hire"}' > /dev/null

echo "=== LIFECYCLE COMPLETE ==="
```

---

## Error Responses

All errors follow this format:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Interview not found with id: xxx",
  "path": "/api/v1/interviews/xxx",
  "timestamp": "2026-06-19T10:30:00Z"
}
```

| Status | Meaning |
|--------|---------|
| 400 | Bad Request - validation failed |
| 401 | Unauthorized - invalid/expired token |
| 403 | Forbidden - insufficient permissions |
| 404 | Not Found - resource doesn't exist |
| 409 | Conflict - duplicate resource |
| 429 | Too Many Requests - rate limited |
| 500 | Internal Server Error |

---

## Audit Trail

**Base:** `/api/v1/audit`

### Get All Audit Logs
```bash
curl -X GET $BASE/audit \
  -H "Authorization: Bearer $TOKEN"
```

### Get Audit by Entity
```bash
curl -X GET $BASE/audit/entity/INTERVIEW/$INTERVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Get Audit by User
```bash
curl -X GET $BASE/audit/user/admin@interview.local \
  -H "Authorization: Bearer $TOKEN"
```

---

## 49. Testing SAML/SSO Locally

### Prerequisites
You need a SAML IdP. Use one of:
- **Keycloak** (free, Docker): `docker run -p 8180:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:23.0 start-dev`
- **samlidp.io** (free hosted test IdP): https://samlidp.io
- **Okta Developer** (free): https://developer.okta.com

### Step 1: Configure SSO in the Platform
```bash
# Create SSO configuration (as Admin)
curl -X POST $BASE/sso \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "<org-uuid>",
    "providerType": "OKTA",
    "registrationId": "test-okta-sso",
    "entityId": "http://www.okta.com/exk123456",
    "ssoUrl": "https://dev-123456.okta.com/app/exk123456/sso/saml",
    "certificate": "-----BEGIN CERTIFICATE-----\nMIID...your-idp-x509-cert...\n-----END CERTIFICATE-----",
    "spEntityId": "interview-platform-sp",
    "acsUrl": "http://localhost:8080/login/saml2/sso/test-okta-sso",
    "signRequests": false,
    "enabled": true
  }'
```

### Step 2: Get SSO Login URL
```bash
# Get login URLs for a tenant
curl -X GET $BASE/sso/tenant/<tenantId>/login-urls
```

**Response:**
```json
[
  {
    "registrationId": "test-okta-sso",
    "providerType": "OKTA",
    "loginUrl": "http://localhost:8080/saml2/authenticate/test-okta-sso"
  }
]
```

### Step 3: Initiate SP-Initiated SSO
Open in browser (cannot use curl for full SAML redirect flow):
```
http://localhost:8080/saml2/authenticate/test-okta-sso
```

This redirects to IdP → user authenticates → IdP POSTs SAML assertion back → platform creates/finds user → redirects with JWT tokens.

### Step 4: Toggle SSO Configuration
```bash
# Disable SSO
curl -X PATCH $BASE/sso/<configId>/toggle \
  -H "Authorization: Bearer $TOKEN"
```

### Testing with samlidp.io (Quickest)
1. Go to https://samlidp.io
2. Use their metadata URL as your IdP metadata
3. Set ACS URL to `http://localhost:8080/login/saml2/sso/<registrationId>`
4. Set SP Entity ID to `interview-platform-sp`
5. Download their X.509 certificate for the `certificate` field

---

## 50. Testing Kafka

### Verify Kafka is Running
```bash
# Check topics
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Expected topics (auto-created on first event):
# interview-notifications
# audit-events
# webhook-deliveries
```

### Consume Messages (Watch Events in Real-Time)
```bash
# Open a terminal to watch notification events
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic interview-notifications \
  --from-beginning \
  --max-messages 10

# Watch audit events
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic audit-events \
  --from-beginning
```

### Trigger Kafka Events
```bash
# Schedule an interview (triggers INTERVIEW_SCHEDULED event to Kafka)
curl -X POST $BASE/interviews \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Kafka Test Interview\",
    \"candidateId\": \"$CANDIDATE_ID\",
    \"startTime\": \"2026-08-01T10:00:00Z\",
    \"endTime\": \"2026-08-01T11:00:00Z\",
    \"timeZone\": \"UTC\",
    \"type\": \"TECHNICAL\",
    \"mode\": \"ONLINE\",
    \"interviewerIds\": [\"$INTERVIEWER_ID\"]
  }"

# Check Kafka consumer terminal - you should see the notification message
```

### Produce a Test Message
```bash
# Manually produce a message (for testing consumers)
echo '{"event":"TEST","data":{"message":"hello"}}' | \
  docker compose exec -T kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic interview-notifications
```

### Kafka Consumer Groups
```bash
# List consumer groups
docker compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 --list

# Describe a consumer group (check lag)
docker compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group interview-platform-group \
  --describe
```

### Kafka Events Triggered by the Platform

| Action | Kafka Topic | Event Type |
|--------|------------|------------|
| Schedule interview | `interview-notifications` | `INTERVIEW_SCHEDULED` |
| Cancel interview | `interview-notifications` | `INTERVIEW_CANCELLED` |
| Reschedule interview | `interview-notifications` | `INTERVIEW_RESCHEDULED` |
| Submit feedback | `interview-notifications` | `FEEDBACK_SUBMITTED` |
| Pipeline advancement | `interview-notifications` | `CANDIDATE_ADVANCED` |
| Audit operations | `audit-events` | Various |

---

## 51. Testing Redis

### Verify Redis is Running
```bash
# Ping Redis
docker compose exec redis redis-cli ping
# Expected: PONG

# Check Redis info
docker compose exec redis redis-cli info server
```

### Inspect Cache Contents
```bash
# List all keys
docker compose exec redis redis-cli KEYS "*"

# Check specific cache entries
docker compose exec redis redis-cli KEYS "*roles*"
docker compose exec redis redis-cli KEYS "*permissions*"
docker compose exec redis redis-cli KEYS "*jobPositions*"

# Get a cached value
docker compose exec redis redis-cli GET "roles::allRoles"

# Check TTL on a key
docker compose exec redis redis-cli TTL "roles::allRoles"
```

### Test Rate Limiting (Redis-backed)
```bash
# Hit login endpoint rapidly to trigger rate limit (5 req/min for login)
for i in {1..10}; do
  echo "Attempt $i:"
  curl -s -o /dev/null -w "%{http_code}" -X POST $BASE/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com","password":"wrong"}'
  echo ""
done
# After 5 attempts: expect 429 Too Many Requests

# Check rate limit keys in Redis
docker compose exec redis redis-cli KEYS "*rate*"
```

### Test Cache Invalidation
```bash
# 1. Call an endpoint that uses cache
curl -X GET $BASE/roles -H "Authorization: Bearer $TOKEN"

# 2. Check it was cached
docker compose exec redis redis-cli KEYS "*roles*"

# 3. Create a new role (triggers cache eviction)
curl -X POST $BASE/roles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"TEST_CACHE","description":"Testing cache eviction"}'

# 4. Verify cache was invalidated
docker compose exec redis redis-cli KEYS "*roles*"
# Should be empty or refreshed
```

### Monitor Redis in Real-Time
```bash
# Watch all Redis commands in real-time
docker compose exec redis redis-cli MONITOR
# Then perform API operations and watch cache hits/misses
```

### Flush Cache (Development)
```bash
# Clear all cached data
docker compose exec redis redis-cli FLUSHALL
```

---

## 52. Testing OpenTelemetry & Tracing

### Verify OTel Collector is Running
```bash
# Health check
curl http://localhost:13133/

# Check Prometheus metrics from collector
curl http://localhost:8889/metrics | grep "otelcol_receiver"
```

### View Traces in Jaeger UI
```bash
# Open Jaeger UI
open http://localhost:16686

# Or check Jaeger health
curl http://localhost:14269/
```

### Generate Traces
```bash
# Any API call generates traces automatically. Make several calls:
curl -X GET $BASE/users/me -H "Authorization: Bearer $TOKEN"
curl -X GET $BASE/interviews -H "Authorization: Bearer $TOKEN"
curl -X GET $BASE/dashboard/admin -H "Authorization: Bearer $TOKEN"

# Now go to Jaeger UI:
# 1. Select service: "interview-platform-backend"
# 2. Click "Find Traces"
# 3. Click any trace to see the full span tree
```

### Trace Correlation with Logs
```bash
# Application logs include trace IDs:
docker compose logs app | grep "trace_id"

# Example log line:
# 2026-06-19 10:30:00 INFO [trace_id=abc123, span_id=def456] c.i.s.AuthService : Login successful

# Copy the trace_id and search in Jaeger UI to see the full request flow
```

### Check Prometheus Metrics
```bash
# View raw metrics exposed by the OTel Collector
curl http://localhost:8889/metrics

# Filter for HTTP request metrics
curl http://localhost:8889/metrics | grep "http_server"

# Filter for JVM metrics
curl http://localhost:8889/metrics | grep "jvm_memory"

# Filter for DB metrics
curl http://localhost:8889/metrics | grep "db_client"
```

### Custom Span Verification
```bash
# Endpoints that create custom spans:
# - Pipeline advancement: "advance-candidate-stage"
# - Scorecard calculation: "calculate-weighted-score"
# - Bulk operations: "bulk-schedule-interviews"

# Trigger a pipeline advancement and look for its span in Jaeger
curl -X POST "$BASE/pipelines/candidates/<candidatePipelineId>/advance" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"feedback": "Testing OTel spans"}'
```

### Environment-Specific Configuration
```bash
# Development (100% sampling - all traces captured)
OTEL_TRACES_SAMPLER_ARG=1.0

# Production (10% sampling - reduce volume)
OTEL_TRACES_SAMPLER_ARG=0.1

# Disable tracing entirely (for tests)
OTEL_TRACES_SAMPLER=always_off
```

---

## 53. Testing New Auth Functionalities

### Account Lockout Testing
```bash
# 1. Trigger lockout by failing 5+ times
for i in {1..6}; do
  echo "Attempt $i:"
  curl -s -X POST $BASE/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@interview.local","password":"WrongPassword!"}' \
    | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('message',''))" 2>/dev/null || echo "Error"
done
# After 5th attempt: account locked

# 2. Verify lockout status
curl -X GET $BASE/security/lockout/admin@interview.local \
  -H "Authorization: Bearer $TOKEN"

# 3. Try login (should fail with "account locked")
curl -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@interview.local","password":"ChangeMe123!"}'

# 4. Unlock account (admin API)
curl -X POST $BASE/security/lockout/admin@interview.local/unlock \
  -H "Authorization: Bearer $TOKEN"

# 5. Login again (should succeed)
curl -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@interview.local","password":"ChangeMe123!"}'
```

### IP Blocking Testing
```bash
# 1. Check current blocked IPs
curl -X GET $BASE/security/blocked-ips \
  -H "Authorization: Bearer $TOKEN"

# 2. Manually block an IP
curl -X POST $BASE/security/block-ip \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"ipAddress": "10.0.0.50", "reason": "Manual test block"}'

# 3. Verify it's blocked
curl -X GET $BASE/security/blocked-ips \
  -H "Authorization: Bearer $TOKEN"

# 4. Unblock
curl -X POST $BASE/security/unblock-ip/10.0.0.50 \
  -H "Authorization: Bearer $TOKEN"
```

### MFA/TOTP Testing
```bash
# 1. Enable MFA (returns QR code data and secret)
MFA_RESPONSE=$(curl -s -X POST $BASE/mfa/enable \
  -H "Authorization: Bearer $TOKEN")
echo $MFA_RESPONSE | python3 -m json.tool
# Save the 'secret' field - use it with Google Authenticator or 'oathtool'

# 2. Generate TOTP code (requires oathtool)
# Install: brew install oath-toolkit
SECRET="<secret-from-step-1>"
CODE=$(oathtool --totp --base32 "$SECRET")
echo "TOTP Code: $CODE"

# 3. Verify MFA setup
curl -X POST $BASE/mfa/verify \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"code\": \"$CODE\"}"

# 4. Check MFA status
curl -X GET $BASE/mfa/status \
  -H "Authorization: Bearer $TOKEN"

# 5. Login with MFA (after enabling)
# First login returns a partial token requiring MFA verification
curl -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@interview.local","password":"ChangeMe123!","mfaCode":"123456"}'

# 6. Disable MFA
CODE=$(oathtool --totp --base32 "$SECRET")
curl -X POST $BASE/mfa/disable \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"code\": \"$CODE\"}"
```

### API Key Authentication Testing
```bash
# 1. Create an API key
API_KEY_RESPONSE=$(curl -s -X POST $BASE/api-keys \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Integration","scopes":["READ_INTERVIEWS","READ_USERS"],"expiresInDays":30}')
echo $API_KEY_RESPONSE | python3 -m json.tool
# IMPORTANT: Save the 'key' field - it's only shown once!

API_KEY="<key-from-response>"

# 2. Use API key instead of Bearer token
curl -X GET $BASE/interviews \
  -H "X-API-Key: $API_KEY"

# 3. List API keys
curl -X GET $BASE/api-keys \
  -H "Authorization: Bearer $TOKEN"

# 4. Revoke API key
curl -X DELETE $BASE/api-keys/<keyId> \
  -H "Authorization: Bearer $TOKEN"
```

### OAuth2 Testing
```bash
# 1. Get available OAuth2 providers
curl -X GET $BASE/auth/oauth2/providers

# 2. Initiate OAuth2 flow (open in browser):
# Google: http://localhost:8080/oauth2/authorization/google
# GitHub: http://localhost:8080/oauth2/authorization/github
# Microsoft: http://localhost:8080/oauth2/authorization/microsoft

# 3. After successful OAuth2, user is redirected to:
# ${FRONTEND_URL}?accessToken=...&refreshToken=...&email=...
```

### Refresh Token Rotation Testing
```bash
# 1. Login to get tokens
RESPONSE=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@interview.local","password":"ChangeMe123!"}')
ACCESS=$(echo $RESPONSE | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
REFRESH=$(echo $RESPONSE | python3 -c "import sys,json; print(json.load(sys.stdin)['refreshToken'])")

# 2. Refresh token (returns NEW tokens, old refresh token is invalidated)
NEW_RESPONSE=$(curl -s -X POST $BASE/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH\"}")
echo $NEW_RESPONSE | python3 -m json.tool

# 3. Try to reuse old refresh token (REPLAY DETECTION - should fail)
curl -s -X POST $BASE/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH\"}"
# Expected: 401 Unauthorized - entire token family revoked
```

### JWKS Endpoint Testing
```bash
# The JWKS endpoint exposes RSA public keys for external JWT verification
curl -X GET http://localhost:8080/.well-known/jwks.json

# Response contains the RSA public key in JWK format:
# {
#   "keys": [{
#     "kty": "RSA",
#     "n": "...",
#     "e": "AQAB",
#     "use": "sig",
#     "alg": "RS256"
#   }]
# }
```

---

## Swagger UI

Interactive API docs available at:
```
http://localhost:8080/swagger-ui/index.html
```
