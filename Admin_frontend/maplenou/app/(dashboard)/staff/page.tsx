"use client";

import { useEffect, useState, type FormEvent } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import { ROLE_LABELS } from "@/lib/permissions";
import type { Page, Role, StaffCredentialsResponse, StaffResponse } from "@/lib/types";
import {
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Pagination,
  StatusBadge,
  Table,
  formatDate,
} from "@/components/ui";

const ASSIGNABLE_ROLES: Exclude<Role, null | "DELIVERY_AGENT">[] = ["DELIVERY_ADMIN", "SUPPORT_AGENT", "AUDITOR", "ADMIN"];

/**
 * Comptes du personnel (Super Admin uniquement) : création, rôle, activation/désactivation,
 * réinitialisation. Jamais d'inscription publique ; chaque action est tracée dans le journal d'audit.
 */
export default function StaffPage() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<StaffResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  const [fullName, setFullName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<Exclude<Role, null | "DELIVERY_AGENT">>("SUPPORT_AGENT");
  const [pricing, setPricing] = useState(false);
  const [creating, setCreating] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [credentials, setCredentials] = useState<StaffCredentialsResponse | null>(null);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<StaffResponse>>("/api/admin/staff", { page, size: 20 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [page]);

  async function create(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setCreating(true);
    try {
      const res = await apiClient.post<StaffCredentialsResponse>("/api/admin/staff", {
        fullName,
        phoneNumber,
        email,
        role,
        deliveryPricingGranted: role === "DELIVERY_ADMIN" && pricing,
      });
      setCredentials(res);
      setFullName("");
      setPhoneNumber("");
      setEmail("");
      setPricing(false);
      load();
    } catch (err) {
      setFormError(err instanceof ApiException ? err.message : "Erreur lors de la création.");
    } finally {
      setCreating(false);
    }
  }

  async function act(id: string, call: () => Promise<unknown>) {
    setActingId(id);
    try {
      await call();
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  function toggleActive(s: StaffResponse) {
    if (s.active) {
      const justification = prompt(`Désactiver ${s.fullName} ? Motif (enregistré dans le journal d'audit) :`);
      if (justification === null) return;
      return act(s.id, () => apiClient.patch(`/api/admin/staff/${s.id}/active`, { active: false, justification }));
    }
    return act(s.id, () => apiClient.patch(`/api/admin/staff/${s.id}/active`, { active: true }));
  }

  async function resetPassword(s: StaffResponse) {
    if (!confirm(`Réinitialiser le mot de passe et la MFA de ${s.fullName} ? Un nouveau mot de passe initial sera généré.`)) return;
    setActingId(s.id);
    try {
      setCredentials(await apiClient.post<StaffCredentialsResponse>(`/api/admin/staff/${s.id}/reset-password`));
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  function changeRole(s: StaffResponse, newRole: string) {
    if (newRole === s.role) return;
    if (!confirm(`Passer ${s.fullName} au rôle « ${ROLE_LABELS[newRole]} » ? Ses sessions seront révoquées.`)) {
      load();
      return;
    }
    return act(s.id, () => apiClient.patch(`/api/admin/staff/${s.id}`, { role: newRole }));
  }

  /** Identifiants (email, téléphone) et nom : modifiables uniquement par le Super Admin, qui a créé le compte. */
  function editIdentifiers(s: StaffResponse) {
    const name = prompt("Nom complet :", s.fullName);
    if (name === null) return;
    const newEmail = prompt("Email (identifiant de connexion) :", s.email ?? "");
    if (newEmail === null) return;
    const newPhone = prompt("Téléphone :", s.phoneNumber);
    if (newPhone === null) return;
    if (!confirm("Modifier les identifiants révoque les sessions ouvertes de ce compte. Continuer ?")) return;
    return act(s.id, () =>
      apiClient.patch(`/api/admin/staff/${s.id}`, {
        fullName: name.trim() || undefined,
        email: newEmail.trim() || undefined,
        phoneNumber: newPhone.trim() || undefined,
      }),
    );
  }

  function togglePricing(s: StaffResponse) {
    return act(s.id, () => apiClient.patch(`/api/admin/staff/${s.id}`, { deliveryPricingGranted: !s.deliveryPricingGranted }));
  }

  const input =
    "w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary";

  return (
    <div>
      <PageHeader
        title="Personnel du back-office"
        subtitle="Comptes créés uniquement par le Super Admin. MFA obligatoire, mot de passe à changer à la première connexion."
      />

      {credentials && (
        <Card className="mb-6 max-w-xl border-l-4 border-primary">
          <h2 className="mb-2 text-sm font-semibold text-foreground">Mot de passe initial — à transmettre de façon sécurisée</h2>
          <p className="mb-2 text-sm text-muted">
            Compte : <strong>{credentials.staff.fullName}</strong> ({credentials.staff.email ?? credentials.staff.phoneNumber}) —{" "}
            {ROLE_LABELS[credentials.staff.role ?? ""]}
          </p>
          <p className="mb-2 rounded-xl border border-border bg-input-fill px-3 py-2 text-center font-mono text-lg tracking-wider text-foreground">
            {credentials.temporaryPassword}
          </p>
          <p className="mb-3 text-xs text-muted">
            Ce mot de passe ne sera plus affiché. La personne devra le changer et activer la MFA dès sa première
            connexion.
          </p>
          <Button variant="secondary" onClick={() => setCredentials(null)}>
            J&apos;ai noté le mot de passe
          </Button>
        </Card>
      )}

      <Card className="mb-6 max-w-xl">
        <h2 className="mb-3 text-sm font-semibold text-foreground">Créer un compte du personnel</h2>
        {formError && (
          <div className="mb-3">
            <ErrorState message={formError} />
          </div>
        )}
        <form onSubmit={create} className="flex flex-col gap-3">
          <input required placeholder="Nom complet" value={fullName} onChange={(e) => setFullName(e.target.value)} className={input} />
          <input required type="email" placeholder="Email (identifiant de connexion)" value={email} onChange={(e) => setEmail(e.target.value)} className={input} />
          <input required placeholder="Téléphone (ex. +22890123456)" value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} className={input} />
          <select value={role} onChange={(e) => setRole(e.target.value as typeof role)} className={input}>
            {ASSIGNABLE_ROLES.map((r) => (
              <option key={r} value={r}>
                {ROLE_LABELS[r]}
              </option>
            ))}
          </select>
          {role === "DELIVERY_ADMIN" && (
            <label className="flex items-center gap-2 text-sm text-foreground">
              <input type="checkbox" checked={pricing} onChange={(e) => setPricing(e.target.checked)} />
              Autoriser la gestion de la tarification de livraison
            </label>
          )}
          <div>
            <Button type="submit" disabled={creating}>
              Créer le compte
            </Button>
          </div>
        </form>
      </Card>

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {data && (
        <Card>
          {data.content.length === 0 ? (
            <EmptyState message="Aucun compte du personnel." />
          ) : (
            <Table headers={["Nom", "Email", "Téléphone", "Rôle", "Statut", "MFA", "Tarification", "Créé le", "Actions"]}>
              {data.content.map((s) => (
                <tr key={s.id}>
                  <td className="px-4 py-3 font-medium text-foreground">{s.fullName}</td>
                  <td className="px-4 py-3 text-muted">{s.email ?? "—"}</td>
                  <td className="px-4 py-3 text-muted">{s.phoneNumber}</td>
                  <td className="px-4 py-3">
                    <select
                      value={s.role ?? ""}
                      disabled={actingId === s.id}
                      onChange={(e) => changeRole(s, e.target.value)}
                      className="rounded-lg border border-border bg-input-fill px-2 py-1 text-xs"
                    >
                      {ASSIGNABLE_ROLES.map((r) => (
                        <option key={r} value={r}>
                          {ROLE_LABELS[r]}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={s.active ? "ACTIVE" : "SUSPENDED"} label={s.active ? "Actif" : "Désactivé"} />
                    {s.mustChangePassword && <span className="ml-2 text-xs text-muted">1ʳᵉ connexion en attente</span>}
                  </td>
                  <td className="px-4 py-3 text-muted">{s.totpEnabled ? "Activée" : "À configurer"}</td>
                  <td className="px-4 py-3">
                    {s.role === "DELIVERY_ADMIN" ? (
                      <Button variant="secondary" onClick={() => togglePricing(s)} disabled={actingId === s.id}>
                        {s.deliveryPricingGranted ? "Attribuée" : "Non attribuée"}
                      </Button>
                    ) : (
                      <span className="text-muted">—</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(s.createdAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      <Button variant={s.active ? "danger" : "primary"} onClick={() => toggleActive(s)} disabled={actingId === s.id}>
                        {s.active ? "Désactiver" : "Réactiver"}
                      </Button>
                      <Button variant="secondary" onClick={() => editIdentifiers(s)} disabled={actingId === s.id}>
                        Modifier les identifiants
                      </Button>
                      <Button variant="secondary" onClick={() => resetPassword(s)} disabled={actingId === s.id}>
                        Réinitialiser
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </Table>
          )}
        </Card>
      )}
      {data && <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  );
}
