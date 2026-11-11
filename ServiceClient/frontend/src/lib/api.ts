import type { Problem } from "./types";
import { clearApiCache } from "./apiCache";

/** All browser calls go through the Next.js proxy, which adds the session token server-side. */
export const API_BASE = "/api/backend/api/v1/support";

export class ApiError extends Error {
  readonly status: number;
  readonly problem: Problem;

  constructor(problem: Problem) {
    super(problem.detail || problem.title || "Une erreur est survenue.");
    this.status = problem.status;
    this.problem = problem;
  }

  get code() {
    return this.problem.code;
  }
}

async function parseError(res: Response): Promise<ApiError> {
  let problem: Problem = { status: res.status };
  try {
    const body = await res.json();
    problem = { ...body, status: body.status ?? res.status };
  } catch {
    // not JSON
  }
  if (!problem.detail) {
    problem.detail =
      res.status === 401
        ? "Votre session a expiré. Reconnectez-vous."
        : res.status === 403
          ? "Vous n'êtes pas autorisé à effectuer cette action."
          : res.status === 404
            ? "Élément introuvable."
            : res.status >= 500
              ? "Le service est momentanément indisponible. Réessayez."
              : "La requête n'a pas pu être traitée.";
  }
  return new ApiError(problem);
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, { ...init, cache: "no-store" });
  } catch {
    throw new ApiError({ status: 0, detail: "Connexion impossible. Vérifiez votre réseau puis réessayez." });
  }
  if (res.status === 401 && typeof window !== "undefined") {
    window.location.assign(new URL("/login?expired=1", window.location.origin).href);
  }
  if (!res.ok) throw await parseError(res);
  // a change makes cached reads outdated: drop them (they are re-fetched on the next screen)
  if (init.method && init.method !== "GET") clearApiCache();
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const api = {
  get: <T>(path: string) => apiFetch<T>(path),
  post: <T>(path: string, body?: unknown) =>
    apiFetch<T>(path, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body),
    }),
  put: <T>(path: string, body?: unknown) =>
    apiFetch<T>(path, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body),
    }),
  del: <T>(path: string) => apiFetch<T>(path, { method: "DELETE" }),
  /** multipart: one JSON part named `jsonPart` + files under `files`. */
  multipart: <T>(path: string, jsonPart: string, json: unknown, files: File[]) => {
    const form = new FormData();
    form.append(jsonPart, new Blob([JSON.stringify(json)], { type: "application/json" }));
    files.forEach((f) => form.append("files", f, f.name));
    return apiFetch<T>(path, { method: "POST", body: form });
  },
};

export function toQuery(params: Record<string, string | number | boolean | string[] | undefined | null>): string {
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v === undefined || v === null || v === "") continue;
    if (Array.isArray(v)) v.forEach((x) => q.append(k, x));
    else q.append(k, String(v));
  }
  const s = q.toString();
  return s ? `?${s}` : "";
}

/** Stable id for idempotent message sending (AC-SB-006). */
export function clientId(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
