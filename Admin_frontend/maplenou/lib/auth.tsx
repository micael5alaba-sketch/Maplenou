"use client";

import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { apiClient, ApiException, clearTokens, getAccessToken, saveTokens } from "./apiClient";
import type { LoginResponse, UserResponse } from "./types";

interface AuthContextValue {
  user: UserResponse | null;
  loading: boolean;
  /** Renvoie { mfaRequired: true, mfaToken } si le compte a la 2FA activée. */
  login: (phoneNumber: string, password: string) => Promise<{ mfaRequired: boolean; mfaToken?: string }>;
  verifyMfa: (mfaToken: string, code: string) => Promise<void>;
  logout: () => Promise<void>;
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
    if (data.user?.role !== "ADMIN") {
      clearTokens();
      throw new ApiException(403, "Ce compte n'a pas les droits administrateur.");
    }
    applyLoginResult(data, setUser);
    return { mfaRequired: false };
  }

  async function verifyMfa(mfaToken: string, code: string) {
    const data = await apiClient.post<LoginResponse>("/api/auth/2fa/verify-login", { mfaToken, code });
    if (data.user?.role !== "ADMIN") {
      clearTokens();
      throw new ApiException(403, "Ce compte n'a pas les droits administrateur.");
    }
    applyLoginResult(data, setUser);
  }

  async function logout() {
    clearTokens();
    setUser(null);
    router.push("/login");
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, verifyMfa, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
