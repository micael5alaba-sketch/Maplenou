"use client";

import { useRouter } from "next/navigation";
import { useTransition } from "react";

type Theme = "system" | "light" | "dark";
type Locale = "fr" | "en";

interface Labels { language: string; theme: string; themeSystem: string; themeLight: string; themeDark: string }

const ONE_YEAR = 60 * 60 * 24 * 365;

function setCookie(name: string, value: string) {
  document.cookie = `${name}=${value}; path=/; max-age=${ONE_YEAR}; samesite=lax`;
}

/**
 * Language and theme of this device. Both choices are kept in a small cookie (no browser storage) so the server
 * renders the right language and theme on the next visit without a flash.
 */
export function Preferences({ locale, theme, labels }: { locale: Locale; theme: Theme; labels: Labels }) {
  const router = useRouter();
  const [, startTransition] = useTransition();

  return (
    <div className="prefs">
      <label className="sr-only" htmlFor="pref-lang">{labels.language}</label>
      <select
        id="pref-lang"
        defaultValue={locale}
        onChange={(e) => {
          setCookie("maplenou_locale", e.target.value);
          document.documentElement.lang = e.target.value;
          startTransition(() => router.refresh());
        }}
      >
        <option value="fr">FR</option>
        <option value="en">EN</option>
      </select>
      <label className="sr-only" htmlFor="pref-theme">{labels.theme}</label>
      <select
        id="pref-theme"
        defaultValue={theme}
        onChange={(e) => {
          const value = e.target.value as Theme;
          setCookie("maplenou_theme", value);
          if (value === "system") delete document.documentElement.dataset.theme;
          else document.documentElement.dataset.theme = value;
        }}
      >
        <option value="system">{labels.themeSystem}</option>
        <option value="light">{labels.themeLight}</option>
        <option value="dark">{labels.themeDark}</option>
      </select>
    </div>
  );
}
