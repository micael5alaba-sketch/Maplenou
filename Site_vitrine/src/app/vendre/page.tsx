import type { Metadata } from "next";
import { CommissionCalculator } from "@/components/CommissionCalculator";
import { siteConfig } from "@/lib/config";
import { getMessages } from "@/lib/i18n";
import { Visual } from "@/components/Visual";

export async function generateMetadata(): Promise<Metadata> {
  const { t } = await getMessages();
  return { title: t.meta.sellTitle, description: t.sell.lead };
}

export default async function SellPage() {
  const { locale, t } = await getMessages();
  const s = t.sell;
  const steps = [1, 2, 3, 4, 5, 6].map((i) => ({
    title: s[`s${i}Title` as keyof typeof s],
    text: s[`s${i}Text` as keyof typeof s],
  }));
  return (
    <>
      <section className="page-hero" aria-labelledby="sell-title">
        <div className="wrap page-hero-split">
          <div className="page-hero-copy">
            <p className="eyebrow">{s.eyebrow}</p>
            <h1 id="sell-title">{s.title}</h1>
            <p className="lead">{s.lead}</p>
            {siteConfig.appUrl && <div className="row"><a className="btn btn-primary" href={siteConfig.appUrl}>{s.cta}</a></div>}
          </div>
          <div className="page-hero-media">
            <Visual name="plus-de-clients" alt={t.visuals.clients} priority sizes="(max-width: 980px) calc(100vw - 32px), 420px" />
          </div>
        </div>
      </section>

      <section className="band band-alt" aria-labelledby="sell-steps">
        <div className="wrap">
          <div className="section-head"><h2 id="sell-steps">{s.stepsTitle}</h2></div>
          <ol className="steps six">
            {steps.map((st) => <li key={st.title}><h3>{st.title}</h3><p>{st.text}</p></li>)}
          </ol>
        </div>
      </section>

      <section className="band" aria-labelledby="sell-pricing">
        <div className="wrap split">
          <div className="stack">
            <h2 id="sell-pricing">{s.pricingTitle}</h2>
            <p className="muted">{s.pricingText}</p>
            <ul className="check">
              <li>{s.tierLow}</li>
              <li>{s.tierHigh}</li>
              <li>{s.minPrice}</li>
            </ul>
            <p className="small">{s.example}</p>
            <p className="small muted">{s.categoryChoice}</p>
            <p className="small muted">{s.otherFees}</p>
            <p className="small muted">{s.cancelRule}</p>
          </div>
          <div id="calc">
          <CommissionCalculator
            locale={locale}
            labels={{
              calcTitle: s.calcTitle, calcLabel: s.calcLabel, calcCategory: s.calcCategory,
              catStandard: s.catStandard, catHigh: s.catHigh, calcCommission: s.calcCommission,
              calcDisplay: s.calcDisplay, calcNet: s.calcNet, calcBelowMin: s.calcBelowMin, calcHighHint: s.calcHighHint,
            }}
          />
          </div>
        </div>
      </section>
    </>
  );
}
