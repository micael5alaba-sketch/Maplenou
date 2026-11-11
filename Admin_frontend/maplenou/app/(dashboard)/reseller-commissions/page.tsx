"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { Page, ResellerCommissionResponse } from "@/lib/types";
import {
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

const LABELS: Record<string, string> = { PENDING: "En attente", CREDITED: "Créditée", CANCELLED: "Annulée" };

export default function ResellerCommissionsPage() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<ResellerCommissionResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    apiClient
      .get<Page<ResellerCommissionResponse>>("/api/admin/resellers/commissions", { page, size: 20 })
      .then((d) => {
        setData(d);
        setError(null);
      })
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }, [page]);

  return (
    <div>
      <PageHeader
        title="Commissions partenaires"
        subtitle="Une ligne par unité vendue via un lien partenaire. Montants figés à la commande ; crédités au portefeuille à la livraison."
      />
      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}
      {data && (
        <Card>
          {data.content.length === 0 ? (
            <EmptyState message="Aucune commission pour le moment." />
          ) : (
            <Table
              headers={["Produit", "Prix unitaire", "Taux", "Commission Maplenou", "Part partenaire", "Reste Maplenou", "Version", "Statut", "Date"]}
            >
              {data.content.map((c) => (
                <tr key={c.id}>
                  <td className="px-4 py-3 font-medium text-foreground">{c.productName}</td>
                  <td className="px-4 py-3">{formatFcfa(c.unitPrice)}</td>
                  <td className="px-4 py-3">{(c.commissionRate * 100).toFixed(1)} %</td>
                  <td className="px-4 py-3">{formatFcfa(c.maplenouCommission)}</td>
                  <td className="px-4 py-3 font-semibold">{formatFcfa(c.partnerShare)}</td>
                  <td className="px-4 py-3">{formatFcfa(c.maplenouShare)}</td>
                  <td className="px-4 py-3 text-muted">v{c.settingsVersion}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={c.status} label={LABELS[c.status]} />
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(c.createdAt)}</td>
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
