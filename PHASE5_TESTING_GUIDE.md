# Phase 5 Testing Guide — Interview Execution

This guide walks you through testing all Phase 5 features end-to-end using `curl` commands.

## Prerequisites

1. **Start infrastructure:**
   ```bash
   docker compose up -d
   ```

2. **Run the application:**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

3. **Get a JWT token (login as admin):**
   ```bash
   # Login as the seeded admin user
   TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@interview.local","password":"ChangeMe123!"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

   echo "Token: $TOKEN"
   ```

   If the admin account needs email verification first:
   ```bash
   # The seeded users are already ACTIVE, so login should work directly
   ```

4. **Get the seeded user IDs:**
   ```bash
   # Get all users to find IDs
   curl -s http://localhost:8080/api/v1/users \
     -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
   ```

   Save these:
   ```bash
   ADMIN_ID="<admin-uuid>"
   INTERVIEWER_ID="<interviewer-uuid>"
   CANDIDATE_ID="<candidate-uuid>"
   RECRUITER_ID="<recruiter-uuid>"
   ```

---

## 1. Create an Interview (triggers notification)

```bash
# Schedule an interview 2 days from now
START_TIME=$(date -u -v+2d '+%Y-%m-%dT10:00:00Z')
END_TIME=$(date -u -v+2d '+%Y-%m-%dT11:00:00Z')

INTERVIEW=$(curl -s -X POST http://localhost:8080/api/v1/interviews \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Senior Java Developer - Technical Round\",
    \"description\": \"DSA + System Design assessment\",
    \"candidateId\": \"$CANDIDATE_ID\",
    \"startTime\": \"$START_TIME\",
    \"endTime\": \"$END_TIME\",
    \"timeZone\": \"Asia/Kolkata\",
    \"type\": \"TECHNICAL\",
    \"mode\": \"ONLINE\",
    \"interviewerIds\": [\"$INTERVIEWER_ID\"]
  }")

echo "$INTERVIEW" | python3 -m json.tool

# Save the interview ID
INTERVIEW_ID=$(echo "$INTERVIEW" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Interview ID: $INTERVIEW_ID"
```

### ✅ What happens behind the scenes:
- `InterviewScheduledEvent` is published
- `InterviewEventListener` picks it up and sends to Kafka:
  - **Candidate** receives: "Your interview has been scheduled" (EMAIL + SMS)
  - **Interviewer** receives: "You have been assigned as interviewer" (EMAIL)
- Check the application logs for: `Interview scheduled event: interviewId=...`

### See notifications in logs:
```bash
# Since app.notifications.enabled=false (dev mode), check logs:
# "Email notifications disabled. Would send to=candidate@interview.local, subject=Interview Scheduled: ..."
```

---

## 2. Generate Meeting Link

```bash
# Generate an internal meeting link
curl -s -X POST "http://localhost:8080/api/v1/interviews/$INTERVIEW_ID/meeting" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "INTERNAL",
    "topic": "Senior Java Developer Interview",
    "durationMinutes": 60
  }' | python3 -m json.tool
```

**Expected response:**
```json
{
  "id": "uuid",
  "interviewId": "<interview-id>",
  "provider": "INTERNAL",
  "meetingUrl": "https://meet.interview-platform.com/room/abc123def456",
  "hostUrl": "https://meet.interview-platform.com/room/abc123def456?role=host&key=...",
  "meetingId": "abc123def456",
  "passcode": "A1B2C3",
  "createdAt": "...",
  "expiresAt": "..."
}
```

### Get the meeting link:
```bash
curl -s "http://localhost:8080/api/v1/interviews/$INTERVIEW_ID/meeting" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### Try duplicate (should get 409):
```bash
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "http://localhost:8080/api/v1/interviews/$INTERVIEW_ID/meeting" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"provider": "INTERNAL"}'
```

---

## 3. Calendar — Set Interviewer Availability

```bash
# Add Monday 9 AM - 12 PM recurring slot
curl -s -X POST "http://localhost:8080/api/v1/calendar/interviewers/$INTERVIEWER_ID/availability" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "dayOfWeek": 1,
    "startTime": "09:00",
    "endTime": "12:00",
    "timeZone": "Asia/Kolkata",
    "isRecurring": true
  }' | python3 -m json.tool

# Add Monday 2 PM - 5 PM recurring slot
curl -s -X POST "http://localhost:8080/api/v1/calendar/interviewers/$INTERVIEWER_ID/availability" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "dayOfWeek": 1,
    "startTime": "14:00",
    "endTime": "17:00",
    "timeZone": "Asia/Kolkata",
    "isRecurring": true
  }' | python3 -m json.tool

