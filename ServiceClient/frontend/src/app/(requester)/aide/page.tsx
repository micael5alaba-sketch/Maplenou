"use client";

import Link from "next/link";
import { useSession } from "@/components/session";
import { useApi } from "@/lib/useApi";
import type { Category, Page, TicketSummary } from "@/lib/types";
import { Loading, StatusBadge } from "@/components/ui";
import { useLocale, useMessages } from "@/lib/i18n/client";
import { HelpSearch } from "@/components/HelpSearch";
import { format } from "@/lib/i18n/define";
import { helpHome } from "@/lib/i18n/messages/requester";

const CATEGORY_ICONS: Record<string, string> = {
  ORDER: "🛍", PAYMENT: "💳", DELIVERY: "🚚", SELLER: "🏪", ACCOUNT: "👤", RETURN_REFUND: "↩", OTHER: "…",
};

type HelpHomeMessages = (typeof helpHome)["fr"];

function introFor(role: string, t: HelpHomeMessages): { title: string; subtitle: string } {
  if (role === "SELLER") return { title: t.titleSeller, subtitle: t.subtitleSeller };
  if (role === "DELIVERY_DRIVER") return { title: t.titleDriver, subtitle: t.subtitleDriver };
  return { title: t.titleDefault, subtitle: t.subtitleDefault };
}

/** A1 — Help & Support entry point. */
export default function HelpPage() {
  const t = useMessages(helpHome);
  const session = useSession();
  const categories = useApi<Category[]>("/categories");
  const open = useApi<Page<TicketSummary>>("/tickets?open=true&size=3");
  const intro = introFor(session.role, t);
  const firstName = session.name.split(" ")[0];
  const locale = useLocale();

  return (
    <div className="stack">
      <h1>{intro.title}</h1>
      <div className="greeting">
        <p style={{ fontWeight: 700, fontSize: "1.05rem" }}>{format(t.greeting, { name: firstName })}</p>
        <p className="secondary small">{intro.subtitle}</p>
      </div>

      <div className="row">
        <Link href="/aide/nouveau" className="btn btn-primary grow">
          {session.role === "DELIVERY_DRIVER" ? t.reportIncident : t.createTicket}
        </Link>
        <Link href="/aide/tickets" className="btn btn-secondary grow">
          {t.myTickets}
        </Link>
      </div>

      <HelpSearch />

      {open.data && open.data.totalItems > 0 && (
        <section className="card stack-sm" aria-labelledby="open-title">
          <div className="row-between">
            <h2 id="open-title">{format(t.openRequests, { count: open.data.totalItems })}</h2>
            <Link href="/aide/tickets?filtre=ouverts" className="small">{t.seeAll}</Link>
          </div>
          <ul className="list-links">
            {open.data.items.map((ticket) => (
              <li key={ticket.id}>
                <Link href={`/aide/tickets/${ticket.id}`} className="list-link">
                  <span className="grow">
                    <span className="ticket-number">#{ticket.ticketNumber}</span>
                    <br />
                    {ticket.subject}
                  </span>
                  <StatusBadge status={ticket.status} kind={ticket.statusKind} />
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className="stack-sm" aria-labelledby="cat-title">
        <h2 id="cat-title" className="eyebrow">{t.categories}</h2>
        {categories.loading && <Loading />}
        <ul className="list-links">
          {categories.data?.map((c) => (
            <li key={c.code}>
              <Link href={`/aide/nouveau?categorie=${c.code}`} className="list-link">
                <span className="icon-tile" aria-hidden="true">{CATEGORY_ICONS[c.code] ?? "•"}</span>
                <span className="grow">
                  <strong>{(locale === "en" && c.nameEn) || c.name}</strong>
                  {((locale === "en" && c.descriptionEn) || c.description) && <><br /><span className="small muted">{(locale === "en" && c.descriptionEn) || c.description}</span></>}
                </span>
                <span className="chev" aria-hidden="true">›</span>
              </Link>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
