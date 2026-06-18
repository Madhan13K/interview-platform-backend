# Services & Credentials Guide

A practical "how to get everything working" reference for the Interview Platform Backend.

---

## 1. Service Inventory Table

| Service | Purpose | Required? | Free Tier? | Credentials Needed |
|---------|---------|-----------|------------|-------------------|
| PostgreSQL | Primary database | Yes | Self-hosted (Docker) | Username/password (local defaults) |
| Redis | Caching, session store, rate limiting | Yes | Self-hosted (Docker) | None (no auth in dev) |
| Apache Kafka | Async event streaming, notifications | Yes | Self-hosted (Docker) | None (no auth in dev) |
| LocalStack (S3) | File/document storage | Yes | Self-hosted (Docker) | Fake AWS keys (`test`/`test`) |
| HashiCorp Vault | Secret management | No (dev) / Yes (prod) | Self-hosted (Docker) | Dev root token |
| OTel Collector | Telemetry pipeline | No | Self-hosted (Docker) | None |
| Jaeger | Distributed tracing UI | No | Self-hosted (Docker) | None |
| Google OAuth2 | Social login | No | Yes (free) | Client ID + Client Secret |
| GitHub OAuth2 | Social login | No | Yes (free) | Client ID + Client Secret |
| Microsoft OAuth2 | Social login | No | Yes (free) | Client ID + Client Secret |
| Gmail SMTP | Email notifications | No | Yes (app password) | Email + App Password |
| Twilio | SMS notifications | No | Yes (trial) | Account SID + Auth Token + Phone Number |
| Slack | Webhook notifications | No | Yes (free) | Webhook URL |
| Microsoft Teams | Webhook notifications | No | Yes (free) | Webhook URL |
| Zoom | Video meeting links | No | Yes (free tier) | API Key + API Secret |
| Google Meet | Video meeting links | No | Yes (reuses OAuth2) | Google OAuth2 credentials |
| DocuSign | E-signatures on offer letters | No | Yes (sandbox) | Integration Key + Account ID |
| HelloSign (Dropbox Sign) | E-signatures on offer letters | No | Yes (test mode) | API Key |
| Google Calendar API | Calendar sync | No | Yes (reuses OAuth2) | Google OAuth2 credentials |
| Microsoft Graph API | Outlook calendar sync | No | Yes (reuses OAuth2) | Microsoft OAuth2 credentials |
| SonarCloud | Static code analysis | No | Yes (open source) | Token |
| OWASP Dependency-Check | Vulnerability scanning | No | Yes (free/public NVD) | None |
| Docker | Code execution engine | Yes | Yes (free) | Docker socket access |

---

## 2. Infrastructure Services (Self-Hosted via Docker)

All infrastructure runs locally via `docker compose up`. No external signups needed.

### PostgreSQL 16

**What it does:** Primary relational database for all application data.

**Default local credentials:**
```
Host: localhost
Port: 5433 (mapped from container 5432)
Database: interview_platform
Username: admin
Password: postgres
```

**Health check:**
```bash
# From host
pg_isready -h localhost -p 5433 -U admin -d interview_platform

# Or connect directly
psql -h localhost -p 5433 -U admin -d interview_platform -c "SELECT 1;"

# Via Docker
docker compose exec postgres pg_isready -U admin -d interview_platform
```

---

### Redis 7

**What it does:** Caching layer, distributed rate limiting, session store, and pub/sub for real-time features.

**Default local credentials:**
```
Host: localhost
Port: 6379
Password: (none - no auth in dev)
Max Memory: 128mb (LRU eviction)
```

**Health check:**
```bash
# From host (requires redis-cli)
redis-cli ping
# Expected: PONG

# Via Docker
docker compose exec redis redis-cli ping

# Check info
redis-cli info server | head -5
```

---

### Apache Kafka (Confluent 7.6)

**What it does:** Event-driven messaging for async notifications, audit events, and inter-service communication.

**Default local credentials:**
```
Bootstrap Servers: localhost:9092
Security Protocol: PLAINTEXT (no auth in dev)
Zookeeper: localhost:2181
Auto-create topics: enabled
```

**Health check:**
```bash
# List topics
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check broker API versions (verifies broker is alive)
docker compose exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# Produce a test message
docker compose exec kafka kafka-console-producer --bootstrap-server localhost:9092 --topic test-topic
```

---

### LocalStack (S3)

**What it does:** Local AWS S3-compatible storage for resumes, offer letters, documents, and other file uploads.

**Default local credentials:**
```
Endpoint: http://localhost:4566
Region: us-east-1
Access Key: test
Secret Key: test
Bucket: interview-platform-documents
```

**Health check:**
```bash
# Check LocalStack health
curl http://localhost:4566/_localstack/health

# List S3 buckets
aws --endpoint-url=http://localhost:4566 s3 ls

# Create bucket manually (if needed)
aws --endpoint-url=http://localhost:4566 s3 mb s3://interview-platform-documents

# Upload a test file
echo "test" | aws --endpoint-url=http://localhost:4566 s3 cp - s3://interview-platform-documents/test.txt

# Verify
aws --endpoint-url=http://localhost:4566 s3 ls s3://interview-platform-documents/
```

