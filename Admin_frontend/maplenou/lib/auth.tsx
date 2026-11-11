"use client";

import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { apiClient, ApiException, clearTokens, getAccessToken, saveTokens } from "./apiClient";
import type { LoginResponse, UserResponse } from "./types";
import { isStaffRole } from "./permissions";

interface AuthContextValue {
  user: UserResponse | null;
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
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);
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

  async function logout() {
    clearTokens();
    setUser(null);
    router.push("/login");
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, verifyMfa, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
