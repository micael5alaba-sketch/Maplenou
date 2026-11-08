"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { API_BASE, toQuery } from "@/lib/api";
import { useFormat, useLabels, useLocale, useMessages } from "@/lib/i18n/client";
import { priorityCodes, statusCodes, useVocabulary } from "@/lib/vocabulary";
import { common } from "@/lib/i18n/messages/common";
import { queue } from "@/lib/i18n/messages/queue";
import { useApi } from "@/lib/useApi";
import type { Agent, Category, Page, TicketSummary } from "@/lib/types";
import { EmptyState, ErrorState, Loading, Pagination, PriorityBadge, SlaBadge, StatusBadge } from "./ui";

const FILTER_KEYS = ["q", "status", "priority", "category", "assignee", "requesterRole", "slaBreached", "escalated", "open", "sort"] as const;
const PAGE_SIZE = 20;

/**
 * B2/B3 — Support queue with search and filters (FR-014). Filters live in the URL so a filtered view can be
 * bookmarked or shared; `fixed` filters (e.g. assignee=me) are imposed by the page.
 */
export function TicketQueue({ fixed = {}, exportable = false }: { fixed?: Record<string, string>; exportable?: boolean }) {
  const t = useMessages(queue);
  const locale = useLocale();
  const vocabulary = useVocabulary();
  const c = useMessages(common);
  const labels = useLabels();
  const f = useFormat();
  const router = useRouter();
  const params = useSearchParams();
  const categories = useApi<Category[]>("/categories");
  const agents = useApi<Agent[]>("/agents");
  const [page, setPage] = useState(0);

  const filters = useMemo(() => {
    const active: Record<string, string> = {};
    for (const k of FILTER_KEYS) {
      const v = params.get(k);
      if (v) active[k] = v;
    }
    return { ...active, ...fixed };
  }, [params, fixed]);
  const [q, setQ] = useState(filters.q ?? "");

  // eslint-disable-next-line react-hooks/set-state-in-effect -- reset paging whenever the filters change
  useEffect(() => setPage(0), [params]);

  const query = toQuery({ ...filters, sort: filters.sort ?? "createdAt,desc", page, size: PAGE_SIZE });
  const list = useApi<Page<TicketSummary>>(`/tickets${query}`);

  function setFilter(key: string, value: string) {
    const next = new URLSearchParams(params.toString());
    if (value) next.set(key, value);
    else next.delete(key);
    router.replace(`?${next.toString()}`, { scroll: false });
  }

  const statuses = statusCodes(vocabulary);
  const priorities = priorityCodes(vocabulary);
  // the CSV follows the filters on screen; the BFF adds the session token (managers and administrators only)
  const csvHref = `${API_BASE}/tickets/export.csv${toQuery({ ...filters, sort: undefined, lang: locale })}`;
  const activeCount = FILTER_KEYS.filter((k) => k !== "sort" && params.get(k)).length;

  return (
    <div className="stack">
      <form
        className="filters"
        role="search"
        onSubmit={(e) => {
          e.preventDefault();
          setFilter("q", q.trim());
        }}
      >
        <label className="sr-only" htmlFor="q">{t.search}</label>
        <input id="q" className="input" type="search" placeholder={t.searchPlaceholder} value={q} onChange={(e) => setQ(e.target.value)} />
        <button type="submit" className="btn btn-secondary btn-sm">{t.search}</button>

        <label className="sr-only" htmlFor="f-category">{t.category}</label>
        <select id="f-category" className="select" value={filters.category ?? ""} onChange={(e) => setFilter("category", e.target.value)}>
          <option value="">{t.category}</option>
          {categories.data?.map((c) => <option key={c.code} value={c.code}>{(locale === "en" && c.nameEn) || c.name}</option>)}
        </select>

        <label className="sr-only" htmlFor="f-status">{t.status}</label>
        <select id="f-status" className="select" value={filters.status ?? (filters.open ? "__open" : "")}
                onChange={(e) => {
                  const v = e.target.value;
                  const next = new URLSearchParams(params.toString());
                  next.delete("status");
                  next.delete("open");
                  if (v === "__open") next.set("open", "true");
                  else if (v) next.set("status", v);
                  router.replace(`?${next.toString()}`, { scroll: false });
                }}>
          <option value="">{t.status}</option>
          <option value="__open">{t.allOpen}</option>
          {statuses.map((s) => <option key={s} value={s}>{labels.status[s]}</option>)}
        </select>

        <label className="sr-only" htmlFor="f-priority">{t.priority}</label>
        <select id="f-priority" className="select" value={filters.priority ?? ""} onChange={(e) => setFilter("priority", e.target.value)}>
          <option value="">{t.priority}</option>
          {priorities.map((p) => <option key={p} value={p}>{labels.priority[p]}</option>)}
        </select>

        {!fixed.assignee && (
          <>
            <label className="sr-only" htmlFor="f-assignee">{t.assignedTo}</label>
            <select id="f-assignee" className="select" value={filters.assignee ?? ""} onChange={(e) => setFilter("assignee", e.target.value)}>
              <option value="">{t.assignedTo}</option>
              <option value="me">{t.me}</option>
              <option value="unassigned">{t.unassigned}</option>
              {agents.data?.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
            </select>
          </>
        )}

        <label className="sr-only" htmlFor="f-role">{t.requesterRole}</label>
        <select id="f-role" className="select" value={filters.requesterRole ?? ""} onChange={(e) => setFilter("requesterRole", e.target.value)}>
          <option value="">{t.requesterRole}</option>
          {(["CUSTOMER", "SELLER", "DELIVERY_DRIVER"] as const).map((r) => <option key={r} value={r}>{labels.role[r]}</option>)}
        </select>

        <label className="checkbox">
          <input type="checkbox" checked={filters.slaBreached === "true"} onChange={(e) => setFilter("slaBreached", e.target.checked ? "true" : "")} />
          {t.slaBreached}
        </label>
        <label className="checkbox">
          <input type="checkbox" checked={filters.escalated === "true"} onChange={(e) => setFilter("escalated", e.target.checked ? "true" : "")} />
          {t.escalatedFilter}
        </label>

        <label className="sr-only" htmlFor="f-sort">{t.sort}</label>
        <select id="f-sort" className="select" value={filters.sort ?? "createdAt,desc"} onChange={(e) => setFilter("sort", e.target.value)}>
          <option value="createdAt,desc">{t.sortNewest}</option>
          <option value="createdAt,asc">{t.sortOldest}</option>
          <option value="priorityRank,desc">{t.sortPriority}</option>
          <option value="updatedAt,desc">{t.sortActivity}</option>
        </select>

        {activeCount > 0 && (
          <button type="button" className="btn btn-tertiary btn-sm" onClick={() => { setQ(""); router.replace("?", { scroll: false }); }}>
            {t.reset}
          </button>
        )}
        {exportable && (
          <a className="btn btn-secondary btn-sm" href={csvHref} download>
            {t.exportCsv}
          </a>
        )}
      </form>

      {list.error && <ErrorState error={list.error} onRetry={list.reload} />}
      <div className="table-wrap" aria-busy={list.loading}>
        <table className="data">
          <caption className="sr-only">{t.caption}</caption>
          <thead>
            <tr>
              <th scope="col">{t.colNumber}</th>
              <th scope="col">{t.colSubject}</th>
              <th scope="col">{t.colRequester}</th>
              <th scope="col">{t.colCategory}</th>
              <th scope="col">{t.colStatus}</th>
              <th scope="col">{t.colPriority}</th>
              <th scope="col">{t.colSla}</th>
              <th scope="col">{t.colAssignee}</th>
              <th scope="col">{t.colCreated}</th>
            </tr>
          </thead>
          <tbody>
            {list.data?.items.map((ticket) => (
              <tr key={ticket.id}>
                <td><Link href={`/support/tickets/${ticket.id}`} className="row-link ticket-number">{ticket.ticketNumber}</Link></td>
                <td>
                  <Link href={`/support/tickets/${ticket.id}`} className="row-link">{ticket.subject}</Link>
                  {ticket.escalated && <span className="badge tone-danger" style={{ marginLeft: 6 }}>{t.escalated}</span>}
                </td>
                <td>
                  {ticket.requester.name}
                  <div className="xsmall muted">{ticket.requester.role ? labels.role[ticket.requester.role] : ""}</div>
                </td>
                <td>{(locale === "en" && ticket.category.nameEn) || ticket.category.name}</td>
                <td><StatusBadge status={ticket.status} kind={ticket.statusKind} /></td>
                <td><PriorityBadge priority={ticket.priority} /></td>
                <td>{ticket.slaState && <SlaBadge state={ticket.slaState} />}</td>
                <td>{ticket.assignedAgent?.name ?? <span className="muted">{c.none}</span>}</td>
                <td className="mono xsmall">{f.dateTime(ticket.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {list.loading && !list.data && <div style={{ padding: 16 }}><Loading /></div>}
        {list.data && list.data.items.length === 0 && (
          <EmptyState icon="🔍" title={activeCount ? t.emptyFiltered : t.empty}>
            {activeCount ? t.emptyFilteredHint : null}
          </EmptyState>
        )}
        {list.data && (
          <Pagination page={list.data.page} totalPages={list.data.totalPages} totalItems={list.data.totalItems}
                      size={list.data.size} onPage={setPage} />
        )}
      </div>
    </div>
  );
}
