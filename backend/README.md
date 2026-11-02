# SMS Curtains backend (MongoDB Atlas)

This is the **server** for cross-device bills and approvals. The Android phone/terminal must call the HTTPS API; **never embed the MongoDB connection string inside an APK**.

## Deploy (required for anywhere access)

1. Create a MongoDB Atlas cluster and a database user with access only to your SMS Curtains database. Follow Atlas networking rules. Avoid an unrestricted network allowlist where possible.
2. On a Node.js 20+ host (Render, Railway, VPS, etc.), deploy this `backend/` folder as a Node service and expose it through HTTPS.
3. Copy `.env.example` to `.env` **on the server only** and configure `MONGODB_URI`, `JWT_SECRET`, `ADMIN_PASSWORD`, and `EMPLOYEE_PASSWORD`. Generate a random 32+ character secret. Production will refuse the demo passwords.
4. Run `npm install` and `npm start`. Test `https://YOUR-DOMAIN/health`.
5. On the Android login screen enter the same HTTPS API base URL (no `/auth`), then sign in. Example: `https://sms-curtains-api.example.com`.

**Demo/test credentials** (development only, if `.env` uses the supplied examples):
`admin / admin` — full access; `employee / employee` — create/read bills and progress; submit payment and change requests. Production must change both passwords.

For existing accounts, modifying `.env` does not automatically rotate a MongoDB user's stored bcrypt hash. Reset the account through a controlled administrative procedure before rolling out the real service.

## API

- `POST /auth/login` body `{"username":"employee","password":"employee"}` → signed JWT + role
- `POST /api/sync` with bearer token and `{"events": [...]}` → accepted events, rejection reason, bills, and review requests
- `GET /health` → database health

The API enforces permissions **on the server**. Employee `PAYMENT` and `MODIFY` operations are rejected; employees can only use `REQUEST_PAYMENT` and `REQUEST_MODIFY`. Admin decisions create the payment/changes after approval.

## Offline behavior

The Android app stores bills, payments, requests, and a durable event queue in Room. It shows a toast and plays the bundled recorded sound when connectivity is lost. A WorkManager job and foreground refresh retry sync. Offline employee payment requests never count as received money until approved. Accounts must connect once to obtain an authenticated cached login; a local-only demo mode is provided if the API URL is blank.

## Scope and hardening before live payment use

- This is an MVP, not a hosted database. To use bills on multiple devices you **must deploy the API**.
- Store your Atlas URI and JWT secret in server environment variables, not Git or Android app.
- Back up MongoDB regularly and restrict access; use TLS/HTTPS only.
- Add an audited administrative password-rotation workflow, session/token revocation, and automated integration tests before broad production rollout.
- Consider MongoDB transactions for payment and approval consistency under simultaneous operator access; current MVP uses idempotent IDs but is not a full financial ledger.
- This does **not** disable PAYable/Wiseasy certificate verification. PAYable authorization is still required for installing APKs on its customized terminals.
