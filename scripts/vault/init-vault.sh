#!/bin/bash
# =============================================================================
# Vault Initialization Script for Development
# =============================================================================
# This script initializes HashiCorp Vault with secrets needed by the application.
# Run this ONCE after starting the Vault container.
#
# Usage: ./scripts/vault/init-vault.sh
# =============================================================================

set -e

VAULT_ADDR="${VAULT_ADDR:-http://localhost:8200}"
VAULT_TOKEN="${VAULT_TOKEN:-dev-root-token}"

echo "Initializing Vault at ${VAULT_ADDR}..."

# Wait for Vault to be ready
until curl -s "${VAULT_ADDR}/v1/sys/health" > /dev/null 2>&1; do
    echo "Waiting for Vault to start..."
    sleep 2
done

export VAULT_ADDR
export VAULT_TOKEN

# Enable KV secrets engine v2
echo "Enabling KV secrets engine..."
vault secrets enable -path=secret -version=2 kv 2>/dev/null || true

# Generate RSA keys if they don't exist
echo "Generating RSA key pair..."
TMPDIR=$(mktemp -d)
openssl genrsa -out "${TMPDIR}/private.pem" 2048 2>/dev/null
openssl rsa -in "${TMPDIR}/private.pem" -pubout -out "${TMPDIR}/public.pem" 2>/dev/null

RSA_PRIVATE_KEY=$(cat "${TMPDIR}/private.pem")
RSA_PUBLIC_KEY=$(cat "${TMPDIR}/public.pem")

# Store application secrets
echo "Storing application secrets..."
vault kv put secret/interview-platform \
    db.url="jdbc:postgresql://postgres:5432/interview_platform" \
    db.username="app_user" \
    db.password="app_secure_password_$(openssl rand -hex 8)" \
    db.ddl-username="ddl_admin" \
    db.ddl-password="ddl_secure_password_$(openssl rand -hex 8)" \
    jwt.secret="$(openssl rand -base64 48)" \
    jwt.refresh-secret="$(openssl rand -base64 48)" \
    encryption.secret-key="$(openssl rand -base64 32)" \
    rsa.public-key-pem="${RSA_PUBLIC_KEY}" \
    rsa.private-key-pem="${RSA_PRIVATE_KEY}" \
    mail.password="changeme" \
    google.client-id="placeholder" \
    google.client-secret="placeholder" \
    github.client-id="placeholder" \
    github.client-secret="placeholder" \
    microsoft.client-id="placeholder" \
    microsoft.client-secret="placeholder"

# Store per-environment secrets
echo "Storing dev environment secrets..."
vault kv put secret/interview-platform/dev \
    db.url="jdbc:postgresql://localhost:5433/interview_platform" \
    db.username="admin" \
    db.password="postgres"

echo "Storing prod environment secrets..."
vault kv put secret/interview-platform/prod \
    db.url="jdbc:postgresql://prod-db:5432/interview_platform" \
    db.username="app_user" \
    db.password="CHANGE_ME_IN_PRODUCTION"

# Create a policy for the application
echo "Creating application policy..."
vault policy write interview-platform-app - <<EOF
path "secret/data/interview-platform" {
  capabilities = ["read"]
}
path "secret/data/interview-platform/*" {
  capabilities = ["read"]
}
EOF

# Create an AppRole for the application (for non-dev environments)
echo "Enabling AppRole auth..."
vault auth enable approle 2>/dev/null || true

vault write auth/approle/role/interview-platform \
    token_policies="interview-platform-app" \
    token_ttl=1h \
    token_max_ttl=4h \
    secret_id_ttl=720h

# Get the role ID and secret ID
ROLE_ID=$(vault read -field=role_id auth/approle/role/interview-platform/role-id)
SECRET_ID=$(vault write -field=secret_id -force auth/approle/role/interview-platform/secret-id)

echo ""
echo "============================================="
echo "Vault initialized successfully!"
echo "============================================="
echo "Dev Token:   ${VAULT_TOKEN}"
echo "AppRole ID:  ${ROLE_ID}"
echo "Secret ID:   ${SECRET_ID}"
echo ""
echo "To use in production, set:"
echo "  VAULT_HOST=your-vault-host"
echo "  VAULT_TOKEN=<token> OR use AppRole auth"
echo "  SPRING_PROFILES_ACTIVE=prod,vault"
echo "============================================="

# Cleanup
rm -rf "${TMPDIR}"
