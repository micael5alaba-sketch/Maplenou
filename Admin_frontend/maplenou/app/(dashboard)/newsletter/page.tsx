"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { Page, SubscriberResponse } from "@/lib/types";
import { Card, EmptyState, ErrorState, LoadingState, PageHeader, Pagination, Table, formatDate } from "@/components/ui";

export default function NewsletterPage() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<SubscriberResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    setLoading(true);
    apiClient
      .get<Page<SubscriberResponse>>("/api/admin/newsletter/subscribers", { page, size: 20 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }, [page]);

  async function exportCsv() {
    setExporting(true);
    try {
      // Export complet, indépendant de la pagination affichée à l'écran.
      const all = await apiClient.get<Page<SubscriberResponse>>("/api/admin/newsletter/subscribers", { size: 5000 });
      const rows = ["email,date d'inscription", ...all.content.map((s) => `${s.email},${s.subscribedAt}`)];
      const blob = new Blob([rows.join("\n")], { type: "text/csv" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "newsletter-abonnes.csv";
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur lors de l'export.");
    } finally {
      setExporting(false);
    }
  }

  return (
    <div>
      <PageHeader
        title="Newsletter"
        subtitle="Abonnés actifs — export vers l'outil d'emailing choisi"
        action={
          data && data.content.length > 0 ? (
            <button
              onClick={exportCsv}
              disabled={exporting}
              className="rounded-full border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-input-fill disabled:opacity-50"
            >
              {exporting ? "Export..." : "Exporter en CSV"}
            </button>
          ) : undefined
        }
      />

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {data && (
        <Card>
          {data.content.length === 0 ? (
            <EmptyState message="Aucun abonné." />
          ) : (
            <Table headers={["Email", "Inscrit le"]}>
              {data.content.map((s) => (
                <tr key={s.id}>
                  <td className="px-4 py-3 font-medium text-foreground">{s.email}</td>
                  <td className="px-4 py-3 text-muted">{formatDate(s.subscribedAt)}</td>
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
