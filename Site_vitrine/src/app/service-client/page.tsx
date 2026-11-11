import type { Metadata } from "next";
import { helpCenterFor, siteConfig } from "@/lib/config";
import { getMessages } from "@/lib/i18n";
import { format } from "@/lib/messages";
import { Visual } from "@/components/Visual";

export async function generateMetadata(): Promise<Metadata> {
  const { t } = await getMessages();
  return { title: t.meta.supportTitle, description: t.support.lead };
}

/** First-response targets of the customer service (ServiceClient SlaTargets, DEC-001), counted 24/7. */
const TARGETS = [
  { key: "delayCritical", hours: 1 },
  { key: "delayHigh", hours: 4 },
  { key: "delayMedium", hours: 8 },
  { key: "delayLow", hours: 24 },
] as const;

export default async function SupportPage() {
  const { locale, t } = await getMessages();
  const s = t.support;
  const topics = [s.topic1, s.topic2, s.topic3, s.topic4, s.topic5, s.topic6, s.topic7];
  const flow = [
    { title: s.f1, text: s.f1Text }, { title: s.f2, text: s.f2Text },
    { title: s.f3, text: s.f3Text }, { title: s.f4, text: s.f4Text },
  ];
  return (
    <>
      <section className="page-hero" aria-labelledby="support-page-title">
        <div className="wrap page-hero-split">
          <div className="page-hero-copy">
            <p className="eyebrow">{s.eyebrow}</p>
            <h1 id="support-page-title">{s.title}</h1>
            <p className="lead">{s.lead}</p>
            <div className="row">
              <a className="btn btn-primary" href={siteConfig.serviceClientUrl}>{s.openTicket}</a>
              <a className="btn btn-ghost" href={helpCenterFor(locale)}>{s.helpCenter}</a>
            </div>
          </div>
          <div className="page-hero-media">
            <Visual name="experience-repensee" alt={t.visuals.experience} priority sizes="(max-width: 980px) calc(100vw - 32px), 520px" />
          </div>
        </div>
      </section>

      <section className="band band-alt" aria-labelledby="topics-title">
        <div className="wrap split">
          <div className="stack">
            <h2 id="topics-title">{s.topicsTitle}</h2>
            <ul className="topics">{topics.map((topic) => <li key={topic}>{topic}</li>)}</ul>
          </div>
          <div className="panel" id="delays">
            <h2>{s.delaysTitle}</h2>
            <table className="tiers">
              <tbody>
                {TARGETS.map((target) => (
                  <tr key={target.key}><td>{s[target.key]}</td><td>{format(s.hours, { h: target.hours })}</td></tr>
                ))}
              </tbody>
            </table>
            <p className="small muted">{s.delaysText}</p>
          </div>
        </div>
      </section>

      <section className="band" aria-labelledby="flow-title">
        <div className="wrap">
          <div className="section-head"><h2 id="flow-title">{s.flowTitle}</h2></div>
          <ol className="flow">
            {flow.map((f) => <li key={f.title}><strong>{f.title}</strong><p>{f.text}</p></li>)}
          </ol>
        </div>
      </section>

      <section className="band band-brand" aria-labelledby="assistant-title">
        <div className="wrap stack">
          <h2 id="assistant-title">{s.assistantTitle}</h2>
          <p className="lead">{s.assistantText}</p>
          <div className="row"><a className="btn btn-primary" href={helpCenterFor(locale)}>{s.helpCenter}</a></div>
        </div>
      </section>
    </>
  );
}
