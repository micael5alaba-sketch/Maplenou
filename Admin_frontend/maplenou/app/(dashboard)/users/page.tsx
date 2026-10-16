"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { AdminUserResponse, Page, Role } from "@/lib/types";
import {
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Pagination,
  Table,
  formatDate,
} from "@/components/ui";

const ROLE_FILTERS: { label: string; value: Role | "ALL" }[] = [
  { label: "Tous", value: "ALL" },
  { label: "Admin", value: "ADMIN" },
  { label: "Livreurs", value: "DELIVERY_AGENT" },
  { label: "Comptes standards", value: null },
];

export default function UsersPage() {
  const [roleFilter, setRoleFilter] = useState<Role | "ALL">("ALL");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<AdminUserResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<AdminUserResponse>>("/api/admin/users", {
        role: roleFilter === "ALL" || roleFilter === null ? undefined : roleFilter,
        page,
        size: 20,
      })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [roleFilter, page]);

  async function toggleBan(u: AdminUserResponse) {
    setActingId(u.id);
    try {
      await apiClient.patch(`/api/admin/users/${u.id}/${u.active ? "ban" : "unban"}`);
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  async function anonymize(u: AdminUserResponse) {
    if (
      !confirm(
        `Anonymiser définitivement ${u.fullName} ?\n\nNom, téléphone et email seront effacés, le compte sera désactivé. Action tracée et irréversible (obligation RGPD).`,
      )
    )
      return;
    setActingId(u.id);
    try {
      await apiClient.post(`/api/admin/gdpr/users/${u.id}/anonymize`);
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  async function changeRole(u: AdminUserResponse) {
    const newRole = prompt("Nouveau rôle (ADMIN ou DELIVERY_AGENT) :", u.role ?? "");
    if (!newRole || (newRole !== "ADMIN" && newRole !== "DELIVERY_AGENT")) {
      if (newRole !== null) alert("Rôle invalide — seules les valeurs ADMIN ou DELIVERY_AGENT sont acceptées.");
      return;
    }
    setActingId(u.id);
    try {
      await apiClient.patch(`/api/admin/users/${u.id}/role`, { role: newRole });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  return (
    <div>
      <PageHeader title="Utilisateurs" subtitle="Un compte unique par personne — rôle optionnel (ADMIN / DELIVERY_AGENT)" />

      <div className="mb-4 flex gap-2">
        {ROLE_FILTERS.map((f) => (
          <button
            key={f.label}
            onClick={() => {
              setRoleFilter(f.value);
              setPage(0);
            }}
            className={`rounded-full px-4 py-1.5 text-sm font-semibold transition ${
              roleFilter === f.value ? "bg-primary text-white" : "bg-input-fill text-foreground"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {data && (
        <Card>
          {data.content.length === 0 ? (
            <EmptyState message="Aucun utilisateur." />
          ) : (
            <Table headers={["Nom", "Téléphone", "Rôle", "2FA", "Statut", "Inscrit le", "Actions"]}>
              {data.content.map((u) => (
                <tr key={u.id}>
                  <td className="px-4 py-3 font-medium text-foreground">{u.fullName}</td>
                  <td className="px-4 py-3 text-muted">{u.phoneNumber}</td>
                  <td className="px-4 py-3 text-muted">{u.role ?? "Standard"}</td>
                  <td className="px-4 py-3 text-muted">{u.totpEnabled ? "Activée" : "—"}</td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                        u.active ? "bg-primary/10 text-primary" : "bg-error/10 text-error"
                      }`}
                    >
                      {u.active ? "Actif" : "Banni"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(u.createdAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <Button variant="secondary" onClick={() => changeRole(u)} disabled={actingId === u.id}>
                        Rôle
                      </Button>
                      <Button
                        variant={u.active ? "danger" : "primary"}
                        onClick={() => toggleBan(u)}
                        disabled={actingId === u.id}
                      >
                        {u.active ? "Bannir" : "Débannir"}
                      </Button>
                      {u.role !== "ADMIN" && (
                        <Button variant="danger" onClick={() => anonymize(u)} disabled={actingId === u.id}>
                          Anonymiser (RGPD)
                        </Button>
                      )}
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
