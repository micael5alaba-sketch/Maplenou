/**
 * Client HTTP central pour le back-office admin — parle au même backend
 * Spring Boot que l'app Flutter (voir CLAUDE.md du dépôt Bakend). Injecte
 * automatiquement le JWT, gère le rafraîchissement sur 401, et uniformise
 * les erreurs (miroir de `frontend/lib/services/api_client.dart`).
 */

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "https://maplenou-backend.onrender.com";

const ACCESS_TOKEN_KEY = "maplenou_admin_access_token";
const REFRESH_TOKEN_KEY = "maplenou_admin_refresh_token";

export class ApiException extends Error {
  status: number;
  fieldErrors?: { field: string; message: string }[];

  constructor(status: number, message: string, fieldErrors?: { field: string; message: string }[]) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function saveTokens(accessToken: string, refreshToken: string) {
  window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function clearTokens() {
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}

async function tryRefresh(): Promise<boolean> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    const data = await res.json();
    saveTokens(data.accessToken, data.refreshToken);
    return true;
  } catch {
    return false;
  }
}

function extractFieldErrors(body: unknown): { field: string; message: string }[] | undefined {
  if (body && typeof body === "object" && "fieldErrors" in body) {
    const errors = (body as { fieldErrors?: unknown }).fieldErrors;
    if (Array.isArray(errors)) return errors as { field: string; message: string }[];
  }
  return undefined;
}

async function request<T>(
  method: string,
  path: string,
  options: { body?: unknown; query?: Record<string, string | number | boolean | undefined>; isRetry?: boolean } = {},
): Promise<T> {
  const { body, query, isRetry } = options;

  const url = new URL(`${API_BASE_URL}${path}`);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== "") url.searchParams.set(key, String(value));
    }
  }

  const token = getAccessToken();
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  let res: Response;
  try {
    res = await fetch(url.toString(), {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiException(0, "Le serveur ne répond pas (délai dépassé ou hors ligne)");
  }

  if (res.status === 401 && !isRetry && token) {
    const refreshed = await tryRefresh();
    if (refreshed) return request<T>(method, path, { ...options, isRetry: true });
    clearTokens();
    if (typeof window !== "undefined") window.location.href = "/login";
    throw new ApiException(401, "Session expirée, veuillez vous reconnecter");
  }

  if (res.status === 204) return undefined as T;

  const text = await res.text();
  const data = text ? JSON.parse(text) : undefined;

  if (!res.ok) {
    const message = (data && typeof data === "object" && "message" in data && typeof data.message === "string")
      ? data.message
      : `Erreur serveur (${res.status})`;
    throw new ApiException(res.status, message, extractFieldErrors(data));
  }

  return data as T;
}

export const apiClient = {
  get: <T>(path: string, query?: Record<string, string | number | boolean | undefined>) =>
    request<T>("GET", path, { query }),
  post: <T>(path: string, body?: unknown) => request<T>("POST", path, { body }),
  put: <T>(path: string, body?: unknown) => request<T>("PUT", path, { body }),
  patch: <T>(path: string, body?: unknown) => request<T>("PATCH", path, { body }),
  delete: <T>(path: string, body?: unknown) => request<T>("DELETE", path, { body }),
};
