"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { AdminUserResponse, DeliveryResponse, Page } from "@/lib/types";
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

export default function DeliveriesPage() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<DeliveryResponse> | null>(null);
  const [couriers, setCouriers] = useState<AdminUserResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [assigningId, setAssigningId] = useState<string | null>(null);
  const [selectedCourier, setSelectedCourier] = useState<Record<string, string>>({});

  function load() {
    setLoading(true);
    setError(null);
    Promise.all([
      apiClient.get<Page<DeliveryResponse>>("/api/deliveries/pending", { page, size: 20 }),
      apiClient.get<Page<AdminUserResponse>>("/api/admin/users", { role: "DELIVERY_AGENT", active: true, size: 100 }),
    ])
      .then(([deliveries, agents]) => {
        setData(deliveries);
        setCouriers(agents.content);
      })
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [page]);

  async function assign(deliveryId: string) {
    const agentId = selectedCourier[deliveryId];
    if (!agentId) {
      alert("Choisis un livreur d'abord.");
      return;
    }
    setAssigningId(deliveryId);
    try {
      await apiClient.patch(`/api/deliveries/${deliveryId}/assign`, { agentId });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur — le livreur n'a peut-être pas tous ses documents validés.");
    } finally {
      setAssigningId(null);
    }
  }

  return (
    <div>
      <PageHeader title="Livraisons en attente" subtitle="Affecter un livreur aux colis prêts à être récupérés" />

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {data && (
        <Card>
          {data.content.length === 0 ? (
            <EmptyState message="Aucune livraison en attente d'affectation." />
          ) : (
            <Table headers={["Commande", "Boutique", "Statut", "Créée le", "Affecter"]}>
              {data.content.map((d) => (
                <tr key={d.id}>
                  <td className="px-4 py-3 font-medium text-foreground">#{d.orderNumber}</td>
                  <td className="px-4 py-3 text-muted">{d.shopName}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={d.status} />
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(d.createdAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <select
                        value={selectedCourier[d.id] ?? ""}
                        onChange={(e) => setSelectedCourier((prev) => ({ ...prev, [d.id]: e.target.value }))}
                        className="rounded-lg border border-border bg-input-fill px-2 py-1.5 text-sm"
                      >
                        <option value="">Choisir un livreur</option>
                        {couriers.map((c) => (
                          <option key={c.id} value={c.id}>
                            {c.fullName}
                          </option>
                        ))}
                      </select>
                      <Button onClick={() => assign(d.id)} disabled={assigningId === d.id}>
                        Affecter
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
