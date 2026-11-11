"use client";

import { PageHeader, formatDate, formatFcfa } from "@/components/ui";
import { WithdrawalsManager } from "@/components/WithdrawalsManager";
import type { ResellerWithdrawalResponse } from "@/lib/types";

export default function ResellerWithdrawalsPage() {
  return (
    <div>
      <PageHeader
        title="Retraits partenaires"
        subtitle="Même circuit que les autres retraits : vérification, soumission, approbation du Super Admin (MFA), paiement. Les frais de retrait ne sont prélevés qu'au paiement."
      />
      <WithdrawalsManager<ResellerWithdrawalResponse>
        basePath="/api/admin/resellers/withdrawals"
        title="retrait partenaire"
        columns={["Partenaire", "Montant", "Frais / Net", "Paiement", "Demandé le"]}
        renderCells={(w) => (
          <>
            <td className="px-4 py-3 font-medium text-foreground">{w.partnerName}</td>
            <td className="px-4 py-3 font-semibold text-foreground">{formatFcfa(w.amount)}</td>
            <td className="px-4 py-3 text-muted">
              {formatFcfa(w.feeAmount)} / {formatFcfa(w.netAmount)}
            </td>
            <td className="px-4 py-3 text-muted">
              {w.method === "MIX_BY_YAS" ? "Mix by Yas" : "Moov Africa"} — {w.paymentPhone}
            </td>
            <td className="px-4 py-3 text-muted">{formatDate(w.createdAt)}</td>
          </>
        )}
      />
    </div>
  );
}
