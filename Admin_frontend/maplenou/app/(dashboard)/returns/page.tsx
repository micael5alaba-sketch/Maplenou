"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { Page, ReturnRequestResponse } from "@/lib/types";
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

export default function ReturnsPage() {
  const [data, setData] = useState<Page<ReturnRequestResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<ReturnRequestResponse>>("/api/return-requests/admin/pending", { size: 50 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function decide(id: string, decision: "APPROVED" | "REJECTED") {
    const adminNotes = prompt("Notes (optionnel) :") ?? "";
    setActingId(id);
    try {
      await apiClient.patch(`/api/return-requests/${id}/decision`, { decision, adminNotes });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  return (
    <div>
      <PageHeader title="Demandes de retour" subtitle="Fenêtre de 30 jours après livraison — décision manuelle au cas par cas" />

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {data && (
        <Card>
          {data.content.length === 0 ? (
            <EmptyState message="Aucune demande de retour en attente." />
          ) : (
            <Table headers={["Acheteur", "Boutique", "Motif", "État produit", "Échéance", "Statut", "Actions"]}>
              {data.content.map((r) => (
                <tr key={r.id}>
                  <td className="px-4 py-3 font-medium text-foreground">{r.buyerName}</td>
                  <td className="px-4 py-3 text-muted">{r.shopName}</td>
                  <td className="px-4 py-3 text-muted">{r.reason}</td>
                  <td className="px-4 py-3 text-muted">{r.productCondition}</td>
                  <td className="px-4 py-3 text-muted">{formatDate(r.returnDeadline)}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={r.decision ?? "PENDING"} />
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <Button onClick={() => decide(r.id, "APPROVED")} disabled={actingId === r.id}>
                        Approuver
                      </Button>
                      <Button variant="danger" onClick={() => decide(r.id, "REJECTED")} disabled={actingId === r.id}>
                        Rejeter
                      </Button>
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
