"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { Page, SubOrderResponse, SubOrderStatus } from "@/lib/types";
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
  formatFcfa,
} from "@/components/ui";

const STATUS_LABELS: Record<SubOrderStatus, string> = {
  PENDING: "En attente",
  PREPARING: "En préparation",
  READY_FOR_PICKUP: "Prêt pour retrait",
  IN_DELIVERY: "En livraison",
  DELIVERED: "Livré",
  CANCELLED: "Annulé",
  RETURN_REQUESTED: "Retour demandé",
  RETURNED: "Retourné",
};

const ALL_STATUSES = Object.keys(STATUS_LABELS) as SubOrderStatus[];

const FILTERS: { value: SubOrderStatus | "ALL"; label: string }[] = [
  { value: "ALL", label: "Toutes" },
  ...ALL_STATUSES.map((s) => ({ value: s, label: STATUS_LABELS[s] })),
];

export default function SubOrdersPage() {
  const [status, setStatus] = useState<SubOrderStatus | "ALL">("ALL");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<SubOrderResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);
  const [forceStatusFor, setForceStatusFor] = useState<Record<string, SubOrderStatus>>({});

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<SubOrderResponse>>("/api/sub-orders/admin", {
        status: status === "ALL" ? undefined : status,
        page,
        size: 20,
      })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [status, page]);

  async function createDelivery(subOrderId: string) {
    setActingId(subOrderId);
    try {
      await apiClient.post(`/api/deliveries/sub-orders/${subOrderId}`);
      alert("Livraison créée — va dans \"Livraisons en attente\" pour assigner un livreur.");
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur lors de la création de la livraison.");
    } finally {
      setActingId(null);
    }
  }

  async function forceStatus(subOrderId: string) {
    const newStatus = forceStatusFor[subOrderId];
    if (!newStatus) {
      alert("Choisis d'abord un statut dans la liste déroulante.");
      return;
    }
    if (!confirm(`Forcer le statut à "${STATUS_LABELS[newStatus]}" ? Ceci contourne les transitions normales vendeur/livreur.`))
      return;
    setActingId(subOrderId);
    try {
      await apiClient.patch(`/api/sub-orders/admin/${subOrderId}/status`, { status: newStatus });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  return (
    <div>
      <PageHeader
        title="Commandes"
        subtitle="Toutes les sous-commandes de la plateforme, toutes boutiques confondues. Statut « Prêt pour retrait » : crée la livraison pour pouvoir ensuite assigner un livreur depuis « Livraisons en attente »."
      />

      <div className="mb-4 flex flex-wrap gap-2">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            onClick={() => {
              setStatus(f.value);
              setPage(0);
            }}
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
            <EmptyState message="Aucune commande pour ce filtre." />
          ) : (
            <Table headers={["Commande", "Boutique", "Acheteur", "Sous-total", "Livraison", "Statut", "Créée le", "Actions"]}>
              {data.content.map((so) => (
                <tr key={so.id}>
                  <td className="px-4 py-3 font-medium text-foreground">#{so.orderNumber}</td>
                  <td className="px-4 py-3 text-muted">{so.shopName}</td>
                  <td className="px-4 py-3 text-muted">{so.buyerName}</td>
                  <td className="px-4 py-3 text-muted">{formatFcfa(so.subtotal)}</td>
                  <td className="px-4 py-3 text-muted">{formatFcfa(so.deliveryFee)}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={so.status} label={STATUS_LABELS[so.status]} />
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(so.createdAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap items-center gap-2">
                      {so.status === "READY_FOR_PICKUP" && (
                        <Button onClick={() => createDelivery(so.id)} disabled={actingId === so.id}>
                          Créer la livraison
                        </Button>
                      )}
                      <select
                        value={forceStatusFor[so.id] ?? ""}
                        onChange={(e) =>
                          setForceStatusFor((prev) => ({ ...prev, [so.id]: e.target.value as SubOrderStatus }))
                        }
                        className="rounded-lg border border-border bg-input-fill px-2 py-1.5 text-sm"
                      >
                        <option value="">Forcer vers...</option>
                        {ALL_STATUSES.map((s) => (
                          <option key={s} value={s}>
                            {STATUS_LABELS[s]}
                          </option>
                        ))}
                      </select>
                      <Button variant="secondary" onClick={() => forceStatus(so.id)} disabled={actingId === so.id}>
                        Forcer
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
