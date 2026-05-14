#!/usr/bin/env bash
# ============================================================
# generate-jwt.sh — Generate test JWTs for the Conference API
# ============================================================
#
# Usage:
#   ./generate-jwt.sh [ROLE]
#
# Roles: ORGANIZER (default), SPEAKER, ATTENDEE
#
# Prerequisites: openssl, base64, uuidgen (macOS/Linux built-in;
# on Windows use WSL or Git Bash).
#
# Keys:
#   Private key → /tmp/confapi_private.pem  (runtime use only)
#   Public key  → src/main/resources/META-INF/publicKey.pem
#
# ⚠️  FIRST-RUN SIDE EFFECT: if the private key is missing, this
#     script generates a fresh RSA-2048 key pair AND OVERWRITES the
#     committed src/main/resources/META-INF/publicKey.pem. You must
#     rebuild and restart the runtime afterwards. Subsequent runs
#     reuse the existing key and have no side effects.
#
# Note: Integration tests use a separate, committed key at
#       src/test/resources/test-private-key.pem — they are unaffected
#       by this script.
# ============================================================

set -euo pipefail

ROLE="${1:-ORGANIZER}"
PRIVATE_KEY="/tmp/confapi_private.pem"
ISSUER="https://confapi.example.com"
SUBJECT="test-user"
SPEAKER_ID="spk-duke"

# Generate key if missing
if [ ! -f "$PRIVATE_KEY" ]; then
    echo "⚠️  Private key not found. Generating a new RSA key pair..."
    openssl genpkey -algorithm RSA -out "$PRIVATE_KEY" -pkeyopt rsa_keygen_bits:2048 2>/dev/null
    openssl rsa -pubout -in "$PRIVATE_KEY" -out src/main/resources/META-INF/publicKey.pem 2>/dev/null
    echo "✅ Keys written. Restart the runtime to pick up the new public key."
fi

# --- Helper: Base64url encode (no padding) ---
b64url() {
    openssl enc -base64 -A | tr '+/' '-_' | tr -d '='
}

# --- JWT Header ---
HEADER=$(printf '{"alg":"RS256","typ":"JWT"}' | b64url)

# --- JWT Payload ---
NOW=$(date +%s)
EXP=$(( NOW + 86400 )) # 24 hours

PAYLOAD=$(printf '{
  "iss": "%s",
  "sub": "%s",
  "iat": %d,
  "exp": %d,
  "upn": "%s@example.com",
  "groups": ["%s"],
  "speaker_id": "%s",
  "jti": "%s"
}' "$ISSUER" "$SUBJECT" "$NOW" "$EXP" "$SUBJECT" "$ROLE" "$SPEAKER_ID" "$(uuidgen 2>/dev/null || cat /proc/sys/kernel/random/uuid 2>/dev/null || echo test-jti)" | b64url)

# --- Signature ---
SIGNATURE=$(printf '%s.%s' "$HEADER" "$PAYLOAD" \
    | openssl dgst -sha256 -sign "$PRIVATE_KEY" -binary \
    | b64url)

JWT="${HEADER}.${PAYLOAD}.${SIGNATURE}"

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  Generated JWT — Role: $ROLE"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "$JWT"
echo ""
echo "--- Use it: ---"
echo "curl -H 'Authorization: Bearer $JWT' http://localhost:8080/api/v1/sessions"
echo ""

