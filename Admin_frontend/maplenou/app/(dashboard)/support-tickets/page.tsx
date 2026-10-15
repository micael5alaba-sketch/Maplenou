"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { Page, SupportTicketResponse, SupportTicketStatus } from "@/lib/types";
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

const FILTERS: { label: string; value: SupportTicketStatus | "ALL" }[] = [
  { label: "Ouverts", value: "OPEN" },
  { label: "En cours", value: "IN_PROGRESS" },
  { label: "Résolus", value: "RESOLVED" },
  { label: "Tous", value: "ALL" },
];

export default function SupportTicketsPage() {
  const [filter, setFilter] = useState<SupportTicketStatus | "ALL">("OPEN");
  const [data, setData] = useState<Page<SupportTicketResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<SupportTicketResponse>>("/api/admin/support-tickets", {
        status: filter === "ALL" ? undefined : filter,
        size: 50,
      })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [filter]);

  async function resolve(id: string) {
    const resolutionNotes = prompt("Note de résolution (optionnel) :") ?? "";
    setActingId(id);
    try {
      await apiClient.patch(`/api/admin/support-tickets/${id}/resolve`, { status: "RESOLVED", resolutionNotes });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  return (
    <div>
      <PageHeader title="Tickets de support" subtitle="Demandes d'acheteurs et de livreurs" />

      <div className="mb-4 flex gap-2">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            onClick={() => setFilter(f.value)}
            className={`rounded-full px-4 py-1.5 text-sm font-semibold transition ${
              filter === f.value ? "bg-primary text-white" : "bg-input-fill text-foreground"
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
            <EmptyState message="Aucun ticket." />
          ) : (
            <Table headers={["N°", "Demandeur", "Catégorie", "Sujet", "Statut", "Créé le", "Actions"]}>
              {data.content.map((t) => (
                <tr key={t.id}>
                  <td className="px-4 py-3 font-mono text-foreground">{t.ticketNumber}</td>
                  <td className="px-4 py-3 text-muted">{t.requesterName}</td>
                  <td className="px-4 py-3 text-muted">{t.category}</td>
                  <td className="px-4 py-3 text-foreground">{t.subject}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={t.status} />
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(t.createdAt)}</td>
                  <td className="px-4 py-3">
                    {t.status !== "RESOLVED" ? (
                      <Button onClick={() => resolve(t.id)} disabled={actingId === t.id}>
                        Résoudre
                      </Button>
                    ) : (
                      <span className="text-sm text-muted">—</span>
                    )}
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
