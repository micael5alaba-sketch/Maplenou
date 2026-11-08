import Link from "next/link";
import { getMessages } from "@/lib/i18n";

export default async function NotFound() {
  const { t } = await getMessages();
  return (
    <section className="page-hero">
      <div className="wrap">
        <p className="eyebrow">404</p>
        <h1>{t.notFound.title}</h1>
        <p className="lead">{t.notFound.text}</p>
        <div className="row"><Link className="btn btn-primary" href="/">{t.notFound.back}</Link></div>
      </div>
    </section>
  );
}
