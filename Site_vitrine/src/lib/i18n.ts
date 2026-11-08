import "server-only";
import { cookies, headers } from "next/headers";
import { MESSAGES, type Locale, type Messages } from "./messages";

export const LOCALE_COOKIE = "maplenou_locale";
export const THEME_COOKIE = "maplenou_theme";
export type Theme = "system" | "light" | "dark";

/** Language: the visitor's choice (cookie), otherwise the browser's language, otherwise French. */
export async function getLocale(): Promise<Locale> {
  const chosen = (await cookies()).get(LOCALE_COOKIE)?.value;
  if (chosen === "fr" || chosen === "en") return chosen;
  const accept = (await headers()).get("accept-language") ?? "";
  return /^\s*en\b/i.test(accept) ? "en" : "fr";
}

export async function getTheme(): Promise<Theme> {
  const t = (await cookies()).get(THEME_COOKIE)?.value;
  return t === "light" || t === "dark" ? t : "system";
}

export async function getMessages(): Promise<{ locale: Locale; t: Messages }> {
  const locale = await getLocale();
  return { locale, t: MESSAGES[locale] };
}
