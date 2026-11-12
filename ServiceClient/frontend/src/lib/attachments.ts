// Client-side mirror of DEC-003 so users get immediate feedback (AC-UX-009, AC-SB-005).
// The server validates again; these checks are a convenience, never a security boundary.

import { DEFAULT_LOCALE, type Locale } from "./i18n/config";
import { format } from "./i18n/define";
import { files as filesMessages } from "./i18n/messages/files";

export const ATTACHMENT_RULES = {
  maxFileBytes: 10 * 1024 * 1024,
  maxFilesPerMessage: 5,
  maxTicketBytes: 50 * 1024 * 1024,
  extensions: ["jpg", "jpeg", "png", "webp", "pdf"],
  accept: ".jpg,.jpeg,.png,.webp,.pdf,image/jpeg,image/png,image/webp,application/pdf",
} as const;

/** The first rule a selection breaks, as a language-neutral code (the wording lives in messages/files.ts). */
export type FileProblem =
  | { code: "tooMany"; max: number }
  | { code: "badFormat" | "empty" | "tooLarge"; name: string }
  | { code: "ticketTooLarge" };

export function checkFiles(files: { name: string; size: number }[], existingTicketBytes = 0): FileProblem | null {
  if (files.length > ATTACHMENT_RULES.maxFilesPerMessage) {
    return { code: "tooMany", max: ATTACHMENT_RULES.maxFilesPerMessage };
  }
  let total = existingTicketBytes;
  for (const f of files) {
    const ext = f.name.includes(".") ? f.name.split(".").pop()!.toLowerCase() : "";
    if (!(ATTACHMENT_RULES.extensions as readonly string[]).includes(ext)) return { code: "badFormat", name: f.name };
    if (f.size === 0) return { code: "empty", name: f.name };
    if (f.size > ATTACHMENT_RULES.maxFileBytes) return { code: "tooLarge", name: f.name };
    total += f.size;
  }
  return total > ATTACHMENT_RULES.maxTicketBytes ? { code: "ticketTooLarge" } : null;
}

/** Same check, worded for the user in the given language (null = the selection is valid). */
export function validateFiles(
  files: { name: string; size: number }[],
  existingTicketBytes = 0,
  locale: Locale = DEFAULT_LOCALE,
): string | null {
  const problem = checkFiles(files, existingTicketBytes);
  if (!problem) return null;
  const t = filesMessages[locale];
  return format(t[problem.code], "name" in problem ? { name: problem.name } : "max" in problem ? { max: problem.max } : {});
}