# Add Wednesday slot
curl -s -X POST "http://localhost:8080/api/v1/calendar/interviewers/$INTERVIEWER_ID/availability" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "dayOfWeek": 3,
    "startTime": "10:00",
    "endTime": "16:00",
    "timeZone": "Asia/Kolkata",
    "isRecurring": true
  }' | python3 -m json.tool
```

### Get all availability:
```bash
curl -s "http://localhost:8080/api/v1/calendar/interviewers/$INTERVIEWER_ID/availability" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### Check availability for a specific date (with conflict detection):
```bash
# Check next Monday (find the date)
NEXT_MONDAY=$(date -v+monday '+%Y-%m-%d')
echo "Checking availability for: $NEXT_MONDAY"

curl -s "http://localhost:8080/api/v1/calendar/interviewers/$INTERVIEWER_ID/availability/check?date=$NEXT_MONDAY" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

**Expected:** Shows slots with `isAvailable: true/false` based on existing interviews.

---

## 4. Question Bank

### Create categories (if not already seeded):
```bash
# Categories are seeded by migration, but you can add more:
curl -s -X POST http://localhost:8080/api/v1/questions/categories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Machine Learning", "description": "ML/AI interview questions"}' \
  | python3 -m json.tool
```

### List all categories:
```bash
curl -s http://localhost:8080/api/v1/questions/categories \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

Save a category ID:
```bash
DSA_CATEGORY_ID=$(curl -s http://localhost:8080/api/v1/questions/categories \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; cats=json.load(sys.stdin); print(next(c['id'] for c in cats if 'Algorithm' in c['name']))")
echo "DSA Category: $DSA_CATEGORY_ID"
```

### Create questions:
```bash
# DSA Question
curl -s -X POST http://localhost:8080/api/v1/questions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Two Sum - Find pair with target sum\",
    \"description\": \"Given an array of integers and a target sum, return indices of two numbers that add up to the target. You may assume each input has exactly one solution.\",
    \"categoryId\": \"$DSA_CATEGORY_ID\",
    \"difficulty\": \"EASY\",
    \"type\": \"CODING\",
    \"expectedDurationMinutes\": 15,
    \"sampleAnswer\": \"Use a HashMap to store complement values. O(n) time, O(n) space.\",
    \"hints\": \"Think about what value you need to find for each element. Can you look it up in O(1)?\",
    \"tags\": \"array,hashmap,two-pointer\"
  }" | python3 -m json.tool

# System Design Question
curl -s -X POST http://localhost:8080/api/v1/questions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Design a URL Shortener like bit.ly\",
    \"description\": \"Design a URL shortening service. The system should generate a short URL for a given long URL, redirect short URL to the original, handle high traffic, and be horizontally scalable.\",
    \"categoryId\": \"$DSA_CATEGORY_ID\",
    \"difficulty\": \"MEDIUM\",
    \"type\": \"SYSTEM_DESIGN\",
    \"expectedDurationMinutes\": 45,
    \"sampleAnswer\": \"Use Base62 encoding of auto-increment ID or MD5 hash. Store in DB with cache layer (Redis). Use consistent hashing for distribution.\",
    \"hints\": \"Consider: How to generate unique short codes? How to handle collisions? What about analytics?\",
    \"tags\": \"system-design,distributed,caching,database\"
  }" | python3 -m json.tool

# Behavioral Question
curl -s -X POST http://localhost:8080/api/v1/questions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Tell me about a time you disagreed with your manager\",
    \"description\": \"Describe a situation where you had a different opinion from your manager. How did you handle it? What was the outcome?\",
    \"categoryId\": \"$DSA_CATEGORY_ID\",
    \"difficulty\": \"MEDIUM\",
    \"type\": \"BEHAVIORAL\",
    \"expectedDurationMinutes\": 10,
    \"tags\": \"conflict-resolution,communication,leadership\"
  }" | python3 -m json.tool
```