> **Note:** Set `AWS_ACCESS_KEY_ID=test` and `AWS_SECRET_ACCESS_KEY=test` in your shell or use `--no-sign-request`.

---

### HashiCorp Vault 1.15

**What it does:** Secret management for production credentials (JWT keys, DB passwords, API keys). In dev mode, it uses an in-memory store with a known root token.

**Default local credentials:**
```
Address: http://localhost:8200
Root Token: dev-root-token
Auth Method: Token
KV Backend: secret/interview-platform
```

**Health check:**
```bash
# Check status
curl http://localhost:8200/v1/sys/health

# Using vault CLI
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=dev-root-token
vault status
vault secrets list

# Write a test secret
vault kv put secret/interview-platform jwt-secret="my-secret-key"

# Read it back
vault kv get secret/interview-platform
```

---

### OpenTelemetry Collector 0.102

**What it does:** Receives traces, metrics, and logs from the application (via OTLP) and routes them to Jaeger (traces) and Prometheus (metrics).

**Default local config:**
```
OTLP gRPC receiver: localhost:4317
OTLP HTTP receiver: localhost:4318
Prometheus exporter: localhost:8889
Health check: localhost:13133
```

**Health check:**
```bash
# Collector health
curl http://localhost:13133/

# Check Prometheus metrics endpoint
curl http://localhost:8889/metrics | head -20
```

---

### Jaeger 1.57

**What it does:** Trace visualization UI. View distributed traces, latency analysis, and service dependency graphs.

**Default local config:**
```
UI: http://localhost:16686
gRPC (from OTel Collector): localhost:14250
```

**Health check:**
```bash
# Jaeger health
curl http://localhost:14269/

# Open UI in browser
open http://localhost:16686
```

---

## 3. OAuth2 Providers

### Google OAuth2

**Step-by-step setup:**

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project (or select existing)
3. Navigate to **APIs & Services > Credentials**
4. Click **+ CREATE CREDENTIALS > OAuth client ID**
5. If prompted, configure the **OAuth consent screen** first:
   - User Type: **External** (for testing; Internal if using Google Workspace)
   - App name: `Interview Platform`
   - User support email: your email
   - Scopes: Add `openid`, `profile`, `email`
   - Test users: add your email
6. Back in Credentials, select **Web application**
7. Set **Authorized redirect URIs**:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
8. Copy the **Client ID** and **Client Secret**

**Scopes required:** `openid`, `profile`, `email`

**Additional scopes for Calendar Sync:** `https://www.googleapis.com/auth/calendar` (enable Calendar API in the project)

**Environment variables:**
```bash
GOOGLE_CLIENT_ID=123456789-abcdef.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxxxxxxxxxxxx
```

**Test locally:**
```bash
# Start app, then visit:
open "http://localhost:8080/oauth2/authorization/google"
# Should redirect to Google login, then back to your redirect URI
```

---

### GitHub OAuth2

**Step-by-step setup:**

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click **OAuth Apps > New OAuth App**
3. Fill in:
   - Application name: `Interview Platform (dev)`
   - Homepage URL: `http://localhost:8080`
   - Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`
4. Click **Register application**
5. Copy the **Client ID**
6. Click **Generate a new client secret** and copy it immediately

**Scopes required:** `user:email`, `read:user`

**Environment variables:**
```bash
GITHUB_CLIENT_ID=Ov23lixxxxxxxxxxx
GITHUB_CLIENT_SECRET=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**Test locally:**
```bash
open "http://localhost:8080/oauth2/authorization/github"
```

---

### Microsoft OAuth2 (Azure AD)

**Step-by-step setup:**

1. Go to [Azure Portal - App registrations](https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps/ApplicationsListBlade)
2. Click **+ New registration**
3. Fill in:
   - Name: `Interview Platform`
   - Supported account types: **Accounts in any organizational directory and personal Microsoft accounts**
   - Redirect URI: **Web** > `http://localhost:8080/login/oauth2/code/microsoft`
4. Click **Register**
5. Copy the **Application (client) ID**
6. Go to **Certificates & secrets > + New client secret**
7. Add a description, select expiry, click **Add**
8. Copy the **Value** (not the Secret ID) immediately

**Scopes required:** `openid`, `profile`, `email`

**Additional scopes for Outlook Calendar:** `Calendars.ReadWrite`, `offline_access`

**Environment variables:**
```bash
MICROSOFT_CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
MICROSOFT_CLIENT_SECRET=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**Test locally:**
```bash
open "http://localhost:8080/oauth2/authorization/microsoft"
```

---

## 4. Email Service (SMTP)

### Option A: Gmail App Password (Recommended for Dev)

**Step-by-step setup:**

1. Go to [Google Account Security](https://myaccount.google.com/security)
2. Enable **2-Step Verification** (required for app passwords)
3. Go to [App passwords](https://myaccount.google.com/apppasswords)
4. Select app: **Mail**, Select device: **Other** (enter "Interview Platform")
5. Click **Generate**
6. Copy the 16-character password (e.g., `abcd efgh ijkl mnop`)

**Environment variables:**
```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-real-email@gmail.com
MAIL_PASSWORD=abcdefghijklmnop    # No spaces, 16 chars
```

**Test:**
```bash
# The app will send emails via Gmail SMTP on user registration, password reset, etc.
# Check app logs for: "Email sent successfully to..."
# Or use the /api/v1/auth/forgot-password endpoint with a real email
```

---

### Option B: MailHog for Local Testing (No Real SMTP Needed)

MailHog captures all outgoing emails without actually sending them.

**Add to `compose.yaml`:**
```yaml
mailhog:
  image: mailhog/mailhog:latest
  ports:
    - '1025:1025'   # SMTP
    - '8025:8025'   # Web UI
