# Maplenou Customer Service Frontend

Next.js 16 (React 19, TypeScript) web application for customer, seller, delivery-driver and support-agent
Customer Service workflows. Interface language: French.

## Responsibilities

- Support entry points (A1), ticket creation (A3), ticket listing (A2) and details (A4)
- Conversation and attachments
- Agent dashboard (B1), queue with search and filters (B2/B3), ticket detail with internal notes, actions and SLA (B4)
- Order/payment/delivery contextual information
- Notifications, account, administration (agents, categories, SLA calendar, report)

## Development

Requirements: Node 22. The backend must be running (see `../backend/README.md`, demo profile).

```bash
npm ci --legacy-peer-deps
cp .env.example .env.local     # BACKEND_URL=http://localhost:8081
npm run dev                    # http://localhost:3001
```

The browser never calls the API directly: `/api/backend/...` is a server-side proxy that adds the access token
stored in an httpOnly cookie (`src/app/api/backend/[...path]/route.ts`). The demo login page uses the backend's
demo endpoint; in production it is replaced by the Maplenou identity provider.

Frontend behavior must remain consistent with the backend API contract and documented business rules: every
action button is driven by the `allowedActions` returned by the API.

## Quality

```bash
npm run lint && npm run typecheck && npm test && npm run build
npx playwright test            # end-to-end, needs backend (demo profile) + `npm run build && npm start`
```

Install with `npm ci --legacy-peer-deps` (or add `legacy-peer-deps=true` to a local `.npmrc`): plain npm 10 hits a
peer-resolution bug with the optional peers of Next.js.
