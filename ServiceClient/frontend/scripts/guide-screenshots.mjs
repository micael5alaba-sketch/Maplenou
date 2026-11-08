// Screenshots of the user and staff guides, with numbered circles on the parts each page talks about (CS-12).
//
//   1. start the demo stack:  docker compose up -d --build --wait   (or the native backend + frontend); with the
//      FIREBASE_WEB_* values set (.env), so the push card shows its "turn on" button as in production
//   2. cd frontend && npx playwright install chromium   (once)
//   3. node scripts/guide-screenshots.mjs [--base http://localhost:3001] [--only help-home,queue]
//
// Writes documentation/<site>/<lang>/docs/images/<name>.jpg in French and English (the interface language of each
// guide). The circles are drawn in the page before the capture; the numbered legend is written in the guide page,
// so it is translated and searchable. Personas and data come from the demo profile (never a production account).
import { chromium } from "@playwright/test";
import { mkdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const docsRoot = path.resolve(here, "../../documentation");
const args = process.argv.slice(2);
const opt = (name, fallback) => {
  const i = args.indexOf(`--${name}`);
  return i >= 0 ? args[i + 1] : fallback;
};
const BASE = opt("base", process.env.E2E_BASE_URL ?? "http://localhost:3001");
const ONLY = opt("only", "").split(",").filter(Boolean);

const PHONE = { width: 412, height: 860, deviceScaleFactor: 1.5, isMobile: true, hasTouch: true };
const DESKTOP = { width: 1280, height: 800, deviceScaleFactor: 1 };
const ACCENT = "#d9480f"; // readable on the light theme used for the captures; white halo for dark backgrounds

/** A text in both interface languages. */
const t = (fr, en) => ({ fr, en });
const pick = (v, lang) => (v && typeof v === "object" && "fr" in v ? v[lang] : v);

/**
 * Each shot: guide site, persona, viewport, how to reach the screen, and the marks (1, 2, 3…) in legend order.
 * A mark is a function (page, lang) => Locator; its first visible match is circled.
 */
const SHOTS = [
  // ------------------------------------------------------------------ user guide (phone)
  {
    name: "help-home", site: "user", persona: "Jean K.", device: PHONE, go: "/aide",
    marks: [
      (p, l) => p.getByRole("link", { name: pick(t("Créer un ticket", "Create a ticket"), l), exact: true }),
      (p, l) => p.locator("section.card").filter({ hasText: pick(t(/Trouver une r/, /Find an answer/), l) }),
      (p, l) => p.getByRole("heading", { name: pick(t(/Demandes en cours/, /Open requests/), l) }).locator(".."),
      (p) => p.locator("nav").last(),
    ],
  },
  {
    name: "new-ticket", site: "user", persona: "Jean K.", device: { ...PHONE, height: 1180 }, go: "/aide/nouveau",
    marks: [
      (p, l) => p.getByLabel(pick(t("Catégorie", "Category"), l), { exact: true }),
      (p, l) => p.getByLabel(pick(t(/Commande concernée/, /Related order/), l)),
      (p, l) => p.getByText(pick(t("Moyenne", "Medium"), l), { exact: true }).locator("xpath=ancestor-or-self::label[1]/.."),
      (p) => p.locator("label.checkbox").first(),
      (p, l) => p.getByLabel(pick(t("Sujet", "Subject"), l), { exact: true }),
      (p, l) => p.getByRole("button", { name: pick(t(/Ajouter une pièce jointe/, /Add an attachment/), l) }),
      (p, l) => p.getByRole("button", { name: pick(t("Envoyer le ticket", "Send ticket"), l) }),
    ],
  },
  {
    name: "my-tickets", site: "user", persona: "Jean K.", device: PHONE, go: "/aide/tickets",
    marks: [
      (p) => p.getByRole("tablist"),
      (p) => p.getByRole("link", { name: /TKT-2026-00001/ }),
      (p, l) => p.getByRole("link", { name: pick(t(/Nouveau/, /New/), l) }).first(),
    ],
  },
  {
    name: "ticket-conversation", site: "user", persona: "Jean K.", device: { ...PHONE, height: 1100 },
    go: async (p) => { await p.goto(`${BASE}/aide/tickets`); await p.getByRole("link", { name: /TKT-2026-00001/ }).click(); },
    marks: [
      (p) => p.locator("main .badge").first(),
      (p, l) => p.getByRole("heading", { name: pick(t("Conversation", "Conversation"), l) }),
      (p, l) => p.getByLabel(pick(t("Votre message", "Your message"), l)),
      (p, l) => p.getByRole("button", { name: pick(t("Envoyer", "Send"), l), exact: true }),
      (p, l) => p.getByRole("heading", { name: pick(t("Suivi du ticket", "Ticket history"), l) }),
    ],
    scrollTo: (p, l) => p.getByRole("heading", { name: pick(t("Conversation", "Conversation"), l) }),
  },
  {
    name: "resolved-ticket", site: "user", persona: "Jean K.", device: { ...PHONE, height: 1000 },
    go: async (p) => { await p.goto(`${BASE}/aide/tickets`); await p.getByRole("link", { name: /TKT-2026-00004/ }).click(); },
    marks: [
      (p, l) => p.getByRole("heading", { name: pick(t("Votre avis sur la réponse", "Your opinion of the answer"), l) }).locator(".."),
      (p, l) => p.getByRole("button", { name: pick(t("Demander la réouverture", "Request reopening"), l) }),
    ],
  },
  {
    name: "notifications", site: "user", persona: "Jean K.", device: PHONE, go: "/notifications",
    marks: [
      (p, l) => p.getByRole("heading", { name: pick(t("Notifications push", "Push notifications"), l) }).locator(".."),
      (p) => p.getByRole("tablist"),
      (p, l) => p.getByRole("button", { name: pick(t("Tout marquer comme lu", "Mark all as read"), l) }),
      (p) => p.locator("main a").filter({ hasText: /TKT-2026/ }).first(),
    ],
    scrollTo: null,
  },
  {
    name: "account", site: "user", persona: "Jean K.", device: PHONE, go: "/compte",
    marks: [
      (p, l) => p.getByRole("heading", { name: pick(t("Préférences", "Preferences"), l) }).locator(".."),
      (p, l) => p.getByRole("link", { name: pick(t(/Centre d'aide/, /Help centre/), l) }),
      (p, l) => p.getByRole("button", { name: pick(t("Se déconnecter", "Sign out"), l) }).last(),
    ],
  },
  // ------------------------------------------------------------------ staff guide (desktop)
  {
    name: "dashboard", site: "staff", persona: "Sophie N.", device: DESKTOP, go: "/support",
    marks: [
      (p) => p.locator(".kpis").first(),
      (p, l) => p.getByRole("heading", { name: pick(t("Tickets ouverts par priorité", "Open tickets by priority"), l) }).locator(".."),
      (p, l) => p.getByRole("link", { name: pick(t(/File d'attente/, /Queue/), l) }).first(),
      (p, l) => p.getByRole("link", { name: pick(t(/Mes tickets/, /My tickets/), l) }).first(),
    ],
  },
  {
    name: "queue", site: "staff", persona: "Amina K.", device: DESKTOP, go: "/support/tickets",
    marks: [
      (p, l) => p.getByLabel(pick(t("Rechercher", "Search"), l), { exact: true }),
      (p, l) => p.getByLabel(pick(t("Statut", "Status"), l), { exact: true }),
      (p, l) => p.getByLabel(pick(t("SLA dépassé", "SLA breached"), l)),
      (p, l) => p.getByRole("link", { name: pick(t(/Exporter/, /Export/), l) }).or(p.getByRole("button", { name: pick(t(/Exporter/, /Export/), l) })),
      (p) => p.locator("table.data tbody tr").first(),
    ],
  },
  {
    name: "ticket-workspace", site: "staff", persona: "Sophie N.", device: { ...DESKTOP, height: 1000 },
    go: async (p) => {
      await p.goto(`${BASE}/support/tickets`);
      await p.getByRole("link", { name: /Problème de livraison/ }).first().click();
      await p.waitForURL(/\/support\/tickets\/[0-9a-f-]{36}/);
    },
    marks: [
      (p) => p.getByRole("tablist"),
      (p, l) => p.getByLabel(pick(t("Insérer une réponse type", "Insert a canned reply"), l)),
      (p, l) => p.getByRole("button", { name: pick(t(/Brouillon par l'assistant/, /Draft by the assistant/), l) }),
      (p, l) => p.getByRole("heading", { name: pick(t(/ACTIONS SUR LE TICKET/i, /TICKET ACTIONS/i), l) }).locator(".."),
      (p, l) => p.getByRole("heading", { name: pick(t(/ACTIONS SUR MAPLENOU/i, /ACTIONS ON MAPLENOU/i), l) }).locator(".."),
      (p, l) => p.getByRole("heading", { name: pick(t(/^COMMANDE$/i, /^ORDER$/i), l) }).locator(".."),
    ],
  },
  {
    name: "admin-workflow", site: "staff", persona: "Admin Maplenou", device: { ...DESKTOP, height: 1100 }, go: "/support/admin/flux",
    marks: [
      (p, l) => p.getByRole("heading", { name: pick(t("Statuts", "Statuses"), l), exact: true }),
      (p, l) => p.getByRole("button", { name: pick(t("Ajouter un statut", "Add a status"), l) }),
      (p, l) => p.getByRole("heading", { name: pick(t(/PASSAGES POSSIBLES/i, /ALLOWED MOVES/i), l) }),
      (p, l) => p.getByRole("button", { name: pick(t("Enregistrer les statuts", "Save statuses"), l) }),
    ],
  },
  {
    name: "admin-permissions", site: "staff", persona: "Admin Maplenou", device: DESKTOP, go: "/support/admin/permissions",
    marks: [
      (p, l) => p.getByLabel(pick(t("Agent support", "Support agent"), l), { exact: true }).locator("xpath=ancestor::*[self::fieldset or self::div][1]"),
      (p) => p.locator("table.data thead").first(),
      (p) => p.locator("table.data tbody tr").first(),
      (p, l) => p.getByRole("button", { name: pick(t("Enregistrer les droits", "Save permissions"), l) }),
      (p, l) => p.getByRole("button", { name: pick(t("Revenir aux droits d'origine", "Back to the original permissions"), l) }),
    ],
  },
  {
    name: "admin-templates", site: "staff", persona: "Admin Maplenou", device: DESKTOP, go: "/support/admin/modeles",
    marks: [
      (p, l) => p.getByRole("heading", { name: pick(t("Réponses types", "Canned replies"), l) }),
      (p, l) => p.getByRole("button", { name: pick(t("Ajouter une réponse type", "Add a canned reply"), l) }),
      (p, l) => p.getByRole("heading", { name: pick(t("Notifications envoyées aux utilisateurs", "Notifications sent to users"), l) }),
    ],
  },
  {
    name: "sla-calendar", site: "staff", persona: "Admin Maplenou", device: DESKTOP, go: "/support/admin/calendrier",
    marks: [
      (p, l) => p.getByRole("heading", { name: pick(t("Calendrier SLA", "SLA calendar"), l), exact: true }),
      (p, l) => p.getByRole("heading", { name: pick(t("Jours fériés", "Public holidays"), l) }),
      (p, l) => p.getByRole("button", { name: pick(t("Ajouter", "Add"), l), exact: true }),
    ],
  },
  {
    name: "reports", site: "staff", persona: "Amina K.", device: DESKTOP, go: "/support/rapports",
    marks: [
      (p, l) => p.getByLabel(pick(t("Du", "From"), l), { exact: true }),
      (p) => p.locator(".kpis").first(),
      (p, l) => p.getByRole("heading", { name: pick(t("Tickets créés par jour", "Tickets created per day"), l) }),
      (p, l) => p.getByRole("heading", { name: pick(t("Répartition par catégorie", "Breakdown by category"), l) }),
    ],
  },
];

/** Draws an ellipse and a numbered badge around each box (page coordinates). */
async function annotate(page, boxes) {
  await page.evaluate(({ boxes, accent }) => {
    document.querySelectorAll("[data-guide-mark]").forEach((e) => e.remove());
    boxes.forEach((b, i) => {
      const pad = 6;
      const ring = document.createElement("div");
      ring.dataset.guideMark = "ring";
      Object.assign(ring.style, {
        position: "absolute", left: `${b.x - pad}px`, top: `${b.y - pad}px`,
        width: `${b.width + 2 * pad}px`, height: `${b.height + 2 * pad}px`,
        // a circle around a small control, a rounded frame around a large area
        border: `3px solid ${accent}`, borderRadius: b.height < 64 ? "999px" : "18px", boxShadow: "0 0 0 2px #fff",
        pointerEvents: "none", zIndex: "2147483646", boxSizing: "border-box",
      });
      const badge = document.createElement("div");
      badge.dataset.guideMark = "badge";
      badge.textContent = String(i + 1);
      Object.assign(badge.style, {
        position: "absolute", left: `${Math.max(2, b.x - pad - 12)}px`, top: `${Math.max(2, b.y - pad - 12)}px`,
        width: "24px", height: "24px", borderRadius: "50%", background: accent, color: "#fff",
        font: "700 14px/24px system-ui, sans-serif", textAlign: "center", boxShadow: "0 0 0 2px #fff",
        pointerEvents: "none", zIndex: "2147483647",
      });
      document.body.append(ring, badge);
    });
  }, { boxes, accent: ACCENT });
}

async function signIn(context, persona) {
  const page = await context.newPage();
  await page.goto(`${BASE}/login`);
  await page.getByRole("button", { name: new RegExp(persona) }).click();
  await page.waitForURL(/\/(aide|support)/);
  return page;
}

async function capture(browser, shot, lang) {
  const context = await browser.newContext({
    viewport: { width: shot.device.width, height: shot.device.height },
    deviceScaleFactor: shot.device.deviceScaleFactor, isMobile: shot.device.isMobile ?? false,
    hasTouch: shot.device.hasTouch ?? false, locale: lang === "fr" ? "fr-FR" : "en-GB", colorScheme: "light",
    timezoneId: "Africa/Lome",
  });
  const host = new URL(BASE).hostname;
  await context.addCookies([
    { name: "maplenou_locale", value: lang, domain: host, path: "/" },
    { name: "maplenou_theme", value: "light", domain: host, path: "/" },
  ]);
  // headless Chromium always answers "denied": show the push card as a person sees it the first time ("turn on")
  await context.addInitScript(() => {
    if ("Notification" in window) Object.defineProperty(Notification, "permission", { get: () => "default" });
  });
  const page = await signIn(context, shot.persona);
  if (typeof shot.go === "function") await shot.go(page);
  else await page.goto(`${BASE}${shot.go}`);
  await page.waitForLoadState("networkidle");
  await page.addStyleTag({ content: "*,*::before,*::after{transition:none!important;animation:none!important;caret-color:transparent!important}" });
  if (shot.scrollTo) await shot.scrollTo(page, lang).first().scrollIntoViewIfNeeded();
  const measure = async () => {
    const boxes = [];
    for (const [i, mark] of shot.marks.entries()) {
      const loc = mark(page, lang).first();
      try {
        await loc.waitFor({ state: "visible", timeout: 8000 });
      } catch {
        throw new Error(`${shot.name} (${lang}): mark ${i + 1} not found`);
      }
      const box = await loc.boundingBox();
      const scroll = await page.evaluate(() => ({ x: window.scrollX, y: window.scrollY }));
      boxes.push({ x: box.x + scroll.x, y: box.y + scroll.y, width: box.width, height: box.height });
    }
    return boxes;
  };
  let boxes = await measure();
  // a mark outside the visible area: make the window as tall as the page (fixed menus then stay in place), max 2400 px
  const view = await page.evaluate(() => ({ top: window.scrollY, bottom: window.scrollY + window.innerHeight,
    page: document.documentElement.scrollHeight }));
  if (boxes.some((bx) => bx.y + bx.height > view.bottom || bx.y < view.top)) {
    // + room for the fixed bottom menu of the phone layout, so it never covers the last mark
    await page.setViewportSize({ width: shot.device.width, height: Math.min(view.page + (shot.device.isMobile ? 100 : 0), 2400) });
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(300);
    boxes = await measure();
  }
  await annotate(page, boxes);
  const out = path.join(docsRoot, shot.site, lang, "docs", "images", `${shot.name}.jpg`);
  await mkdir(path.dirname(out), { recursive: true });
  await page.screenshot({ path: out, type: "jpeg", quality: 82 });
  await context.close();
  return out;
}

const browser = await chromium.launch();
let failed = 0;
try {
  for (const shot of SHOTS.filter((s) => !ONLY.length || ONLY.includes(s.name))) {
    for (const lang of ["fr", "en"]) {
      try {
        console.log("✓", path.relative(process.cwd(), await capture(browser, shot, lang)));
      } catch (e) {
        failed++;
        console.error("✗", e.message);
      }
    }
  }
} finally {
  await browser.close();
}
process.exit(failed ? 1 : 0);
