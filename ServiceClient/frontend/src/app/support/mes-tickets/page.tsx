"use client";

import { Suspense } from "react";
import { TopBar } from "@/components/SupportShell";
import { useMessages } from "@/lib/i18n/client";
import { queue } from "@/lib/i18n/messages/queue";
import { TicketQueue } from "@/components/TicketQueue";
import { Loading } from "@/components/ui";

const MINE = { assignee: "me" };

/** B3 — Support My Tickets: personal workload. */
export default function MyQueuePage() {
  const t = useMessages(queue);
  return (
    <>
      <TopBar title={t.myTicketsTitle} />
      <div className="support-content">
        <Suspense fallback={<Loading />}>
          <TicketQueue fixed={MINE} />
        </Suspense>
      </div>
    </>
  );
}
