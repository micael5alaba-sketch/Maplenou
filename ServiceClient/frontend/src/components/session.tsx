"use client";

import { createContext, useContext, type ReactNode } from "react";
import type { Role } from "@/lib/types";
import { clearApiCache } from "@/lib/apiCache";
import { unsubscribeThisBrowser } from "@/lib/push";

export interface ClientSession {
  id: string;
  role: Role;
  name: string;
}

const SessionContext = createContext<ClientSession | null>(null);

export function SessionProvider({ session, children }: { session: ClientSession; children: ReactNode }) {
  return <SessionContext.Provider value={session}>{children}</SessionContext.Provider>;
}

export function useSession(): ClientSession {
  const s = useContext(SessionContext);
  if (!s) throw new Error("useSession must be used inside SessionProvider");
  return s;
}

export async function logout() {
  clearApiCache(); // never keep a previous user's data in memory
  await unsubscribeThisBrowser(); // the next user of this browser must not receive this user's pushes
  await fetch("/api/session", { method: "DELETE" });
  // full reload so server layouts re-read the (now cleared) session cookie
  window.location.assign(new URL("/login", window.location.origin).href);
}
