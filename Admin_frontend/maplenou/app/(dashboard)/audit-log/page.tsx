"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { AuditLogResponse, Page } from "@/lib/types";
import { Card, EmptyState, ErrorState, LoadingState, PageHeader, Table, formatDate } from "@/components/ui";

export default function AuditLogPage() {
  const [data, setData] = useState<Page<AuditLogResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    apiClient
      .get<Page<AuditLogResponse>>("/api/admin/audit", { size: 100 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <PageHeader title="Journal d'audit" subtitle="Traçabilité des actions admin sensibles (modération, remboursement, rôles…)" />

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {data && (
        <Card>
          {data.content.length === 0 ? (
            <EmptyState message="Aucune entrée." />
          ) : (
            <Table headers={["Action", "Type d'entité", "Entité", "Détails", "IP", "Date"]}>
              {data.content.map((log) => (
                <tr key={log.id}>
                  <td className="px-4 py-3 font-medium text-foreground">{log.action}</td>
                  <td className="px-4 py-3 text-muted">{log.entityType}</td>
                  <td className="px-4 py-3 font-mono text-xs text-muted">{log.entityId?.slice(0, 8) ?? "—"}</td>
                  <td className="px-4 py-3 text-muted">{log.details ?? "—"}</td>
                  <td className="px-4 py-3 text-muted">{log.ipAddress ?? "—"}</td>
                  <td className="px-4 py-3 text-muted">{formatDate(log.createdAt)}</td>
                </tr>
              ))}
            </Table>
          )}
        </Card>
      )}
    </div>
  );
}
