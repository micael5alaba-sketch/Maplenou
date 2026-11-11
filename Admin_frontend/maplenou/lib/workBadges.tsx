"use client";

import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { apiClient } from "./apiClient";
import { useAuth } from "./auth";
import type { RoleDashboardResponse } from "./types";

/** Pastilles rouges : nombre de choses à faire par page du menu, mis à jour toutes les 30 s. */
interface WorkBadges {
  /** Par route du menu (ex. "/deliveries"). Absent ou 0 = rien à faire. */
  badges: Record<string, number>;
  total: number;
}

const WorkBadgesContext = createContext<WorkBadges>({ badges: {}, total: 0 });

const POLL_MS = 30_000;

function computeBadges(d: RoleDashboardResponse): Record<string, number> {
  const badges: Record<string, number> = {};
  if (d.logistics) {
    badges["/deliveries"] = d.logistics.pendingDeliveries;
    badges["/courier-documents"] = d.logistics.courierDocumentsToReview ?? 0;
  }
  if (d.support) {
    badges["/support-tickets"] = d.support.openTickets;
    badges["/support-conversations"] = d.support.unreadSupportMessages;
    badges["/sellers"] = d.support.pendingSellerApplications;
    badges["/seller-fees"] = d.support.pendingCategoryChanges;
    badges["/returns"] = d.support.pendingReturns;
  }
  if (d.finance) {
    badges["/payouts"] = d.finance.actionRequiredSellerPayouts;
    badges["/courier-withdrawals"] = d.finance.actionRequiredCourierWithdrawals;
  }
  return badges;
}

/**
 * Interroge le tableau de bord du rôle toutes les 30 s et en tire les pastilles rouges du menu
 * (candidatures à traiter, retraits à vérifier ou approuver, tickets sans réponse, messages
 * non lus, livraisons à affecter...). Le titre de l'onglet affiche le total, et une alerte du
 * navigateur prévient quand du nouveau travail arrive alors que l'onglet est en arrière-plan.
 */
export function WorkBadgesProvider({ children }: { children: ReactNode }) {
  const { realUser, viewAsRole } = useAuth();
  const [state, setState] = useState<WorkBadges>({ badges: {}, total: 0 });
  const previousTotal = useRef<number | null>(null);

  useEffect(() => {
    if (!realUser) return;
    let cancelled = false;

    async function poll() {
      try {
        const data = await apiClient.get<RoleDashboardResponse>("/api/admin/dashboard", { viewAs: viewAsRole ?? undefined });
        if (cancelled) return;
        const badges = computeBadges(data);
        const total = Object.values(badges).reduce((sum, n) => sum + n, 0);
        setState({ badges, total });

        // Nouveau travail arrivé pendant que l'onglet n'est pas au premier plan : alerte du navigateur.
        if (
          previousTotal.current !== null &&
          total > previousTotal.current &&
          document.hidden &&
          typeof Notification !== "undefined" &&
          Notification.permission === "granted"
        ) {
          const added = total - previousTotal.current;
          new Notification("Maplenou — du travail vous attend", {
            body: `${added} nouvelle${added > 1 ? "s" : ""} tâche${added > 1 ? "s" : ""} à traiter (${total} au total).`,
          });
        }
        previousTotal.current = total;
      } catch {
        // Silencieux : la pastille se met simplement à jour au prochain passage.
      }
    }

    void poll();
    const timer = setInterval(poll, POLL_MS);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [realUser, viewAsRole]);

  useEffect(() => {
    document.title = state.total > 0 ? `(${state.total}) Maplenou Admin` : "Maplenou Admin";
  }, [state.total]);

  return <WorkBadgesContext.Provider value={state}>{children}</WorkBadgesContext.Provider>;
}

export function useWorkBadges(): WorkBadges {
  return useContext(WorkBadgesContext);
}
