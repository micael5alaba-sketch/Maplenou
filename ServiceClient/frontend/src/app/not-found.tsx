import Link from "next/link";
import { getMessages } from "@/lib/i18n/server";
import { pages } from "@/lib/i18n/messages/shell";

export default async function NotFound() {
  const t = await getMessages(pages);
  return (
    <main className="login-page">
      <div className="card stack" style={{ maxWidth: 440, textAlign: "center" }}>
        <h1>{t.notFoundTitle}</h1>
        <p className="secondary">{t.notFoundText}</p>
        <Link className="btn btn-primary" href="/">
          {t.backHome}
        </Link>
      </div>
    </main>
  );
}
