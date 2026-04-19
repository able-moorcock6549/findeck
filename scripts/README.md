# scripts

中文：本地开发与部署使用的运维脚本。  
English: Operational helper scripts for local development and deployment.

## Included Scripts / 包含脚本

- `findeck.sh`: unified repo-local entry for startup, status, logs, web launch, and doctor checks
- `findeck.sh pair`: create and print a one-time pairing code from the local server
- `install-launchd.sh`: generate and install launch agents from templates
- `findeckctl.sh`: inspect, restart, start, stop, and tail logs
- `findeck-server.sh`: start the built Fastify server using `.env.local`
- `findeck-web.sh`: start the built web app
- `clear-inbox.sh`: remove inbox records from SQLite and delete matching staging directories
- `publish-android-apk.sh`: build the Android debug APK, rename it into a dated `findeck` APK artifact, and copy it to the configured SMB share

## Environment / 环境变量

中文：复制 `.env.example` 为 `.env.local`，至少设置以下变量：  
English: Copy `.env.example` to `.env.local` and set at least:

- `FINDECK_PASSWORD`
- optionally `HOST`
- optionally `PORT`
