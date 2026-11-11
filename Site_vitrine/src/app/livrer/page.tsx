import type { Metadata } from "next";
import { siteConfig } from "@/lib/config";
import { getMessages } from "@/lib/i18n";
import { Visual } from "@/components/Visual";

export async function generateMetadata(): Promise<Metadata> {
  const { t } = await getMessages();
  return { title: t.meta.deliverTitle, description: t.deliver.lead };
}

export default async function DeliverPage() {
  const { t } = await getMessages();
  const d = t.deliver;
  return (
    <>
      <section className="page-hero" aria-labelledby="deliver-title">
        <div className="wrap page-hero-split">
          <div className="page-hero-copy">
            <p className="eyebrow">{d.eyebrow}</p>
            <h1 id="deliver-title">{d.title}</h1>
            <p className="lead">{d.lead}</p>
            {siteConfig.appUrl && <div className="row"><a className="btn btn-primary" href={siteConfig.appUrl}>{d.cta}</a></div>}
          </div>
          <div className="page-hero-media">
            <Visual name="application-maplenou" alt={t.visuals.application} priority sizes="(max-width: 980px) calc(100vw - 32px), 520px" />
          </div>
        </div>
      </section>

      <section className="band band-alt" aria-labelledby="deliver-docs">
        <div className="wrap split">
          <div className="panel">
            <h2 id="deliver-docs">{d.docsTitle}</h2>
            <ul className="check">
              <li>{d.doc1}</li>
              <li>{d.doc2}</li>
              <li>{d.doc3}</li>
              <li>{d.doc4}</li>
            </ul>
            <p className="small muted">{d.docsNote}</p>
          </div>
          <div className="role-photo photo-frame">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/images/role_courier.jpg" alt="" width={600} height={450} loading="lazy" />
          </div>
        </div>
      </section>

      <section className="band" aria-labelledby="deliver-how">
        <div className="wrap">
          <div className="section-head"><h2 id="deliver-how">{d.howTitle}</h2></div>
          <ol className="steps">
            <li><h3>{d.h1Title}</h3><p>{d.h1Text}</p></li>
            <li><h3>{d.h2Title}</h3><p>{d.h2Text}</p></li>
            <li><h3>{d.h3Title}</h3><p>{d.h3Text}</p></li>
            <li><h3>{d.h4Title}</h3><p>{d.h4Text}</p></li>
          </ol>
        </div>
      </section>
    </>
  );
}
