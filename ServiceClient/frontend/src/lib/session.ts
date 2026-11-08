import "server-only";
import { cookies } from "next/headers";
import type { Role } from "./types";

export const SESSION_COOKIE = "maplenou_session";

/**
 * Where the BFF reaches the API: `BACKEND_URL` (Compose, a public URL on Render's free plan) or, on a Render private
 * network, `BACKEND_HOSTPORT` (`host:port` given by render.yaml `fromService.property: hostport`).
 */
export const BACKEND_URL = backendUrl(process.env);

export function backendUrl(env: Record<string, string | undefined>): string {
  const url = env.BACKEND_URL?.trim();
  if (url) return url.replace(/\/+$/, "");
  const hostport = env.BACKEND_HOSTPORT?.trim();
  if (hostport) return `http://${hostport}`;
  return "http://localhost:8081";
}

export interface SessionUser {
  id: string;
  role: Role;
  name: string;
  expiresAt: number;
}

/**
 * Reads identity hints from the token payload for routing/display only.
 * The token is verified by the backend on every API call; nothing here is trusted for authorization.
 */
export function decodeToken(token: string): SessionUser | null {
  try {
    const payload = JSON.parse(Buffer.from(token.split(".")[1], "base64url").toString("utf8"));
    const roles: string[] = Array.isArray(payload.roles) ? payload.roles : [];
    const order: Role[] = ["ADMINISTRATOR", "SUPPORT_MANAGER", "SUPPORT_AGENT", "SELLER", "DELIVERY_DRIVER", "CUSTOMER"];
    const role = order.find((r) => roles.includes(r));
    if (!payload.sub || !role) return null;
    const expiresAt = Number(payload.exp) * 1000;
    if (expiresAt && expiresAt < Date.now()) return null;
    return { id: payload.sub, role, name: payload.name ?? "Utilisateur", expiresAt };
  } catch {
    return null;
  }
}

export async function getSessionToken(): Promise<string | null> {
  const store = await cookies();
  return store.get(SESSION_COOKIE)?.value ?? null;
}

export async function getSession(): Promise<SessionUser | null> {
  const token = await getSessionToken();
  return token ? decodeToken(token) : null;
}
