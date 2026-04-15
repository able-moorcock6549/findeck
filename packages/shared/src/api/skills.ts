import { z } from "zod";

// --- Shared skill-browser contract ---

export const SkillSource = z.enum(["repo-local", "user-home"]);
export type SkillSource = z.infer<typeof SkillSource>;

export const SkillEntry = z.object({
  id: z.string(),
  name: z.string(),
  description: z.string(),
  source: SkillSource,
  sourceRoot: z.string(),
  skillPath: z.string(),
  definitionPath: z.string(),
  relativePath: z.string(),
});
export type SkillEntry = z.infer<typeof SkillEntry>;

export const ListSkillsQuery = z.object({
  source: SkillSource.optional(),
});
export type ListSkillsQuery = z.infer<typeof ListSkillsQuery>;

export const ListSkillsResponse = z.object({
  skills: z.array(SkillEntry),
});
export type ListSkillsResponse = z.infer<typeof ListSkillsResponse>;
