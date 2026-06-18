#!/bin/bash
# =============================================================================
# Setup Branch Protection for master
# =============================================================================
# Run this AFTER pushing to GitHub.
# Requires: gh cli authenticated (gh auth login)
#
# This locks the master branch so:
# - Direct pushes are blocked
# - Changes can only be merged via Pull Requests
# - PRs require at least 1 approval
# - Status checks must pass before merging
# - Conversations must be resolved
# - Force pushes are disabled
# - Branch cannot be deleted
# =============================================================================

set -e

REPO="madhan13k/interview-platform-backend"
BRANCH="master"

echo "Setting up branch protection for ${BRANCH} on ${REPO}..."

# Set branch protection rules using GitHub API via gh cli
gh api repos/${REPO}/branches/${BRANCH}/protection \
  --method PUT \
  --input - <<EOF
{
  "required_status_checks": {
    "strict": true,
    "contexts": [
      "Build & Unit Tests",
      "Integration Tests (Testcontainers)",
      "Docker Build & Trivy Scan"
    ]
  },
  "enforce_admins": false,
  "required_pull_request_reviews": {
    "required_approving_review_count": 1,
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false,
    "require_last_push_approval": false
  },
  "restrictions": null,
  "required_linear_history": true,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "block_creations": false,
  "required_conversation_resolution": true
}
EOF

echo ""
echo "Branch protection enabled for '${BRANCH}'!"
echo ""
echo "Rules applied:"
echo "  - Direct push to master: BLOCKED"
echo "  - Pull Request required: YES"
echo "  - Approvals required: 1"
echo "  - Stale reviews dismissed: YES"
echo "  - Status checks required: Build, Integration Tests, Docker Scan"
echo "  - Status checks must be up-to-date: YES"
echo "  - Conversations must be resolved: YES"
echo "  - Force push: DISABLED"
echo "  - Delete branch: DISABLED"
echo "  - Linear history (no merge commits): YES"
echo ""
echo "To contribute:"
echo "  1. git checkout -b feature/my-feature"
echo "  2. git push origin feature/my-feature"
echo "  3. gh pr create --base master"
echo "  4. Wait for CI checks + get 1 approval"
echo "  5. Merge via GitHub UI"
