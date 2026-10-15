"use client";

import { createContext, useContext, useEffect, useState, type ReactNode } from "react";

type Theme = "light" | "dark" | "system";

const THEME_KEY = "maplenou_admin_theme";

const ThemeContext = createContext<{ theme: Theme; setTheme: (t: Theme) => void }>({
  theme: "system",
  setTheme: () => {},
});

function applyTheme(theme: Theme) {
  const root = document.documentElement;
  if (theme === "system") root.removeAttribute("data-theme");
  else root.setAttribute("data-theme", theme);
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>("system");

  useEffect(() => {
    let stored: Theme = "system";
    try {
      stored = (window.localStorage.getItem(THEME_KEY) as Theme) ?? "system";
    } catch {
      // stockage indisponible (navigation privée...) — on reste sur "system"
    }
    setThemeState(stored);
    applyTheme(stored);
  }, []);

  function setTheme(t: Theme) {
    setThemeState(t);
    applyTheme(t);
    try {
      window.localStorage.setItem(THEME_KEY, t);
    } catch {
      // ignore
    }
  }

  return <ThemeContext.Provider value={{ theme, setTheme }}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  return useContext(ThemeContext);
}