### Search questions:
```bash
# All questions
curl -s "http://localhost:8080/api/v1/questions/search" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# Filter by difficulty
curl -s "http://localhost:8080/api/v1/questions/search?difficulty=EASY" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# Filter by type
curl -s "http://localhost:8080/api/v1/questions/search?type=CODING" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# Search by keyword
curl -s "http://localhost:8080/api/v1/questions/search?keyword=hashmap" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

---

## 5. Coding Session (Collaborative Editor)

### Start a coding session:
```bash
curl -s -X POST "http://localhost:8080/api/v1/interviews/$INTERVIEW_ID/code/start?language=java" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### Get active session:
```bash
curl -s "http://localhost:8080/api/v1/interviews/$INTERVIEW_ID/code" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### Save code (simulating auto-save from editor):
```bash
curl -s -X PUT "http://localhost:8080/api/v1/interviews/$INTERVIEW_ID/code/save" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"code\": \"import java.util.*;\\n\\npublic class Solution {\\n    public int[] twoSum(int[] nums, int target) {\\n        Map<Integer, Integer> map = new HashMap<>();\\n        for (int i = 0; i < nums.length; i++) {\\n            int complement = target - nums[i];\\n            if (map.containsKey(complement)) {\\n                return new int[]{map.get(complement), i};\\n            }\\n            map.put(nums[i], i);\\n        }\\n        return new int[]{};\\n    }\\n}\",
    \"language\": \"java\",
    \"userId\": \"$CANDIDATE_ID\"
  }" | python3 -m json.tool
```

### End session:
```bash
curl -s -X POST "http://localhost:8080/api/v1/interviews/$INTERVIEW_ID/code/end" \
  -H "Authorization: Bearer $TOKEN"
echo "Session ended"
```

### Get session history:
```bash
curl -s "http://localhost:8080/api/v1/interviews/$INTERVIEW_ID/code/history" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

---

## 6. WebSocket — Real-Time Code Collaboration

Connect to the WebSocket for real-time sync. You can use **websocat**, **wscat**, or a browser console.

### Using wscat (install: `npm i -g wscat`):

```bash
# Connect to WebSocket (note: STOMP over WebSocket)
# For full STOMP testing, use a STOMP client or the browser

# In browser console (or a simple HTML file):
```

### Browser test (paste in DevTools console):
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
  { Authorization: 'Bearer YOUR_TOKEN_HERE' },
  function(frame) {
    console.log('Connected: ' + frame);

    // Subscribe to code updates
    stompClient.subscribe('/topic/interview/INTERVIEW_ID/code', function(message) {
      console.log('Code update:', JSON.parse(message.body));
    });

    // Subscribe to general interview messages
    stompClient.subscribe('/topic/interview/INTERVIEW_ID', function(message) {
      console.log('Interview message:', JSON.parse(message.body));
    });

    // Send a code update
    stompClient.send('/app/interview/INTERVIEW_ID/code', {}, JSON.stringify({
      senderId: 'USER_UUID',
      senderName: 'Cathy Candidate',
      type: 'CODE',
      content: 'public class Solution { ... }'
    }));
  }
);
```

Replace `INTERVIEW_ID` and `YOUR_TOKEN_HERE` with actual values.

---

## 7. Full End-to-End Flow

Here's the complete happy path:

```bash
# ────────────────────────────────────────
# STEP 1: Login as Recruiter
# ────────────────────────────────────────
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"recruiter@interview.local","password":"ChangeMe123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# ────────────────────────────────────────
# STEP 2: Check interviewer availability
# ────────────────────────────────────────
curl -s "http://localhost:8080/api/v1/calendar/interviewers/$INTERVIEWER_ID/availability/check?date=2026-06-02" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# ────────────────────────────────────────
# STEP 3: Schedule interview at available slot
# ────────────────────────────────────────
INTERVIEW=$(curl -s -X POST http://localhost:8080/api/v1/interviews \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Backend Engineer - DSA Round\",
    \"candidateId\": \"$CANDIDATE_ID\",
    \"startTime\": \"2026-06-02T09:30:00Z\",
    \"endTime\": \"2026-06-02T10:30:00Z\",
    \"timeZone\": \"Asia/Kolkata\",
    \"type\": \"TECHNICAL\",
    \"mode\": \"ONLINE\",
    \"interviewerIds\": [\"$INTERVIEWER_ID\"]
  }")
INTERVIEW_ID=$(echo "$INTERVIEW" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Created interview: $INTERVIEW_ID"
# 📧 Notification sent to candidate + interviewer (check logs)

# ────────────────────────────────────────
# STEP 4: Generate meeting link
# ────────────────────────────────────────
curl -s -X POST "http://localhost:8080/api/v1/interviews/$INTERVIEW_ID/meeting" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"provider": "INTERNAL", "durationMinutes": 60}' | python3 -m json.tool

