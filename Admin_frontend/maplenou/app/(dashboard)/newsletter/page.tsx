"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { Page, SubscriberResponse } from "@/lib/types";
import { Card, EmptyState, ErrorState, LoadingState, PageHeader, Table, formatDate } from "@/components/ui";

export default function NewsletterPage() {
  const [data, setData] = useState<Page<SubscriberResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    apiClient
      .get<Page<SubscriberResponse>>("/api/admin/newsletter/subscribers", { size: 100 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }, []);

  function exportCsv() {
    if (!data) return;
    const rows = ["email,date d'inscription", ...data.content.map((s) => `${s.email},${s.subscribedAt}`)];
    const blob = new Blob([rows.join("\n")], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "newsletter-abonnes.csv";
    a.click();
    URL.revokeObjectURL(url);
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
              className="rounded-full border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-input-fill"
            >
              Exporter en CSV
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
    </div>
  );
}
