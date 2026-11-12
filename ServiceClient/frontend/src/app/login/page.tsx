"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { isSupportRole } from "@/lib/labels";
import type { User } from "@/lib/types";
import { Alert, Avatar, Loading } from "@/components/ui";
import { Preferences } from "@/components/Preferences";
import { useLabels, useMessages } from "@/lib/i18n/client";
import { common } from "@/lib/i18n/messages/common";
import { login } from "@/lib/i18n/messages/login";

type LoginError = keyof typeof login.fr;
const ACCOUNT_ERRORS = ["INVALID_CREDENTIALS", "INVALID_PHONE", "INVALID_MFA_CODE", "MFA_EXPIRED", "TOO_MANY_ATTEMPTS",
  "ACCOUNT_DISABLED", "CORE_UNAVAILABLE"] as const;

/**
 * Sign-in. `AUTH_MODE=maplenou`: phone number + password of the Maplenou account (and the 2FA code when the
 * account has one), checked by the Maplenou platform. `AUTH_MODE=demo`: pick a demo persona (backend demo profile).
 */
export default function LoginPage() {
  const [mode, setMode] = useState<"demo" | "maplenou" | null>(null);
  useEffect(() => {
    fetch("/api/auth-config", { cache: "no-store" })
      .then((r) => r.json())
      .then((c: { mode: "demo" | "maplenou" }) => setMode(c.mode))
      .catch(() => setMode("demo"));
  }, []);
  if (!mode) return null;
  return mode === "maplenou" ? <AccountSignIn /> : <DemoSignIn />;
}

function Header() {
  const c = useMessages(common);
  return (
    <div className="row-between">
      <div className="row">
        <span className="logo">
          <span className="logo-mark" aria-hidden="true">M</span> {c.appName}
        </span>
        <span className="muted">{c.serviceName}</span>
      </div>
      <Preferences variant="inline" />
    </div>
  );
}

function AccountSignIn() {
  const t = useMessages(login);
  const router = useRouter();
  const [step, setStep] = useState<"password" | "code">("password");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<LoginError | null>(null);
  const [expired, setExpired] = useState(false);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- read the query string once on mount
    setExpired(new URLSearchParams(window.location.search).has("expired"));
  }, []);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const body = step === "password" ? { phoneNumber: phone.replace(/[\s.-]/g, ""), password } : { code };
    const res = await fetch("/api/session", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }).catch(() => null);
    const data = res ? await res.json().catch(() => ({})) : {};
    setBusy(false);
    if (!res || !res.ok) {
      const known = (ACCOUNT_ERRORS as readonly string[]).includes(data.code) ? (data.code as LoginError) : "signInFailed";
      setError(known);
      if (data.code === "MFA_EXPIRED") setStep("password");
      return;
    }
    if (data.mfaRequired) {
      setPassword("");
      setStep("code");
      return;
    }
    router.push(isSupportRole(data.role) ? "/support" : "/aide");
    router.refresh();
  }

  return (
    <main className="login-page">
      <form className="card login-card stack" onSubmit={submit} style={{ maxWidth: 480 }}>
        <Header />
        <div>
          <h1>{step === "password" ? t.accountTitle : t.mfaTitle}</h1>
          <p className="secondary small">{step === "password" ? t.accountIntro : t.mfaIntro}</p>
        </div>
        {expired && <Alert tone="warning">{t.expired}</Alert>}
        {error && <Alert tone="danger">{t[error]}</Alert>}
        {step === "password" ? (
          <>
            <div className="field">
              <label htmlFor="login-phone" className="required">{t.phone}</label>
              <input id="login-phone" className="input" type="tel" inputMode="tel" autoComplete="tel" required
                value={phone} onChange={(e) => setPhone(e.target.value)} aria-describedby="login-phone-hint" />
              <span id="login-phone-hint" className="xsmall muted">{t.phoneHint}</span>
            </div>
            <div className="field">
              <label htmlFor="login-password" className="required">{t.password}</label>
              <input id="login-password" className="input" type="password" autoComplete="current-password" required
                value={password} onChange={(e) => setPassword(e.target.value)} />
            </div>
          </>
        ) : (
          <div className="field">
            <label htmlFor="login-code" className="required">{t.mfaCode}</label>
            <input id="login-code" className="input mono" inputMode="numeric" autoComplete="one-time-code"
              pattern="[0-9]{6}" maxLength={6} required value={code} onChange={(e) => setCode(e.target.value)} />
          </div>
        )}
        <div className="row">
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? t.signingIn : step === "password" ? t.submit : t.mfaSubmit}
          </button>
          {step === "code" && (
            <button type="button" className="btn btn-secondary" onClick={() => { setStep("password"); setCode(""); }}>
              {t.back}
            </button>
          )}
        </div>
        <p className="xsmall muted">{t.forgotten}</p>
      </form>
    </main>
  );
}

/** Demo sign-in: pick a persona; the demo endpoint only exists when the backend runs with the `demo` profile. */
function DemoSignIn() {
  const t = useMessages(login);
  const labels = useLabels();
  const router = useRouter();
  const [users, setUsers] = useState<User[] | null>(null);
  // Stored as a message key so the text follows a language switch.
  const [error, setError] = useState<"demoUnavailable" | "signInFailed" | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [expired, setExpired] = useState(false);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- read the query string once on mount
    setExpired(new URLSearchParams(window.location.search).has("expired"));
    fetch("/api/demo-users", { cache: "no-store" })
      .then(async (r) => {
        if (!r.ok) throw new Error();
        setUsers(await r.json());
      })
      .catch(() => setError("demoUnavailable"));
  }, []);

  async function signIn(u: User) {
    setBusy(u.id);
    setError(null);
    const res = await fetch("/api/session", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId: u.id }),
    });
    if (!res.ok) {
      setBusy(null);
      setError("signInFailed");
      return;
    }
    router.push(isSupportRole(u.role) ? "/support" : "/aide");
    router.refresh();
  }

  const groups: { key: string; title: string; filter: (u: User) => boolean }[] = [
    { key: "requesters", title: t.requesters, filter: (u) => !isSupportRole(u.role) },
    { key: "support", title: t.supportTeam, filter: (u) => isSupportRole(u.role) },
  ];

  return (
    <main className="login-page">
      <div className="card login-card stack">
        <Header />
        <div>
          <h1>{t.title}</h1>
          <p className="secondary small">{t.intro}</p>
        </div>
        {expired && <Alert tone="warning">{t.expired}</Alert>}
        {error && <Alert tone="danger">{t[error]}</Alert>}
        {!users && !error && <Loading label={t.loadingProfiles} />}
        {users &&
          groups.map((g) => (
            <section key={g.key} className="stack-sm">
              <h2 className="eyebrow">{g.title}</h2>
              <div className="persona-grid">
                {users.filter(g.filter).map((u) => (
                  <button key={u.id} type="button" className="persona" onClick={() => signIn(u)} disabled={busy !== null}>
                    <Avatar name={u.displayName} />
                    <span>
                      <strong>{u.displayName}</strong>
                      <br />
                      <span className="small muted">{busy === u.id ? t.signingIn : labels.role[u.role]}</span>
                    </span>
                  </button>
                ))}
              </div>
            </section>
          ))}
      </div>
    </main>
  );
}
