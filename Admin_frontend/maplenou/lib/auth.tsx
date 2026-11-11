"use client";

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { apiClient, ApiException, clearTokens, getAccessToken, saveTokens } from "./apiClient";
import type { LoginResponse, Role, UserResponse } from "./types";
import { isStaffRole } from "./permissions";

interface AuthContextValue {
  /** Utilisateur « effectif » : en mode « Voir en tant que », rôle et permissions du rôle visualisé. */
  user: UserResponse | null;
  /** Compte réellement connecté (toujours le Super Admin pendant un aperçu). */
  realUser: UserResponse | null;
  /** Rôle visualisé par le Super Admin (null = son propre tableau de bord). */
  viewAsRole: Role;
  setViewAsRole: (role: Role) => Promise<void>;
  loading: boolean;
  /** Renvoie { mfaRequired: true, mfaToken } si le compte a la 2FA activée. */
  login: (phoneNumber: string, password: string) => Promise<{ mfaRequired: boolean; mfaToken?: string }>;
  verifyMfa: (mfaToken: string, code: string) => Promise<void>;
  logout: () => Promise<void>;
  /** Recharge le profil (permissions, MFA, mot de passe) — après la première connexion par ex. */
  refreshUser: () => Promise<UserResponse | null>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function applyLoginResult(data: LoginResponse, setUser: (u: UserResponse) => void) {
  if (!data.accessToken || !data.refreshToken || !data.user) return;
  saveTokens(data.accessToken, data.refreshToken);
  setUser(data.user);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [realUser, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [viewAsRole, setViewAsRoleState] = useState<Role>(null);
  const [rolePermissions, setRolePermissions] = useState<Record<string, string[]> | null>(null);
  const router = useRouter();

  useEffect(() => {
    const token = getAccessToken();
    if (!token) {
      setLoading(false);
      return;
    }
    apiClient
      .get<UserResponse>("/api/users/me")
      .then((me) => setUser(me))
      .catch(() => clearTokens())
      .finally(() => setLoading(false));
  }, []);

  async function login(phoneNumber: string, password: string) {
    const data = await apiClient.post<LoginResponse>("/api/auth/login", { phoneNumber, password });
    if (data.mfaRequired) {
      return { mfaRequired: true, mfaToken: data.mfaToken ?? undefined };
    }
    if (!isStaffRole(data.user?.role)) {
      clearTokens();
      throw new ApiException(403, "Ce compte n'a pas accès au tableau de bord Maplenou.");
    }
    applyLoginResult(data, setUser);
    return { mfaRequired: false };
  }

  async function verifyMfa(mfaToken: string, code: string) {
    const data = await apiClient.post<LoginResponse>("/api/auth/2fa/verify-login", { mfaToken, code });
    if (!isStaffRole(data.user?.role)) {
      clearTokens();
      throw new ApiException(403, "Ce compte n'a pas accès au tableau de bord Maplenou.");
    }
    applyLoginResult(data, setUser);
  }

  async function refreshUser() {
    try {
      const me = await apiClient.get<UserResponse>("/api/users/me");
      setUser(me);
      return me;
    } catch {
      return null;
    }
  }

  /**
   * Aperçu du tableau de bord d'un autre rôle (Super Admin uniquement) : l'interface affiche les
   * menus, pages et boutons de ce rôle. Simple simulation d'affichage — la session reste celle du
   * Super Admin (aucune connexion à la place de quelqu'un, donc aucune confusion dans l'audit).
   */
  async function setViewAsRole(role: Role) {
    if (role === null) {
      setViewAsRoleState(null);
      window.sessionStorage.removeItem("maplenou_view_as");
      return;
    }
    if (!realUser?.permissions.includes("STAFF_MANAGE")) return;
    let map = rolePermissions;
    if (!map) {
      map = await apiClient.get<Record<string, string[]>>("/api/admin/staff/roles");
      setRolePermissions(map);
    }
    if (map[role]) {
      setViewAsRoleState(role);
      window.sessionStorage.setItem("maplenou_view_as", role);
    }
  }

  // Retrouve l'aperçu en cours après un rechargement de page.
  useEffect(() => {
    const saved = typeof window !== "undefined" ? (window.sessionStorage.getItem("maplenou_view_as") as Role) : null;
    if (saved && realUser?.permissions.includes("STAFF_MANAGE") && !viewAsRole) {
      void setViewAsRole(saved);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [realUser]);

  const user = useMemo<UserResponse | null>(() => {
    if (!realUser) return null;
    if (viewAsRole && rolePermissions?.[viewAsRole]) {
      return { ...realUser, role: viewAsRole, permissions: rolePermissions[viewAsRole] };
    }
    return realUser;
  }, [realUser, viewAsRole, rolePermissions]);

  async function logout() {
    clearTokens();
    window.sessionStorage.removeItem("maplenou_view_as");
    setUser(null);
    router.push("/login");
  }

  return (
    <AuthContext.Provider value={{ user, realUser, viewAsRole, setViewAsRole, loading, login, verifyMfa, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
