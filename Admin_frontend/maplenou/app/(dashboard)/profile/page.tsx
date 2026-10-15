"use client";

import { useEffect, useState, type FormEvent } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import { useAuth } from "@/lib/auth";
import type { AdminUserResponse } from "@/lib/types";
import { Button, Card, ErrorState, LoadingState, PageHeader } from "@/components/ui";

export default function ProfilePage() {
  const { user } = useAuth();
  const [me, setMe] = useState<AdminUserResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [setupSecret, setSetupSecret] = useState<string | null>(null);
  const [setupUri, setSetupUri] = useState<string | null>(null);
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [pwError, setPwError] = useState<string | null>(null);
  const [pwSuccess, setPwSuccess] = useState<string | null>(null);
  const [pwBusy, setPwBusy] = useState(false);

  useEffect(() => {
    if (!user) return;
    apiClient
      .get<AdminUserResponse>(`/api/admin/users/${user.id}`)
      .then(setMe)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }, [user]);

  async function startSetup() {
    setActionError(null);
    setActionSuccess(null);
    setBusy(true);
    try {
      const res = await apiClient.post<{ secret: string; otpAuthUri: string }>("/api/auth/2fa/setup");
      setSetupSecret(res.secret);
      setSetupUri(res.otpAuthUri);
    } catch (e) {
      setActionError(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setBusy(false);
    }
  }

  async function activate(e: FormEvent) {
    e.preventDefault();
    setActionError(null);
    setBusy(true);
    try {
      await apiClient.post("/api/auth/2fa/activate", { code });
      setSetupSecret(null);
      setSetupUri(null);
      setCode("");
      setActionSuccess("2FA activée sur ce compte.");
      if (user) setMe(await apiClient.get<AdminUserResponse>(`/api/admin/users/${user.id}`));
    } catch (e) {
      setActionError(e instanceof ApiException ? e.message : "Code invalide.");
    } finally {
      setBusy(false);
    }
  }

  async function disable(e: FormEvent) {
    e.preventDefault();
    setActionError(null);
    setBusy(true);
    try {
      await apiClient.delete("/api/auth/2fa/disable", { code });
      setCode("");
      setActionSuccess("2FA désactivée sur ce compte.");
      if (user) setMe(await apiClient.get<AdminUserResponse>(`/api/admin/users/${user.id}`));
    } catch (e) {
      setActionError(e instanceof ApiException ? e.message : "Code invalide.");
    } finally {
      setBusy(false);
    }
  }

  async function handleChangePassword(e: FormEvent) {
    e.preventDefault();
    setPwError(null);
    setPwSuccess(null);
    setPwBusy(true);
    try {
      await apiClient.patch("/api/users/me/password", { currentPassword, newPassword });
      setCurrentPassword("");
      setNewPassword("");
      setPwSuccess("Mot de passe modifié avec succès.");
    } catch (e) {
      setPwError(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setPwBusy(false);
    }
  }

  if (!user) return null;

  return (
    <div>
      <PageHeader title="Mon profil" subtitle="Identité et sécurité du compte administrateur" />

      <Card className="mb-6 max-w-xl">
        <h2 className="mb-3 text-sm font-semibold text-foreground">Identité</h2>
        <dl className="grid grid-cols-[120px_1fr] gap-y-2 text-sm">
          <dt className="text-muted">Nom</dt>
          <dd className="text-foreground">{user.fullName}</dd>
          <dt className="text-muted">Téléphone</dt>
          <dd className="text-foreground">{user.phoneNumber}</dd>
          <dt className="text-muted">Email</dt>
          <dd className="text-foreground">{user.email ?? "—"}</dd>
          <dt className="text-muted">Rôle</dt>
          <dd className="text-foreground">{user.role ?? "—"}</dd>
        </dl>
      </Card>

      <Card className="mb-6 max-w-xl">
        <h2 className="mb-3 text-sm font-semibold text-foreground">Changer le mot de passe</h2>
        {pwError && (
          <div className="mb-3">
            <ErrorState message={pwError} />
          </div>
        )}
        {pwSuccess && (
          <div className="mb-3 rounded-xl border border-primary/30 bg-primary/10 px-4 py-3 text-sm text-primary">
            {pwSuccess}
          </div>
        )}
        <form onSubmit={handleChangePassword} className="flex flex-col gap-3">
          <div>
            <label className="mb-1 block text-xs font-medium text-muted">Mot de passe actuel</label>
            <input
              required
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-muted">Nouveau mot de passe</label>
            <input
              required
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            />
            <p className="mt-1 text-xs text-muted">Au moins 8 caractères, une majuscule et un chiffre.</p>
          </div>
          <div>
            <Button type="submit" disabled={pwBusy}>
              Enregistrer
            </Button>
          </div>
        </form>
      </Card>

      <Card className="max-w-xl">
        <h2 className="mb-3 text-sm font-semibold text-foreground">Authentification à deux facteurs (TOTP)</h2>

        {loading && <LoadingState />}
        {error && <ErrorState message={error} />}
        {actionError && (
          <div className="mb-3">
            <ErrorState message={actionError} />
          </div>
        )}
        {actionSuccess && (
          <div className="mb-3 rounded-xl border border-primary/30 bg-primary/10 px-4 py-3 text-sm text-primary">
            {actionSuccess}
          </div>
        )}

        {me && !loading && (
          <>
            <p className="mb-4 text-sm text-muted">
              Statut actuel :{" "}
              <span className={`font-semibold ${me.totpEnabled ? "text-primary" : "text-muted"}`}>
                {me.totpEnabled ? "Activée" : "Désactivée"}
              </span>
            </p>

            {!me.totpEnabled && !setupSecret && (
              <Button onClick={startSetup} disabled={busy}>
                Activer la 2FA
              </Button>
            )}

            {setupSecret && setupUri && (
              <form onSubmit={activate} className="flex flex-col gap-3">
                <p className="text-sm text-muted">
                  Ajoutez ce secret dans votre application d&apos;authentification (Google Authenticator, Authy…),
                  puis entrez le code à 6 chiffres généré pour confirmer.
                </p>
                <div className="rounded-xl border border-border bg-input-fill px-3 py-2 font-mono text-xs break-all text-foreground">
                  {setupSecret}
                </div>
                <div className="rounded-xl border border-border bg-input-fill px-3 py-2 font-mono text-xs break-all text-muted">
                  {setupUri}
                </div>
                <div className="flex items-end gap-3">
                  <div className="w-40">
                    <label className="mb-1 block text-xs font-medium text-muted">Code à 6 chiffres</label>
                    <input
                      required
                      value={code}
                      onChange={(e) => setCode(e.target.value)}
                      maxLength={6}
                      className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
                    />
                  </div>
                  <Button type="submit" disabled={busy}>
                    Confirmer
                  </Button>
                </div>
              </form>
            )}

            {me.totpEnabled && (
              <form onSubmit={disable} className="flex items-end gap-3">
                <div className="w-40">
                  <label className="mb-1 block text-xs font-medium text-muted">Code à 6 chiffres</label>
                  <input
                    required
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    maxLength={6}
                    className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
                  />
                </div>
                <Button variant="danger" type="submit" disabled={busy}>
                  Désactiver la 2FA
                </Button>
              </form>
            )}
          </>
        )}
      </Card>
    </div>
  );
}
