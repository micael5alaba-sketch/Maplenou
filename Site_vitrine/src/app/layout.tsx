import "@fontsource-variable/bricolage-grotesque";
import "@fontsource-variable/figtree";
import "./globals.css";
import type { Metadata, Viewport } from "next";
import type { ReactNode } from "react";
import { Footer } from "@/components/Footer";
import { Header } from "@/components/Header";
import { helpCenterFor, siteConfig } from "@/lib/config";
import { getMessages, getTheme } from "@/lib/i18n";

export async function generateMetadata(): Promise<Metadata> {
  const { locale, t } = await getMessages();
  return {
    metadataBase: new URL(siteConfig.publicUrl),
    title: { default: t.meta.title, template: "%s · Maplenou" },
    description: t.meta.description,
    openGraph: {
      type: "website",
      siteName: "Maplenou",
      title: t.meta.title,
      description: t.meta.description,
      locale: locale === "fr" ? "fr_TG" : "en_GB",
      images: ["/images/role_buyer.jpg"],
    },
  };
}

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f8f7f4" },
    { media: "(prefers-color-scheme: dark)", color: "#0b141a" },
  ],
};

export default async function RootLayout({ children }: { children: ReactNode }) {
  const { locale, t } = await getMessages();
  const theme = await getTheme();
  return (
    <html lang={locale} data-theme={theme === "system" ? undefined : theme}>
      <body>
        <Header
          t={t}
          locale={locale}
          theme={theme}
          links={{
            appUrl: siteConfig.appUrl,
            downloadUrl: siteConfig.androidUrl || siteConfig.iosUrl || siteConfig.appUrl,
            serviceClientUrl: siteConfig.serviceClientUrl,
            helpCenterUrl: helpCenterFor(locale),
          }}
        />
        <main id="main">{children}</main>
        <Footer
          t={t}
          links={{
            appUrl: siteConfig.appUrl,
            androidUrl: siteConfig.androidUrl,
            iosUrl: siteConfig.iosUrl,
            serviceClientUrl: siteConfig.serviceClientUrl,
            helpCenterUrl: helpCenterFor(locale),
            social: siteConfig.social,
          }}
        />
      </body>
    </html>
  );
}
