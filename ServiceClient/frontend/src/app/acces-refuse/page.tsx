import Link from "next/link";
import { getMessages } from "@/lib/i18n/server";
import { pages } from "@/lib/i18n/messages/shell";

/** C3 — Access denied: explains without revealing protected data (AC-SB-017). */
export default async function AccessDenied() {
  const t = await getMessages(pages);
  return (
    <main className="login-page">
      <div className="card stack" style={{ maxWidth: 460, textAlign: "center" }}>
        <div style={{ fontSize: "2rem" }} aria-hidden="true">🔒</div>
        <h1>{t.deniedTitle}</h1>
        <p className="secondary">{t.deniedText}</p>
        <Link className="btn btn-primary" href="/">
          {t.backToMySpace}
        </Link>
      </div>
    </main>
  );
}
