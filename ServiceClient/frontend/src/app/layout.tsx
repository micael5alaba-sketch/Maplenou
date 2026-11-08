import type { Metadata, Viewport } from "next";
import { ToastProvider } from "@/components/ui";
import { I18nProvider } from "@/lib/i18n/client";
import { defineMessages } from "@/lib/i18n/define";
import { DOCS_BASE_URL, getLocale, getMessages, getTheme } from "@/lib/i18n/server";
import "./globals.css";

const meta = defineMessages({
  fr: {
    title: "Maplenou — Service client",
    template: "%s · Maplenou Service client",
    description: "Créez et suivez vos demandes d'assistance Maplenou.",
  },
  en: {
    title: "Maplenou — Customer service",
    template: "%s · Maplenou Customer service",
    description: "Create and follow your Maplenou support requests.",
  },
});

export async function generateMetadata(): Promise<Metadata> {
  const t = await getMessages(meta);
  return { title: { default: t.title, template: t.template }, description: t.description };
}

export const viewport: Viewport = { width: "device-width", initialScale: 1 };

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const [locale, theme] = await Promise.all([getLocale(), getTheme()]);
  return (
    // data-theme absent = follow the operating system (prefers-color-scheme)
    <html lang={locale} data-theme={theme === "system" ? undefined : theme} suppressHydrationWarning>
      <body>
        <I18nProvider locale={locale} theme={theme} docsBaseUrl={DOCS_BASE_URL}>
          <ToastProvider>{children}</ToastProvider>
        </I18nProvider>
      </body>
    </html>
  );
}