```

**Environment variables:**
```bash
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=anything
MAIL_PASSWORD=anything
```

**Access captured emails:**
```bash
open http://localhost:8025
```

---

### Option C: SendGrid (Production)

1. Sign up at [SendGrid](https://signup.sendgrid.com/)
2. Go to **Settings > API Keys > Create API Key**
3. Select **Restricted Access** with Mail Send permissions
4. Copy the key (starts with `SG.`)

```bash
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=SG.xxxxxxxxxxxxxxxxxxxx
```

---

### Option D: Mailgun (Production)

1. Sign up at [Mailgun](https://signup.mailgun.com/new/signup)
2. Add and verify your domain
3. Get SMTP credentials from **Sending > Domain settings > SMTP credentials**

```bash
MAIL_HOST=smtp.mailgun.org
MAIL_PORT=587
MAIL_USERNAME=postmaster@your-domain.mailgun.org
MAIL_PASSWORD=your-mailgun-smtp-password
```

---

## 5. Messaging & Notifications

### Twilio SMS

**Step-by-step setup:**

1. Sign up at [Twilio Console](https://www.twilio.com/try-twilio) (free trial gives $15 credit)
2. After signup, you'll see your **Account SID** and **Auth Token** on the dashboard
3. Get a phone number: **Phone Numbers > Manage > Buy a number** (free trial includes one)
4. Verify your personal number for trial (trial only sends to verified numbers)

**Environment variables:**
```bash
SMS_ENABLED=true
SMS_PROVIDER=twilio

# Add these to application.yml or .env (not yet in defaults - add when implementing):
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_FROM_NUMBER=+15551234567
```

**Test:**
```bash
# Verify credentials via Twilio CLI
curl -X POST "https://api.twilio.com/2010-04-01/Accounts/${TWILIO_ACCOUNT_SID}/Messages.json" \
  --data-urlencode "Body=Test from Interview Platform" \
  --data-urlencode "From=${TWILIO_FROM_NUMBER}" \
  --data-urlencode "To=+1YOUR_VERIFIED_NUMBER" \
  -u "${TWILIO_ACCOUNT_SID}:${TWILIO_AUTH_TOKEN}"
```

> **Note:** Current implementation logs SMS messages. The Twilio SDK integration is scaffolded but requires adding `com.twilio.sdk:twilio` dependency to `pom.xml`.

---

### Slack Webhook

**Step-by-step setup:**

1. Go to [Slack API - Your Apps](https://api.slack.com/apps)
2. Click **Create New App > From scratch**
3. Name: `Interview Platform Notifications`, select your workspace
4. Go to **Incoming Webhooks** in the left sidebar
5. Toggle **Activate Incoming Webhooks** to ON
6. Click **Add New Webhook to Workspace**
7. Select the channel (e.g., `#hiring-notifications`)
8. Click **Allow**
9. Copy the **Webhook URL**

**Environment variables:**
```bash
SLACK_ENABLED=true
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/TXXXXX/BXXXXX/your-webhook-token-here
```

**Test:**
```bash
curl -X POST "${SLACK_WEBHOOK_URL}" \
  -H 'Content-Type: application/json' \
  -d '{"text": "Interview Platform test notification"}'
# Expected: "ok"
```

---

### Microsoft Teams Webhook

**Step-by-step setup:**

1. Open Microsoft Teams
2. Go to the channel where you want notifications
3. Click the **...** (more options) next to the channel name
4. Select **Connectors** (or **Manage channel** > **Connectors**)
5. Find **Incoming Webhook** and click **Configure**
6. Name: `Interview Platform`, optionally upload an icon
7. Click **Create**
8. Copy the **Webhook URL**

**Environment variables:**
```bash
TEAMS_ENABLED=true
TEAMS_WEBHOOK_URL=https://outlook.office.com/webhook/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx@xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx/IncomingWebhook/xxxxxxxxxxxxxxxxxxxxxxxxxxxx/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

**Test:**
```bash
curl -X POST "${TEAMS_WEBHOOK_URL}" \
  -H 'Content-Type: application/json' \
  -d '{"text": "Interview Platform test notification"}'
