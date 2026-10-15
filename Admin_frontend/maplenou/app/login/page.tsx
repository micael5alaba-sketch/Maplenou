"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import { useAuth } from "@/lib/auth";
import { apiClient, ApiException } from "@/lib/apiClient";

type Mode = "login" | "mfa" | "forgot" | "reset" | "resetDone";

export default function LoginPage() {
  const { login, verifyMfa } = useAuth();
  const router = useRouter();

  const [mode, setMode] = useState<Mode>("login");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [mfaToken, setMfaToken] = useState<string | null>(null);
  const [resetCode, setResetCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      if (mode === "mfa" && mfaToken) {
        await verifyMfa(mfaToken, code);
        router.push("/");
        return;
      }
      const result = await login(phoneNumber, password);
      if (result.mfaRequired && result.mfaToken) {
        setMfaToken(result.mfaToken);
        setMode("mfa");
      } else {
        router.push("/");
      }
    } catch (err) {
      setError(err instanceof ApiException ? err.message : "Une erreur est survenue.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleForgotSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post("/api/auth/forgot-password", { phoneNumber });
      setMode("reset");
    } catch (err) {
      setError(err instanceof ApiException ? err.message : "Une erreur est survenue.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleResetSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post("/api/auth/reset-password", { phoneNumber, code: resetCode, newPassword });
      setMode("resetDone");
    } catch (err) {
      setError(err instanceof ApiException ? err.message : "Une erreur est survenue.");
    } finally {
      setSubmitting(false);
    }
  }

  function backToLogin() {
    setMode("login");
    setError(null);
    setCode("");
    setResetCode("");
    setNewPassword("");
  }

  const titles: Record<Mode, { title: string; subtitle: string }> = {
    login: { title: "Back-office Admin", subtitle: "Réservé aux comptes administrateur Maplenou." },
    mfa: {
      title: "Vérification en deux étapes",
      subtitle: "Entrez le code à 6 chiffres de votre application d'authentification.",
    },
    forgot: {
      title: "Mot de passe oublié",
      subtitle: "Entrez votre numéro de téléphone, un code vous sera envoyé par email.",
    },
    reset: {
      title: "Réinitialiser le mot de passe",
      subtitle: "Entrez le code reçu par email et votre nouveau mot de passe.",
    },
    resetDone: {
      title: "Mot de passe réinitialisé !",
      subtitle: "Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.",
    },
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex justify-center">
          <Image src="/logo.svg" alt="Maplenou" width={180} height={53} priority />
        </div>

        <div className="rounded-2xl bg-surface p-8 shadow-sm ring-1 ring-border">
          <h1 className="mb-1 text-center text-xl font-bold text-foreground">{titles[mode].title}</h1>
          <p className="mb-6 text-center text-sm text-muted">{titles[mode].subtitle}</p>

          {error && (
            <div className="mb-4 rounded-xl border border-error/30 bg-error/10 px-3 py-2 text-sm text-error">
              {error}
            </div>
          )}

          {mode === "resetDone" ? (
            <button
              onClick={backToLogin}
              className="w-full rounded-full bg-primary py-2.5 text-sm font-semibold text-white transition hover:bg-primary-dark"
            >
              Retour à la connexion
            </button>
          ) : mode === "forgot" ? (
            <form onSubmit={handleForgotSubmit} className="flex flex-col gap-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-foreground">Numéro de téléphone</label>
                <input
                  type="tel"
                  required
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  placeholder="+228..."
                  className="w-full rounded-xl border border-border bg-input-fill px-4 py-2.5 text-sm outline-none focus:border-primary"
                />
              </div>
              <button
                type="submit"
                disabled={submitting}
                className="mt-2 rounded-full bg-primary py-2.5 text-sm font-semibold text-white transition hover:bg-primary-dark disabled:opacity-60"
              >
                {submitting ? "..." : "Envoyer le code"}
              </button>
              <button type="button" onClick={backToLogin} className="text-center text-xs font-semibold text-primary">
                Retour à la connexion
              </button>
            </form>
          ) : mode === "reset" ? (
            <form onSubmit={handleResetSubmit} className="flex flex-col gap-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-foreground">Code à 6 chiffres</label>
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  required
                  value={resetCode}
                  onChange={(e) => setResetCode(e.target.value)}
                  className="w-full rounded-xl border border-border bg-input-fill px-4 py-2.5 text-center text-lg tracking-widest outline-none focus:border-primary"
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium text-foreground">Nouveau mot de passe</label>
                <input
                  type="password"
                  required
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="w-full rounded-xl border border-border bg-input-fill px-4 py-2.5 text-sm outline-none focus:border-primary"
                />
                <p className="mt-1 text-xs text-muted">Au moins 8 caractères, une majuscule et un chiffre.</p>
              </div>
              <button
                type="submit"
                disabled={submitting}
                className="mt-2 rounded-full bg-primary py-2.5 text-sm font-semibold text-white transition hover:bg-primary-dark disabled:opacity-60"
              >
                {submitting ? "..." : "Réinitialiser le mot de passe"}
              </button>
              <button type="button" onClick={backToLogin} className="text-center text-xs font-semibold text-primary">
                Retour à la connexion
              </button>
            </form>
          ) : (
            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
              {mode === "login" ? (
                <>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-foreground">Numéro de téléphone</label>
                    <input
                      type="tel"
                      required
                      value={phoneNumber}
                      onChange={(e) => setPhoneNumber(e.target.value)}
                      placeholder="+228..."
                      className="w-full rounded-xl border border-border bg-input-fill px-4 py-2.5 text-sm outline-none focus:border-primary"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-foreground">Mot de passe</label>
                    <input
                      type="password"
                      required
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="w-full rounded-xl border border-border bg-input-fill px-4 py-2.5 text-sm outline-none focus:border-primary"
                    />
                    <button
                      type="button"
                      onClick={() => {
                        setError(null);
                        setMode("forgot");
                      }}
                      className="mt-1 text-xs font-semibold text-primary"
                    >
                      Mot de passe oublié ?
                    </button>
                  </div>
                </>
              ) : (
                <div>
                  <label className="mb-1 block text-sm font-medium text-foreground">Code à 6 chiffres</label>
                  <input
                    type="text"
                    inputMode="numeric"
                    maxLength={6}
                    required
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    className="w-full rounded-xl border border-border bg-input-fill px-4 py-2.5 text-center text-lg tracking-widest outline-none focus:border-primary"
                  />
                </div>
              )}

              <button
                type="submit"
                disabled={submitting}
                className="mt-2 rounded-full bg-primary py-2.5 text-sm font-semibold text-white transition hover:bg-primary-dark disabled:opacity-60"
              >
                {submitting ? "..." : mode === "mfa" ? "Vérifier" : "Se connecter"}
              </button>
            </form>
          )}
        </div>

        <p className="mt-6 text-center text-xs text-muted">
          Accès réservé — toute action est tracée (journal d&apos;audit).
        </p>
      </div>
    </main>
  );
}
