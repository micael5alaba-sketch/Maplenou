"use client";

import { PageHeader, formatDate, formatFcfa } from "@/components/ui";
import { WithdrawalsManager } from "@/components/WithdrawalsManager";
import type { CourierWithdrawalResponse } from "@/lib/types";

export default function CourierWithdrawalsPage() {
  return (
    <div>
      <PageHeader
        title="Retraits livreurs"
        subtitle="Même circuit que les reversements vendeurs : réception, vérification, soumission, approbation du Super Admin (MFA), paiement."
      />
      <WithdrawalsManager<CourierWithdrawalResponse>
        basePath="/api/admin/courier-withdrawals"
        title="retrait livreur"
        columns={["Livreur", "Montant", "Coordonnées de paiement", "Demandé le"]}
        renderCells={(w) => (
          <>
            <td className="px-4 py-3 font-medium text-foreground">
              {w.agentName}
              <span className="block text-xs text-muted">{w.agentPhone}</span>
            </td>
            <td className="px-4 py-3 font-semibold text-foreground">{formatFcfa(w.amount)}</td>
            <td className="px-4 py-3 text-muted">{w.paymentDetails}</td>
            <td className="px-4 py-3 text-muted">{formatDate(w.createdAt)}</td>
          </>
        )}
      />
    </div>
  );
}
