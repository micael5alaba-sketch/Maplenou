import "server-only";
import { cookies, headers } from "next/headers";
import { isLocale, isTheme, LOCALE_COOKIE, negotiateLocale, THEME_COOKIE, type Locale, type Theme } from "./config";
import { pick, type Messages } from "./define";

/** Cookie first (explicit choice), then the browser's Accept-Language, then French. */
export async function getLocale(): Promise<Locale> {
  const fromCookie = (await cookies()).get(LOCALE_COOKIE)?.value;
  if (isLocale(fromCookie)) return fromCookie;
  return negotiateLocale((await headers()).get("accept-language"));
}

export async function getTheme(): Promise<Theme> {
  const v = (await cookies()).get(THEME_COOKIE)?.value;
  return isTheme(v) ? v : "system";
}

export async function getMessages<T extends Record<string, string>>(messages: Messages<T>): Promise<T> {
  return pick(messages, await getLocale());
}

/** Base URL of the documentation sites (docs profile of docker-compose). */
export const DOCS_BASE_URL = (process.env.DOCS_BASE_URL ?? "http://localhost:8006").replace(/\/$/, "");