# Expected: "1" (Teams returns "1" on success)
```

---

## 6. Meeting Providers

### Zoom

**Step-by-step setup:**

1. Go to [Zoom App Marketplace](https://marketplace.zoom.us/)
2. Click **Develop > Build App**
3. Select **Server-to-Server OAuth** (for automated meeting creation)
4. Fill in app information
5. Add scopes:
   - `meeting:write:admin` (create meetings)
   - `meeting:read:admin` (read meeting details)
6. Once activated, go to **App Credentials**
7. Copy **Account ID**, **Client ID**, and **Client Secret**

**Alternative (JWT - deprecated but simpler for dev):**
1. In the Marketplace, create a **JWT App**
2. Copy the **API Key** and **API Secret**

**Environment variables:**
```bash
ZOOM_ENABLED=true
ZOOM_API_KEY=your-zoom-api-key
ZOOM_API_SECRET=your-zoom-api-secret
```

**Test:**
```bash
# Generate a JWT token manually to test
# The platform generates meeting links via the MeetingService when scheduling interviews
# Check logs for: "Generated Zoom meeting link: ..."
```

---

### Google Meet

**What it does:** Creates Google Meet links for interviews using the Google Calendar API (a calendar event with `conferenceData`).

**Setup:** Reuses the same Google OAuth2 credentials configured in Section 3.

**Additional steps:**

1. In [Google Cloud Console](https://console.cloud.google.com/), go to **APIs & Services > Library**
2. Search for **Google Calendar API** and click **Enable**
3. Ensure your OAuth consent screen includes the scope:
   ```
   https://www.googleapis.com/auth/calendar
   ```

**Environment variables:**
```bash
GOOGLE_MEET_ENABLED=true
GOOGLE_MEET_CLIENT_ID=${GOOGLE_CLIENT_ID}   # Same as OAuth2 client
```

**Test:**
```bash
# Create an interview with provider=GOOGLE_MEET via the API
# The platform will use the user's OAuth2 token to create a Calendar event with Meet link
```

---

## 7. E-Signature Providers

### DocuSign

**Step-by-step setup:**

1. Go to [DocuSign Developer Center](https://developers.docusign.com/)
2. Click **Create Free Account** (developer sandbox)
3. After signup, go to **Settings > Apps and Keys**
4. Click **Add App and Integration Key**
5. Name: `Interview Platform`
6. Copy the **Integration Key** (this is your client ID)
7. Under **Authentication**, add a redirect URI:
   ```
   http://localhost:8080/api/v1/offers/docusign/callback
   ```
8. Note your **API Account ID** (shown on the Apps and Keys page)
9. For server-to-server, generate an RSA keypair in the app settings

**Environment variables:**
```bash
DOCUSIGN_ENABLED=true
DOCUSIGN_API_URL=https://demo.docusign.net/restapi
DOCUSIGN_ACCOUNT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
DOCUSIGN_INTEGRATION_KEY=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

**Test:**
```bash
# Use the DocuSign sandbox UI to verify your app:
open "https://appdemo.docusign.com"
# Login with your developer account to see sent envelopes
```

> **Note:** Current implementation is scaffolded with placeholder logic. Full integration requires the `docusign-esign-java` SDK.

---

### HelloSign (Dropbox Sign)

**Step-by-step setup:**

