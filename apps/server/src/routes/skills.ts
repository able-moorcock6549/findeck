import { readdir, readFile, stat } from "node:fs/promises";
import { homedir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import type { FastifyInstance, FastifyReply, FastifyRequest } from "fastify";
import {
  ListSkillsQuery,
  type ListSkillsResponse,
  type SkillEntry,
  type SkillSource,
} from "@findeck/shared";
import { LOCAL_HOST_ID } from "../constants.js";

const REPO_ROOT = path.resolve(fileURLToPath(new URL("../../../../", import.meta.url)));
const REPO_SKILLS_ROOT = path.join(REPO_ROOT, "skills");

function homeSkillsRoot(): string {
  return path.join(homedir(), ".codex", "skills");
}

interface SkillSourceRoot {
  source: SkillSource;
  sourceRoot: string;
}

function parseFrontMatter(content: string): {
  name?: string;
  description?: string;
  body: string;
} {
  const lines = content.split(/\r?\n/);
  if (lines[0]?.trim() !== "---") {
    return { body: content };
  }

  const frontMatterLines: string[] = [];
  let index = 1;
  while (index < lines.length && lines[index]?.trim() !== "---") {
    frontMatterLines.push(lines[index] ?? "");
    index += 1;
  }

  if (index >= lines.length) {
    return { body: content };
  }

  const meta: Record<string, string> = {};
  for (const line of frontMatterLines) {
    const separatorIndex = line.indexOf(":");
    if (separatorIndex < 0) continue;
    const key = line.slice(0, separatorIndex).trim();
    const value = line.slice(separatorIndex + 1).trim();
    if (!key) continue;
    meta[key] = value.replace(/^["']|["']$/g, "");
  }

  return {
    name: meta.name,
    description: meta.description,
    body: lines.slice(index + 1).join("\n"),
  };
}

function firstParagraph(body: string): string {
  const trimmed = body.trim();
  if (!trimmed) return "";
  const paragraph = trimmed
    .split(/\n\s*\n/)
    .map((chunk) => chunk.replace(/\s+/g, " ").trim())
    .find((chunk) => chunk.length > 0);
  return paragraph ?? "";
}

function sourcePriority(source: SkillSource): number {
  return source === "repo-local" ? 0 : 1;
}

async function walkSkillDefinitions(
  source: SkillSource,
  sourceRoot: string,
): Promise<SkillEntry[]> {
  const entries: SkillEntry[] = [];

  async function visitDirectory(directory: string): Promise<void> {
    const dirents = await readdir(directory, { withFileTypes: true }).catch(() => null);
    if (!dirents) return;

    for (const dirent of dirents) {
      const childPath = path.join(directory, dirent.name);
      if (dirent.isDirectory()) {
        await visitDirectory(childPath);
        continue;
      }
      if (!dirent.isFile() || dirent.name !== "SKILL.md") {
        continue;
      }

      const content = await readFile(childPath, "utf8").catch(() => null);
      if (!content) continue;

      const parsed = parseFrontMatter(content);
      const skillPath = path.dirname(childPath);
      const relativePath = path.relative(sourceRoot, skillPath) || path.basename(skillPath);
      const fallbackName = path.basename(skillPath);
      const description = parsed.description ?? firstParagraph(parsed.body);

      entries.push({
        id: `${source}:${relativePath}`,
        name: parsed.name ?? fallbackName,
        description,
        source,
        sourceRoot,
        skillPath,
        definitionPath: childPath,
        relativePath,
      });
    }
  }

  const rootInfo = await stat(sourceRoot).catch(() => null);
  if (!rootInfo || !rootInfo.isDirectory()) {
    return entries;
  }

  await visitDirectory(sourceRoot);
  return entries;
}

async function listAllSkills(): Promise<SkillEntry[]> {
  const roots: SkillSourceRoot[] = [
    { source: "repo-local", sourceRoot: REPO_SKILLS_ROOT },
    { source: "user-home", sourceRoot: homeSkillsRoot() },
  ];

  const nested = await Promise.all(
    roots.map(async (root) => walkSkillDefinitions(root.source, root.sourceRoot)),
  );

  return nested
    .flat()
    .sort((left, right) => {
      const sourceDiff = sourcePriority(left.source) - sourcePriority(right.source);
      if (sourceDiff !== 0) return sourceDiff;
      const nameDiff = left.name.localeCompare(right.name, "zh-CN");
      if (nameDiff !== 0) return nameDiff;
      return left.relativePath.localeCompare(right.relativePath, "zh-CN");
    });
}

function filterSkills(
  skills: SkillEntry[],
  options: {
    source?: SkillSource;
  },
): SkillEntry[] {
  const sourceFiltered = options.source
    ? skills.filter((skill) => skill.source === options.source)
    : skills;
  return sourceFiltered;
}

export function skillRoutes() {
  return async function register(app: FastifyInstance): Promise<void> {
    app.get(
      "/api/skills",
      async (request: FastifyRequest, reply: FastifyReply) => {
        const query = ListSkillsQuery.safeParse(request.query);
        if (!query.success) {
          return reply.status(400).send({ error: "Invalid query params" });
        }

        const skills = filterSkills(await listAllSkills(), {
          source: query.data.source,
        });
        const body: ListSkillsResponse = { skills };
        return reply.send(body);
      },
    );
  };
}
