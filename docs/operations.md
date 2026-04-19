# Operations

## Environment

Copy `.env.example` to `.env.local` and set:

- `FINDECK_PASSWORD`: required
- `HOST`: optional, defaults to `0.0.0.0`
- `PORT`: optional, defaults to `31807`
- `FINDECK_API_URL`: web runtime target, defaults to `http://127.0.0.1:31807`

## Local startup

```bash
npm install
npm run findeck -- doctor
npm run findeck -- up
```

The unified helper will build missing production artifacts before installing or refreshing the local launchd services on macOS.
If you need to pair a phone, run `npm run findeck -- pair` after the local server is up to print a fresh one-time pairing code.

Pairing contract:

- `POST /api/pairing/code` is local-only and generates a short-lived, one-time code.
- `POST /api/pairing/claim` consumes that code and returns `token`, `expiresAt`, and `trustedClient.clientId/clientSecret`.
- `POST /api/auth/reconnect` accepts the persisted `clientId/clientSecret` pair and mints a fresh auth token without reusing the pairing code.

Development:

```bash
npm run dev --workspace @findeck/server
npm run dev --workspace @findeck/web -- --port 31817
```

Production-style local run:

```bash
./scripts/findeck-server.sh
./scripts/findeck-web.sh
```

## Unified Control

The repo-local operator entrypoint is:

```bash
npm run findeck -- <command>
```

Supported commands:

- `up`
- `pair`
- `status`
- `logs`
- `restart`
- `web`
- `doctor`

Examples:

```bash
npm run findeck -- status
npm run findeck -- logs
npm run findeck -- web
npm run findeck -- pair
```

## launchd

Install launch agents:

```bash
npm run findeck -- up
```

Inspect services:

```bash
npm run findeck -- status
npm run findeck -- logs
```

launchd launchers and logs live under:

- `~/Library/Application Support/findeck/launchd/`
- `~/Library/Logs/findeck/`

This avoids macOS permission issues caused by pointing launchd directly at scripts and logs inside the repository tree.

## Inbox cleanup

Recent inbox/import history is backend state. To clear it, you only need to clean
the backend database rows and corresponding staging directories:

```bash
./scripts/clear-inbox.sh --host-id local
```

Use `--dry-run` to inspect matches first, and add `--submission-id` or
`--title-contains` for narrower cleanup. This does not require an APK update
unless you want a visible delete button in the app.
