"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { CourierDocumentResponse, CourierDocumentType, Page } from "@/lib/types";
import { Button, Card, EmptyState, ErrorState, LoadingState, PageHeader, Table, formatDate } from "@/components/ui";

const TYPE_LABELS: Record<CourierDocumentType, string> = {
  DRIVING_LICENSE: "Permis de conduire",
  ID_CARD: "Carte d'identité",
  VEHICLE_INSURANCE: "Assurance véhicule",
  VEHICLE_REGISTRATION: "Carte grise",
};

export default function CourierDocumentsPage() {
  const [data, setData] = useState<Page<CourierDocumentResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<CourierDocumentResponse>>("/api/admin/courier-documents", { size: 50 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function approve(id: string) {
    setActingId(id);
    try {
      await apiClient.patch(`/api/admin/courier-documents/${id}/review`, { decision: "VALID", rejectionReason: null });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  async function reject(id: string) {
    const reason = prompt("Motif du refus ?");
    if (!reason) return;
    setActingId(id);
    try {
      await apiClient.patch(`/api/admin/courier-documents/${id}/review`, { decision: "REJECTED", rejectionReason: reason });
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
        title="Documents livreurs"
        subtitle="File d'attente de vérification — un livreur ne peut recevoir de livraison que si ses 4 documents sont validés."
      />

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {data && (
        <Card>
          {data.content.length === 0 ? (
            <EmptyState message="Aucun document en attente." />
          ) : (
            <Table headers={["Livreur", "Document", "Fichier", "Soumis le", "Actions"]}>
              {data.content.map((d) => (
                <tr key={d.id}>
                  <td className="px-4 py-3 font-medium text-foreground">{d.courierName}</td>
                  <td className="px-4 py-3 text-muted">{TYPE_LABELS[d.type]}</td>
                  <td className="px-4 py-3">
                    <a href={d.fileUrl} target="_blank" rel="noreferrer" className="text-primary underline">
                      Voir le fichier
                    </a>
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(d.submittedAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <Button onClick={() => approve(d.id)} disabled={actingId === d.id}>
                        Valider
                      </Button>
                      <Button variant="danger" onClick={() => reject(d.id)} disabled={actingId === d.id}>
                        Refuser
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
