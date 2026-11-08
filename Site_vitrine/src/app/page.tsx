/* eslint-disable @next/next/no-img-element -- catalogue images come from the Maplenou API (Cloudinary), already sized */
import Link from "next/link";
import { subscribeToNewsletter } from "@/app/actions";
import { socialList } from "@/components/Footer";
import { Visual } from "@/components/Visual";
import { getCategories, getDeliveryTiers, getProducts, getShops, type Product } from "@/lib/catalog";
import { fcfa } from "@/lib/commission";
import { helpCenterFor, siteConfig } from "@/lib/config";
import { getMessages } from "@/lib/i18n";
import { format } from "@/lib/messages";

/** Rendered on each request (language and theme come from cookies); API data is cached 5 minutes in catalog.ts. */
export const dynamic = "force-dynamic";

function absolute(url: string | null): string | null {
  if (!url) return null;
  return url.startsWith("/") ? `${siteConfig.apiUrl}${url}` : url;
}

/** Price the buyer pays: Bakend's displayPrice (commission included), else the base price of an older Bakend. */
function shown(p: Product): { price: number; old: number | null } {
  const withCommission = p.displayPrice != null;
  const price = Number(withCommission ? p.displayPrice : p.basePrice);
  const rawOld = withCommission ? p.displayOldPrice : p.oldPrice;
  const old = rawOld != null && Number(rawOld) > price ? Number(rawOld) : null;
  return { price, old };
}

const ICONS = {
  buy: "M6 7h12l-1 13H7L6 7z M9 7a3 3 0 0 1 6 0",
  sell: "M4 10h16v10H4z M3 10l2-5h14l2 5 M10 20v-5h4v5",
  deliver: "M3 7h11v9H3z M14 10h4l3 3v3h-7 M7 19a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z M17 19a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z",
  help: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6v.6 M12 17h.01",
  ticket: "M4 7h16v4a2 2 0 0 0 0 4v4H4v-4a2 2 0 0 0 0-4V7z M13 7v12",
  book: "M4 19.5A2.5 2.5 0 0 1 6.5 17H20V3H6.5A2.5 2.5 0 0 0 4 5.5v14z",
  faq: "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z",
  pin: "M12 21s-7-6.2-7-11a7 7 0 0 1 14 0c0 4.8-7 11-7 11z M12 12.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z",
};

function Icon({ d }: { d: string }) {
  return (
    <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"
         strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={d} /></svg>
  );
}

