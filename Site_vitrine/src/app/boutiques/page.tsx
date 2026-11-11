/* eslint-disable @next/next/no-img-element -- shop logos come from the Maplenou API (Cloudinary), already sized */
import type { Metadata } from "next";
import Link from "next/link";
import { citiesOf, getShops } from "@/lib/catalog";
import { siteConfig } from "@/lib/config";
import { getMessages } from "@/lib/i18n";
import { format } from "@/lib/messages";
import { Visual } from "@/components/Visual";

export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  const { t } = await getMessages();
  return { title: t.meta.shopsTitle, alternates: { canonical: "/boutiques" } };
}

function absolute(url: string | null): string | null {
  if (!url) return null;
  return url.startsWith("/") ? `${siteConfig.apiUrl}${url}` : url;
}

/** Shop finder: the approved shops from Bakend, filtered by city with a plain GET form (works without JavaScript). */
export default async function ShopsPage({ searchParams }: { searchParams: Promise<{ ville?: string }> }) {
  const { locale, t } = await getMessages();
  const s = t.shops;
  const { ville } = await searchParams;
  const shops = await getShops();
  const cities = shops ? citiesOf(shops, locale) : [];
  const city = ville && cities.includes(ville) ? ville : "";
  const list = (shops ?? []).filter((shop) => !city || shop.city?.trim() === city);

  return (
    <>
      <section className="page-hero" aria-labelledby="shops-page-title">
        <div className="wrap page-hero-split">
          <div className="page-hero-copy">
            <p className="eyebrow">{t.mega.shops}</p>
            <h1 id="shops-page-title">{s.title}</h1>
            <p className="lead">{s.lead}</p>
          </div>
          <div className="page-hero-media">
            <Visual name="ecrans-application" alt={t.visuals.ecrans} priority sizes="(max-width: 980px) calc(100vw - 32px), 520px" />
          </div>
        </div>
      </section>

      <section className="band" aria-label={s.title}>
        <div className="wrap stack">
          {shops === null ? (
            <p className="empty-note" role="status">{s.unavailable}</p>
          ) : (
            <>
              <form method="get" action="/boutiques" className="filter-form">
                <label htmlFor="ville">{s.filterLabel}</label>
                <select id="ville" name="ville" defaultValue={city}>
                  <option value="">{s.allCities}</option>
                  {cities.map((c) => <option key={c} value={c}>{c}</option>)}
                </select>
                <button type="submit" className="btn btn-primary btn-sm">{s.filter}</button>
                <span className="small muted" role="status">{format(s.count, { n: list.length })}</span>
              </form>
              {list.length > 0 ? (
                <ul className="shops shops-full">
                  {list.map((shop) => {
                    const logo = absolute(shop.logoUrl);
                    const place = [shop.district, shop.city].filter(Boolean).join(", ");
                    return (
                      <li className="shop" key={shop.id}>
                        <span className="shop-logo">
                          {logo ? <img src={logo} alt="" width={56} height={56} loading="lazy" /> : <span aria-hidden="true">{shop.name.charAt(0)}</span>}
                        </span>
                        <span className="shop-body">
                          <strong>{shop.name}</strong>
                          {place && <span className="small muted">{place}</span>}
                          {shop.description && <span className="small shop-desc">{shop.description}</span>}
                        </span>
                      </li>
                    );
                  })}
                </ul>
              ) : <p className="empty-note">{s.none}</p>}
              {siteConfig.appUrl && <div><a className="btn btn-accent" href={siteConfig.appUrl}>{s.cta}</a></div>}
            </>
          )}
        </div>
      </section>

      <section className="band band-tight" aria-labelledby="shops-sell-title">
        <div className="wrap">
          <div className="banner">
            <div className="stack">
              <h2 id="shops-sell-title">{s.sellTitle}</h2>
              <p>{s.sellText}</p>
            </div>
            <Link className="btn btn-primary" href="/vendre">{t.home.bannerCta}</Link>
          </div>
        </div>
      </section>
    </>
  );
}
