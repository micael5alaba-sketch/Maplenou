"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Alert, Loading } from "@/components/ui";
import { useMessages } from "@/lib/i18n/client";
import { login } from "@/lib/i18n/messages/login";
import { isSupportRole } from "@/lib/labels";
import type { User } from "@/lib/types";

/**
 * Arrival from the Maplenou back office (Super Admin menu "Service client"): the Maplenou access token travels in the
 * URL fragment (`/maplenou#token=…`), which browsers never send to a server. It is removed from the address bar at once,
 * exchanged for a support session by `/api/session`, then the person lands on their home.
 */
export default function MaplenouHandoffPage() {
  const t = useMessages(login);
  const router = useRouter();
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const token = new URLSearchParams(window.location.hash.replace(/^#/, "")).get("token");
    window.history.replaceState(null, "", "/maplenou");
    if (!token) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- one-time read of the URL fragment on mount
      setFailed(true);
      return;
    }
    fetch("/api/session", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ coreAccessToken: token }),
    })
      .then(async (res) => {
        if (!res.ok) throw new Error(String(res.status));
        const user = (await res.json()) as User;
        router.replace(isSupportRole(user.role) ? "/support" : "/aide");
      })
      .catch(() => setFailed(true));
  }, [router]);

  return (
    <main className="login-page">
      <div className="card login-card stack">
      {failed ? <Alert tone="danger">{t.handoffFailed}</Alert> : <Loading label={t.handoffSigningIn} />}
      </div>
    </main>
  );
}