export default async function HomePage({ searchParams }: { searchParams: Promise<{ newsletter?: string }> }) {
  const { locale, t } = await getMessages();
  const { newsletter } = await searchParams;
  const [categories, products, tiers, shops] = await Promise.all([getCategories(), getProducts(), getDeliveryTiers(), getShops()]);
  const h = t.home;
  const appUrl = siteConfig.appUrl;
  const helpCenterUrl = helpCenterFor(locale);
  const socials = socialList(t, siteConfig.social);

  const fallbackCats = [
    { name: h.catFashion, img: null },
    { name: h.catBeauty, img: null },
    { name: h.catElectronics, img: null },
    { name: h.catHome, img: null },
  ];
  // Without the API (or before the first categories exist): the main departments, shown with their initial.
  const cats: { name: string; img: string | null }[] = categories.length > 0
    ? categories.map((c) => ({ name: c.name, img: absolute(c.imageUrl) }))
    : fallbackCats;

  const faq = [1, 2, 3, 4, 5, 6].map((i) => ({
    q: t.faq[`q${i}` as keyof typeof t.faq],
    a: t.faq[`a${i}` as keyof typeof t.faq],
  }));

  const services = [
    { key: "buy", icon: ICONS.buy, img: "role_buyer", title: h.buyerTitle, text: h.buyerText, href: "/#steps" },
    { key: "sell", icon: ICONS.sell, img: "role_seller", title: h.sellerTitle, text: h.sellerText, href: "/vendre" },
    { key: "deliver", icon: ICONS.deliver, img: "role_courier", title: h.courierTitle, text: h.courierText, href: "/livrer" },
    { key: "help", icon: ICONS.help, img: null, title: h.supportCardTitle, text: h.supportCardText, href: "/service-client" },
  ];
  const nearbyShops = (shops ?? []).slice(0, 6);
  const newsletterMessage = newsletter === "ok" ? t.newsletter.ok
    : newsletter === "invalid" ? t.newsletter.invalid
      : newsletter === "error" ? t.newsletter.error : null;
  const stores = [
    siteConfig.androidUrl && { href: siteConfig.androidUrl, label: t.footer.android },
    siteConfig.iosUrl && { href: siteConfig.iosUrl, label: t.footer.ios },
  ].filter(Boolean) as { href: string; label: string }[];

  return (
    <>
      <section className="hero" aria-labelledby="hero-title">
        <div className="wrap">
          <div className="hero-copy">
            <p className="eyebrow">{h.eyebrow}</p>
            <h1 id="hero-title">{h.heroTitle} <span className="hero-accent">{h.heroAccent}</span></h1>
            <p className="lead">{h.lead}</p>
            <div className="row">
              {appUrl
                ? <a className="btn btn-accent" href={appUrl}>{h.cta}</a>
                : <a className="btn btn-accent" href="#services">{h.cta}</a>}
              <Link className="btn btn-on-dark" href="/vendre">{h.ctaSell}</Link>
            </div>
            <ul className="chips">
              <li>{h.chipPayment}</li>
              <li>{h.chipTracking}</li>
              <li>{h.chipReturns}</li>
            </ul>
          </div>
          <div className="hero-visual">
            <div className="hero-photo hero-photo-wide">
              <Visual name="decouvrez-maplenou" alt={t.visuals.decouvrez} priority
                      sizes="(max-width: 980px) calc(100vw - 32px), 540px" />
            </div>
          </div>
        </div>
      </section>

      <section id="services" className="band" aria-labelledby="services-title">
        <div className="wrap">
          <div className="section-head">
            <h2 id="services-title">{h.servicesTitle}</h2>
            <p className="lead">{h.servicesLead}</p>
          </div>
          <ul className="services">
            {services.map((s) => (
              <li key={s.key}>
                <Link href={s.href} className="service">
                  <span className="service-media">
                    {s.img
                      ? <img src={`/images/${s.img}.jpg`} alt="" width={600} height={450} loading="lazy" />
                      : <span className="service-glyph"><Icon d={s.icon} /></span>}
                  </span>
                  <span className="service-body">
                    <span className="service-icon"><Icon d={s.icon} /></span>
                    <strong>{s.title}</strong>
                    <span className="muted">{s.text}</span>
                    <span className="service-more">{h.more} <span aria-hidden="true">→</span></span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="band band-tight" aria-labelledby="story-title">
        <div className="wrap story">
          <div className="story-media photo-frame">
            <Visual name="commerce-local" alt={t.visuals.commerceLocal} sizes="(max-width: 980px) calc(100vw - 32px), 600px" />
          </div>
          <div className="stack">
            <h2 id="story-title">{h.storyTitle}</h2>
            <p className="lead">{h.storyText}</p>
            <div><Link className="btn btn-primary" href="/boutiques">{h.storyCta}</Link></div>
          </div>
        </div>
      </section>

      <section className="band band-tight" aria-labelledby="banner-title">
        <div className="wrap">
          <div className="banner banner-media">
            <Visual name="vos-produits-visibilite" alt={t.visuals.visibilite} className="banner-img"
                    sizes="(max-width: 560px) calc(100vw - 80px), 220px" />
            <div className="stack">
              <p className="eyebrow">{h.bannerEyebrow}</p>
              <h2 id="banner-title">{h.bannerTitle}</h2>
              <p>{h.bannerText}</p>
            </div>
            <Link className="btn btn-primary" href="/vendre">{h.bannerCta}</Link>
          </div>
        </div>
      </section>

      {products.length > 0 && (
        <section id="produits" className="band band-alt" aria-labelledby="products-title">
          <div className="wrap">
            <div className="section-head head-row">
              <div className="stack">
                <h2 id="products-title">{h.productsTitle}</h2>
                <p className="lead">{h.productsLead}</p>
              </div>
              {appUrl && <a className="btn btn-ghost" href={appUrl}>{h.seeAllProducts}</a>}
            </div>
            <ul className="products">
              {products.map((p) => {
                const img = absolute(p.thumbnailUrl);
                const reviews = Number(p.reviewCount ?? 0);
                return (
                  <li className="product" key={p.id}>
                    <div className="product-img">{img && <img src={img} alt="" width={400} height={400} loading="lazy" />}</div>
                    <div className="product-body">
                      <strong title={p.name}>{p.name}</strong>
                      <span className="small muted">{p.shopName}</span>
                      {reviews > 0 && (
                        <span className="small rating">
                          <span aria-hidden="true">★ </span>
                          {format(h.rating, { rating: Number(p.averageRating ?? 0).toLocaleString(locale, { maximumFractionDigits: 1 }), count: reviews })}
                        </span>
                      )}
                      <span className="price">
                        <span className="nowrap">{fcfa(shown(p).price, locale)}</span>
                        {shown(p).old != null && <s className="nowrap">{fcfa(shown(p).old!, locale)}</s>}
                      </span>
                    </div>
                  </li>
                );
              })}
            </ul>
          </div>
        </section>
      )}

      <section id="rayons" className={products.length > 0 ? "band" : "band band-alt"} aria-labelledby="cats-title">
        <div className="wrap">
          <div className="section-head">
            <h2 id="cats-title">{h.categoriesTitle}</h2>
            <p className="lead">{h.categoriesLead}</p>
          </div>
          <ul className="cats">
            {cats.map((c) => (
              <li className="cat" key={c.name}>
                <div className="cat-img">
                  {c.img
                    ? <img src={c.img} alt="" width={400} height={400} loading="lazy" />
                    : <span className="initial" aria-hidden="true">{c.name.charAt(0)}</span>}
                </div>
                <span>{c.name}</span>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section id="steps" className="band band-alt" aria-labelledby="steps-title">
        <div className="wrap">
          <div className="section-head"><h2 id="steps-title">{h.stepsTitle}</h2></div>
          <div className="steps-layout">
            <ol className="steps steps-two">
              <li><h3>{h.step1Title}</h3><p>{h.step1Text}</p></li>
              <li><h3>{h.step2Title}</h3><p>{h.step2Text}</p></li>
              <li><h3>{h.step3Title}</h3><p>{h.step3Text}</p></li>
              <li><h3>{h.step4Title}</h3><p>{h.step4Text}</p></li>
            </ol>
            <div className="receipt" aria-label={`${h.receiptTitle} (${h.receiptExample})`}>
              <div className="receipt-head"><strong>{h.receiptTitle}</strong><span className="tag">{h.receiptExample}</span></div>
              <span className="small muted">{h.receiptShops}</span>
              <div className="receipt-line"><span>{h.receiptItem1}</span><span>{fcfa(6500, locale)}</span></div>
              <div className="receipt-line"><span>{h.receiptItem2}</span><span>{fcfa(9000, locale)}</span></div>
              <div className="receipt-line"><span>{h.receiptDelivery}</span><span>{fcfa(1000, locale)}</span></div>
              <div className="receipt-line receipt-total"><span>{h.receiptTotal}</span><span>{fcfa(16500, locale)}</span></div>
              <span className="receipt-paid">{h.receiptPaid}</span>
            </div>
          </div>
        </div>
      </section>

      <section id="livraison" className="band" aria-labelledby="features-title">
        <div className="wrap">
          <h2 id="features-title" className="sr-only">{h.deliveryTitle}</h2>
          <div className="features">
            <div className="feature">
              <span className="feature-mark" aria-hidden="true">km</span>
              <h3>{h.deliveryTitle}</h3>
              <p>{h.deliveryText}</p>
              {tiers.length > 0 ? (
                <table className="tiers">
                  <caption className="sr-only">{h.deliveryTiers}</caption>
                  <tbody>
                    {tiers.map((tier) => (
                      <tr key={tier.maxDistanceKm}>
                        <td>{format(h.deliveryUpTo, { km: Number(tier.maxDistanceKm).toLocaleString(locale) })}</td>
                        <td>{fcfa(Number(tier.price), locale)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : <p className="small">{h.deliveryNoTiers}</p>}
            </div>
            <div className="feature">
              <span className="feature-mark" aria-hidden="true">CFA</span>
              <h3>{h.paymentTitle}</h3>
              <p>{h.paymentText}</p>
            </div>
            <div className="feature">
              <span className="feature-mark" aria-hidden="true">30</span>
              <h3>{h.returnsTitle}</h3>
              <p>{h.returnsText}</p>
            </div>
          </div>
        </div>
      </section>

      <section id="aide" className="band band-brand support-band" aria-labelledby="support-title">
        <div className="wrap">
          <div className="stack">
            <p className="eyebrow">{t.support.eyebrow}</p>
            <h2 id="support-title">{h.helpTitle}</h2>
            <p className="lead">{h.helpLead}</p>
            <ul className="help-tiles">
              <li><a href={siteConfig.serviceClientUrl} className="help-tile"><Icon d={ICONS.ticket} /><strong>{t.support.openTicket}</strong><span>{h.helpTicketText}</span></a></li>
              <li><a href={helpCenterUrl} className="help-tile"><Icon d={ICONS.book} /><strong>{t.support.helpCenter}</strong><span>{h.helpCenterText}</span></a></li>
              <li><a href="#faq" className="help-tile"><Icon d={ICONS.faq} /><strong>{h.helpFaq}</strong><span>{h.helpFaqText}</span></a></li>
            </ul>
          </div>
          <div className="clock">
            <span className="small">{t.support.delaysTitle}</span>
            <div className="clock-row"><span>{t.support.delayHigh}</span><strong>{format(t.support.hours, { h: 4 })}</strong></div>
            <div className="clock-row"><span>{t.support.delayMedium}</span><strong>{format(t.support.hours, { h: 8 })}</strong></div>
            <div className="clock-row"><span>{t.support.delayLow}</span><strong>{format(t.support.hours, { h: 24 })}</strong></div>
            <span className="small">{t.support.delaysText}</span>
            <Link className="btn btn-accent" href="/service-client">{h.supportCta}</Link>
          </div>
        </div>
      </section>

      <section id="boutiques" className="band band-alt" aria-labelledby="shops-title">
        <div className="wrap">
          <div className="section-head head-row">
            <div className="stack">
              <h2 id="shops-title">{h.shopsTitle}</h2>
              <p className="lead">{h.shopsLead}</p>
            </div>
            <Link className="btn btn-primary" href="/boutiques"><Icon d={ICONS.pin} />{h.shopsAll}</Link>
          </div>
          {nearbyShops.length > 0 ? (
            <ul className="shops">
              {nearbyShops.map((s) => {
                const logo = absolute(s.logoUrl);
                const place = [s.district, s.city].filter(Boolean).join(", ");
                return (
                  <li className="shop" key={s.id}>
                    <span className="shop-logo">
                      {logo ? <img src={logo} alt="" width={56} height={56} loading="lazy" /> : <span aria-hidden="true">{s.name.charAt(0)}</span>}
                    </span>
                    <span className="shop-body">
                      <strong>{s.name}</strong>
                      {place && <span className="small muted">{place}</span>}
                    </span>
                  </li>
                );
              })}
            </ul>
          ) : (
            <div className="empty-note">
              <p>{h.shopsNone}</p>
              <Link className="btn btn-ghost btn-sm" href="/vendre">{h.shopsOpen}</Link>
            </div>
          )}
        </div>
      </section>

      <section id="faq" className="band" aria-labelledby="faq-title">
        <div className="wrap">
          <div className="section-head"><h2 id="faq-title">{h.faqTitle}</h2></div>
          <div className="faq">
            {faq.map((f) => (
              <details key={f.q}><summary>{f.q}</summary><p>{f.a}</p></details>
            ))}
          </div>
        </div>
      </section>

      <section id="newsletter" className="band band-accent" aria-labelledby="newsletter-title">
        <div className="wrap news-grid">
          <div className="stack">
            <h2 id="newsletter-title">{t.newsletter.title}</h2>
            <p>{t.newsletter.lead}</p>
            <form action={subscribeToNewsletter} className="news-form">
              <label htmlFor="newsletter-email" className="sr-only">{t.newsletter.label}</label>
              <input id="newsletter-email" name="email" type="email" required autoComplete="email" maxLength={254}
                     placeholder={t.newsletter.placeholder} aria-describedby={newsletterMessage ? "newsletter-status" : undefined} />
              <button type="submit" className="btn btn-primary">{t.newsletter.submit}</button>
            </form>
            {newsletterMessage && (
              <p id="newsletter-status" role="status" className={newsletter === "ok" ? "news-ok" : "news-error"}>{newsletterMessage}</p>
            )}
          </div>
          {(socials.length > 0 || stores.length > 0 || appUrl) && (
            <div className="stack news-side">
              {(stores.length > 0 || appUrl) && (
                <div className="stack">
                  <h3>{t.social.appTitle}</h3>
                  <p className="small">{t.social.appText}</p>
                  <div className="row">
                    {stores.map((s) => <a key={s.href} className="btn btn-ink btn-sm" href={s.href}>{s.label}</a>)}
                    {stores.length === 0 && appUrl && <a className="btn btn-ink btn-sm" href={appUrl}>{t.nav.openApp}</a>}
                  </div>
                </div>
              )}
              {socials.length > 0 && (
                <div className="stack">
                  <Visual name="suivez-nous" alt={t.visuals.suivezNous} className="news-visual"
                          sizes="(max-width: 980px) calc(100vw - 32px), 320px" />
                  <h3>{t.social.title}</h3>
                  <ul className="social-list">
                    {socials.map((s) => <li key={s.key}><a href={s.href} rel="noopener noreferrer" target="_blank">{s.label}</a></li>)}
                  </ul>
                </div>
              )}
            </div>
          )}
        </div>
      </section>
    </>
  );
}