# ────────────────────────────────────────
# STEP 5: Pick questions for the interview
# ────────────────────────────────────────
curl -s "http://localhost:8080/api/v1/questions/search?type=CODING&difficulty=EASY" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# ────────────────────────────────────────
# STEP 6: Start coding session (day of interview)
# ────────────────────────────────────────
curl -s -X POST "http://localhost:8080/api/v1/interviews/$INTERVIEW_ID/code/start?language=java" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# ────────────────────────────────────────
# STEP 7: Candidate writes code (auto-saved)
# ────────────────────────────────────────
curl -s -X PUT "http://localhost:8080/api/v1/interviews/$INTERVIEW_ID/code/save" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"code\": \"class Solution {\\n  // Two Sum implementation\\n}\", \"language\": \"java\"}" \
  | python3 -m json.tool

# ────────────────────────────────────────
# STEP 8: End session + Submit feedback
# ────────────────────────────────────────
curl -s -X POST "http://localhost:8080/api/v1/interviews/$INTERVIEW_ID/code/end" \
  -H "Authorization: Bearer $TOKEN"

# Update status to COMPLETED
curl -s -X PATCH "http://localhost:8080/api/v1/interviews/$INTERVIEW_ID/status?status=COMPLETED" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# Login as interviewer to submit feedback
INT_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"interviewer@interview.local","password":"ChangeMe123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

curl -s -X POST "http://localhost:8080/api/v1/interviews/$INTERVIEW_ID/feedback" \
  -H "Authorization: Bearer $INT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 4,
    "recommendation": "HIRE",
    "strengths": "Strong DSA fundamentals, clean code",
    "weaknesses": "Could improve on time complexity analysis",
    "comments": "Good candidate for the role"
  }' | python3 -m json.tool
# 📧 Feedback notification sent (check logs)
```

---

## 8. Verify Notifications in Logs

Since `app.notifications.enabled=false` (dev mode), notifications are logged but not sent.  
Watch the logs for:

```bash
# In the terminal running the app, you'll see:
# INFO  - Email notifications disabled. Would send to=candidate@interview.local, subject=Interview Scheduled: ...
# INFO  - Interview scheduled event: interviewId=..., candidate=candidate@interview.local
# INFO  - Feedback submitted: interviewId=..., by=Ian Interviewer, rating=4, recommendation=HIRE
```

To see **Kafka messages** (if Kafka is running):
```bash
# Consume from the notifications topic
docker exec -it $(docker ps --filter "ancestor=confluentinc/cp-kafka:7.6.0" --format "{{.ID}}") \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic interview-notifications --from-beginning --max-messages 10
```

---

## 9. Swagger UI

All new endpoints are documented with OpenAPI annotations. Visit:

```
http://localhost:8080/swagger-ui/index.html
```

Look for these new tags:
- **Meeting** — Generate and get meeting links
- **Calendar** — Interviewer availability management
- **Code Editor** — Collaborative coding session management
- **Question Bank** — Question CRUD and search

---

## Quick Reference: New Endpoints

| Feature | Method | Endpoint |
|---------|--------|----------|
| Generate meeting | POST | `/api/v1/interviews/{id}/meeting` |
| Get meeting | GET | `/api/v1/interviews/{id}/meeting` |
| Add availability | POST | `/api/v1/calendar/interviewers/{id}/availability` |
| Get availability | GET | `/api/v1/calendar/interviewers/{id}/availability` |
| Check date | GET | `/api/v1/calendar/interviewers/{id}/availability/check?date=` |
| Delete slot | DELETE | `/api/v1/calendar/interviewers/{id}/availability/{slotId}` |
| Start code session | POST | `/api/v1/interviews/{id}/code/start` |
| Get active session | GET | `/api/v1/interviews/{id}/code` |
| Save code | PUT | `/api/v1/interviews/{id}/code/save` |
| End session | POST | `/api/v1/interviews/{id}/code/end` |
| Session history | GET | `/api/v1/interviews/{id}/code/history` |
| Create category | POST | `/api/v1/questions/categories` |
| List categories | GET | `/api/v1/questions/categories` |
| Create question | POST | `/api/v1/questions` |
| Get question | GET | `/api/v1/questions/{id}` |
| Update question | PUT | `/api/v1/questions/{id}` |
| Delete question | DELETE | `/api/v1/questions/{id}` |
| Search questions | GET | `/api/v1/questions/search` |
| By category | GET | `/api/v1/questions/category/{categoryId}` |
| WebSocket connect | WS | `/ws` (STOMP over SockJS) |
| Code updates | SUB | `/topic/interview/{id}/code` |
| Send code | PUB | `/app/interview/{id}/code` |

