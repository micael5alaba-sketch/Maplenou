# Test the API with Swagger UI and Postman

Three tools call the same REST API. All of them need the stack running with the **demo** profile
(`SPRING_PROFILES_ACTIVE=demo` in `.env`, the default), which provides demo personas and a token endpoint.

| Tool | Best for | Where |
| --- | --- | --- |
| **Swagger UI** | discovering every endpoint, its parameters and its responses; trying one call | <http://localhost:8081/swagger-ui.html> |
| **Postman** | running the whole scenario with automatic checks; keeping your own requests | `backend/http/postman/` |
| **REST Client / IntelliJ HTTP client** | the same scenario from the code editor | `backend/http/support-api.http` |

The port is `BACKEND_HOST_PORT` in `.env` (default `8081`; 8080 is left to Bakend, the Maplenou core API).

## Get a token (demo profile)

The API expects `Authorization: Bearer <token>`. With the demo profile, `POST /api/v1/dev/token` returns one for a
demo persona:

| Persona | Role | `userId` |
| --- | --- | --- |
| Jean K. | customer | `11111111-1111-4111-8111-111111111111` |
| Sophie N. | support agent | `44444444-4444-4444-8444-444444444444` |
| Amina K. | support manager | `66666666-6666-4666-8666-666666666666` |

`GET /api/v1/dev/users` lists all seven personas (seller, delivery driver, administrator…). These two endpoints
do not exist outside the demo profile.

## Swagger UI

1. Open <http://localhost:8081/swagger-ui.html>.
2. Section **Demo login** (demo profile only) → `POST /api/v1/dev/token` → **Try it out** → body
   `{"userId": "11111111-1111-4111-8111-111111111111"}` → **Execute**. Copy `accessToken` from the response.
3. Click **Authorize** (padlock, top right), paste the token (without `Bearer `), **Authorize**, **Close**.
4. Any endpoint → **Try it out** → **Execute**. Errors come back as `application/problem+json` with a stable
   `code` (for example `CRITICAL_PRIORITY_RESERVED`).

The machine-readable description is <http://localhost:8081/v3/api-docs> (also versioned in
`docs/architecture/openapi.json`). Tokens expire after the configured lifetime: request a new one when calls
return `401`.

## Postman

### Import

1. Postman → **Import** → select both files of `backend/http/postman/`:
    - `Maplenou-Support-API.postman_collection.json` — 28 requests in 7 folders, with checks;
    - `Maplenou-local.postman_environment.json` — `baseUrl = http://localhost:8081`.
2. Select the environment **Maplenou — local (Docker)** (top right).

### Run

- **Whole scenario:** right-click the collection → **Run collection** → **Run**. Expected: 28 requests,
  36 checks passed. Each run creates a new ticket.
- **One request at a time:** run folder **0 · Setup** first (it stores the three tokens), then any request.
  Variables (`jeanToken`, `ticketId`, `categoriesEtag`, `reopenId`…) are filled by the test scripts and visible
  in the collection's **Variables** tab.

| Folder | What it shows |
| --- | --- |
| 0 · Setup | health, demo personas, tokens for Jean, Sophie and Amina |
| 1 · Requester | categories with `ETag` then `304` (cache), orders, ticket creation, `422` on the Critical priority |
| 2 · Support | queue, assignment, internal note, reply, ecosystem context |
| 3 · Requester follow-up | conversation without the internal note, notifications |
| 4 · Resolution and reopening | resolve, reopening request and approval, history |
| 5 · Manager | dashboard, report, `403` for a customer |
| 6 · Push notifications | push channel, device registration, test push, unregistration (see [Firebase](firebase.md)) |

Another server: duplicate the environment and change `baseUrl`. You can also generate a complete collection from
the OpenAPI description: **Import → Link → `http://localhost:8081/v3/api-docs`** (no scenario or checks).

### Command line (CI or quick check)

```bash
npx newman run backend/http/postman/Maplenou-Support-API.postman_collection.json \
  -e backend/http/postman/Maplenou-local.postman_environment.json
```

## Typical errors

| Answer | Meaning |
| --- | --- |
| `401` | no token, wrong token or expired token → get a new one |
| `403 FORBIDDEN` | the persona's role may not do this (e.g. a customer on `/dashboard`) |
| `404` on `/api/v1/dev/token` | the backend does not run the demo profile |
| `409 CONCURRENT_MODIFICATION` | the ticket changed in the meantime → read it again |
| `422 <CODE>` | a business rule refused the change; `rule` names it (BR-xxx) |
| Connection refused | the backend is not running or uses another port → `docker compose ps`, `scripts/doctor.ps1` |
