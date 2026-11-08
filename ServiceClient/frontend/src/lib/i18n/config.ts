// Language and theme preferences shared by server and client code (no framework imports here).

export const LOCALES = ["fr", "en"] as const;
export type Locale = (typeof LOCALES)[number];
export const DEFAULT_LOCALE: Locale = "fr";
export const LOCALE_NAMES: Record<Locale, string> = { fr: "Français", en: "English" };

export const THEMES = ["system", "light", "dark"] as const;
export type Theme = (typeof THEMES)[number];

export const LOCALE_COOKIE = "maplenou_locale";
export const THEME_COOKIE = "maplenou_theme";
/** One year: these are UI preferences, not session data. */
export const PREFERENCE_MAX_AGE = 60 * 60 * 24 * 365;

export const isLocale = (v: unknown): v is Locale => typeof v === "string" && (LOCALES as readonly string[]).includes(v);
export const isTheme = (v: unknown): v is Theme => typeof v === "string" && (THEMES as readonly string[]).includes(v);

/** Picks the first supported language of an Accept-Language header ("en-GB,en;q=0.9,fr;q=0.8" → "en"). */
export function negotiateLocale(acceptLanguage: string | null | undefined): Locale {
  if (!acceptLanguage) return DEFAULT_LOCALE;
  const ranked = acceptLanguage
    .split(",")
    .map((part) => {
      const [tag, ...params] = part.trim().split(";");
      const q = params.find((p) => p.trim().startsWith("q="));
      return { lang: tag.trim().toLowerCase().split("-")[0], q: q ? Number(q.split("=")[1]) || 0 : 1 };
    })
    .filter((x) => x.lang)
    .sort((a, b) => b.q - a.q);
  return ranked.find((x) => isLocale(x.lang))?.lang as Locale | undefined ?? DEFAULT_LOCALE;
}

/** Intl locale used for dates and numbers. */
export const INTL_LOCALE: Record<Locale, string> = { fr: "fr-FR", en: "en-GB" };
