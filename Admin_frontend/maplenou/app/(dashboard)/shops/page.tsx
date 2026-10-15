"use client";

import { useEffect, useState, type FormEvent } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { Page, SellerProfileResponse, ShopResponse, ShopStatus } from "@/lib/types";
import {
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  StatusBadge,
  Table,
  formatDate,
} from "@/components/ui";

const STATUS_FILTERS: { value: ShopStatus | null; label: string }[] = [
  { value: "PENDING", label: "En attente" },
  { value: "APPROVED", label: "Approuvées" },
  { value: "SUSPENDED", label: "Suspendues" },
  { value: "REJECTED", label: "Rejetées" },
  { value: null, label: "Toutes" },
];

export default function ShopsPage() {
  const [status, setStatus] = useState<ShopStatus | null>("PENDING");
  const [data, setData] = useState<Page<ShopResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  const [approvedSellers, setApprovedSellers] = useState<SellerProfileResponse[]>([]);
  const [ownerId, setOwnerId] = useState("");
  const [shopName, setShopName] = useState("");
  const [city, setCity] = useState("");
  const [district, setDistrict] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  function loadApprovedSellers() {
    apiClient
      .get<Page<SellerProfileResponse>>("/api/sellers/admin", { status: "APPROVED", size: 100 })
      .then((res) => setApprovedSellers(res.content))
      .catch(() => setApprovedSellers([]));
  }

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setCreating(true);
    try {
      await apiClient.post("/api/admin/shops", { ownerId, name: shopName, city, district: district || null });
      setOwnerId("");
      setShopName("");
      setCity("");
      setDistrict("");
      load();
    } catch (err) {
      setFormError(err instanceof ApiException ? err.message : "Erreur lors de la création.");
    } finally {
      setCreating(false);
    }
  }

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<ShopResponse>>("/api/admin/shops", { status: status ?? undefined, size: 50 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [status]);
  useEffect(loadApprovedSellers, []);

  async function setStatusFor(id: string, newStatus: ShopStatus) {
    setActingId(id);
    try {
      await apiClient.patch(`/api/admin/shops/${id}/status`, { status: newStatus });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur lors de la décision.");
    } finally {
      setActingId(null);
    }
  }

  return (
    <div>
      <PageHeader title="Boutiques" subtitle="Approbation et modération des boutiques vendeurs" />

      <Card className="mb-6 max-w-2xl">
        <h2 className="mb-3 text-sm font-semibold text-foreground">Créer une boutique pour un vendeur</h2>
        {approvedSellers.length === 0 ? (
          <p className="text-sm text-muted">
            Aucun vendeur approuvé pour l&apos;instant — approuve d&apos;abord une candidature dans
            &laquo;&nbsp;Candidatures vendeurs&nbsp;&raquo;, un compte doit déjà avoir un profil vendeur approuvé
            avant qu&apos;on puisse lui créer une boutique.
          </p>
        ) : (
          <>
            {formError && (
              <div className="mb-3">
                <ErrorState message={formError} />
              </div>
            )}
            <form onSubmit={handleCreate} className="flex flex-wrap items-end gap-3">
              <div className="min-w-[200px]">
                <label className="mb-1 block text-xs font-medium text-muted">Vendeur</label>
                <select
                  required
                  value={ownerId}
                  onChange={(e) => setOwnerId(e.target.value)}
                  className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
                >
                  <option value="">Choisir...</option>
                  {approvedSellers.map((s) => (
                    <option key={s.userId} value={s.userId}>
                      {s.userFullName} ({s.userPhoneNumber})
                    </option>
                  ))}
                </select>
              </div>
              <div className="flex-1 min-w-[160px]">
                <label className="mb-1 block text-xs font-medium text-muted">Nom de la boutique</label>
                <input
                  required
                  value={shopName}
                  onChange={(e) => setShopName(e.target.value)}
                  className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
                />
              </div>
              <div className="min-w-[140px]">
                <label className="mb-1 block text-xs font-medium text-muted">Ville</label>
                <input
                  required
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  placeholder="Lomé"
                  className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
                />
              </div>
              <div className="min-w-[140px]">
                <label className="mb-1 block text-xs font-medium text-muted">Quartier (optionnel)</label>
                <input
                  value={district}
                  onChange={(e) => setDistrict(e.target.value)}
                  className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
                />
              </div>
              <Button type="submit" disabled={creating}>
                Créer
              </Button>
            </form>
          </>
        )}
      </Card>

      <div className="mb-4 flex gap-2">
        {STATUS_FILTERS.map((f) => (
          <button
            key={f.label}
            onClick={() => setStatus(f.value)}
            className={`rounded-full px-3 py-1.5 text-sm font-medium transition ${
              status === f.value ? "bg-primary text-white" : "bg-surface text-muted ring-1 ring-border"
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
            <EmptyState message="Aucune boutique." />
          ) : (
            <Table headers={["Boutique", "Ville", "Statut", "Créée le", "Actions"]}>
              {data.content.map((s) => (
                <tr key={s.id}>
                  <td className="px-4 py-3 font-medium text-foreground">{s.name}</td>
                  <td className="px-4 py-3 text-muted">
                    {s.city}
                    {s.district ? `, ${s.district}` : ""}
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={s.status} />
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(s.createdAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      {s.status !== "APPROVED" && (
                        <Button onClick={() => setStatusFor(s.id, "APPROVED")} disabled={actingId === s.id}>
                          Approuver
                        </Button>
                      )}
                      {s.status === "APPROVED" && (
                        <Button
                          variant="secondary"
                          onClick={() => setStatusFor(s.id, "SUSPENDED")}
                          disabled={actingId === s.id}
                        >
                          Suspendre
                        </Button>
                      )}
                      {s.status !== "REJECTED" && (
                        <Button
                          variant="danger"
                          onClick={() => setStatusFor(s.id, "REJECTED")}
                          disabled={actingId === s.id}
                        >
                          Rejeter
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
    </div>
  );
}
