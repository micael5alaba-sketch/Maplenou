import Link from "next/link";
import { format, type Messages } from "@/lib/messages";
import { Logo } from "./Logo";

export type SocialLinks = Record<"facebook" | "instagram" | "tiktok" | "whatsapp" | "x" | "youtube", string>;
interface Links {
  appUrl: string; androidUrl: string; iosUrl: string; serviceClientUrl: string; helpCenterUrl: string;
  social: SocialLinks;
}

/** Social networks that have a link in .env, in a fixed order. */
export function socialList(t: Messages, social: SocialLinks): { key: string; href: string; label: string }[] {
  return (["facebook", "instagram", "tiktok", "whatsapp", "x", "youtube"] as const)
    .filter((k) => social[k])
    .map((k) => ({ key: k, href: social[k], label: t.social[k] }));
}

/** Footer in columns: brand and app, buy, sell and deliver, help; then the social networks and the copyright. */
export function Footer({ t, links }: { t: Messages; links: Links }) {
  const year = new Date().getFullYear();
  const socials = socialList(t, links.social);
  return (
    <footer className="site-footer">
      <div className="wrap">
        <div className="stack brand-col">
          <span className="logo"><Logo /></span>
          <p className="muted">{t.footer.tagline}</p>
          {(links.androidUrl || links.iosUrl) && (
            <div className="row">
              <span className="small muted">{t.footer.getApp}</span>
              {links.androidUrl && <a className="btn btn-ghost btn-sm" href={links.androidUrl}>{t.footer.android}</a>}
              {links.iosUrl && <a className="btn btn-ghost btn-sm" href={links.iosUrl}>{t.footer.ios}</a>}
            </div>
          )}
        </div>
        <nav aria-labelledby="footer-buy">
          <h2 id="footer-buy">{t.footer.buy}</h2>
          <Link href="/#steps">{t.mega.buyHow}</Link>
          <Link href="/#rayons">{t.mega.buyCats}</Link>
          <Link href="/boutiques">{t.topbar.shops}</Link>
          {links.appUrl && <a href={links.appUrl}>{t.nav.openApp}</a>}
        </nav>
        <nav aria-labelledby="footer-sell">
          <h2 id="footer-sell">{t.footer.sellDeliver}</h2>
          <Link href="/vendre">{t.mega.sellOpen}</Link>
          <Link href="/vendre#sell-pricing">{t.mega.sellFees}</Link>
          <Link href="/livrer">{t.meta.deliverTitle}</Link>
        </nav>
        <nav aria-labelledby="footer-help">
          <h2 id="footer-help">{t.footer.help}</h2>
          <Link href="/service-client">{t.nav.support}</Link>
          <a href={links.serviceClientUrl}>{t.support.openTicket}</a>
          <a href={links.helpCenterUrl}>{t.support.helpCenter}</a>
          <Link href="/#faq">{t.mega.supportFaq}</Link>
        </nav>
        {socials.length > 0 && (
          <nav aria-labelledby="footer-follow" className="footer-social">
            <h2 id="footer-follow">{t.footer.follow}</h2>
            <ul>{socials.map((s) => <li key={s.key}><a href={s.href} rel="noopener noreferrer" target="_blank">{s.label}</a></li>)}</ul>
          </nav>
        )}
        <p className="copyright">{format(t.footer.rights, { year })}</p>
      </div>
    </footer>
  );
}
