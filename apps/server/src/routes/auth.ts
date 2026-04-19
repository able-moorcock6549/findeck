import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { spawn } from "node:child_process";
import { mkdirSync, readFileSync, writeFileSync, existsSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
  ChangePasswordRequest,
  type ChangePasswordResponse,
  LoginRequest,
  type LoginResponse,
  type LogoutResponse,
  type GetAuthSessionResponse,
} from "@findeck/shared";
import {
  getAppPassword,
  createToken,
  verifyToken,
  revokeToken,
} from "../auth/store.js";
import { extractBearerToken, requireAuth } from "../auth/middleware.js";
import { writeAuditLog } from "../audit/log.js";

export interface AuthRouteOptions {
  authRateLimitMax: number;
  authRateLimitWindowMs: number;
}

// ── Sliding-window auth rate limiter ───────────────────────────────
//
// Independent of @fastify/rate-limit — runs as a preHandler so it
// stacks on top of the plugin's onRequest global limiter.  Each
// login request is therefore checked by both the global per-IP cap
// AND this stricter auth-specific cap.

interface SlidingWindowLimiter {
  check(ip: string): { allowed: boolean; retryAfterSec: number };
}

function createSlidingWindowLimiter(
  max: number,
  windowMs: number,
): SlidingWindowLimiter {
  const attempts = new Map<string, number[]>();

  // Periodic cleanup of expired entries so the Map doesn't grow
  // unboundedly.  unref() ensures this doesn't keep the process alive.
  const cleanup = setInterval(() => {
    const cutoff = Date.now() - windowMs;
    for (const [ip, timestamps] of attempts) {
      const live = timestamps.filter((t) => t > cutoff);
      if (live.length === 0) attempts.delete(ip);
      else attempts.set(ip, live);
    }
  }, Math.max(windowMs, 60_000));
  cleanup.unref();

  return {
    check(ip: string) {
      const now = Date.now();
      const cutoff = now - windowMs;
      let timestamps = attempts.get(ip) ?? [];
      timestamps = timestamps.filter((t) => t > cutoff);

      if (timestamps.length >= max) {
        attempts.set(ip, timestamps);
        const retryAfterSec = Math.ceil(
          (timestamps[0] + windowMs - now) / 1000,
        );
        return { allowed: false, retryAfterSec };
      }

      timestamps.push(now);
      attempts.set(ip, timestamps);
      return { allowed: true, retryAfterSec: 0 };
    },
  };
}

/**
 * Auth route group — POST /api/auth/login, POST /api/auth/logout, GET /api/auth/session
 *
 * Password is checked against the FINDECK_PASSWORD env var.
 * Tokens are persisted in SQLite via the auth store.
 *
 * The login endpoint has a strict per-IP rate limit (preHandler) that
 * stacks on top of the global @fastify/rate-limit onRequest limiter,
 * giving true two-tier protection.
 */
