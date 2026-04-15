import { beforeEach, describe, expect, it } from "vitest";
import { getDb, initDb } from "../db.js";
import {
  claimPairingCode,
  createPairingCode,
  reconnectTrustedClient,
  revokeTrustedClient,
  listTrustedClients,
} from "../pairing/store.js";
import { cleanTables } from "./helpers.js";

describe("Pairing store", () => {
  beforeEach(() => {
    initDb();
    cleanTables();
  });

  it("creates a one-time pairing code and persists only the hashed form", () => {
    const offer = createPairingCode();

    expect(offer.code).toMatch(/^[A-Z0-9]{4}-[A-Z0-9]{4}$/);

    const db = getDb();
    const row = db
      .prepare(
        "SELECT code_hash, code_display, claimed_at FROM pairing_codes WHERE code_display = ?",
      )
      .get(offer.code) as
      | { code_hash: string; code_display: string; claimed_at: string | null }
      | undefined;

    expect(row).toBeDefined();
    expect(row!.code_display).toBe(offer.code);
    expect(row!.code_hash).not.toBe(offer.code);
    expect(row!.claimed_at).toBeNull();
  });

  it("claims a pairing code exactly once and issues a trusted client", () => {
    const offer = createPairingCode();

    const firstClaim = claimPairingCode(offer.code, "Pixel 9");
    expect(firstClaim).not.toBeNull();
    expect(firstClaim!.trustedClient.deviceLabel).toBe("Pixel 9");
    expect(firstClaim!.trustedClient.clientSecret).toBeDefined();
    expect(firstClaim!.token.tokenId).toBeDefined();

    const secondClaim = claimPairingCode(offer.code, "Pixel 9");
    expect(secondClaim).toBeNull();

    const clients = listTrustedClients();
    expect(clients).toHaveLength(1);
    expect(clients[0].deviceLabel).toBe("Pixel 9");
  });

  it("reconnects with the persisted trusted client secret", () => {
    const offer = createPairingCode();
    const claim = claimPairingCode(offer.code, "Pixel 9");
    expect(claim).not.toBeNull();

    const reconnect = reconnectTrustedClient(
      claim!.trustedClient.clientId,
      claim!.trustedClient.clientSecret,
      "Pixel 9 Pro",
    );

    expect(reconnect).not.toBeNull();
    expect(reconnect!.trustedClient.clientId).toBe(claim!.trustedClient.clientId);
    expect(reconnect!.trustedClient.deviceLabel).toBe("Pixel 9 Pro");
    expect(reconnect!.token.tokenId).not.toBe(claim!.token.tokenId);
  });

  it("rejects reconnect when the trusted client is revoked", () => {
    const offer = createPairingCode();
    const claim = claimPairingCode(offer.code, "Pixel 9");
    expect(claim).not.toBeNull();

    expect(revokeTrustedClient(claim!.trustedClient.clientId)).toBe(true);
    expect(
      reconnectTrustedClient(
        claim!.trustedClient.clientId,
        claim!.trustedClient.clientSecret,
      ),
    ).toBeNull();
  });
});
