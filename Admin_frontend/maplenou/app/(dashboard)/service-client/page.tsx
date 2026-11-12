"use client";

import { useEffect, useState } from "react";
import { ApiException } from "@/lib/apiClient";
import { SERVICE_CLIENT_WEB_URL, openServiceClient, serviceClient } from "@/lib/serviceClient";
import type { ServiceClientDashboard, ServiceClientStaffRole, ServiceClientTicket } from "@/lib/types";
import { Button, Card, EmptyState, ErrorState, LoadingState, PageHeader, StatusBadge, Table, formatDate } from "@/components/ui";

const ROLE_LABELS: Record<string, string> = {
  SUPPORT_AGENT: "Agent du service client",
  SUPPORT_MANAGER: "Responsable du service client",
  ADMINISTRATOR: "Administrateur du service client",
};

const STATUS_LABELS: Record<string, string> = {
  NEW: "Nouveau",
  ASSIGNED: "Pris en charge",
  IN_PROGRESS: "En cours",
  WAITING_FOR_CUSTOMER: "Attente client",
  RESOLVED: "Résolu",
  CLOSED: "Clôturé",
};

function Kpi({ label, value, urgent }: { label: string; value: number; urgent?: boolean }) {
  return (
    <Card>
      <p className="text-xs font-semibold uppercase tracking-wide text-muted">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${urgent && value > 0 ? "text-error" : "text-foreground"}`}>{value}</p>
    </Card>
  );
}

/**
 * Supervision du service client, en lecture seule : le service client a sa propre administration et ses propres rôles
 * (équipe service client) ; le Super Admin Maplenou voit ici ce qui s'y passe, sans quitter son back-office.
 */
export default function ServiceClientSupervisionPage() {
  const [dashboard, setDashboard] = useState<ServiceClientDashboard | null>(null);
  const [breached, setBreached] = useState<ServiceClientTicket[]>([]);
  const [staff, setStaff] = useState<ServiceClientStaffRole[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  function load() {
    setLoading(true);
    setError(null);
    Promise.all([
      serviceClient.dashboard(),
      serviceClient.tickets({ open: true, slaBreached: true, size: 10, sort: "updatedAt,desc" }),
      serviceClient.staffRoles(),
    ])
      .then(([d, b, s]) => {
        setDashboard(d);
        setBreached(b.items);
        setStaff(s);
      })
      .catch((e) =>
        setError(
          e instanceof ApiException
            ? e.status === 403
              ? "Accès refusé : ce compte n'est pas administrateur du service client."
              : e.message
            : "Erreur de chargement.",
        ),
      )
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  return (
    <div>
      <PageHeader
        title="Supervision du service client"
        subtitle="Vue en lecture seule. L'équipe du service client gère ses agents, ses rôles et ses tickets dans son propre espace."
        action={
          <Button onClick={() => openServiceClient().catch((e) => alert(e instanceof Error ? e.message : "Indisponible."))}>
            Ouvrir le service client ↗
          </Button>
        }
      />

      {loading && <LoadingState />}
      {error && (
        <div className="space-y-3">
          <ErrorState message={error} />
          <Button variant="secondary" onClick={load}>
            Réessayer
          </Button>
        </div>
      )}

      {dashboard && (
        <>
          <div className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
            <Kpi label="Tickets ouverts" value={dashboard.openCount} />
            <Kpi label="Sans agent" value={dashboard.unassignedCount} urgent />
            <Kpi label="Délai dépassé" value={dashboard.slaBreachedOpenCount} urgent />
            <Kpi label="Escaladés" value={dashboard.escalatedOpenCount} urgent />
            <Kpi label="En cours" value={dashboard.inProgressCount} />
            <Kpi label="Attente du client" value={dashboard.waitingForCustomerCount} />
            <Kpi label="Résolus aujourd'hui" value={dashboard.resolvedTodayCount} />
            <Card>
              <p className="text-xs font-semibold uppercase tracking-wide text-muted">Satisfaction (30 jours)</p>
              <p className="mt-1 text-2xl font-bold text-foreground">
                {dashboard.satisfactionLast30Days?.average != null
                  ? `${dashboard.satisfactionLast30Days.average.toFixed(1)} / 5`
                  : "—"}
              </p>
            </Card>
          </div>

          <h2 className="mb-3 text-base font-bold text-foreground">Activité récente</h2>
          <Card>
            {dashboard.recent.length === 0 ? (
              <EmptyState message="Aucun ticket pour le moment." />
            ) : (
              <Table headers={["Ticket", "Sujet", "Client", "Agent", "Statut", "Mis à jour"]}>
                {dashboard.recent.map((t) => (
                  <tr key={t.id}>
                    <td className="px-4 py-3 font-medium text-foreground">{t.ticketNumber}</td>
                    <td className="px-4 py-3">{t.subject}</td>
                    <td className="px-4 py-3 text-muted">{t.requester.name}</td>
                    <td className="px-4 py-3 text-muted">{t.assignedAgent?.name ?? "Non affecté"}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={t.status} label={STATUS_LABELS[t.status] ?? t.status} />
                    </td>
                    <td className="px-4 py-3 text-muted">{formatDate(t.updatedAt)}</td>
                  </tr>
                ))}
              </Table>
            )}
          </Card>

          <h2 className="mb-3 mt-8 text-base font-bold text-foreground">Délais de réponse dépassés</h2>
          <Card>
            {breached.length === 0 ? (
              <EmptyState message="Aucun ticket en retard." />
            ) : (
              <Table headers={["Ticket", "Sujet", "Agent", "Statut", "Créé le"]}>
                {breached.map((t) => (
                  <tr key={t.id}>
                    <td className="px-4 py-3 font-medium text-foreground">{t.ticketNumber}</td>
                    <td className="px-4 py-3">{t.subject}</td>
                    <td className="px-4 py-3 text-muted">{t.assignedAgent?.name ?? "Non affecté"}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={t.status} label={STATUS_LABELS[t.status] ?? t.status} />
                    </td>
                    <td className="px-4 py-3 text-muted">{formatDate(t.createdAt)}</td>
                  </tr>
                ))}
              </Table>
            )}
          </Card>

          <h2 className="mb-3 mt-8 text-base font-bold text-foreground">Équipe du service client</h2>
          <Card>
            {staff.length === 0 ? (
              <EmptyState message="Aucun rôle attribué : seuls les administrateurs Maplenou y ont accès." />
            ) : (
              <Table headers={["Téléphone", "Rôle", "Attribué le"]}>
                {staff.map((s) => (
                  <tr key={s.phone}>
                    <td className="px-4 py-3 font-medium text-foreground">{s.phone}</td>
                    <td className="px-4 py-3">{ROLE_LABELS[s.role] ?? s.role}</td>
                    <td className="px-4 py-3 text-muted">{formatDate(s.assignedAt)}</td>
                  </tr>
                ))}
              </Table>
            )}
          </Card>
          <p className="mt-4 text-xs text-muted">
            Les rôles se gèrent dans l&apos;espace du service client ({SERVICE_CLIENT_WEB_URL}), page « Agents ».
          </p>
        </>
      )}
    </div>
  );
}