export function authRoutes(options: AuthRouteOptions) {
  return async function register(app: FastifyInstance): Promise<void> {
    const authLimiter = createSlidingWindowLimiter(
      options.authRateLimitMax,
      options.authRateLimitWindowMs,
    );

    // Stricter auth-specific check that runs AFTER the global rate
    // limiter (which fires in onRequest).  If the global limiter
    // already rejected the request this hook never executes.
    async function authRateLimitHook(
      request: FastifyRequest,
      reply: FastifyReply,
    ): Promise<void> {
      const { allowed, retryAfterSec } = authLimiter.check(request.ip);
      if (!allowed) {
        reply.header("retry-after", String(retryAfterSec));
        reply
          .status(429)
          .send({ error: "Too many login attempts — try again later" });
      }
    }

    // --- POST /api/auth/login ---
    // No config.rateLimit override — the global @fastify/rate-limit
    // plugin applies its onRequest hook normally.  The preHandler
    // below adds the stricter auth-specific tier on top.
    app.post("/api/auth/login", {
      preHandler: authRateLimitHook,
      handler: async (request, reply) => {
        const parsed = LoginRequest.safeParse(request.body);
        if (!parsed.success) {
          return reply.status(400).send({ error: "Invalid request body" });
        }

        const { password, deviceLabel } = parsed.data;

        if (password !== getAppPassword()) {
          writeAuditLog({
            eventType: "login_failure",
            ip: request.ip,
            deviceLabel: deviceLabel ?? null,
            detail: "invalid_password",
          });
          return reply.status(401).send({ error: "Invalid password" });
        }

        const token = createToken(deviceLabel);
        writeAuditLog({
          eventType: "login_success",
          ip: request.ip,
          tokenId: token.tokenId,
          deviceLabel: token.deviceLabel ?? null,
        });
        const body: LoginResponse = {
          token: token.tokenId,
          expiresAt: token.expiresAt,
        };
        return reply.send(body);
      },
    });

    // --- POST /api/auth/logout ---
    app.post("/api/auth/logout", async (request, reply) => {
      const tokenId = extractBearerToken(request);
      if (!tokenId) {
        return reply.status(401).send({ error: "Missing or invalid token" });
      }

      // Capture device label before revocation deletes the row.
      const session = verifyToken(tokenId);
      revokeToken(tokenId);
      writeAuditLog({
        eventType: "logout",
        ip: request.ip,
        tokenId,
        deviceLabel: session?.deviceLabel ?? null,
      });
      const body: LogoutResponse = { ok: true };
      return reply.send(body);
    });

    app.post("/api/auth/password", {
      preHandler: requireAuth,
      handler: async (request, reply) => {
        const parsed = ChangePasswordRequest.safeParse(request.body);
        if (!parsed.success) {
          return reply.status(400).send({ error: "Invalid request body" });
        }

        const { currentPassword, newPassword } = parsed.data;
        if (currentPassword !== getAppPassword()) {
          return reply.status(401).send({ error: "Current password is incorrect" });
        }
        if (currentPassword === newPassword) {
          return reply.status(400).send({ error: "New password must be different" });
        }

        persistFindeckPassword(newPassword);
        scheduleServerRestart();

        const body: ChangePasswordResponse = {
          ok: true,
          restartScheduled: true,
        };
        return reply.send(body);
      },
    });

    // --- GET /api/auth/session ---
    app.get("/api/auth/session", async (request, reply) => {
      const tokenId = extractBearerToken(request);
      if (!tokenId) {
        return reply.status(401).send({ error: "Missing or invalid token" });
      }

      const session = verifyToken(tokenId);
      if (!session) {
        return reply.status(401).send({ error: "Token expired or invalid" });
      }

      const body: GetAuthSessionResponse = {
        tokenId: session.tokenId,
        createdAt: session.createdAt,
        expiresAt: session.expiresAt,
        deviceLabel: session.deviceLabel,
      };
      return reply.send(body);
    });
  };
}

const REPO_ROOT = resolve(fileURLToPath(new URL("../../../../", import.meta.url)));
const ENV_FILE =
  process.env["FINDECK_ENV_FILE"] ||
  process.env["CODEXREMOTE_ENV_FILE"] ||
  resolve(REPO_ROOT, ".env.local");
const SERVER_PLIST = resolve(
  process.env["HOME"] || "",
  "Library/LaunchAgents/dev.findeck.server.plist",
);

function persistFindeckPassword(newPassword: string): void {
  const envDir = dirname(ENV_FILE);
  mkdirSync(envDir, { recursive: true });

  const existing = existsSync(ENV_FILE) ? readFileSync(ENV_FILE, "utf8") : "";
  const withFindeck = upsertEnvLine(existing, "FINDECK_PASSWORD", newPassword);
  const nextEnv = upsertEnvLine(withFindeck, "CODEXREMOTE_PASSWORD", newPassword);
  writeFileSync(ENV_FILE, nextEnv, "utf8");

  if (existsSync(SERVER_PLIST)) {
    try {
      const updatedPlist = readFileSync(SERVER_PLIST, "utf8").replace(
        /(<key>FINDECK_PASSWORD<\/key>\s*<string>)([^<]*)(<\/string>)/,
        (_, prefix: string, _old: string, suffix: string) =>
          `${prefix}${escapeXml(newPassword)}${suffix}`,
      );
      writeFileSync(SERVER_PLIST, updatedPlist, "utf8");
    } catch {
      // .env.local is the primary source; plist update is best-effort.
    }
  }
}

function upsertEnvLine(content: string, key: string, value: string): string {
  const escaped = value.replace(/\\/g, "\\\\").replace(/"/g, '\\"');
  const line = `${key}="${escaped}"`;
  const pattern = new RegExp(`^${key}=.*$`, "m");
  if (pattern.test(content)) {
    return content.replace(pattern, line);
  }
  const trimmed = content.trimEnd();
  return trimmed + (trimmed.length > 0 ? "\n" : "") + line + "\n";
}

function escapeXml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

function scheduleServerRestart(): void {
  const uid = typeof process.getuid === "function" ? process.getuid() : 501;
  const command = [
    "sleep 1",
    `launchctl bootout gui/${uid} "${SERVER_PLIST}" >/dev/null 2>&1 || true`,
    `launchctl bootstrap gui/${uid} "${SERVER_PLIST}"`,
  ].join(" && ");

  const child = spawn("/bin/zsh", ["-lc", command], {
    detached: true,
    stdio: "ignore",
  });
  child.unref();
}
