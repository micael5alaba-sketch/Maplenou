"use client";

import { Suspense } from "react";
import { TopBar } from "@/components/SupportShell";
import { useMessages } from "@/lib/i18n/client";
import { isSupervisor } from "@/lib/labels";
import { useSession } from "@/components/session";
import { queue } from "@/lib/i18n/messages/queue";
import { TicketQueue } from "@/components/TicketQueue";
import { Loading } from "@/components/ui";

/** B2 — Ticket queue. */
export default function QueuePage() {
  const t = useMessages(queue);
  const session = useSession();
  return (
    <>
      <TopBar title={t.queueTitle} />
      <div className="support-content">
        <Suspense fallback={<Loading />}>
          <TicketQueue exportable={isSupervisor(session.role)} />
        </Suspense>
      </div>
    </>
  );
}
