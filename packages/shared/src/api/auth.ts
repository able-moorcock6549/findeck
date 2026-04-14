import { z } from "zod";
import { AccessSession } from "../schemas/access-session.js";
import { MAX_PASSWORD_LENGTH, MAX_DEVICE_LABEL_LENGTH } from "../limits.js";

// --- POST /api/auth/login ---

export const LoginRequest = z.object({
  password: z.string().min(1).max(MAX_PASSWORD_LENGTH),
  deviceLabel: z.string().max(MAX_DEVICE_LABEL_LENGTH).optional(),
});
export type LoginRequest = z.infer<typeof LoginRequest>;

export const LoginResponse = z.object({
  token: z.string(),
  expiresAt: z.string().datetime(),
});
export type LoginResponse = z.infer<typeof LoginResponse>;

// --- POST /api/auth/password ---

export const ChangePasswordRequest = z.object({
  currentPassword: z.string().min(1).max(MAX_PASSWORD_LENGTH),
  newPassword: z.string().min(1).max(MAX_PASSWORD_LENGTH),
});
export type ChangePasswordRequest = z.infer<typeof ChangePasswordRequest>;

export const ChangePasswordResponse = z.object({
  ok: z.literal(true),
  restartScheduled: z.literal(true),
});
export type ChangePasswordResponse = z.infer<typeof ChangePasswordResponse>;

// --- POST /api/auth/logout ---

// No request body required (token comes from header/cookie).
// Response is a simple acknowledgment.
export const LogoutResponse = z.object({
  ok: z.literal(true),
});
export type LogoutResponse = z.infer<typeof LogoutResponse>;

// --- GET /api/auth/session ---

export const GetAuthSessionResponse = AccessSession;
export type GetAuthSessionResponse = z.infer<typeof GetAuthSessionResponse>;
