import { z } from "zod";

// --- Trusted client entity (§6.2 extension) ---

export const TrustedClient = z.object({
  clientId: z.string(),
  deviceLabel: z.string().nullable(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  lastSeenAt: z.string().datetime().nullable(),
  revokedAt: z.string().datetime().nullable(),
});
export type TrustedClient = z.infer<typeof TrustedClient>;

export const TrustedClientCredentials = TrustedClient.extend({
  clientSecret: z.string(),
});
export type TrustedClientCredentials = z.infer<typeof TrustedClientCredentials>;
