# 🌐 HTTP Files — Quick Reference

This folder contains [JetBrains HTTP Client](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html) `.http` files for exploring the API. They also work with the [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client) extension for VS Code.

---

## 🚀 Setup (do this once)

1. **Start the app** (see the main [README](../README.md#-running-the-application)):
   ```bash
   mvn clean compile quarkus:dev -Pquarkus
   ```

2. **Generate JWT tokens** and paste them into [`http-client.env.json`](http-client.env.json):
   ```bash
   ./generate-jwt.sh ORGANIZER   # → jwt_organizer
   ./generate-jwt.sh SPEAKER     # → jwt_speaker
   ./generate-jwt.sh ATTENDEE    # → jwt_attendee
   ```

3. **(Optional) Start Jaeger** for tracing demos:
   ```bash
   docker compose up -d   # Jaeger UI → http://localhost:16686
   ```

In your HTTP client, select the **`dev`** environment from `http-client.env.json` so `{{jwt_organizer}}` etc. resolve.

---

## 📂 File Catalogue

### 🎤 For presenting / demoing

| File | Use it when… |
|---|---|
| **[`demos.http`](demos.http)** | **You're giving the talk.** Chronological walkthrough of every demo from every chapter, in slide order, with section headers tied to slide numbers. **Run top-to-bottom on stage.** |

### 📖 Per-resource references

Use these for ad-hoc exploration or when you want all calls for one resource in one place.

| File | Covers | Endpoints |
|---|---|---|
| [`sessions.http`](sessions.http) | Session CRUD (V1) | `GET/POST/PUT/DELETE /api/v1/sessions` |
| [`speakers.http`](speakers.http) | Speaker CRUD | `GET/POST/PUT/DELETE /api/v1/speakers` |
| [`rooms.http`](rooms.http) | Room reads (read-only domain) | `GET /api/v1/rooms` |
| [`versioning.http`](versioning.http) | URI vs. header-based versioning | `/api/v1/...`, `/api/v2/...`, `X-API-Version`, `Accept; version=` |
| [`security.http`](security.http) | JWT + RBAC (401 / 403 / 201 flow) | `POST /api/v1/sessions` with various tokens |
| [`signatures.http`](signatures.http) | HMAC-SHA256 request signature filter. ⚠️ **Filter is implemented but not applied to any endpoint by default** — see file header for how to enable. | `POST /api/v1/sessions` with `X-Signature` |
| [`health.http`](health.http) | MicroProfile Health probes | `GET /health`, `/health/live`, `/health/ready` |
| [`errors.http`](errors.http) | RFC 9457 Problem Details responses | 404, 400 (validation + profanity), sanitization, OpenAPI |

### ⚙️ Configuration

| File | Purpose |
|---|---|
| [`http-client.env.json`](http-client.env.json) | Environment variables: `host`, `jwt_organizer`, `jwt_speaker`, `jwt_attendee`, `session_id`, `speaker_id`. Edit before first use. |

---

## 🗺️ Demo → Chapter map

If you're looking for the HTTP calls that back a specific chapter of the talk:

| Chapter / Slide | Primary file | Also see |
|---|---|---|
| Ch1 — The Gatekeepers (sanitization, validation, audit) | `demos.http` § Ch1 | `errors.http`, `sessions.http` |
| Ch2 — The Security Shield (JWT, RBAC, signatures) | `demos.http` § Ch2 | `security.http`, `signatures.http` |
| Ch3 — The Lens (health, correlation, tracing) | `demos.http` § Ch3 | `health.http` |
| Ch4 — The Living Contract (OpenAPI) | `demos.http` § Ch4 | `errors.http` (OpenAPI section) |
| Ch5 — The Evolution (versioning) | `demos.http` § Ch5 | `versioning.http` |
| Ch6 — Sane Error Handling (RFC 9457) | `demos.http` § Ch6 | `errors.http` |

---

## 💡 Tips

- **JetBrains IDEs:** click the green ▶︎ in the gutter next to any `###` block to fire that request.
- **VS Code:** install [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client), then click `Send Request` above each block.
- **CLI alternative:** every request can be translated to `curl` — but the env-file token substitution is the main reason to prefer `.http` files during a live demo.
- **Body sanitization demos** (`<script>` payloads) are intentional — the server should respond `2xx` with the dangerous content stripped, never `5xx`. That's the whole point.

