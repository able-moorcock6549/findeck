# Release Checklist

## Release focus for v0.4.0

- Android now supports one-time pairing plus trusted reconnect
- Android session navigation now includes archived-session restore and search
- Android composer now supports lightweight `/`, `@`, and `$` suggestions backed by real file and skill data
- Android settings now cover appearance, notification state, runtime defaults, and richer trusted-host metadata
- Android foreground feedback is clearer for run completion, stop, failure, and repo-action success

## Verified in this workspace

- `cd apps/server && ../../node_modules/.bin/vitest run src/__tests__/codex-cli-spawn.test.ts src/__tests__/codex-local-new-session.test.ts src/__tests__/codex-local-start-run.test.ts`
- `cd apps/server && ../../node_modules/.bin/vitest run src/__tests__/codex-sessions.test.ts src/__tests__/repo-status.test.ts src/__tests__/repo-actions.test.ts`
- `cd apps/server && ../../node_modules/.bin/tsc --noEmit`
- `npm run build --workspace @codexremote/shared`
- `npm test --workspace @codexremote/web`
- `npm run build --workspace @codexremote/server`
- `npm run build --workspace @codexremote/web`
- `cd apps/android && ./gradlew :app:compileDebugKotlin`
- `cd apps/android && ANDROID_HOME=/opt/homebrew/share/android-commandlinetools ANDROID_SDK_ROOT=/opt/homebrew/share/android-commandlinetools ./gradlew :app:assembleDebug`
- `cd apps/android && ./gradlew :app:testDebugUnitTest --tests dev.codexremote.android.ui.sessions.ComposerSuggestionsTest`
- `npm run test --workspace @codexremote/server -- src/__tests__/pairing-store.test.ts src/__tests__/pairing.test.ts src/__tests__/session-archive.test.ts src/__tests__/composer-ux.test.ts`
- resumed-session manual check: resume an existing thread and verify it can create a file under the project root
- Android manual check: trusted-host pairing succeeds and later cold launch prefers reconnect
- Android manual check: archived sessions can be viewed, searched, and restored
- Android manual check: smart composer shows `/`, `@`, and `$` suggestions and inserts the selected token correctly
- Android manual check: settings can change appearance, notification preferences, runtime defaults, trusted reconnect, and password safely
- Android manual check: session detail shows clear foreground feedback for repo actions and completed / failed / stopped runs

## Before publishing to GitHub

- keep release responsibilities split clearly:
  - `skills/codexremote-release/` is for GitHub repo release only
  - `skills/apk-smb-sync/` is for APK delivery to the SMB share only
- set a fresh password in `.env.local`
- if you changed the password through the app, confirm `.env.local` and the generated launchd plist agree on the same value before release
- do not commit `.env.local`, `data/`, `.run/`, or build outputs
- if needed, clear local inbox history with `./scripts/clear-inbox.sh --host-id local`
- ensure generated launchd files in `~/Library/LaunchAgents` are not copied back into the repo
- ensure generated launchd launcher scripts in `~/Library/Application Support/CodexRemote/launchd/` are not copied back into the repo
- review Android `local.properties` stays local-only
- keep `apps/android/local.properties.example` as the committed template, not your real SDK path

## Expected ignored outputs

- `node_modules/`
- `apps/web/.next/`
- `apps/server/dist/`
- `packages/shared/dist/`
- `apps/android/.gradle/`
- `apps/android/app/build/`
- `data/`
- `.run/`
