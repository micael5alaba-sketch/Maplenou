"use client";

import { useId } from "react";
import { LOCALE_NAMES, LOCALES, THEMES, isLocale, isTheme, type Theme } from "@/lib/i18n/config";
import { useMessages, usePreferences } from "@/lib/i18n/client";
import { common } from "@/lib/i18n/messages/common";

const THEME_ICON: Record<Theme, string> = { system: "◐", light: "☀", dark: "☾" };

/**
 * Language and theme switch. Both are stored in cookies (no account setting needed) so the server renders
 * the right language and theme on the next request, without a flash.
 */
export function Preferences({ variant = "inline" }: { variant?: "inline" | "stacked" | "sidebar" }) {
  const t = useMessages(common);
  const { locale, theme, setLocale, setTheme } = usePreferences();
  const id = useId();
  const themeLabel: Record<Theme, string> = { system: t.themeSystem, light: t.themeLight, dark: t.themeDark };
  return (
    <div className={`prefs prefs-${variant}`} role="group" aria-label={t.preferences}>
      <label className="prefs-field" htmlFor={`${id}-lang`}>
        <span className={variant === "stacked" ? "label" : "sr-only"}>{t.language}</span>
        <span className="prefs-icon" aria-hidden="true">
          ⌘
        </span>
        <select
          id={`${id}-lang`}
          className="select select-sm"
          value={locale}
          onChange={(e) => isLocale(e.target.value) && setLocale(e.target.value)}
        >
          {LOCALES.map((l) => (
            <option key={l} value={l} lang={l}>
              {variant === "stacked" ? LOCALE_NAMES[l] : l.toUpperCase()}
            </option>
          ))}
        </select>
      </label>
      <label className="prefs-field" htmlFor={`${id}-theme`}>
        <span className={variant === "stacked" ? "label" : "sr-only"}>{t.theme}</span>
        <span className="prefs-icon" aria-hidden="true">
          {THEME_ICON[theme]}
        </span>
        <select
          id={`${id}-theme`}
          className="select select-sm"
          value={theme}
          onChange={(e) => isTheme(e.target.value) && setTheme(e.target.value)}
        >
          {THEMES.map((th) => (
            <option key={th} value={th}>
              {themeLabel[th]}
            </option>
          ))}
        </select>
      </label>
    </div>
  );
}
