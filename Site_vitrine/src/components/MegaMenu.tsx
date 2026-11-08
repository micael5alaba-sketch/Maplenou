"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";

export interface MegaLink { href: string; label: string; text: string; external?: boolean }
export interface MegaSection { id: string; label: string; intro: string; links: MegaLink[]; allHref?: string; allLabel?: string }
export interface MegaItem { href: string; label: string }

function Item({ link, onPick }: { link: MegaLink; onPick: () => void }) {
  const body = (<><strong>{link.label}</strong><span>{link.text}</span></>);
  return link.external || link.href.startsWith("http")
    ? <a href={link.href} className="mega-link" onClick={onPick}>{body}</a>
    : <Link href={link.href} className="mega-link" onClick={onPick}>{body}</Link>;
}

/**
 * Two-level menu by audience (buy, sell, deliver, customer service). Desktop: a button per section opens a panel
 * under the header (Escape, a click outside or a choice closes it). Phone: a "Menu" button opens the same sections
 * as an accordion. Plain links and buttons: keyboard and screen readers work without extra roles.
 */
export function MegaMenu({ sections, links, label, menuLabel, cta }: {
  sections: MegaSection[]; links: MegaItem[]; label: string; menuLabel: string; cta?: MegaItem;
}) {
  const [open, setOpen] = useState<string | null>(null);
  const [mobile, setMobile] = useState(false);
  const root = useRef<HTMLDivElement>(null);
  const path = usePathname();

  // closes the menus after a navigation (state adjusted during render, as React recommends, not in an effect)
  const [shownPath, setShownPath] = useState(path);
  if (shownPath !== path) {
    setShownPath(path);
    setOpen(null);
    setMobile(false);
  }
  useEffect(() => {
    if (!open && !mobile) return;
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") { setOpen(null); setMobile(false); } };
    const onClick = (e: MouseEvent) => {
      if (root.current && !root.current.contains(e.target as Node)) { setOpen(null); setMobile(false); }
    };
    document.addEventListener("keydown", onKey);
    document.addEventListener("mousedown", onClick);
    return () => { document.removeEventListener("keydown", onKey); document.removeEventListener("mousedown", onClick); };
  }, [open, mobile]);

  const close = () => { setOpen(null); setMobile(false); };

  return (
    <div className="mega" ref={root}>
      <nav className="mega-bar" aria-label={label}>
        <ul>
          {sections.map((s) => (
            <li key={s.id}>
              <button type="button" className="mega-toggle" aria-expanded={open === s.id} aria-controls={`mega-${s.id}`}
                      onClick={() => setOpen(open === s.id ? null : s.id)}>
                {s.label}<span className="chevron" aria-hidden="true" />
              </button>
            </li>
          ))}
          {links.map((l) => (
            <li key={l.href}>
              <Link href={l.href} className="mega-toggle" aria-current={path === l.href ? "page" : undefined}>{l.label}</Link>
            </li>
          ))}
        </ul>
      </nav>
      {sections.map((s) => (
        <div key={s.id} id={`mega-${s.id}`} className="mega-panel" hidden={open !== s.id}>
          <div className="wrap">
            <div className="mega-intro">
              <p className="mega-title">{s.label}</p>
              <p className="muted">{s.intro}</p>
              {s.allHref && <Link href={s.allHref} className="btn btn-ghost btn-sm" onClick={close}>{s.allLabel}</Link>}
            </div>
            <div className="mega-links">{s.links.map((l) => <Item key={l.href + l.label} link={l} onPick={close} />)}</div>
          </div>
        </div>
      ))}

      <button type="button" className="burger" aria-expanded={mobile} aria-controls="mega-mobile" onClick={() => setMobile(!mobile)}>
        <span className="burger-icon" aria-hidden="true" />{menuLabel}
      </button>
      <div id="mega-mobile" className="mega-mobile" hidden={!mobile}>
        <nav aria-label={label}>
          {sections.map((s) => (
            <details key={s.id}>
              <summary>{s.label}</summary>
              <div className="mega-mobile-links">{s.links.map((l) => <Item key={l.href + l.label} link={l} onPick={close} />)}</div>
            </details>
          ))}
          {links.map((l) => <Link key={l.href} href={l.href} className="mega-mobile-plain" onClick={close}>{l.label}</Link>)}
          {cta && <a href={cta.href} className="btn btn-primary">{cta.label}</a>}
        </nav>
      </div>
    </div>
  );
}
