# Technical Reference: Interview Platform Backend

> **Spring Boot 4.0.6 / Java 21 / PostgreSQL 16 / Kafka / Redis / Docker**
>
> This document is a deep-dive reference for senior developers. It covers every major subsystem's internals, data flow, and how to test each component locally.

---

## Table of Contents

1. [Authentication Deep Dive](#1-authentication-deep-dive)
2. [How to Test SAML Locally](#2-how-to-test-saml-locally)
3. [How to Test Kafka](#3-how-to-test-kafka)
4. [How to Test Redis](#4-how-to-test-redis)
5. [How to Test OpenTelemetry](#5-how-to-test-opentelemetry)
6. [Module Technical Details](#6-module-technical-details)
7. [Database Architecture](#7-database-architecture)
8. [Error Handling](#8-error-handling)

---

## 1. Authentication Deep Dive

### 1.1 Local Auth Flow

#### Registration

**Entry point:** `AuthController.register()` -> `AuthenticationServiceImpl.register()`

**Step-by-step:**

1. **Validate input** - `@Valid RegisterRequest` with Bean Validation (`@NotBlank`, `@Email`, `@Size`).
2. **Check uniqueness** - `userRepository.existsByEmail(email)`. Throws `DuplicateResourceException` (409) if duplicate.
3. **Resolve role** - `roleRepository.findByName("CANDIDATE")`. Every self-registered user gets CANDIDATE.
4. **BCrypt hash** - `passwordEncoder.encode(request.getPassword())`. Spring's `BCryptPasswordEncoder` uses a cost factor of 10 (default).
5. **Create User entity** - Status set to `UserStatus.PENDING_VERIFICATION`. `createdAt` = `Instant.now()`.
6. **Create UserProfile** - One-to-one relationship, created alongside the user.
7. **Persist** - `userRepository.save(user)` followed by `userRoleRepository.save(userRole)`.
8. **Send verification email** - `emailVerificationService.sendVerificationEmail(savedUser)`. Generates a token, persists it, and sends via the email notification service.
9. **Generate tokens** - Both access (RSA-256) and refresh (HMAC-256) JWTs generated.
10. **Persist refresh token** - `refreshTokenService.create(savedUser, refreshToken)`. Creates a new token family.
11. **Return `AuthResponse`** - Contains `accessToken` and `refreshToken`.

```
Client -> POST /api/v1/auth/register
       -> Validate -> BCrypt hash -> Save User (PENDING_VERIFICATION)
       -> Send verification email
       -> Generate JWT + Refresh Token
       -> Return { accessToken, refreshToken }
```

#### Login

**Entry point:** `AuthController.login()` -> `AuthenticationServiceImpl.login()`

**Step-by-step:**

1. **Extract client context** - IP address (from `X-Forwarded-For` / `X-Real-IP` / `remoteAddr`) and User-Agent.
2. **Pre-login check** - `accountLockoutService.checkPreLogin(email, ipAddress)`:
   - Check IP blocklist first (returns `"IP_BLOCKED"` if blocked).
   - Check account lockout status (returns `"ACCOUNT_LOCKED"` if locked and not expired).
   - If lock expired, auto-unlock the account.
3. **Authenticate** - `authenticationManager.authenticate(UsernamePasswordAuthenticationToken)`.
   - On `BadCredentialsException`: record failed attempt, throw `UnauthorizedException`.
4. **Verify user status**:
   - `PENDING_VERIFICATION` -> "Please verify your email before logging in"
   - `SUSPENDED` -> "Your account has been suspended. Contact support."
   - `INACTIVE` / `DELETED` -> "Your account is no longer active"
5. **Record success** - `accountLockoutService.recordSuccessfulLogin(email, ipAddress, userAgent)`. Resets failed counters.
6. **Build authorities** - Load all `UserRole` for the user, map to `"ROLE_" + roleName`.
7. **Generate access token** - RSA-256 signed JWT.
8. **Generate refresh token** - HMAC-256 signed JWT.
9. **Persist refresh token** - Deletes old tokens for user, creates new one with fresh token family.
10. **Return `AuthResponse`** - `{ accessToken, refreshToken }`.

```
Client -> POST /api/v1/auth/login { email, password }
       -> Check IP blocklist
       -> Check account lockout
       -> AuthenticationManager.authenticate()
       -> Verify user status (ACTIVE only)
       -> Record successful login
       -> Generate RS256 access token + HS256 refresh token
       -> Return { accessToken, refreshToken }
```

#### JWT Structure

**Access Token (RS256):**

```
Header:
{
  "alg": "RS256",
  "access_token": "interview-platform-access-token"
}

Payload:
{
  "sub": "user@example.com",        // email as subject
  "jti": "uuid-v4",                  // unique token ID
  "iat": 1700000000,                 // issued at (epoch seconds)
  "exp": 1700003600,                 // expiration
  "ROLE_CANDIDATE": true,            // (if extra claims added)
  ...extraClaims
}

Signature:
  RSASHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), rsaPrivateKey)
```

**Refresh Token (HS256):**

```
Header:
{
  "alg": "HS256"
}

Payload:
{
  "sub": "user@example.com",
  "jti": "uuid-v4",
  "iat": 1700000000,
  "exp": 1700604800,                 // longer expiry (e.g., 7 days)
  "type": "refresh"                  // distinguishes from access tokens
}

Signature:
  HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), jwtRefreshSecret)
```

**Key distinction:** Access tokens are signed with the RSA private key so that external microservices can verify them via the JWKS endpoint (`/.well-known/jwks.json` served by `JwksController`). Refresh tokens are HMAC-signed because only this backend ever validates them.

---

### 1.2 OAuth2 Flow (PKCE)

**Classes involved:**
- `PkceAuthorizationRequestResolver` - Forces PKCE S256 on all OAuth2 requests
- `CookieAuthorizationRequestRepository` - Stores OAuth2 state in a cookie (stateless sessions)
- `OAuth2SuccessHandler` - Post-authentication user provisioning + JWT generation
- `OAuth2FailureHandler` - Error redirect

#### Step-by-step:

1. **Frontend redirects** to `/oauth2/authorization/{provider}` (where provider = `google`, `github`, or `microsoft`).

2. **PkceAuthorizationRequestResolver** intercepts the request:
   - Wraps `DefaultOAuth2AuthorizationRequestResolver` with `OAuth2AuthorizationRequestCustomizers.withPkce()`.
   - Generates `code_verifier` (43-128 char random string) and `code_challenge = BASE64URL(SHA256(code_verifier))`.
   - Adds `code_challenge` and `code_challenge_method=S256` to the authorization request.

3. **CookieAuthorizationRequestRepository** saves the full `OAuth2AuthorizationRequest` (including state and code_verifier) into a short-lived HttpOnly cookie (`oauth2_auth_request`, 180s TTL). This is necessary because `SessionCreationPolicy.STATELESS` means no HTTP session exists.

4. **Redirect to provider** - Browser is redirected to e.g. `https://accounts.google.com/o/oauth2/v2/auth?...&code_challenge=...&state=...`.

5. **Provider authenticates user** and redirects back to `/login/oauth2/code/{provider}?code=...&state=...`.

6. **Spring Security** validates state (from cookie), exchanges authorization code for tokens (sending `code_verifier` for PKCE verification).

7. **OAuth2SuccessHandler.onAuthenticationSuccess():**
   - Extracts `registrationId` from `OAuth2AuthenticationToken.getAuthorizedClientRegistrationId()`.
   - Extracts user attributes based on provider (see table below).
   - Finds existing user by email OR creates new user with `AuthProvider.GOOGLE/GITHUB/MICROSOFT` and `UserStatus.ACTIVE`.
   - Ensures at least CANDIDATE role is assigned.
   - Generates access token (RS256) and refresh token (HS256).
   - Persists refresh token.
   - Redirects to `${app.oauth2.redirect-uri}?accessToken=...&refreshToken=...&email=...`.

#### Provider Attribute Mapping

| Provider   | Email Attribute               | First Name          | Last Name           |
|-----------|-------------------------------|---------------------|---------------------|
| Google    | `email`                       | `given_name`        | `family_name`       |
| GitHub    | `email` (fallback: `login@github.oauth`) | `name` (split) | `name` (split) |
| Microsoft | `email` or `preferred_username` | `givenName` or `displayName` | `surname` or `displayName` |

**GitHub edge case:** If user has a private email, GitHub returns `null`. The handler falls back to `login + "@github.oauth"` as a synthetic email.

---

### 1.3 SAML/SSO Flow

**Classes involved:**
- `SsoController` - Admin API to manage SSO configurations
- `SsoService` - Creates/updates SSO configs, handles user provisioning
- `DynamicRelyingPartyRegistrationRepository` - Loads SAML registrations from DB at runtime
- `SamlAuthenticationSuccessHandler` - Post-SAML-auth user provisioning + JWT generation

#### How to Configure

1. Admin (ADMIN or RECRUITER role) calls `POST /api/v1/sso` with:
   ```json
   {
     "registrationId": "my-company-okta",
     "idpEntityId": "http://www.okta.com/exk...",
     "idpSsoUrl": "https://company.okta.com/app/.../sso/saml",
     "idpCertificate": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
     "spEntityId": "interview-platform-sp",
     "acsUrl": "https://api.example.com/login/saml2/sso/my-company-okta",
     "signRequests": false,
     "enabled": true
   }
   ```
2. `SsoService` persists this as an `SsoConfiguration` entity.
3. `DynamicRelyingPartyRegistrationRepository` loads it on demand (no restart needed).

#### SP-Initiated SSO Flow

1. User hits `/saml2/authenticate/{registrationId}` (e.g., `/saml2/authenticate/my-company-okta`).
2. Spring Security SAML2 creates an `AuthnRequest` XML document.
3. `DynamicRelyingPartyRegistrationRepository.findByRegistrationId()` loads the config from DB.
4. Converts `SsoConfiguration` -> `RelyingPartyRegistration`:
   - Sets asserting party entity ID, SSO URL, binding (POST).
   - Parses X.509 certificate from PEM format for signature verification.
   - Sets SP entity ID and ACS URL.
5. AuthnRequest is signed (if `signRequests=true`) and POSTed/redirected to the IdP SSO URL.
6. IdP authenticates the user (its own login page).
7. IdP POSTs a SAML Response to `/login/saml2/sso/{registrationId}`.
8. Spring Security validates the SAML assertion (signature, conditions, audience).
9. **SamlAuthenticationSuccessHandler** extracts attributes:
   - Email: tries multiple claim URIs (`emailaddress`, `email`, `Email`, `mail`), falls back to NameID.
   - First name: `givenname`, `firstName`, `first_name`, `givenName`.
   - Last name: `surname`, `lastName`, `last_name`, `sn`.
10. Calls `ssoService.handleSamlAuthentication()` which:
    - Finds or creates the user.
    - Generates JWT access + refresh tokens.
11. Redirects to frontend: `${frontendRedirectUri}?accessToken=...&refreshToken=...&email=...&provider=saml`.

#### IdP-Initiated SSO

The `DynamicRelyingPartyRegistrationRepository` also supports lookup by asserting party entity ID for cases where the IdP initiates the flow without a prior AuthnRequest.

---

### 1.4 Account Lockout

**Configuration:** `AccountLockoutProperties` (prefix: `app.security.lockout`)

| Property                   | Default | Description                                      |
|---------------------------|---------|--------------------------------------------------|
| `enabled`                 | `true`  | Master switch for lockout system                 |
| `maxFailedAttempts`       | `5`     | Failed attempts before account lock              |
| `lockDurationMinutes`     | `30`    | Lock duration (0 = permanent until admin unlock) |
| `attemptWindowMinutes`    | `15`    | Window for counting IP failed attempts           |
| `maxFailedAttemptsPerIp`  | `20`    | Failed attempts from one IP before IP block      |
| `ipBlockDurationMinutes`  | `60`    | IP block duration                                |
| `alertsEnabled`           | `true`  | Send email alerts on suspicious activity         |
| `alertThreshold`          | `3`     | Failed attempts before alert email is sent       |
| `ipBlockingEnabled`       | `true`  | Enable IP-based blocking                         |

#### Pre-Login Check (`checkPreLogin`)

```
1. Is lockout enabled? No -> allow
2. Is IP in blocklist? (ipBlocklistRepository.findByIpAddressAndActiveTrue)
   -> If blocked and not expired -> return "IP_BLOCKED"
   -> If expired -> deactivate block, allow
3. Is account locked? (accountLockoutRepository.findByEmail)
   -> If locked and lockExpiresAt is past -> auto-unlock, allow
   -> If locked and not expired -> return "ACCOUNT_LOCKED"
4. Allow (return null)
```

#### On Failed Login (`recordFailedLogin`)

```
1. Record LoginAttempt entity (email, IP, userAgent, failureReason, timestamp)
2. Upsert AccountLockout for email:
   - Increment failedAttempts
   - Set lastFailedAt
3. If failedAttempts >= maxFailedAttempts:
   - Set locked=true, lockedAt=now
   - Set lockExpiresAt = now + lockDurationMinutes (or null if permanent)
   - Log warning
4. If alertsEnabled AND failedAttempts >= alertThreshold:
   - Send security alert email to user
5. If ipBlockingEnabled:
   - Count failed attempts from this IP in the window
   - If count >= maxFailedAttemptsPerIp AND not already blocked:
     - Create IpBlocklist entry (reason="BRUTE_FORCE", expiresAt=now+ipBlockDurationMinutes)
```

#### On Successful Login (`recordSuccessfulLogin`)

```
1. Record LoginAttempt (successful=true)
2. Reset AccountLockout: failedAttempts=0, locked=false, lockedAt=null, lockExpiresAt=null
```

#### Scheduled Cleanup (Every 5 minutes)

```
@Scheduled(fixedRate = 300000)
1. Find all expired IP blocks (expiresAt < now, active=true)
   -> Set active=false, saveAll
2. Delete LoginAttempt records older than 30 days
```

---

### 1.5 MFA/TOTP

**Classes:** `MfaService`, `MfaController`, `UserMfa` entity

**Library:** `dev.samstevens.totp` (DefaultSecretGenerator, DefaultCodeVerifier, SHA1, 6 digits, 30s period)

#### Setup Flow

1. `POST /api/v1/mfa/setup` (authenticated)
2. `MfaService.setupMfa(userId)`:
   - If MFA already enabled -> throw `BadRequestException`.
   - If unenabled setup exists -> delete it (allow re-setup).
   - Generate secret: `DefaultSecretGenerator.generate()` (Base32-encoded, 20 bytes).
   - Generate 10 backup codes: 8-digit random numbers via `SecureRandom`.
   - Persist `UserMfa` entity with `isEnabled=false`.
   - Build OTP Auth URI: `otpauth://totp/InterviewPlatform:{email}?secret={secret}&issuer=InterviewPlatform&algorithm=SHA1&digits=6&period=30`
3. Return `MfaSetupResponse { secretKey, qrCodeUri, backupCodes }`.
4. User scans QR code with authenticator app (Google Authenticator, Authy, etc.).

#### Verification Flow (Enable MFA)

1. `POST /api/v1/mfa/verify` with `{ code: "123456" }`
2. `MfaService.verifyAndEnable(userId, code)`:
   - Load `UserMfa`, check not already enabled.
   - `codeVerifier.isValidCode(secret, code)` - validates the 6-digit TOTP against the secret with a time window tolerance.
   - If valid: set `isEnabled=true`, `verifiedAt=now`.
   - If invalid: throw `BadRequestException("Invalid verification code")`.

#### Login Verification (When MFA enabled)

1. `MfaService.verifyCode(userId, code)`:
   - First try TOTP code validation.
   - If TOTP fails, try backup codes (one-time use, removed after use).
   - Return `true/false`.

#### Backup Code Handling

- 10 codes generated at setup.
- Each code is single-use (removed from the array after use).
- `POST /api/v1/mfa/regenerate-backup-codes` generates a fresh set of 10.

---

### 1.6 Refresh Token Rotation

#### Token Family Concept

Every refresh token belongs to a **token family** (a UUID string). When a user first logs in, a new family is created. On rotation, new tokens inherit the same family. This enables **replay detection**: if a revoked token from a family is reused, the entire family is invalidated.

#### Flow

```
Client sends refresh token -> POST /api/v1/auth/refresh { refreshToken }

1. Validate HMAC signature (jwtService.isRefreshTokenValid)
   - Parse claims with HMAC secret key
   - Check type == "refresh"
   - Check not expired
   -> Invalid signature/expired -> 401

2. Find in DB (refreshTokenService.findByToken)
   -> Not found -> 404

3. REPLAY DETECTION: Check if revoked
   -> If revoked:
      - Someone is reusing a stolen token!
      - refreshTokenService.revokeTokenFamily(tokenFamily) -- revokes ALL tokens in the family
      - Throw 401 "Refresh token reuse detected -- all sessions invalidated"

4. Check usability (not revoked + not expired)
   -> Not usable -> 401

5. Extract username from refresh token claims

6. Revoke the old refresh token (single-use enforcement)
   -> refreshToken.setRevoked(true)

7. Generate new access token (RS256) + new refresh token (HS256)

8. Create rotated token in same family:
   -> refreshTokenService.createRotated(user, newRefreshToken, existingTokenFamily)

9. Return { accessToken: newJwt, refreshToken: newRefreshToken }
```

#### Security Properties

- **Single-use:** Each refresh token can only be used once. After use, it's marked revoked.
- **Family-based revocation:** If a previously-revoked token is presented (indicating theft), ALL tokens in the family are revoked, forcing re-login on all devices sharing that family.
- **Bounded lifetime:** Refresh tokens have an expiry (`jwt.refresh-expiration` config).

---

## 2. How to Test SAML Locally

### Option A: Keycloak as Local IdP

1. **Start the platform:**
   ```bash
   docker compose up -d postgres redis
   ./mvnw spring-boot:run
   ```

2. **Start Keycloak:**
   ```bash
   docker run -d --name keycloak \
     -p 8180:8080 \
     -e KEYCLOAK_ADMIN=admin \
     -e KEYCLOAK_ADMIN_PASSWORD=admin \
     quay.io/keycloak/keycloak:24.0 start-dev
   ```

3. **Configure Keycloak:**
   - Open `http://localhost:8180/admin` (admin/admin).
   - Create a realm (e.g., `interview-test`).
   - Create a client with protocol = SAML:
     - Client ID: `interview-platform-sp`
     - Valid Redirect URIs: `http://localhost:8080/login/saml2/sso/keycloak-local`
     - Set Name ID Format to email.
   - Create a test user with email attribute.
   - Download the IdP metadata or note the SSO URL and certificate.

4. **Register SSO configuration via API:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/sso \
     -H "Authorization: Bearer <admin-jwt>" \
     -H "Content-Type: application/json" \
     -d '{
       "registrationId": "keycloak-local",
       "idpEntityId": "http://localhost:8180/realms/interview-test",
       "idpSsoUrl": "http://localhost:8180/realms/interview-test/protocol/saml",
       "idpCertificate": "<paste PEM certificate from Keycloak>",
       "spEntityId": "interview-platform-sp",
       "acsUrl": "http://localhost:8080/login/saml2/sso/keycloak-local",
       "signRequests": false,
       "enabled": true
     }'
   ```

5. **Test the flow:**
   - Navigate to `http://localhost:8080/saml2/authenticate/keycloak-local`.
   - You'll be redirected to Keycloak login page.
   - Login with the test user.
   - Keycloak redirects back to ACS URL with SAML Response.
   - Backend processes and redirects to frontend with JWT tokens.

6. **Verify:**
   - Check the redirect URL contains `accessToken` and `refreshToken` params.
   - Decode the JWT at jwt.io to verify claims.

### Option B: samltool.io (Quick Test)

1. Go to `https://www.samltool.com/idp_metadata.php` to generate mock IdP metadata.
2. Use the metadata values to configure via the SSO API.
3. Use the same tool to generate a SAML Response for testing assertion parsing.

---

## 3. How to Test Kafka

### Topics Used

| Topic                    | Producer                  | Consumer               | Event Types                            |
|--------------------------|---------------------------|------------------------|----------------------------------------|
| `notification-events`    | `NotificationProducer`    | `NotificationConsumer` | Email/SMS notifications (multi-channel)|
| `interview-events`       | `NotificationProducer`    | `NotificationConsumer` | Interview lifecycle (scheduled, cancelled, rescheduled, feedback) |

### Event Flow

```
API Action (e.g., schedule interview)
  -> InterviewEventListener (Spring @EventListener)
  -> NotificationProducer.sendNotification() / sendInterviewEvent()
  -> Kafka topic
  -> NotificationConsumer.consumeNotification() / consumeInterviewEvent()
  -> Email/SMS dispatch
```

### Setup

1. **Start Kafka with Docker Compose:**
   ```bash
   docker compose up -d zookeeper kafka
   ```
   Kafka is available at `localhost:9092`.

2. **Configure the app:**
   ```properties
   app.kafka.enabled=true
   spring.kafka.bootstrap-servers=localhost:9092
   ```

3. **Watch events with console consumer:**
   ```bash
   # Notification events
   docker compose exec kafka kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic notification-events \
     --from-beginning

   # Interview lifecycle events
   docker compose exec kafka kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic interview-events \
     --from-beginning
   ```

4. **Trigger events via API:**
   ```bash
   # Schedule an interview (triggers InterviewScheduledEvent -> Kafka)
   curl -X POST http://localhost:8080/api/v1/interviews \
     -H "Authorization: Bearer <jwt>" \
     -H "Content-Type: application/json" \
     -d '{
       "candidateId": "...",
       "interviewerIds": ["..."],
       "title": "Technical Interview",
       "startTime": "2025-01-15T10:00:00Z",
       "endTime": "2025-01-15T11:00:00Z"
     }'
   ```

5. **Verify:**
   - Check console consumer output for the notification message JSON.
   - Verify the consumer processed it: `docker compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group notification-service`
   - Check consumer group lag (should be 0 after processing).

### Message Format (NotificationMessage)

```json
{
  "eventType": "INTERVIEW_SCHEDULED",
  "recipientEmail": "candidate@example.com",
  "recipientPhone": "+1234567890",
  "subject": "Interview Scheduled",
  "body": "Your interview has been scheduled for...",
  "channels": ["EMAIL", "SMS"],
  "metadata": { "interviewId": "uuid", "scheduledBy": "recruiter@company.com" }
}
```

### Kafka Disabled Mode

When `app.kafka.enabled=false` (default for local dev without Docker), the `NotificationProducer` logs messages instead of sending to Kafka. The `NotificationConsumer` bean is not created (`@ConditionalOnProperty`).

---

## 4. How to Test Redis

### What's Stored in Redis

| Key Pattern                          | Purpose                       | TTL                |
|--------------------------------------|-------------------------------|--------------------|
| `ratelimit:ip:<ip>:<endpoint>`       | Auth endpoint rate limiting   | 60 seconds         |
| `ratelimit:user:<email>:general`     | Authenticated user rate limit | 60 seconds         |
| `ratelimit:ip:<ip>:general`          | Anonymous rate limit          | 60 seconds         |
| `token:blacklist:<tokenId>`          | Revoked JWT blacklist         | Token remaining TTL|

### Setup

```bash
docker compose up -d redis
# Redis available at localhost:6379
```

### How Rate Limiting Works

The `RateLimitingFilter` applies to all requests:

- **Auth endpoints** (`/api/v1/auth/*`): Per-IP limiting
  - `/login`: 5 req/min
  - `/register`: 10 req/min
  - `/forgot-password`: 3 req/min
  - Other auth: 10 req/min
- **General API**: 60 req/min per authenticated user, 30 req/min per anonymous IP.

Implementation: `RedisRateLimiterService` uses `INCR` + `EXPIRE`:
```
INCR ratelimit:<key>         -> returns current count
EXPIRE ratelimit:<key> 60    -> set TTL on first increment (count == 1)
```

If Redis is unavailable, falls back to `ConcurrentHashMap` in-memory.

### Verify Rate Limiting

```bash
# Monitor Redis commands in real-time
docker compose exec redis redis-cli MONITOR

# Test rate limiting - send 6 login requests rapidly
for i in {1..6}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@test.com","password":"wrong"}';
done

# Expected output:
# 401 (or appropriate error)
# 401
# 401
# 401
# 401
# 429  <-- 6th request is rate limited
```

### Verify via redis-cli

```bash
docker compose exec redis redis-cli

# List all rate limit keys
KEYS ratelimit:*

# Check a specific key's value and TTL
GET ratelimit:ip:172.18.0.1:login
TTL ratelimit:ip:172.18.0.1:login

# Check token blacklist
KEYS token:blacklist:*
```

### Rate Limit Response Headers

Every response includes:
```
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 3
```

When rate limited (429):
```json
{"status":429,"error":"Too Many Requests","message":"Rate limit exceeded. Try again later."}
```

---

## 5. How to Test OpenTelemetry

### Architecture

```
App (OTel Java Agent) --> OTel Collector (port 4318 HTTP) --> Jaeger (port 14250)
                                                          --> (Prometheus exporter :8889)
```

### Setup

```bash
docker compose up -d otel-collector jaeger
# App is configured via JAVA_TOOL_OPTIONS in compose.yaml
```

Key environment variables (already in `compose.yaml`):
```
OTEL_SERVICE_NAME=interview-platform-backend
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
OTEL_TRACES_SAMPLER=parentbased_traceidratio
OTEL_TRACES_SAMPLER_ARG=1.0 (100% sampling in dev)
OTEL_METRICS_EXPORTER=otlp
OTEL_LOGS_EXPORTER=otlp
OTEL_RESOURCE_ATTRIBUTES=service.namespace=interview-platform,deployment.environment=dev
OTEL_INSTRUMENTATION_COMMON_DB_STATEMENT_SANITIZER_ENABLED=true
OTEL_INSTRUMENTATION_KAFKA_EXPERIMENTAL_SPAN_ATTRIBUTES=true
```

### Make API Calls

```bash
# Generate a trace
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: test-correlation-123" \
  -d '{"email":"admin@test.com","password":"password123"}'
```

### View Traces in Jaeger

1. Open **Jaeger UI**: `http://localhost:16686`
2. Select service: `interview-platform-backend`
3. Click "Find Traces"
4. Click on a trace to see the span waterfall

### What You'll See in a Trace

A typical login request trace contains:

```
[HTTP] POST /api/v1/auth/login (root span)
  ├── [JDBC] SELECT from users WHERE email = ?
  ├── [JDBC] SELECT from account_lockouts WHERE email = ?
  ├── [JDBC] INSERT into login_attempts (...)
  ├── [JDBC] SELECT from user_roles WHERE user_id = ?
  ├── [JDBC] DELETE from refresh_tokens WHERE user_id = ?
  ├── [JDBC] INSERT into refresh_tokens (...)
  └── [HTTP Response] 200 OK
```

If Kafka is enabled:
```
[HTTP] POST /api/v1/interviews
  ├── [JDBC] INSERT into interviews (...)
  ├── [Kafka Producer] notification-events
  │     └── [Kafka Consumer] notification-events (linked trace)
  │           ├── [SMTP] Send email
  │           └── [HTTP] POST to SMS provider
  └── [HTTP Response] 201 Created
```

### How Correlation IDs Flow

```
Request arrives with X-Correlation-ID header (or one is generated)
  -> CorrelationIdFilter puts correlationId into MDC
  -> All log entries include correlationId field (via Logstash encoder)
  -> MdcTaskDecorator propagates MDC to @Async threads
  -> OTel agent adds traceId/spanId to MDC automatically
  -> Response includes X-Correlation-ID header for client tracking
```

To find all logs for a request:
```bash
# In log aggregator (e.g., Elasticsearch/Kibana):
correlationId: "test-correlation-123"

# Or search by traceId in Jaeger UI
```

### Verify Instrumentation

1. **DB spans**: Every JPA/JDBC call creates a span with sanitized SQL.
2. **HTTP spans**: Inbound and outbound HTTP requests are traced.
3. **Kafka spans**: Producer and consumer spans are linked (distributed trace context propagated via Kafka headers).
4. **Custom spans**: Any `@Async` method preserves parent trace context via `MdcTaskDecorator`.

---

## 6. Module Technical Details

### 6.1 Code Execution Engine

**Package:** `codeexecution`
**Key class:** `CodeExecutionService`
**Docker client:** `com.github.docker-java` (Docker Java API)

#### Supported Languages

| Language    | Docker Image         | Entry File  | Compile + Run Command                              |
|------------|---------------------|-------------|-----------------------------------------------------|
| Java       | `openjdk:21-slim`   | `Main.java` | `javac Main.java && java Main`                      |
| Python     | `python:3.12-slim`  | `main.py`   | `python3 /tmp/code/main.py`                         |
| JavaScript | `node:20-slim`      | `main.js`   | `node /tmp/code/main.js`                            |
| TypeScript | `node:20-slim`      | `main.ts`   | `npx --yes tsx main.ts`                             |
| C++        | `gcc:13`            | `main.cpp`  | `g++ -o main main.cpp && ./main`                    |
| C          | `gcc:13`            | `main.c`    | `gcc -o main main.c && ./main`                      |
| Go         | `golang:1.22-alpine`| `main.go`   | `go run main.go`                                    |
| Rust       | `rust:1.77-slim`    | `main.rs`   | `rustc -o /tmp/code/main main.rs && /tmp/code/main` |
| Ruby       | `ruby:3.3-slim`     | `main.rb`   | `ruby /tmp/code/main.rb`                            |
| PHP        | `php:8.3-cli`       | `main.php`  | `php /tmp/code/main.php`                            |

#### Container Security Constraints

```java
HostConfig hostConfig = HostConfig.newHostConfig()
    .withMemory(256 * 1024 * 1024L)     // 256MB RAM
    .withMemorySwap(256 * 1024 * 1024L) // No swap (swap == memory means swap disabled)
    .withCpuPeriod(100000)               // 100ms CPU period
    .withCpuQuota(50000)                 // 50% of one core
    .withPidsLimit(64)                   // Max 64 processes (prevents fork bombs)
    .withNetworkMode("none")             // NO network access
    .withReadonlyRootfs(false)           // Working dir must be writable
    .withSecurityOpts("no-new-privileges") // Can't escalate privileges
    .withCapDrop(Capability.ALL);        // Drop ALL Linux capabilities

// Container runs as user "nobody" (UID 65534)
.withUser("nobody")
```

#### Execution Lifecycle

```
1. SUBMIT (submitExecution):
   - Validate enabled, source code size (<100KB), language, timeout (<30s)
   - Check concurrent execution limit (AtomicInteger, max 10)
   - Create CodeExecution entity (status=QUEUED)
   - Dispatch to @Async executeInSandbox()

2. EXECUTE (@Async executeInSandbox):
   a. Set status=RUNNING, record startedAt
   b. Create Docker container with security constraints
   c. TAR archive creation:
      - Build minimal USTAR TAR: 512-byte header + padded file content + 1024-byte terminator
      - Header includes: filename, mode (0644), UID/GID (nobody/nogroup), size (octal), checksum
   d. Copy TAR into container at /tmp/code (copyArchiveToContainerCmd)
   e. Start container
   f. Attach stdin (if provided)
   g. Wait with timeout (WaitContainerResultCallback):
      - awaitCompletion(timeoutMs, TimeUnit.MILLISECONDS)
      - If not completed: kill container, set status=TIMEOUT
   h. Collect stdout (logContainerCmd withStdOut, max 64KB)
   i. Collect stderr (logContainerCmd withStdErr, max 64KB)
   j. Check OOM kill (inspectContainerCmd -> state.getOOMKilled())
   k. Set status=COMPLETED, record exitCode, executionTimeMs

3. CLEANUP (finally block):
   - Decrement activeExecutions counter
   - Force remove container + volumes (removeContainerCmd withForce withRemoveVolumes)
```

#### Configuration Properties (`code-execution.*`)

| Property                  | Default    | Description                              |
|--------------------------|------------|------------------------------------------|
| `enabled`                | `true`     | Feature toggle                           |
| `maxTimeoutMs`           | `30000`    | Maximum allowed timeout (30s)            |
| `defaultTimeoutMs`       | `10000`    | Default if not specified (10s)           |
| `memoryLimitBytes`       | 256MB      | Container memory limit                   |
| `cpuPeriodMicros`        | `100000`   | CPU period (100ms)                       |
| `cpuQuotaMicros`         | `50000`    | CPU quota (50% of period)                |
| `pidsLimit`              | `64`       | Max processes in container               |
| `maxOutputSize`          | `65536`    | Max stdout/stderr chars (64KB)           |
| `networkDisabled`        | `true`     | Disable network in containers            |
| `maxConcurrentExecutions`| `10`       | Global concurrent execution limit        |
| `maxSourceCodeSize`      | `100000`   | Max source code chars (100KB)            |

---

### 6.2 Workflow Engine

**Package:** `workflow`
**Key classes:** `WorkflowEngineService`, `WorkflowConditionEvaluator`, `WorkflowActionExecutor`

#### Architecture

```
Event occurs (e.g., FEEDBACK_SUBMITTED)
  -> Service calls workflowEngineService.processTrigger(event, context)
  -> @Async execution
  -> Find matching rules (by triggerEvent, enabled=true, ordered by priority DESC)
  -> For each rule:
       -> Evaluate condition (WorkflowConditionEvaluator)
       -> If condition met: execute action (WorkflowActionExecutor)
       -> Record WorkflowExecution (SUCCESS/SKIPPED/FAILED)
```

#### Trigger Events

| Event                    | When Fired                                    |
|--------------------------|-----------------------------------------------|
| `INTERVIEW_COMPLETED`    | Interview status changes to COMPLETED         |
| `FEEDBACK_SUBMITTED`     | An interviewer submits feedback               |
| `SCORE_THRESHOLD_MET`    | Average score crosses a configured threshold  |
| `ALL_FEEDBACK_RECEIVED`  | All assigned interviewers have submitted       |
| `CANDIDATE_APPLIED`      | Candidate submits a job application           |
| `OFFER_ACCEPTED`         | Candidate accepts an offer letter             |
| `OFFER_DECLINED`         | Candidate declines an offer letter            |

#### Condition Types

| Type                  | `conditionValue` Format  | Evaluation Logic                                     |
|----------------------|--------------------------|------------------------------------------------------|
| `SCORE_ABOVE`        | `"4.0"`                  | Average feedback rating > threshold                  |
| `SCORE_BELOW`        | `"2.5"`                  | Average feedback rating < threshold                  |
| `ALL_FEEDBACK_IN`    | (ignored)                | feedbackCount >= interviewerCount                    |
| `RECOMMENDATION_COUNT`| `"HIRE:2"`              | Count of feedback with recommendation >= required    |
| `STATUS_EQUALS`      | `"COMPLETED"`            | Interview status matches value                       |
| `CUSTOM_EXPRESSION`  | `"key=value"`            | Metadata key equals value (simple equality)          |

**RECOMMENDATION_COUNT format:** `RECOMMENDATION_TYPE:MINIMUM_COUNT`
- Valid types: `HIRE`, `NO_HIRE`, `STRONG_HIRE`, `STRONG_NO_HIRE` (from `FeedbackRecommendation` enum)

#### Action Types

| Type                       | `actionConfig`                                | Effect                                           |
|---------------------------|-----------------------------------------------|--------------------------------------------------|
| `ADVANCE_PIPELINE_STAGE`  | `{"feedback":"Auto-advanced"}`               | Advances candidate to next pipeline stage        |
| `SEND_EMAIL`              | `{"to":"...","subject":"...","body":"..."}`  | Sends email with placeholder substitution        |
| `CHANGE_INTERVIEW_STATUS` | `"COMPLETED"` or `{"status":"COMPLETED"}`    | Changes interview status                         |
| `REJECT_CANDIDATE`        | `{"feedback":"Auto-rejected"}`               | Rejects candidate in pipeline                    |
| `NOTIFY_RECRUITER`        | `{"subject":"...","body":"..."}`             | Emails the recruiter who scheduled the interview |
| `WEBHOOK_CALL`            | `{"url":"https://...","method":"POST"}`      | Calls external webhook with context payload      |

#### Placeholder Substitution in Action Config

Available placeholders in `subject` and `body` fields:
- `{{interviewId}}` - UUID of the interview
- `{{candidateId}}` - UUID of the candidate
- `{{entityId}}` - UUID of the triggering entity
- `{{entityType}}` - Type string of the entity
- `{{triggerEvent}}` - Name of the trigger event
- `{{meta.keyName}}` - Any value from the context metadata map

#### Workflow Rule Priority

Rules are executed in **descending priority order**. Higher priority number = executed first. If multiple rules match the same event, they all execute independently.

#### Dry-Run Testing

`POST /api/v1/workflows/{ruleId}/test` with a `WorkflowContext` body evaluates the condition but does NOT execute the action. Returns what would happen.

---

### 6.3 Encryption at Rest

**Package:** `encryption`
**Algorithm:** AES-256-GCM (authenticated encryption with associated data)

#### How AES-256-GCM Works

1. **Input:** 256-bit key + 12-byte random IV + plaintext
2. **Output:** ciphertext + 128-bit authentication tag
3. **Properties:**
   - Confidentiality (encryption)
   - Integrity (authentication tag detects tampering)
   - Each encryption uses a unique IV -> same plaintext produces different ciphertext

#### Storage Format

```
"ENC:" + Base64( IV[12 bytes] || Ciphertext[variable] || AuthTag[16 bytes] )
```

The `"ENC:"` prefix identifies encrypted values, enabling backward compatibility.

#### JPA AttributeConverter

```java
@Convert(converter = EncryptedStringConverter.class)
@Column(name = "phone_number")
private String phoneNumber;
```

- **On write** (`convertToDatabaseColumn`): calls `fieldEncryptionService.encrypt(attribute)`.
- **On read** (`convertToEntityAttribute`): calls `fieldEncryptionService.decrypt(dbData)`.
- **Transparent:** Application code works with plaintext; encryption/decryption happens at the JPA layer.

#### Backward Compatibility

The `decrypt()` method checks for the `"ENC:"` prefix:
- If present: decrypt normally.
- If absent: return value as-is (plaintext from before encryption was enabled).

This allows **gradual migration** - existing plaintext data continues to work, and gets encrypted on the next write.

#### Configuration (`app.encryption.*`)

| Property      | Default               | Description                                |
|--------------|----------------------|---------------------------------------------|
| `enabled`    | `true`               | Master toggle (disabled = plaintext storage)|
| `secretKey`  | (none)               | Base64-encoded 32-byte AES key (REQUIRED in prod) |
| `algorithm`  | `AES/GCM/NoPadding`  | Cipher algorithm                            |
| `ivLength`   | `12`                 | IV length in bytes (NIST recommended)       |
| `tagLength`  | `128`                | Auth tag length in bits                     |

#### Key Rotation Procedure

1. Generate new key: `openssl rand -base64 32`
2. Deploy `EncryptionMigrationRunner` (reads all encrypted fields, decrypts with old key, re-encrypts with new key).
3. Update `ENCRYPTION_SECRET_KEY` environment variable.
4. Restart application.
5. Migration runner re-encrypts on startup.

**Important:** Never delete the old key until migration is verified complete.

---

### 6.4 Calendar Sync

**Package:** `calendarsync`
**Providers:** Google Calendar, Microsoft Outlook (via `CalendarProviderService` interface)

#### OAuth Token Exchange Flow

1. Frontend obtains an authorization code from Google/Microsoft OAuth consent screen.
2. Frontend calls `POST /api/v1/calendar/connect` with `{ provider, authorizationCode, redirectUri }`.
3. Backend calls provider's token endpoint to exchange code for tokens:
   ```
   POST https://oauth2.googleapis.com/token   (Google)
   POST https://login.microsoftonline.com/.../oauth2/v2.0/token  (Microsoft)
   ```
4. Stores `accessToken`, `refreshToken`, `tokenExpiresAt` in `CalendarConnection` entity.

#### Bidirectional Sync Logic

**Outbound (local -> external):**
```
1. Load user's upcoming interviews (next 90 days) where user is participant
2. For each interview:
   a. Check if CalendarEvent exists for this connection+interview
   b. If exists: updateEvent() on provider
   c. If not: createEvent() on provider, save CalendarEvent mapping
```

**Inbound (external -> local):**
```
1. Call provider.getEvents(accessToken, calendarId, timeRange)
2. Currently: logs external events for awareness
3. Future: could auto-create tentative interview slots
```

#### Token Refresh Logic

`ensureValidToken(CalendarConnection connection)`:
```
1. If tokenExpiresAt is still in the future -> return current accessToken
2. If no refreshToken available -> throw BadRequestException
3. Call provider.refreshAccessToken(refreshToken)
4. Update connection with new accessToken, refreshToken (if rotated), tokenExpiresAt
5. Return new accessToken
```

#### Supported Operations

| Operation     | Google Calendar API                    | Microsoft Graph API                |
|--------------|----------------------------------------|------------------------------------|
| Create event | `POST /calendars/{id}/events`         | `POST /me/calendars/{id}/events`   |
| Update event | `PATCH /calendars/{id}/events/{id}`   | `PATCH /me/events/{id}`            |
| Delete event | `DELETE /calendars/{id}/events/{id}`  | `DELETE /me/events/{id}`           |
| List events  | `GET /calendars/{id}/events`          | `GET /me/calendarView`             |

---

### 6.5 Structured Logging

#### MDC Fields (Populated by `CorrelationIdFilter`)

| MDC Key          | Source                                    | Example                                    |
|-----------------|-------------------------------------------|--------------------------------------------|
| `correlationId` | `X-Correlation-ID` header or UUID gen     | `a1b2c3d4-e5f6-7890-abcd-ef1234567890`   |
| `traceId`       | `traceparent` header (W3C Trace Context)  | `4bf92f3577b34da6a3ce929d0e0e4736`        |
| `spanId`        | `traceparent` header                      | `00f067aa0ba902b7`                         |
| `requestMethod` | `request.getMethod()`                     | `POST`                                     |
| `requestUri`    | `request.getRequestURI()`                 | `/api/v1/auth/login`                       |
| `clientIp`      | X-Forwarded-For / X-Real-IP / remoteAddr  | `192.168.1.100`                            |
| `userAgent`     | `User-Agent` header (truncated 200 chars) | `Mozilla/5.0 ...`                          |
| `userId`        | SecurityContext (after auth)              | `admin@company.com`                        |

#### Filter Order

```
CorrelationIdFilter (HIGHEST_PRECEDENCE)
  -> RateLimitingFilter
  -> XssSanitizingFilter
  -> JwtAuthenticationFilter
  -> ApiKeyAuthenticationFilter
  -> Controller
```

#### Async Propagation (`MdcTaskDecorator`)

```java
// Problem: @Async methods run in a different thread, losing MDC context.
// Solution: MdcTaskDecorator copies MDC from calling thread to worker thread.

public Runnable decorate(Runnable runnable) {
    Map<String, String> contextMap = MDC.getCopyOfContextMap();  // Capture
    return () -> {
        MDC.setContextMap(contextMap);  // Set in worker
        try { runnable.run(); }
        finally { MDC.clear(); }        // Clean up
    };
}
```

Applied via `AsyncMdcConfig`:
```java
@Bean("taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("async-mdc-");
    executor.setTaskDecorator(mdcTaskDecorator);  // <-- MDC propagation
    return executor;
}
```

#### JSON Format in Production

Uses `net.logstash.logback:logstash-logback-encoder`. Log entries look like:

```json
{
  "@timestamp": "2025-01-15T10:30:00.000Z",
  "@version": "1",
  "message": "Login successful for user admin@company.com",
  "logger_name": "c.i.i.s.a.s.AuthenticationServiceImpl",
  "thread_name": "http-nio-8080-exec-1",
  "level": "INFO",
  "correlationId": "a1b2c3d4-...",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "requestMethod": "POST",
  "requestUri": "/api/v1/auth/login",
  "clientIp": "192.168.1.100",
  "userId": "admin@company.com"
}
```

#### How to Search Logs by Correlation ID

All log entries for a single request share the same `correlationId`. To trace a request across async operations:

```
# Elasticsearch/Kibana query:
correlationId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

# This returns all log lines from:
# - The HTTP handler thread
# - Any @Async worker threads (MDC propagated)
# - Kafka consumer threads (if trace context propagated via headers)
```

---

## 7. Database Architecture

### 7.1 Flyway Migration Strategy

**Location:** `src/main/resources/db/migration/`
**Naming:** `V{number}__{description}.sql` (e.g., `V1__initial_schema.sql`, `V15__add_workflow_tables.sql`)

**Rules:**
- Migrations are **numbered and sequential** (`V1`, `V2`, ..., `V15`, ...).
- **Never modify an existing migration** after it has been applied in any environment.
- To fix a previous migration, create a **new migration** with the corrective DDL.
- `baselineOnMigrate=true` allows Flyway to work with existing databases.
- `flyway.repair()` is called before `migrate()` to sync checksums (handles dev scenarios where migration files are edited).

**Configuration:** `FlywayMigrationConfig` bean manually configures Flyway with `repair()` + `migrate()` on startup.

### 7.2 DDL vs DML User Separation

**Class:** `FlywayDdlUserConfig`
**Activation:** `app.database.separate-users=true`

**Purpose:** Principle of least privilege (SOC2 compliance).

| User      | Privileges                           | Used By                 |
|-----------|--------------------------------------|-------------------------|
| DDL user  | CREATE, ALTER, DROP, SELECT, INSERT, UPDATE, DELETE | Flyway migrations only |
| DML user  | SELECT, INSERT, UPDATE, DELETE       | Application (HikariCP) |

**How it works:**
- When `app.database.separate-users=true`, `FlywayDdlUserConfig` creates a `@Primary` Flyway bean with separate DDL credentials.
- The application datasource (`spring.datasource.*`) uses the DML-only user.
- If the application is compromised, the attacker cannot ALTER/DROP tables.

**Configuration:**
```properties
app.database.separate-users=true
app.database.ddl.url=jdbc:postgresql://host:5432/interview_platform
app.database.ddl.username=ddl_admin
app.database.ddl.password=<ddl-password>
spring.datasource.url=jdbc:postgresql://host:5432/interview_platform
spring.datasource.username=app_user
spring.datasource.password=<app-password>
```

**Safety:** `cleanDisabled=true` prevents Flyway from ever running `clean` in production.

### 7.3 Key Indexes

| Table                | Index                                        | Purpose                                    |
|---------------------|----------------------------------------------|--------------------------------------------|
| `users`             | UNIQUE on `email`                            | Login lookup, duplicate prevention         |
| `refresh_tokens`    | on `token`                                   | Token lookup during refresh                |
| `refresh_tokens`    | on `user_id`                                 | Revoke all tokens for user                 |
| `refresh_tokens`    | on `token_family`                            | Family-based revocation                    |
| `login_attempts`    | on `email`, `attempted_at`                   | Lockout window queries                     |
| `login_attempts`    | on `ip_address`, `attempted_at`              | IP-based blocking queries                  |
| `account_lockouts`  | UNIQUE on `email`                            | Fast lockout status check                  |
| `ip_blocklist`      | on `ip_address`, `active`                    | Pre-login IP check                         |
| `interviews`        | on `start_time`, `end_time`                  | Date range queries for scheduling          |
| `interviews`        | on `candidate_id`                            | Candidate's interview list                 |
| `workflow_rules`    | on `trigger_event`, `enabled`                | Rule lookup on event trigger               |
| `calendar_events`   | on `connection_id`, `interview_id`           | Prevent duplicate sync                     |
| `code_executions`   | on `coding_session_id`, `created_at`         | Session execution history                  |
| `sso_configurations`| UNIQUE on `registration_id`                  | SAML registration lookup                   |

### 7.4 Connection Pooling (HikariCP)

Spring Boot 4.x uses HikariCP by default. Key settings (configurable via `spring.datasource.hikari.*`):

| Setting                       | Default | Recommendation                          |
|------------------------------|---------|------------------------------------------|
| `maximum-pool-size`          | 10      | Set to `(2 * CPU cores) + disk spindles` |
| `minimum-idle`               | 10      | Same as max for consistent performance   |
| `idle-timeout`               | 600000  | 10 minutes                               |
| `max-lifetime`               | 1800000 | 30 minutes (below DB timeout)            |
| `connection-timeout`         | 30000   | 30 seconds                               |
| `leak-detection-threshold`   | 0       | Set to 60000 in dev for debugging        |

---

## 8. Error Handling

### 8.1 GlobalExceptionHandler

**Class:** `GlobalExceptionHandler` (`@RestControllerAdvice`)

Maps exceptions to consistent HTTP responses with the format:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found with email: unknown@test.com",
  "path": "/api/v1/users/unknown@test.com",
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### 8.2 Exception -> HTTP Status Mapping

| Exception Class                           | HTTP Status | When Thrown                                    |
|------------------------------------------|-------------|------------------------------------------------|
| `ResourceNotFoundException`              | 404         | Entity not found by ID/email/etc.              |
| `BadRequestException`                    | 400         | Invalid input, business rule violation         |
| `UnauthorizedException`                  | 401         | Auth failure, invalid/expired token            |
| `ForbiddenException`                     | 403         | Authenticated but insufficient permissions     |
| `DuplicateResourceException`             | 409         | Unique constraint violation (e.g., email)      |
| `BadCredentialsException` (Spring)       | 401         | Invalid email/password                         |
| `AccessDeniedException` (Spring)         | 403         | `@PreAuthorize` check failed                   |
| `MethodArgumentNotValidException`        | 400         | Bean validation failure                        |
| `ConstraintViolationException`           | 400         | JPA/Hibernate validation failure               |
| `MissingServletRequestParameterException`| 400         | Required query param missing                   |
| `MethodArgumentTypeMismatchException`    | 400         | Wrong type for path/query param                |
| `HttpRequestMethodNotSupportedException` | 405         | Wrong HTTP method for endpoint                 |
| `NoHandlerFoundException`                | 404         | No controller mapping found                    |
| `DataIntegrityViolationException`        | 409         | DB unique constraint violation                 |
| `HttpMessageNotReadableException`        | 400         | Malformed JSON body                            |
| `MaxUploadSizeExceededException`         | 400         | File upload exceeds 50MB limit                 |
| `MultipartException`                     | 400         | General file upload error                      |
| `IllegalStateException`                  | 409         | Invalid state transition                       |
| `RuntimeException` (catch-all)           | 500         | Unhandled runtime errors                       |
| `Exception` (catch-all)                  | 500         | Unhandled checked exceptions                   |

### 8.3 Custom Exception Classes

```java
// 404 - Resource not found
public class ResourceNotFoundException extends RuntimeException {
    // Constructor: (String resourceName, String fieldName, Object fieldValue)
    // Message: "{resourceName} not found with {fieldName}: {fieldValue}"
}

// 400 - Bad request / validation failure
public class BadRequestException extends RuntimeException {
    // Constructor: (String message)
}

// 401 - Authentication failure
public class UnauthorizedException extends RuntimeException {
    // Constructor: (String message)
}

// 403 - Forbidden (authenticated but not authorized)
public class ForbiddenException extends RuntimeException {
    // Constructor: (String message)
}

// 409 - Duplicate resource (conflict)
public class DuplicateResourceException extends RuntimeException {
    // Constructor: (String resourceName, String fieldName, Object fieldValue)
    // Message: "{resourceName} already exists with {fieldName}: {fieldValue}"
}
```

### 8.4 Logging Strategy in Error Handler

- All exceptions are logged at `ERROR` level with full stack trace.
- The error message returned to the client is sanitized (no internal details for 500 errors).
- For validation errors, specific field errors are concatenated: `"field1: message1, field2: message2"`.

---

## Appendix: Quick Start

```bash
# Full stack
docker compose up -d

# App only (local dev with Postgres + Redis)
docker compose up -d postgres redis
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
./mvnw test

# OpenAPI docs (when running)
open http://localhost:8080/swagger-ui.html
```

### Environment Variables (Key)

| Variable                  | Purpose                               | Default                  |
|--------------------------|---------------------------------------|--------------------------|
| `DB_URL`                 | PostgreSQL JDBC URL                   | (required)               |
| `DB_USERNAME`            | Database username                     | (required)               |
| `DB_PASSWORD`            | Database password                     | (required)               |
| `JWT_SECRET`             | HMAC secret for refresh tokens        | (required)               |
| `JWT_EXPIRATION`         | Access token TTL in ms                | `3600000` (1h)           |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL in ms               | `604800000` (7d)         |
| `KAFKA_ENABLED`          | Enable Kafka integration              | `false`                  |
| `KAFKA_BOOTSTRAP_SERVERS`| Kafka broker address                  | `localhost:9092`         |
| `REDIS_HOST`             | Redis host                            | `localhost`              |
| `REDIS_PORT`             | Redis port                            | `6379`                   |
| `ENCRYPTION_SECRET_KEY`  | AES-256 key (base64, 32 bytes)        | (dev key if not set)     |
| `OTEL_SERVICE_NAME`      | OpenTelemetry service name            | `interview-platform-backend` |
| `VAULT_TOKEN`            | HashiCorp Vault token                 | `dev-root-token`         |

---

*Last updated: 2025-01-15. Generated from source code analysis.*
