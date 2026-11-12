import Link from "next/link";
import type { Locale, Messages } from "@/lib/messages";
import type { Theme } from "@/lib/i18n";
import { Logo } from "./Logo";
import { MegaMenu, type MegaSection } from "./MegaMenu";
import { Preferences } from "./Preferences";

interface Links { appUrl: string; downloadUrl: string; serviceClientUrl: string; helpCenterUrl: string }

/** Utility bar (help, shops, app, language and theme) above the main bar (logo, menu by audience, app button). */
export function Header({ t, locale, theme, links }: { t: Messages; locale: Locale; theme: Theme; links: Links }) {
  const m = t.mega;
  const sections: MegaSection[] = [
    { id: "buy", label: m.buy, intro: m.buyIntro, links: [
      { href: "/#steps", label: m.buyHow, text: m.buyHowText },
      { href: "/#rayons", label: m.buyCats, text: m.buyCatsText },
      { href: "/#produits", label: m.buyProducts, text: m.buyProductsText },
      { href: "/#livraison", label: m.buyDelivery, text: m.buyDeliveryText },
    ] },
    { id: "sell", label: m.sell, intro: m.sellIntro, allHref: "/vendre", allLabel: m.allOf.replace("{section}", m.sell), links: [
      { href: "/vendre#sell-steps", label: m.sellOpen, text: m.sellOpenText },
      { href: "/vendre#sell-pricing", label: m.sellFees, text: m.sellFeesText },
      { href: "/vendre#calc", label: m.sellCalc, text: m.sellCalcText },
    ] },
    { id: "deliver", label: m.deliver, intro: m.deliverIntro, allHref: "/livrer", allLabel: m.allOf.replace("{section}", m.deliver), links: [
      { href: "/livrer#deliver-docs", label: m.deliverDocs, text: m.deliverDocsText },
      { href: "/livrer#deliver-how", label: m.deliverHow, text: m.deliverHowText },
    ] },
    { id: "support", label: m.support, intro: m.supportIntro, allHref: "/service-client", allLabel: m.allOf.replace("{section}", m.support), links: [
      { href: links.serviceClientUrl, label: m.supportTicket, text: m.supportTicketText, external: true },
      { href: links.helpCenterUrl, label: m.supportHelp, text: m.supportHelpText, external: true },
      { href: "/service-client#delays", label: m.supportDelays, text: m.supportDelaysText },
      { href: "/#faq", label: m.supportFaq, text: m.supportFaqText },
    ] },
  ];
  return (
    <header className="site-header">
      <a className="skip" href="#main">{t.nav.skip}</a>
      <div className="topbar">
        <div className="wrap">
          <nav aria-label={t.topbar.label} className="topbar-links">
            <a href={links.helpCenterUrl}>{t.topbar.help}</a>
            <Link href="/boutiques">{t.topbar.shops}</Link>
            {links.downloadUrl && <a href={links.downloadUrl}>{t.topbar.getApp}</a>}
          </nav>
          <Preferences
            locale={locale}
            theme={theme}
            labels={{
              language: t.nav.language, theme: t.nav.theme, themeSystem: t.nav.themeSystem,
              themeLight: t.nav.themeLight, themeDark: t.nav.themeDark,
            }}
          />
        </div>
      </div>
      <div className="mainbar">
        <div className="wrap">
          <Link href="/" className="logo"><Logo /></Link>
          <MegaMenu
            sections={sections}
            links={[{ href: "/boutiques", label: m.shops }]}
            label={m.label}
            menuLabel={t.nav.menu}
            cta={links.appUrl ? { href: links.appUrl, label: t.nav.openApp } : undefined}
          />
          {links.appUrl && <a href={links.appUrl} className="btn btn-primary btn-sm header-cta">{t.nav.openApp}</a>}
        </div>
      </div>
    </header>
  );
}
