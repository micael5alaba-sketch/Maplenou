"use client";

import { useEffect, useState, type ReactNode } from "react";
import Link from "next/link";
import { apiClient, ApiException } from "@/lib/apiClient";
import { useAuth } from "@/lib/auth";
import { ROLE_LABELS } from "@/lib/permissions";
import type { RoleDashboardResponse } from "@/lib/types";
import { Card, ErrorState, LoadingState, formatDate, formatFcfa } from "@/components/ui";

function formatDuration(minutes: number | null | undefined): string {
  if (minutes == null) return "—";
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  if (hours < 48) return `${hours} h ${minutes % 60} min`;
  return `${Math.floor(hours / 24)} j`;
}

function Tile({
  label,
  value,
  href,
  urgent,
  hint,
}: {
  label: string;
  value: ReactNode;
  href?: string;
  /** Mis en évidence quand il y a du travail en attente. */
  urgent?: boolean;
  hint?: string;
}) {
  const body = (
    <div
      className={`rounded-2xl p-4 ring-1 transition ${
        urgent ? "bg-accent/10 ring-accent/40" : "bg-surface ring-border"
      } ${href ? "hover:ring-primary" : ""}`}
    >
      <p className="text-xs font-semibold uppercase tracking-wide text-muted">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${urgent ? "text-accent" : "text-foreground"}`}>{value}</p>
      {hint && <p className="mt-1 text-xs text-muted">{hint}</p>}
    </div>
  );
  return href ? <Link href={href}>{body}</Link> : body;
}

function Section({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  return (
    <section>
      <h2 className="text-lg font-bold text-foreground">{title}</h2>
      {subtitle && <p className="mb-3 text-sm text-muted">{subtitle}</p>}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">{children}</div>
    </section>
  );
}

/**
 * Tableau de bord dédié au rôle (ou au rôle visualisé par le Super Admin) : seules les sections
 * auxquelles le rôle a droit sont renvoyées par l'API. Chaque indicateur renvoie vers la page où agir.
 */
export function RoleDashboard() {
  const { user, viewAsRole } = useAuth();
  const [data, setData] = useState<RoleDashboardResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    setError(null);
    apiClient
      .get<RoleDashboardResponse>("/api/admin/dashboard", { viewAs: viewAsRole ?? undefined })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }, [viewAsRole]);

  if (loading) return <LoadingState />;
  if (error) return <ErrorState message={error} />;
  if (!data) return null;

  const { logistics, support, finance, audit } = data;
  const roleLabel = ROLE_LABELS[user?.role ?? ""] ?? "Personnel";

  const sections: ReactNode[] = [];

  if (logistics) {
    sections.push(
      <Section key="logistics" title="Logistique" subtitle="Livraisons et livreurs : ce qui demande une action maintenant.">
        <Tile label="À affecter" value={logistics.pendingDeliveries} href="/deliveries" urgent={logistics.pendingDeliveries > 0}
          hint={logistics.oldestPendingMinutes != null ? `La plus ancienne attend depuis ${formatDuration(logistics.oldestPendingMinutes)}` : undefined} />
        <Tile label="Affectées" value={logistics.assignedDeliveries} href="/deliveries" />
        <Tile label="En cours" value={logistics.inTransitDeliveries} href="/deliveries" />
        <Tile label="Livrées aujourd'hui" value={logistics.deliveredToday} />
        <Tile label="Échecs" value={logistics.failedDeliveries} href="/deliveries" urgent={logistics.failedDeliveries > 0} />
        <Tile label="Durée moyenne (7 j)" value={formatDuration(logistics.averageDeliveryMinutes7d == null ? null : Math.round(logistics.averageDeliveryMinutes7d))} hint="De l'affectation à la livraison" />
        <Tile label="Livreurs localisés" value={`${logistics.onlineCouriers} / ${logistics.activeCouriers}`} href="/deliveries" hint="Position reçue il y a moins de 10 min : ils reçoivent les courses automatiquement" />
        {logistics.courierDocumentsToReview != null && (
          <Tile label="Documents à vérifier" value={logistics.courierDocumentsToReview} href="/courier-documents" urgent={logistics.courierDocumentsToReview > 0} />
        )}
        {logistics.courierEarningsAvailable != null && (
          <Tile label="Gains livreurs disponibles" value={formatFcfa(logistics.courierEarningsAvailable)} href="/courier-earnings" hint="Pas encore dans un retrait" />
        )}
      </Section>,
    );
  }

  if (support) {
    sections.push(
      <Section key="support" title="Support & vendeurs" subtitle="Dossiers clients et vendeurs en attente de traitement.">
        <Tile label="Tickets ouverts" value={support.openTickets} href="/support-tickets" urgent={support.openTickets > 0}
          hint={support.oldestOpenTicketHours != null ? `Le plus ancien : ${support.oldestOpenTicketHours} h` : undefined} />
        <Tile label="Tickets en cours" value={support.inProgressTickets} href="/support-tickets" />
        <Tile label="Résolus (7 j)" value={support.resolvedThisWeek} />
        <Tile label="Candidatures vendeurs" value={support.pendingSellerApplications} href="/sellers" urgent={support.pendingSellerApplications > 0} />
        <Tile label="Changements de catégorie" value={support.pendingCategoryChanges} href="/seller-fees" urgent={support.pendingCategoryChanges > 0} />
        <Tile label="Retours à décider" value={support.pendingReturns} href="/returns" urgent={support.pendingReturns > 0} />
      </Section>,
    );
  }

  if (finance) {
    sections.push(
      <Section key="finance" title="Retraits" subtitle="Vendeurs et livreurs : réception, vérification, soumission, approbation du Super Admin, paiement.">
        <Tile label="À vérifier" value={finance.toVerify} href="/payouts" urgent={finance.toVerify > 0} hint="Reçus, pas encore contrôlés" />
        <Tile label="À corriger" value={finance.toCorrect} href="/payouts" hint="Renvoyés pour correction" />
        <Tile label="Préparés" value={finance.prepared} href="/payouts" hint="À soumettre au Super Admin" />
        <Tile label="À approuver" value={finance.awaitingApproval} href="/payouts" urgent={finance.awaitingApproval > 0} hint={formatFcfa(finance.awaitingApprovalAmount)} />
        <Tile label="Approuvés, à payer" value={finance.approvedToPay} href="/payouts" hint={formatFcfa(finance.approvedToPayAmount)} />
        <Tile label="Payés (7 j)" value={formatFcfa(finance.paidLast7Days)} />
      </Section>,
    );
  }

  if (audit) {
    sections.push(
      <section key="audit">
        <h2 className="text-lg font-bold text-foreground">Contrôle & audit</h2>
        <p className="mb-3 text-sm text-muted">Activité et signaux de sécurité.</p>
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          <Tile label="Actions (24 h)" value={audit.actionsLast24h} href="/audit-log" />
          <Tile label="Connexions échouées (24 h)" value={audit.failedLoginsLast24h} href="/audit-log" urgent={audit.failedLoginsLast24h >= 10} />
          <Tile label="Accès refusés (7 j)" value={audit.accessDeniedLast7d} href="/audit-log" urgent={audit.accessDeniedLast7d > 0} />
        </div>
        <Card className="mt-4">
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted">Dernières actions</p>
          <ul className="divide-y divide-border text-sm">
            {audit.recent.map((a) => (
              <li key={a.id} className="flex items-start justify-between gap-4 py-2">
                <span className="text-foreground">
                  <span className="font-semibold">{a.action}</span>
                  {a.details ? <span className="text-muted"> — {a.details}</span> : null}
                </span>
                <span className="flex-shrink-0 text-xs text-muted">{formatDate(a.createdAt)}</span>
              </li>
            ))}
          </ul>
        </Card>
      </section>,
    );
  }

  if (sections.length === 0) return null;

  return (
    <div className="mb-10 space-y-8">
      <div className="rounded-2xl bg-primary/10 px-5 py-4">
        <p className="text-sm font-semibold text-primary">Votre espace de travail</p>
        <p className="text-xl font-bold text-foreground">{roleLabel}</p>
      </div>
      {sections}
    </div>
  );
}
