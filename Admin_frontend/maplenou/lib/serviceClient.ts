/**
 * Pont avec le Service client (`ServiceClient/`, équipe service client). Le back-office Maplenou n'en reprend ni
 * l'administration ni les rôles : il y donne seulement un accès au Super Admin (ouverture de l'espace du personnel sans
 * ressaisir de mot de passe) et une vue de supervision en lecture seule.
 *
 * Le Super Admin est reconnu par le service client comme administrateur : son jeton Maplenou est échangé contre un jeton
 * du service client (`POST /api/v1/auth/maplenou/exchange`), gardé en mémoire seulement.
 */
import { ApiException, apiClient, getAccessToken } from "./apiClient";
import type { ServiceClientDashboard, ServiceClientPage, ServiceClientStaffRole, ServiceClientTicket } from "./types";

const SUPPORT_API_URL = process.env.NEXT_PUBLIC_SUPPORT_API_URL ?? "https://maplenou-support-api.onrender.com";
const SUPPORT_WEB_URL = process.env.NEXT_PUBLIC_SUPPORT_WEB_URL ?? "https://maplenou-support-web.onrender.com";

let cached: { token: string; expiresAt: number } | null = null;

/** Jeton Maplenou valide : un appel authentifié le renouvelle s'il a expiré. */
async function freshCoreToken(): Promise<string> {
  await apiClient.get("/api/users/me");
  const token = getAccessToken();
  if (!token) throw new ApiException(401, "Session expirée, veuillez vous reconnecter.");
  return token;
}

async function supportToken(): Promise<string> {
  if (cached && cached.expiresAt - Date.now() > 120_000) return cached.token;
  const coreAccessToken = await freshCoreToken();
  const res = await fetch(`${SUPPORT_API_URL}/api/v1/auth/maplenou/exchange`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ coreAccessToken }),
  }).catch(() => null);
  if (!res) throw new ApiException(0, "Le service client ne répond pas.");
  const data = (await res.json().catch(() => ({}))) as { accessToken?: string; expiresAt?: string; detail?: string };
  if (!res.ok || !data.accessToken || !data.expiresAt) {
    throw new ApiException(res.status, data.detail ?? "Accès au service client refusé.");
  }
  cached = { token: data.accessToken, expiresAt: new Date(data.expiresAt).getTime() };
  return cached.token;
}

async function supportGet<T>(path: string, query?: Record<string, string | number | boolean>): Promise<T> {
  const url = new URL(`${SUPPORT_API_URL}${path}`);
  Object.entries(query ?? {}).forEach(([k, v]) => url.searchParams.set(k, String(v)));
  const call = async (retried: boolean): Promise<T> => {
    const res = await fetch(url, { headers: { Authorization: `Bearer ${await supportToken()}` }, cache: "no-store" }).catch(
      () => null,
    );
    if (!res) throw new ApiException(0, "Le service client ne répond pas.");
    if (res.status === 401 && !retried) {
      cached = null;
      return call(true);
    }
    if (!res.ok) {
      const problem = (await res.json().catch(() => ({}))) as { detail?: string };
      throw new ApiException(res.status, problem.detail ?? `Erreur du service client (${res.status}).`);
    }
    return (await res.json()) as T;
  };
  return call(false);
}

/** Écriture (rôles du personnel du service client) : même jeton, mêmes erreurs que la lecture. */
async function supportSend(method: "PUT" | "DELETE", path: string, body?: unknown): Promise<void> {
  const call = async (retried: boolean): Promise<void> => {
    const res = await fetch(`${SUPPORT_API_URL}${path}`, {
      method,
      headers: { Authorization: `Bearer ${await supportToken()}`, "Content-Type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body),
    }).catch(() => null);
    if (!res) throw new ApiException(0, "Le service client ne répond pas.");
    if (res.status === 401 && !retried) {
      cached = null;
      return call(true);
    }
    if (!res.ok) {
      const problem = (await res.json().catch(() => ({}))) as { detail?: string };
      throw new ApiException(res.status, problem.detail ?? `Erreur du service client (${res.status}).`);
    }
  };
  return call(false);
}

export const serviceClient = {
  dashboard: () => supportGet<ServiceClientDashboard>("/api/v1/support/dashboard"),
  tickets: (query: Record<string, string | number | boolean>) =>
    supportGet<ServiceClientPage<ServiceClientTicket>>("/api/v1/support/tickets", query),
  staffRoles: () => supportGet<ServiceClientStaffRole[]>("/api/v1/support/admin/staff-roles"),
  /** Donne (ou change) le rôle d'une personne dans le service client, par numéro de téléphone. */
  setStaffRole: (phone: string, role: string) => supportSend("PUT", "/api/v1/support/admin/staff-roles", { phone, role }),
  /** Retire le rôle donné par numéro ; la personne retrouve le rôle automatique de son compte Maplenou. */
  removeStaffRole: (phone: string) => supportSend("DELETE", `/api/v1/support/admin/staff-roles/${encodeURIComponent(phone)}`),
};

/**
 * Pastille du menu : tickets ouverts sans agent (file d'attente du service client). Silencieux : si le service client est
 * injoignable ou refuse l'accès, la pastille reste vide plutôt que de gêner le back-office.
 */
export async function serviceClientBadge(): Promise<number> {
  try {
    const d = await serviceClient.dashboard();
    return d.unassignedCount;
  } catch {
    return 0;
  }
}

/**
 * Ouvre l'espace du personnel du service client dans un nouvel onglet, connecté. La fenêtre est ouverte tout de suite
 * (clic de l'utilisateur, sinon le navigateur la bloque), puis dirigée vers `/maplenou#token=…` : le fragment n'est jamais
 * envoyé à un serveur et la page d'arrivée l'efface de la barre d'adresse.
 */
export async function openServiceClient(): Promise<void> {
  const win = window.open("about:blank", "_blank");
  try {
    const token = await freshCoreToken();
    const target = `${SUPPORT_WEB_URL}/maplenou#token=${encodeURIComponent(token)}`;
    if (win) {
      win.opener = null;
      win.location.href = target;
    } else {
      window.location.href = target;
    }
  } catch (e) {
    win?.close();
    throw e;
  }
}

export const SERVICE_CLIENT_WEB_URL = SUPPORT_WEB_URL;
