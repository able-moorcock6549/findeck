import { describe, it, expect, beforeEach, afterEach } from "vitest";
import type { FastifyInstance } from "fastify";
import { mkdtempSync, mkdirSync, rmSync, writeFileSync, realpathSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import {
  MockCodexAdapter,
  createTestApp,
  loginHelper,
  authHeader,
  cleanTables,
} from "./helpers.js";

let app: FastifyInstance;
let adapter: MockCodexAdapter;
let token: string;
let originalHome: string | undefined;
let projectRoot: string;
let skillsHomeRoot: string;

describe("Composer UX APIs", () => {
  beforeEach(async () => {
    cleanTables();
    originalHome = process.env.HOME;
    projectRoot = mkdtempSync(path.join(tmpdir(), "findeck-files-"));
    skillsHomeRoot = mkdtempSync(path.join(tmpdir(), "findeck-home-"));

    mkdirSync(path.join(projectRoot, "src", "nested"), { recursive: true });
    writeFileSync(path.join(projectRoot, "README.md"), "# Example\n", "utf8");
    writeFileSync(path.join(projectRoot, ".env.local"), "FLAG=1\n", "utf8");
    writeFileSync(
      path.join(projectRoot, "src", "alpha-note.txt"),
      "alpha note\n",
      "utf8",
    );
    writeFileSync(
      path.join(projectRoot, "src", "nested", "beta.ts"),
      "export const beta = true;\n",
      "utf8",
    );

    const homeSkillDir = path.join(
      skillsHomeRoot,
      ".codex",
      "skills",
      "mobile-note",
    );
    mkdirSync(homeSkillDir, { recursive: true });
    writeFileSync(
      path.join(homeSkillDir, "SKILL.md"),
      [
        "---",
        "name: mobile-note",
        "description: Mobile note helper for local composer experiments",
        "---",
        "",
        "# Mobile Note",
        "",
        "Use this skill to verify local skill discovery.",
        "",
      ].join("\n"),
      "utf8",
    );

    process.env.HOME = skillsHomeRoot;

    ({ app, adapter } = await createTestApp());
    adapter.addSession("sess-files", { cwd: projectRoot });
    token = await loginHelper(app);
  });

  afterEach(async () => {
    if (app) {
      await app.close();
    }
    if (originalHome === undefined) {
      delete process.env.HOME;
    } else {
      process.env.HOME = originalHome;
    }
    rmSync(projectRoot, { recursive: true, force: true });
    rmSync(skillsHomeRoot, { recursive: true, force: true });
  });

  it("lists files from a session cwd root", async () => {
    const res = await app.inject({
      method: "GET",
      url: "/api/hosts/local/files?sessionId=sess-files",
      headers: authHeader(token),
    });

    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.body) as {
      rootPath: string;
      currentPath: string;
      parentPath: string | null;
      entries: Array<{ name: string; kind: string; relativePath: string }>;
    };

    expect(body.rootPath).toBe(realpathSync(projectRoot));
    expect(body.currentPath).toBe(realpathSync(projectRoot));
    expect(body.parentPath).toBeNull();
    expect(body.entries.map((entry) => entry.name)).toEqual(
      expect.arrayContaining(["src", "README.md", ".env.local"]),
    );
  });

  it("searches files from an explicit cwd", async () => {
    const res = await app.inject({
      method: "GET",
      url: `/api/hosts/local/files/search?cwd=${encodeURIComponent(projectRoot)}&query=beta`,
      headers: authHeader(token),
    });

    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.body) as {
      rootPath: string;
      currentPath: string;
      query: string;
      limit: number;
      results: Array<{ name: string; relativePath: string; kind: string }>;
    };

    expect(body.rootPath).toBe(realpathSync(projectRoot));
    expect(body.query).toBe("beta");
    expect(body.results.length).toBeGreaterThan(0);
    expect(body.results[0]?.relativePath).toBe("src/nested/beta.ts");
  });

  it("lists both repo-local and home skills", async () => {
    const res = await app.inject({
      method: "GET",
      url: "/api/skills",
      headers: authHeader(token),
    });

    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.body) as {
      skills: Array<{
        id: string;
        name: string;
        description: string;
        source: string;
        skillPath: string;
        definitionPath: string;
      }>;
    };

    expect(body.skills.some((skill) => skill.source === "repo-local")).toBe(true);
    expect(
      body.skills.some(
        (skill) => skill.source === "user-home" && skill.name === "mobile-note",
      ),
    ).toBe(true);
    expect(
      body.skills.find(
        (skill) => skill.source === "user-home" && skill.name === "mobile-note",
      )?.definitionPath,
    ).toMatch(/SKILL\.md$/);
  });
});