1. Go to [Dropbox Sign (HelloSign)](https://www.hellosign.com/)
2. Sign up for a free account
3. Go to **Settings > API** (or [API Dashboard](https://app.hellosign.com/home/myAccount#api))
4. Copy your **API Key**
5. Enable **Test Mode** for development (no real signatures sent)

**Environment variables:**
```bash
HELLOSIGN_ENABLED=true
HELLOSIGN_API_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**Test:**
```bash
# Verify API key
curl -u "${HELLOSIGN_API_KEY}:" https://api.hellosign.com/v3/account
# Should return your account info
```

> **Note:** Current implementation is scaffolded with placeholder logic. Full integration requires the HelloSign Java SDK.

---

## 8. Calendar Sync

### Google Calendar API

**Setup:** Uses the same Google OAuth2 app from Section 3.

**Additional configuration:**

1. In [Google Cloud Console](https://console.cloud.google.com/apis/library):
   - Search **Google Calendar API** and click **Enable**
2. In OAuth consent screen, add scopes:
   ```
   https://www.googleapis.com/auth/calendar
   https://www.googleapis.com/auth/calendar.events
   ```
3. The platform uses the existing OAuth2 credentials (`GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`)

**How it works:**
- Users connect their Google Calendar via OAuth2 flow at `/api/v1/calendar-sync/connect/google`
- The platform exchanges the auth code for access + refresh tokens
- Events (interviews) are created/updated/deleted in the user's calendar
- Token refresh is automatic via the `GoogleCalendarProvider`

**Environment variables:** (already set via Google OAuth2)
```bash
GOOGLE_CLIENT_ID=<same as OAuth2>
GOOGLE_CLIENT_SECRET=<same as OAuth2>
CALENDAR_SYNC_ENABLED=true
CALENDAR_SYNC_INTERVAL=15
```

---

### Microsoft Graph API (Outlook Calendar)

**Setup:** Uses the same Microsoft OAuth2 app from Section 3.

**Additional configuration:**

1. In [Azure Portal > App registrations](https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps/ApplicationsListBlade):
   - Select your app
   - Go to **API permissions > Add a permission**
   - Select **Microsoft Graph > Delegated permissions**
   - Add:
     - `Calendars.ReadWrite`
     - `offline_access` (for refresh tokens)
   - Click **Grant admin consent** (if you're the admin)
2. Add redirect URI for calendar callback:
   ```
   http://localhost:8080/api/v1/calendar-sync/callback/outlook
   ```

**How it works:**
- Users connect their Outlook Calendar via OAuth2 flow at `/api/v1/calendar-sync/connect/outlook`
- Uses Microsoft Graph API (`https://graph.microsoft.com/v1.0/me/calendar/events`)
- Scopes requested during token exchange: `Calendars.ReadWrite offline_access`

**Environment variables:** (already set via Microsoft OAuth2)
```bash
MICROSOFT_CLIENT_ID=<same as OAuth2>
MICROSOFT_CLIENT_SECRET=<same as OAuth2>
```

---

## 9. Security Scanning (CI/CD)

### SonarCloud

**Step-by-step setup:**

1. Go to [SonarCloud](https://sonarcloud.io/) and sign up with GitHub
2. Click **+** > **Analyze new project**
3. Select your repository
4. Go to **My Account > Security > Generate Tokens**
5. Token name: `interview-platform-ci`
6. Copy the token (starts with `sqp_`)

**Run locally:**
```bash
# Set token
export SONAR_TOKEN=sqp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Run analysis
./mvnw sonar:sonar \
  -Dsonar.projectKey=interview-platform-backend \
  -Dsonar.organization=your-org \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.token=${SONAR_TOKEN}
```

**GitHub Actions secret:**
```
SONAR_TOKEN=sqp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

---

### OWASP Dependency-Check

**No credentials needed.** Uses the public NVD (National Vulnerability Database).

```bash
# Run locally
./mvnw dependency-check:check

# With CI profile
./mvnw verify -Psecurity-scan

# Reports generated at:
# target/dependency-check-report.html
# target/dependency-check-report.json
```

> **Note:** NVD rate limits anonymous requests. For CI, optionally set an NVD API key (free):
> Register at https://nvd.nist.gov/developers/request-an-api-key

```bash
# Optional: faster scans with NVD API key
./mvnw dependency-check:check -DnvdApiKey=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

---

### Trivy (Container Scanning)

**No credentials needed.** Open-source vulnerability scanner.

```bash
# Install
brew install trivy

# Scan the Docker image
docker build -t interview-platform:latest .
trivy image interview-platform:latest

# Scan filesystem (dependencies)
trivy fs --scanners vuln .
```

---

## 10. Secret Management

### HashiCorp Vault

#### Local Development (Dev Mode)

Dev mode runs entirely in Docker with no signup required:

```bash
docker compose up vault
```

**Access:**
```bash
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=dev-root-token

# Store secrets
vault kv put secret/interview-platform \
  jwt-secret="your-jwt-secret" \
  db-password="postgres" \
  encryption-key="$(openssl rand -base64 32)"

# Read secrets
vault kv get secret/interview-platform
```

**Application config** (already in `application-vault.yml`):
```bash
VAULT_ENABLED=true
VAULT_HOST=localhost
VAULT_PORT=8200
VAULT_SCHEME=http
VAULT_TOKEN=dev-root-token
```

#### Production Setup

1. Deploy Vault in HA mode (Consul backend or Raft)
2. Initialize and unseal:
   ```bash
   vault operator init -key-shares=5 -key-threshold=3
   vault operator unseal <key1>
   vault operator unseal <key2>
   vault operator unseal <key3>
   ```
3. Use AppRole or Kubernetes auth instead of token:
   ```bash
   vault auth enable approle
   vault write auth/approle/role/interview-platform \
     token_policies="interview-platform-policy" \
     token_ttl=1h \
     token_max_ttl=4h
   ```
4. Create a policy:
   ```hcl
   path "secret/data/interview-platform/*" {
     capabilities = ["read", "list"]
   }
   ```

**Production env vars:**
```bash
VAULT_ENABLED=true
VAULT_HOST=vault.internal.yourcompany.com
VAULT_PORT=8200
VAULT_SCHEME=https
VAULT_AUTH_METHOD=approle
VAULT_ROLE_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
VAULT_SECRET_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

---

## 11. SSO/SAML Identity Providers (for Testing)

### Okta Developer Account

**Step-by-step setup:**

1. Sign up at [Okta Developer](https://developer.okta.com/signup/)
2. After signup, go to **Applications > Create App Integration**
3. Select **SAML 2.0**
4. Fill in:
   - App name: `Interview Platform`
   - Single Sign-On URL: `http://localhost:8080/login/saml2/sso/okta`
   - Audience URI (SP Entity ID): `http://localhost:8080/saml2/service-provider-metadata/okta`
   - Name ID format: `EmailAddress`
   - Attribute Statements:
     - `email` -> `user.email`
     - `firstName` -> `user.firstName`
     - `lastName` -> `user.lastName`
5. Click **Finish**
6. Go to **Sign On** tab > copy the **Metadata URL**
7. Assign users/groups to the app

**Configure in platform:**
```
POST /api/v1/sso/configurations
{
  "organizationId": "<org-uuid>",
  "providerName": "Okta",
  "entityId": "http://www.okta.com/exkxxxxxxxxxxxxxxxxx",
  "ssoUrl": "https://your-org.okta.com/app/xxxxxxxxxxxxxxxxx/sso/saml",
  "certificate": "<X.509 cert from Okta metadata>",
  "enabled": true
}
```

---

### Keycloak (Local SAML IdP)

**Add to `compose.yaml`:**
```yaml
keycloak:
  image: quay.io/keycloak/keycloak:23.0
  ports:
    - '9090:8080'
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
  command: start-dev
```

**Setup:**
```bash
docker compose up keycloak

# Access admin console
open http://localhost:9090/admin
# Login: admin / admin
```

1. Create a realm: `interview-platform`
2. Go to **Clients > Create client**
3. Client type: **SAML**
4. Client ID: `http://localhost:8080/saml2/service-provider-metadata/keycloak`
5. Set:
   - Root URL: `http://localhost:8080`
   - Valid Redirect URIs: `http://localhost:8080/login/saml2/sso/keycloak`
   - IDP-Initiated SSO URL Name: `interview-platform`
6. Under **Keys** tab, disable "Client signature required"
7. Create a test user in **Users > Add user**
8. Export realm metadata from: `http://localhost:9090/realms/interview-platform/protocol/saml/descriptor`

---

### Azure AD B2C (Free Tier)

**Step-by-step setup:**

1. Go to [Azure Portal](https://portal.azure.com/)
2. Search for **Azure AD B2C** > Create a new tenant
3. In the B2C tenant, go to **App registrations > New registration**
4. Register a SAML app:
   - Name: `Interview Platform SAML`
   - Redirect URI: `http://localhost:8080/login/saml2/sso/azure`
5. Go to **Enterprise applications** > select your app > **Single sign-on** > **SAML**
6. Set:
   - Identifier (Entity ID): `http://localhost:8080/saml2/service-provider-metadata/azure`
   - Reply URL: `http://localhost:8080/login/saml2/sso/azure`
7. Download the **Federation Metadata XML**

> **Free tier:** Azure AD B2C is free for up to 50,000 authentications/month.

---

## 12. Minimum Viable Setup

| Setup Level | Services Needed | External Signups | What Works |
|-------------|----------------|------------------|------------|
| **Just run locally** | Docker only | None | Full app with DB, Redis, Kafka, S3 (LocalStack), tracing. Email/SMS/OAuth logged but not sent. |
| **Full local dev** | Docker + Gmail app password | Gmail 2FA + app password | Everything above + real email delivery (registration, password reset, notifications) |
| **All features working** | Docker + OAuth2 providers + SMTP + Slack | Google Cloud, GitHub, Azure, Gmail, Slack | Full OAuth2 login, email, Slack notifications, calendar sync |
| **Production** | Managed DB + Redis + Kafka + Vault + real S3 | All above + DocuSign + Twilio + Zoom + SonarCloud | Everything including e-signatures, SMS, video meetings, secret rotation |

### Quick Start (Minimum - Docker Only)

```bash
# 1. Clone and copy env
cp .env.example .env

# 2. Generate encryption key
ENCRYPTION_KEY=$(openssl rand -base64 32)
sed -i '' "s/ENCRYPTION_SECRET_KEY=/ENCRYPTION_SECRET_KEY=${ENCRYPTION_KEY}/" .env

# 3. Start infrastructure
docker compose up -d postgres redis kafka zookeeper localstack vault otel-collector jaeger

# 4. Run the app
./mvnw spring-boot:run

# 5. Verify
curl http://localhost:8080/actuator/health
```

---

## 13. Environment Variables Master List

Complete `.env` file with ALL variables grouped by service:

```bash
# =============================================================================
# Interview Platform Backend - Complete Environment Variables
# =============================================================================
# Copy this file to .env and update values as needed.
# Variables marked [REQUIRED] must be set for the app to start.
# Variables marked [OPTIONAL] have working defaults or disable features gracefully.
# =============================================================================

# =============================================================================
# DATABASE (PostgreSQL) [REQUIRED]
# =============================================================================
# JDBC connection URL. Use port 5433 for Docker-mapped PostgreSQL.
DB_URL=jdbc:postgresql://localhost:5433/interview_platform
# Database username
DB_USERNAME=admin
# Database password
DB_PASSWORD=postgres
# Maximum connection pool size (tune for production load)
DB_POOL_MAX=10
# Hibernate DDL mode: update (dev), validate (prod), none (flyway-only)
JPA_DDL_AUTO=update
# Separate DDL user (production: least-privilege app user + privileged DDL user)
DB_SEPARATE_USERS=false
DB_DDL_URL=jdbc:postgresql://localhost:5433/interview_platform
DB_DDL_USERNAME=admin
DB_DDL_PASSWORD=postgres

# =============================================================================
# SERVER [REQUIRED]
# =============================================================================
# Application port
SERVER_PORT=8080
# Spring Security default user password (for /actuator basic auth)
SPRING_SECURITY_PASSWORD=admin

# =============================================================================
# JWT AUTHENTICATION [REQUIRED]
# =============================================================================
# HMAC secret for signing JWT tokens (min 256 bits / 32 chars)
# Generate with: openssl rand -base64 32
JWT_SECRET=your-jwt-secret-key-here-min-256-bits
# Token expiration in milliseconds (86400000 = 24 hours)
JWT_EXPIRATION=86400000
# Refresh token secret (separate from access token secret)
JWT_REFRESH_SECRET=your-jwt-refresh-secret-key-here-min-256-bits
# Refresh token expiration (1209600000 = 14 days)
JWT_REFRESH_EXPIRATION=1209600000
# Load RSA keys from Vault instead of classpath (production)
RSA_FROM_VAULT=false

# =============================================================================
# DATA ENCRYPTION AT REST [REQUIRED for production]
# =============================================================================
# AES-256-GCM key for encrypting PII fields (SSN, salary, etc.)
# Generate with: openssl rand -base64 32
ENCRYPTION_ENABLED=true
ENCRYPTION_SECRET_KEY=

# =============================================================================
# OAUTH2 PROVIDERS [OPTIONAL - app starts without these]
# =============================================================================
# --- Google ---
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# --- GitHub ---
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret

# --- Microsoft (Azure AD) ---
MICROSOFT_CLIENT_ID=your-microsoft-client-id
MICROSOFT_CLIENT_SECRET=your-microsoft-client-secret

# Base URL for OAuth2 redirect URIs (must match provider configuration)
OAUTH2_BASE_URL=http://localhost:8080

# =============================================================================
# FRONTEND [REQUIRED]
# =============================================================================
# Frontend URL for OAuth2 callbacks, password reset links, email verification
FRONTEND_URL=http://localhost:5173
# CORS allowed origins (comma-separated)
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# =============================================================================
# EMAIL / SMTP [OPTIONAL - emails logged if invalid]
# =============================================================================
# SMTP host (smtp.gmail.com, smtp.sendgrid.net, localhost for MailHog)
MAIL_HOST=smtp.gmail.com
# SMTP port (587 for TLS, 1025 for MailHog)
MAIL_PORT=587
# SMTP username (email address for Gmail, "apikey" for SendGrid)
MAIL_USERNAME=your-email@gmail.com
# SMTP password (Gmail app password, SendGrid API key, etc.)
MAIL_PASSWORD=your-app-password

# =============================================================================
# REDIS [REQUIRED - used for caching and rate limiting]
# =============================================================================
# Redis host (localhost for Docker, redis hostname for compose network)
REDIS_HOST=localhost
# Redis port
REDIS_PORT=6379

# =============================================================================
# KAFKA [OPTIONAL - can be disabled]
# =============================================================================
# Enable/disable Kafka integration
KAFKA_ENABLED=true
# Kafka broker addresses
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
# Auto-start Kafka listeners
KAFKA_LISTENER_AUTOSTART=true

# =============================================================================
# AWS S3 / LOCALSTACK [REQUIRED for file uploads]
# =============================================================================
# S3 bucket name for documents
AWS_S3_BUCKET_NAME=interview-platform-documents
# AWS region
AWS_S3_REGION=us-east-1
# Access key (use "test" for LocalStack)
AWS_S3_ACCESS_KEY=test
# Secret key (use "test" for LocalStack)
AWS_S3_SECRET_KEY=test
# S3 endpoint (LocalStack URL for dev, omit or set to empty for real AWS)
AWS_S3_ENDPOINT=http://localhost:4566
# Pre-signed URL expiry in minutes
AWS_S3_PRESIGNED_URL_EXPIRY=60

# =============================================================================
# SMS NOTIFICATIONS [OPTIONAL]
# =============================================================================
# Enable/disable SMS sending
SMS_ENABLED=false
# Provider: "log" (dev), "twilio" (production), "sns" (AWS)
SMS_PROVIDER=log
# Twilio credentials (only needed if SMS_PROVIDER=twilio)
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_FROM_NUMBER=+15551234567

# =============================================================================
# SLACK NOTIFICATIONS [OPTIONAL]
# =============================================================================
# Enable/disable Slack notifications
SLACK_ENABLED=false
# Incoming webhook URL from Slack app configuration
SLACK_WEBHOOK_URL=

# =============================================================================
# MICROSOFT TEAMS NOTIFICATIONS [OPTIONAL]
# =============================================================================
# Enable/disable Teams notifications
TEAMS_ENABLED=false
# Incoming webhook URL from Teams connector
TEAMS_WEBHOOK_URL=

# =============================================================================
# GENERAL NOTIFICATIONS [OPTIONAL]
# =============================================================================
# Master switch for all notifications (email, SMS, webhooks)
NOTIFICATIONS_ENABLED=true

# =============================================================================
# MEETING PROVIDERS [OPTIONAL]
# =============================================================================
# --- Zoom ---
ZOOM_ENABLED=false
ZOOM_API_KEY=
ZOOM_API_SECRET=

# --- Google Meet (reuses Google OAuth2 credentials) ---
GOOGLE_MEET_ENABLED=false
GOOGLE_MEET_CLIENT_ID=

# =============================================================================
# E-SIGNATURE PROVIDERS [OPTIONAL]
# =============================================================================
# --- DocuSign ---
DOCUSIGN_ENABLED=false
DOCUSIGN_API_URL=https://demo.docusign.net/restapi
DOCUSIGN_ACCOUNT_ID=
DOCUSIGN_INTEGRATION_KEY=

# --- HelloSign (Dropbox Sign) ---
HELLOSIGN_ENABLED=false
HELLOSIGN_API_KEY=

# =============================================================================
# CALENDAR SYNC [OPTIONAL]
# =============================================================================
# Enable/disable calendar synchronization
CALENDAR_SYNC_ENABLED=true
# Sync interval in minutes
CALENDAR_SYNC_INTERVAL=15

# =============================================================================
# OFFER LETTERS [OPTIONAL]
# =============================================================================
# Days before offer expires
OFFER_EXPIRY_DAYS=7

# =============================================================================
# SSO / SAML [OPTIONAL]
# =============================================================================
# Base URL for SAML metadata and assertion consumer service
SSO_BASE_URL=http://localhost:8080

# =============================================================================
# HASHICORP VAULT [OPTIONAL in dev, REQUIRED in prod]
# =============================================================================
# Enable Vault integration
VAULT_ENABLED=false
# Vault server address
VAULT_HOST=localhost
VAULT_PORT=8200
VAULT_SCHEME=http
# Authentication token (use dev-root-token for local Docker Vault)
VAULT_TOKEN=dev-root-token

# =============================================================================
# OBSERVABILITY (OpenTelemetry) [OPTIONAL]
# =============================================================================
# Service name reported in traces/metrics
OTEL_SERVICE_NAME=interview-platform-backend
# OTLP endpoint (OTel Collector address)
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
# Protocol: http/protobuf or grpc
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
# Sampling strategy
OTEL_TRACES_SAMPLER=parentbased_traceidratio
# Sampling rate (1.0 = 100%, 0.1 = 10%)
OTEL_TRACES_SAMPLER_ARG=1.0
# Metrics exporter
OTEL_METRICS_EXPORTER=otlp
# Logs exporter
OTEL_LOGS_EXPORTER=otlp
# Resource attributes (key=value pairs, comma-separated)
OTEL_RESOURCE_ATTRIBUTES=service.namespace=interview-platform,deployment.environment=dev

# =============================================================================
# CODE EXECUTION ENGINE [OPTIONAL]
# =============================================================================
# Enable sandboxed code execution for technical interviews
CODE_EXECUTION_ENABLED=true
# Maximum allowed timeout per execution (ms)
CODE_EXECUTION_MAX_TIMEOUT=30000
# Default timeout if not specified (ms)
CODE_EXECUTION_DEFAULT_TIMEOUT=10000
# Memory limit per container (bytes, 268435456 = 256MB)
CODE_EXECUTION_MEMORY_LIMIT=268435456
# Max concurrent executions
CODE_EXECUTION_MAX_CONCURRENT=10
# Docker socket path
DOCKER_HOST=unix:///var/run/docker.sock

# =============================================================================
# ACCOUNT LOCKOUT / BRUTE FORCE PROTECTION [OPTIONAL]
# =============================================================================
LOCKOUT_ENABLED=true
LOCKOUT_MAX_ATTEMPTS=5
LOCKOUT_DURATION_MINUTES=30
LOCKOUT_WINDOW_MINUTES=15
LOCKOUT_MAX_IP_ATTEMPTS=20
LOCKOUT_IP_BLOCK_MINUTES=60
LOCKOUT_ALERTS_ENABLED=true
LOCKOUT_ALERT_THRESHOLD=3
LOCKOUT_IP_BLOCKING=true

# =============================================================================
# DOCKER COMPOSE [DEV ONLY]
# =============================================================================
# Let Spring Boot auto-manage Docker Compose lifecycle
DOCKER_COMPOSE_ENABLED=true

# =============================================================================
# CI/CD SECRETS (set in GitHub Actions / CI environment only)
# =============================================================================
# SONAR_TOKEN=sqp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
# NVD_API_KEY=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx  (optional, speeds up OWASP scans)
```

---

## Quick Reference: Which Credentials for Which Feature

| Feature | Minimum Credentials |
|---------|-------------------|
| User registration & login (email/password) | JWT_SECRET + DB |
| OAuth2 social login | GOOGLE/GITHUB/MICROSOFT_CLIENT_ID + SECRET |
| Email notifications | MAIL_USERNAME + MAIL_PASSWORD |
| SMS notifications | TWILIO_ACCOUNT_SID + AUTH_TOKEN + FROM_NUMBER |
| Slack alerts | SLACK_WEBHOOK_URL |
| Teams alerts | TEAMS_WEBHOOK_URL |
| File uploads (resumes, docs) | AWS_S3_* (LocalStack defaults work) |
| Calendar sync (Google) | GOOGLE_CLIENT_ID + SECRET + Calendar API enabled |
| Calendar sync (Outlook) | MICROSOFT_CLIENT_ID + SECRET + Calendars.ReadWrite permission |
| Zoom meetings | ZOOM_API_KEY + ZOOM_API_SECRET |
| Google Meet | GOOGLE_CLIENT_ID + Calendar API enabled |
| E-signatures (DocuSign) | DOCUSIGN_ACCOUNT_ID + INTEGRATION_KEY |
| E-signatures (HelloSign) | HELLOSIGN_API_KEY |
| Code execution (sandboxed) | Docker socket access |
| Distributed tracing | OTEL_* (defaults work with Docker Compose) |
| Secret management | VAULT_TOKEN (dev-root-token for local) |
| SonarCloud analysis | SONAR_TOKEN |
