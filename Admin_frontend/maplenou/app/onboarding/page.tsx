"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import { QRCodeSVG } from "qrcode.react";
import { apiClient, ApiException } from "@/lib/apiClient";
import { useAuth } from "@/lib/auth";
import { needsOnboarding } from "@/lib/permissions";

type Step = "password" | "mfa" | "recovery";

/**
 * Première connexion d'un compte du personnel : changement obligatoire du mot de passe initial,
 * puis activation de la MFA (application d'authentification) avec remise des codes de
 * récupération. Tant que ce n'est pas terminé, le serveur refuse tout le reste de l'API.
 */
export default function OnboardingPage() {
  const { user, loading, refreshUser, logout } = useAuth();
  const router = useRouter();

  const [step, setStep] = useState<Step | null>(null);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [secret, setSecret] = useState<string | null>(null);
  const [otpUri, setOtpUri] = useState<string | null>(null);
  const [code, setCode] = useState("");
  const [recoveryCodes, setRecoveryCodes] = useState<string[]>([]);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (loading) return;
    if (!user) {
      router.replace("/login");
      return;
    }
    if (step === null) {
      if (user.mustChangePassword) setStep("password");
      else if (!user.totpEnabled) setStep("mfa");
      else router.replace("/");
    }
  }, [loading, user, step, router]);

  useEffect(() => {
    if (step === "mfa" && !secret) void startMfaSetup();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [step]);

  async function startMfaSetup() {
    try {
      const res = await apiClient.post<{ secret: string; otpAuthUri: string }>("/api/auth/2fa/setup");
      setSecret(res.secret);
      setOtpUri(res.otpAuthUri);
    } catch (e) {
      setError(e instanceof ApiException ? e.message : "Impossible de démarrer l'activation de la MFA.");
    }
  }

  async function submitPassword(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await apiClient.patch("/api/users/me/password", { currentPassword, newPassword });
      const me = await refreshUser();
      setStep(me && !me.totpEnabled ? "mfa" : "recovery");
      if (me?.totpEnabled) router.replace("/");
    } catch (err) {
      setError(err instanceof ApiException ? err.message : "Erreur.");
    } finally {
      setBusy(false);
    }
  }

  async function submitMfa(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const res = await apiClient.post<{ recoveryCodes: string[] }>("/api/auth/2fa/activate", { code });
      setRecoveryCodes(res.recoveryCodes);
      setStep("recovery");
    } catch (err) {
      setError(err instanceof ApiException ? err.message : "Code invalide.");
    } finally {
      setBusy(false);
    }
  }

  async function finish() {
    const me = await refreshUser();
    if (me && !needsOnboarding(me)) router.replace("/");
  }

  function downloadCodes() {
    const blob = new Blob(
      [`Codes de récupération Maplenou (usage unique)\n\n${recoveryCodes.join("\n")}\n`],
      { type: "text/plain" },
    );
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "maplenou-codes-de-recuperation.txt";
    a.click();
    URL.revokeObjectURL(url);
  }

  const input =
    "w-full rounded-xl border border-border bg-input-fill px-4 py-2.5 text-sm outline-none focus:border-primary";

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10">
      <div className="w-full max-w-md">
        <div className="mb-8 flex justify-center">
          <Image src="/logo.svg" alt="Maplenou" width={180} height={53} priority />
        </div>

        <div className="rounded-2xl bg-surface p-8 shadow-sm ring-1 ring-border">
          <h1 className="mb-1 text-center text-xl font-bold text-foreground">Sécurisez votre compte</h1>
          <p className="mb-6 text-center text-sm text-muted">
            Première connexion : définissez votre mot de passe et activez l&apos;authentification à deux facteurs
            avant d&apos;accéder au tableau de bord.
          </p>

          {error && (
            <div className="mb-4 rounded-xl border border-error/30 bg-error/10 px-3 py-2 text-sm text-error">
              {error}
            </div>
          )}

          {step === "password" && (
            <form onSubmit={submitPassword} className="flex flex-col gap-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted">Étape 1 sur 2 — Mot de passe</p>
              <div>
                <label className="mb-1 block text-sm font-medium text-foreground">Mot de passe initial reçu</label>
                <input type="password" required value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} className={input} />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium text-foreground">Nouveau mot de passe</label>
                <input type="password" required value={newPassword} onChange={(e) => setNewPassword(e.target.value)} className={input} />
                <p className="mt-1 text-xs text-muted">Au moins 8 caractères, une majuscule et un chiffre.</p>
              </div>
              <button type="submit" disabled={busy} className="rounded-full bg-primary py-2.5 text-sm font-semibold text-white transition hover:bg-primary-dark disabled:opacity-60">
                {busy ? "..." : "Continuer"}
              </button>
            </form>
          )}

          {step === "mfa" && (
            <form onSubmit={submitMfa} className="flex flex-col gap-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted">Étape 2 sur 2 — Authentification à deux facteurs</p>
              <p className="text-sm text-muted">
                Scannez ce code avec une application d&apos;authentification (Google Authenticator, Authy, Microsoft
                Authenticator…), puis entrez le code à 6 chiffres affiché.
              </p>
              {otpUri && (
                <div className="flex justify-center rounded-xl bg-white p-4">
                  <QRCodeSVG value={otpUri} size={176} />
                </div>
              )}
              {secret && (
                <p className="break-all rounded-xl border border-border bg-input-fill px-3 py-2 text-center font-mono text-xs text-foreground">
                  {secret}
                </p>
              )}
              <input
                required
                inputMode="numeric"
                maxLength={6}
                value={code}
                onChange={(e) => setCode(e.target.value)}
                placeholder="Code à 6 chiffres"
                className={`${input} text-center text-lg tracking-widest`}
              />
              <button type="submit" disabled={busy || !secret} className="rounded-full bg-primary py-2.5 text-sm font-semibold text-white transition hover:bg-primary-dark disabled:opacity-60">
                {busy ? "..." : "Activer la MFA"}
              </button>
            </form>
          )}

          {step === "recovery" && (
            <div className="flex flex-col gap-4">
              <p className="text-sm font-semibold text-foreground">MFA activée. Conservez ces codes de récupération.</p>
              <p className="text-sm text-muted">
                Si vous perdez votre téléphone, chaque code permet une connexion, <strong>une seule fois</strong>. Ils ne
                seront plus affichés : copiez-les ou téléchargez-les dans un endroit sûr.
              </p>
              <ul className="grid grid-cols-2 gap-2 rounded-xl border border-border bg-input-fill p-3 font-mono text-sm text-foreground">
                {recoveryCodes.map((c) => (
                  <li key={c}>{c}</li>
                ))}
              </ul>
              <div className="flex gap-2">
                <button type="button" onClick={() => navigator.clipboard?.writeText(recoveryCodes.join("\n"))} className="flex-1 rounded-full border border-border py-2 text-sm font-semibold text-foreground hover:bg-input-fill">
                  Copier
                </button>
                <button type="button" onClick={downloadCodes} className="flex-1 rounded-full border border-border py-2 text-sm font-semibold text-foreground hover:bg-input-fill">
                  Télécharger
                </button>
              </div>
              <label className="flex items-center gap-2 text-sm text-foreground">
                <input type="checkbox" checked={saved} onChange={(e) => setSaved(e.target.checked)} />
                J&apos;ai conservé mes codes de récupération.
              </label>
              <button type="button" disabled={!saved} onClick={finish} className="rounded-full bg-primary py-2.5 text-sm font-semibold text-white transition hover:bg-primary-dark disabled:opacity-60">
                Accéder au tableau de bord
              </button>
            </div>
          )}

          <button type="button" onClick={logout} className="mt-5 block w-full text-center text-xs font-semibold text-muted">
            Se déconnecter
          </button>
        </div>
      </div>
    </main>
  );
}
