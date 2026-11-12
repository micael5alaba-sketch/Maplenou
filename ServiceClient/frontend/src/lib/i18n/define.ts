import type { Locale } from "./config";

/**
 * A namespace of UI strings in every supported language. The English dictionary must have exactly the
 * French keys (type-checked), so a missing translation is a compile error rather than a blank label.
 *
 *   export const m = defineMessages({ fr: { hello: "Bonjour {name}" }, en: { hello: "Hello {name}" } });
 *   const t = useMessages(m);  format(t.hello, { name })
 */
export type Messages<T extends Record<string, string>> = { fr: T; en: { [K in keyof T]: string } };

export function defineMessages<T extends Record<string, string>>(messages: Messages<T>): Messages<T> {
  return messages;
}

export function pick<T extends Record<string, string>>(messages: Messages<T>, locale: Locale): T {
  return messages[locale] as T;
}

/** Replaces {placeholders}. Unknown placeholders are left as is so they are visible in review. */
export function format(template: string, vars: Record<string, string | number | null | undefined> = {}): string {
  return template.replace(/\{(\w+)\}/g, (whole, key: string) => {
    const v = vars[key];
    return v === undefined || v === null ? whole : String(v);
  });
}

/**
 * "{count} ticket" / "{count} tickets". French uses the singular for 0 and 1, English only for 1.
 */
export function plural(
  locale: Locale,
  count: number,
  one: string,
  other: string,
  vars: Record<string, string | number> = {},
): string {
  const singular = locale === "fr" ? Math.abs(count) < 2 : count === 1;
  return format(singular ? one : other, { count, ...vars });
}
