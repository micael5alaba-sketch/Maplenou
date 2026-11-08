import type { Role, Vocabulary } from "./types";
import { DEFAULT_LOCALE, INTL_LOCALE, type Locale } from "./i18n/config";
import { historyLabels, priorityLabels, roleLabels, slaLabels, statusLabels } from "./i18n/messages/common";

// UI vocabulary. French is identical to the CS-02 design system and the mockups; English is its translation.
// Components use the hooks `useLabels()` / `useFormat()` (lib/i18n/client.tsx), which call these functions.

/**
 * Built-in labels, completed (and overridden) by the administrators' vocabulary when it is loaded, so a custom
 * status or priority shows its own FR/EN label. Unknown codes fall back to the code itself.
 */
export function labelsFor(locale: Locale = DEFAULT_LOCALE, vocabulary?: Vocabulary) {
  const status: Record<string, string> = { ...statusLabels[locale] };
  const priority: Record<string, string> = { ...priorityLabels[locale] };
  for (const s of vocabulary?.statuses ?? []) status[s.code] = (locale === "en" && s.labelEn) || s.labelFr;
  for (const p of vocabulary?.priorities ?? []) priority[p.code] = (locale === "en" && p.labelEn) || p.labelFr;
  const named = (map: Record<string, string>) => new Proxy(map, { get: (t, k: string) => t[k] ?? k });
  return {
    status: named(status),
    priority: named(priority),
    role: roleLabels[locale],
    sla: slaLabels[locale],
    history: historyLabels[locale] as Record<string, string>,
    /** History entries whose value names a status or a priority are translated too. */
    historyValue: (action: string, value: string | null) => historyValueLabel(action, value, locale, status, priority),
  };
}

export function historyValueLabel(action: string, value: string | null, locale: Locale = DEFAULT_LOCALE,
  status: Record<string, string> = statusLabels[locale], priority: Record<string, string> = priorityLabels[locale]): string | null {
  if (!value) return null;
  if (action === "STATUS_CHANGED" || action === "CREATED" || action === "REOPEN_APPROVED" || action === "AUTO_CLOSED") {
    return status[value] ?? value;
  }
  if (action === "PRIORITY_CHANGED") return priority[value] ?? value;
  return value;
}

export const isSupportRole = (role: Role | undefined | null): boolean =>
  role === "SUPPORT_AGENT" || role === "SUPPORT_MANAGER" || role === "ADMINISTRATOR";

export const isSupervisor = (role: Role | undefined | null): boolean =>
  role === "SUPPORT_MANAGER" || role === "ADMINISTRATOR";

/** Built-in open statuses; screens use `openStatusCodes(useVocabulary())` (custom statuses included). */
export const OPEN_STATUSES: string[] = ["NEW", "ASSIGNED", "IN_PROGRESS", "WAITING_FOR_CUSTOMER"];

// ---------------------------------------------------------------- formatting

const TIME_ZONE = "Africa/Lome";
const dateFormats = new Map<Locale, Intl.DateTimeFormat>();
const dateTimeFormats = new Map<Locale, Intl.DateTimeFormat>();

function dateFormat(locale: Locale) {
  let f = dateFormats.get(locale);
  if (!f) {
    f = new Intl.DateTimeFormat(INTL_LOCALE[locale], { day: "2-digit", month: "short", year: "numeric", timeZone: TIME_ZONE });
    dateFormats.set(locale, f);
  }
  return f;
}

function dateTimeFormat(locale: Locale) {
  let f = dateTimeFormats.get(locale);
  if (!f) {
    f = new Intl.DateTimeFormat(INTL_LOCALE[locale], {
      day: "2-digit",
      month: "short",
      hour: "2-digit",
      minute: "2-digit",
      timeZone: TIME_ZONE,
    });
    dateTimeFormats.set(locale, f);
  }
  return f;
}

export const formatDate = (iso: string | null | undefined, locale: Locale = DEFAULT_LOCALE) =>
  iso ? dateFormat(locale).format(new Date(iso)) : "—";

export const formatDateTime = (iso: string | null | undefined, locale: Locale = DEFAULT_LOCALE) =>
  iso ? dateTimeFormat(locale).format(new Date(iso)) : "—";

export function formatMoney(amount: number | null | undefined, currency = "XOF", locale: Locale = DEFAULT_LOCALE): string {
  if (amount == null) return "—";
  const n = new Intl.NumberFormat(INTL_LOCALE[locale], { maximumFractionDigits: 0 }).format(amount);
  return currency === "XOF" ? `${n} FCFA` : `${n} ${currency}`;
}

export function formatBytes(bytes: number, locale: Locale = DEFAULT_LOCALE): string {
  const [b, kb, mb] = locale === "fr" ? ["o", "Ko", "Mo"] : ["B", "KB", "MB"];
  if (bytes < 1024) return `${bytes} ${b}`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} ${kb}`;
  const v = (bytes / (1024 * 1024)).toFixed(1);
  return `${locale === "fr" ? v.replace(".", ",") : v} ${mb}`;
}

/** Business minutes → "1 h 25" / "2 j 3 h" ("2 d 3 h" in English). One business day = 10 h. */
/**
 * SLA durations. `minutesPerDay` is the length of an SLA day: 1440 with the 24/7 clock (default, SLA_MODE=always),
 * 600 with business hours 08:00–18:00 (SLA_MODE=business-hours) — `GET /admin/sla-settings` gives it.
 */
export function formatBusinessMinutes(minutes: number | null | undefined, locale: Locale = DEFAULT_LOCALE,
  minutesPerDay = 1440): string {
  if (minutes == null) return "—";
  const d = locale === "fr" ? "j" : "d";
  const m = Math.round(minutes);
  if (m < 60) return `${m} min`;
  const days = Math.floor(m / minutesPerDay);
  const hours = Math.floor((m % minutesPerDay) / 60);
  const rest = m % 60;
  if (days > 0) return hours > 0 ? `${days} ${d} ${hours} h` : `${days} ${d}`;
  return rest > 0 ? `${hours} h ${String(rest).padStart(2, "0")}` : `${hours} h`;
}

/** "il y a 5 min" / "5 min ago". */
export function timeAgo(iso: string, now: Date = new Date(), locale: Locale = DEFAULT_LOCALE): string {
  const diff = Math.max(0, now.getTime() - new Date(iso).getTime());
  const min = Math.floor(diff / 60000);
  const fr = locale === "fr";
  if (min < 1) return fr ? "à l'instant" : "just now";
  if (min < 60) return fr ? `il y a ${min} min` : `${min} min ago`;
  const h = Math.floor(min / 60);
  if (h < 24) return fr ? `il y a ${h} h` : `${h} h ago`;
  const d = Math.floor(h / 24);
  return fr ? `il y a ${d} j` : `${d} d ago`;
}

/** All formatters bound to one language (see `useFormat()`). */
export function formattersFor(locale: Locale = DEFAULT_LOCALE) {
  return {
    locale,
    date: (iso: string | null | undefined) => formatDate(iso, locale),
    dateTime: (iso: string | null | undefined) => formatDateTime(iso, locale),
    money: (amount: number | null | undefined, currency = "XOF") => formatMoney(amount, currency, locale),
    bytes: (bytes: number) => formatBytes(bytes, locale),
    businessMinutes: (minutes: number | null | undefined, minutesPerDay?: number) =>
      formatBusinessMinutes(minutes, locale, minutesPerDay),
    timeAgo: (iso: string, now?: Date) => timeAgo(iso, now ?? new Date(), locale),
    number: (n: number) => new Intl.NumberFormat(INTL_LOCALE[locale]).format(n),
  };
}
