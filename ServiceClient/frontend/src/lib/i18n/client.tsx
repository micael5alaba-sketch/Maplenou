"use client";

import { createContext, useCallback, useContext, useMemo, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { DEFAULT_LOCALE, LOCALE_COOKIE, PREFERENCE_MAX_AGE, THEME_COOKIE, type Locale, type Theme } from "./config";
import { pick, type Messages } from "./define";
import { formattersFor, labelsFor } from "../labels";
import { describeError } from "./errors";
import { useVocabulary } from "../vocabulary";

interface I18nValue {
  locale: Locale;
  theme: Theme;
  docsBaseUrl: string;
}

// Defaults keep components usable without a provider (unit tests, error boundaries).
const I18nContext = createContext<I18nValue>({ locale: DEFAULT_LOCALE, theme: "system", docsBaseUrl: "" });

export function I18nProvider({
  locale,
  theme,
  docsBaseUrl,
  children,
}: I18nValue & { children: ReactNode }) {
  const value = useMemo(() => ({ locale, theme, docsBaseUrl }), [locale, theme, docsBaseUrl]);
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export const useLocale = (): Locale => useContext(I18nContext).locale;

/** The current language's strings of a namespace: `const t = useMessages(ticketMessages); t.title`. */
export function useMessages<T extends Record<string, string>>(messages: Messages<T>): T {
  return pick(messages, useLocale());
}

/** Status, priority, role, SLA and history labels in the current language. */
export function useLabels() {
  const locale = useLocale();
  const vocabulary = useVocabulary();
  return useMemo(() => labelsFor(locale, vocabulary), [locale, vocabulary]);
}

/** Date, money, size and duration formatters in the current language. */
export function useFormat() {
  const locale = useLocale();
  return useMemo(() => formattersFor(locale), [locale]);
}

/** Link to a documentation site in the current language: `docsUrl("user")` → http://docs/user/fr/ */
export function useDocsUrl() {
  const { docsBaseUrl, locale } = useContext(I18nContext);
  return useCallback(
    (site: "user" | "staff" | "developer", path = "") => `${docsBaseUrl}/${site}/${locale}/${path}`,
    [docsBaseUrl, locale],
  );
}

function writeCookie(name: string, value: string) {
  document.cookie = `${name}=${encodeURIComponent(value)}; Path=/; Max-Age=${PREFERENCE_MAX_AGE}; SameSite=Lax`;
}

/** Applies a theme immediately (no reload): "system" follows the operating system setting. */
export function applyTheme(theme: Theme) {
  const root = document.documentElement;
  if (theme === "system") root.removeAttribute("data-theme");
  else root.setAttribute("data-theme", theme);
}

export function usePreferences() {
  const ctx = useContext(I18nContext);
  const router = useRouter();
  const setLocale = useCallback(
    (locale: Locale) => {
      writeCookie(LOCALE_COOKIE, locale);
      document.documentElement.lang = locale;
      // notifications are written in each person's language (DEC-CS11-09): tell the server, when signed in.
      // Plain fetch, errors ignored: on the sign-in page there is no session yet.
      void fetch("/api/backend/api/v1/support/me/preferences", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ language: locale }),
      }).catch(() => undefined);
      router.refresh(); // server components and metadata re-render in the new language
    },
    [router],
  );
  const setTheme = useCallback(
    (theme: Theme) => {
      writeCookie(THEME_COOKIE, theme);
      applyTheme(theme);
      router.refresh();
    },
    [router],
  );
  return { locale: ctx.locale, theme: ctx.theme, setLocale, setTheme };
}

/** Error text in the current language: `const errorText = useErrorText(); toast(errorText(e), true)`. */
export function useErrorText() {
  const locale = useLocale();
  return useCallback((error: unknown) => describeError(error, locale), [locale]);
}
