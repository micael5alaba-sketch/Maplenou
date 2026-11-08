import { expect, test, type Browser, type Page } from "@playwright/test";
import path from "node:path";

/**
 * CS-09 final validation — roadmap §5 "Scénario complet de validation", driven through the real UI:
 * CLIENT → création → catégorisation → agent → vérification commande/livraison → réponse → notification
 * → résolution → (réouverture) → fermeture. Requires the stack running with the backend `demo` profile.
 */

async function signIn(browser: Browser, persona: string): Promise<Page> {
  // inherit the project device (viewport, touch, user agent) so the mobile project really runs on a phone profile
  const context = await browser.newContext({ ...test.info().project.use, locale: "fr-FR" });
  const page = await context.newPage();
  await page.goto("/login");
  await page.getByRole("button", { name: new RegExp(persona) }).click();
  await page.waitForURL(/\/(aide|support)/);
  return page;
}

test("complete ticket lifecycle across customer and support agent", async ({ browser }) => {
  const subject = `Colis non reçu E2E ${Date.now()}`;

  // --- Customer creates a ticket linked to an order, with an attachment
  const jean = await signIn(browser, "Jean K.");
  await jean.getByRole("link", { name: "Créer un ticket" }).click();
  await jean.waitForURL(/\/aide\/nouveau/);
  await jean.getByLabel("Catégorie", { exact: true }).selectOption("DELIVERY");
  await jean.getByLabel(/Commande concernée/).selectOption("CMD-2026-0456");
  await jean.getByText("Élevée", { exact: true }).click();
  await jean.getByLabel("Sujet").fill(subject);
  await jean.getByLabel("Description").fill("Le colis est marqué livré mais je n'ai rien reçu.");
  await jean.getByTestId("file-input").setInputFiles(path.join(__dirname, "fixtures/photo-colis.png"));
  await expect(jean.getByText("photo-colis.png")).toBeVisible();
  await jean.getByRole("button", { name: "Envoyer le ticket" }).click();
  await expect(jean.getByRole("heading", { name: "Ticket créé avec succès !" })).toBeVisible();
  const ticketNumber = (await jean.getByTestId("created-ticket-number").innerText()).trim();
  expect(ticketNumber).toMatch(/^TKT-\d{4}-\d{5,}$/);
  await jean.getByRole("link", { name: "Voir mon ticket" }).click();
  await expect(jean.getByText("Nouveau", { exact: true })).toBeVisible();

  // --- Agent finds it in the queue, checks context, takes it, adds an internal note and asks for information
  const sophie = await signIn(browser, "Sophie N.");
  await sophie.getByRole("link", { name: /File d'attente/ }).click();
  await sophie.waitForURL(/\/support\/tickets/);
  await sophie.getByLabel("Rechercher").fill(ticketNumber);
  await sophie.getByRole("button", { name: "Rechercher" }).click();
  await sophie.getByRole("link", { name: subject }).click();
  await expect(sophie.getByText("CMD-2026-0456").first()).toBeVisible();
  await expect(sophie.getByText("Kossi D.")).toBeVisible(); // delivery context
  await sophie.getByRole("button", { name: "Prendre en charge" }).click();
  await expect(sophie.getByText("Attribué à :").locator("..")).toContainText("Sophie N.");

  await sophie.getByRole("tab", { name: "Notes internes" }).click();
  const secret = "NOTE-INTERNE-CONFIDENTIELLE livreur contacté";
  await sophie.getByLabel("Ajouter une note interne").fill(secret);
  await sophie.getByRole("button", { name: "Ajouter la note" }).click();
  await expect(sophie.getByText(secret)).toBeVisible();

  await sophie.getByRole("tab", { name: "Conversation" }).click();
  await sophie.getByLabel("Votre message").fill("Bonjour Jean, confirmez-vous votre disponibilité demain 9 h–12 h ?");
  await sophie.getByRole("button", { name: "Envoyer" }).click();
  await expect(sophie.getByText(/confirmez-vous votre disponibilité/)).toBeVisible();
  await sophie.getByRole("button", { name: "Demander des informations" }).click();
  await expect(sophie.getByText("En attente du client", { exact: true }).first()).toBeVisible();

  // --- Customer sees the request, never the internal note, and replies
  await jean.reload();
  await expect(jean.getByText("Action requise")).toBeVisible();
  await expect(jean.getByText(/confirmez-vous votre disponibilité/)).toBeVisible();
  await expect(jean.getByText(secret)).toHaveCount(0);
  await jean.getByLabel("Votre message").fill("Oui, je serai disponible demain matin.");
  await jean.getByRole("button", { name: "Envoyer" }).click();
  await expect(jean.getByText("Oui, je serai disponible demain matin.")).toBeVisible();
  await expect(jean.getByText("En cours", { exact: true }).first()).toBeVisible();

  // --- Agent resolves (deliberate confirmation)
  await sophie.reload();
  await sophie.getByRole("button", { name: "Marquer résolu" }).click();
  await sophie.getByRole("dialog").getByRole("button", { name: "Marquer résolu" }).click();
  await expect(sophie.getByText("Résolu", { exact: true }).first()).toBeVisible();

  // --- Customer asks to reopen; agent approves; ticket resolved again and closed
  await jean.reload();
  await expect(jean.getByText("Votre ticket est résolu")).toBeVisible();
  await jean.getByRole("button", { name: "Demander la réouverture" }).click();
  await jean.getByLabel(/Pourquoi le problème/).fill("Le livreur n'est finalement pas passé.");
  await jean.getByRole("button", { name: "Envoyer la demande" }).click();
  await expect(jean.getByText("Demande de réouverture en cours d'examen")).toBeVisible();

  await sophie.reload();
  await sophie.getByRole("button", { name: "Rouvrir (En cours)" }).click();
  await expect(sophie.getByText("En cours", { exact: true }).first()).toBeVisible();
  await sophie.getByRole("button", { name: "Marquer résolu" }).click();
  await sophie.getByRole("dialog").getByRole("button", { name: "Marquer résolu" }).click();
  await expect(sophie.getByRole("button", { name: "Clôturer le ticket" })).toBeVisible();
  await sophie.getByRole("button", { name: "Clôturer le ticket" }).click();
  await sophie.getByRole("dialog").getByRole("button", { name: "Clôturer" }).click();
  await expect(sophie.getByText("Fermé", { exact: true }).first()).toBeVisible();

  // --- Customer: closed ticket offers a new ticket only; notifications were delivered in-app
  await jean.reload();
  await expect(jean.getByText("Ce ticket est fermé")).toBeVisible();
  await expect(jean.getByRole("button", { name: "Demander la réouverture" })).toHaveCount(0);
  await jean.getByRole("link", { name: /Notifications/ }).click();
  await expect(jean.getByText(`Votre ticket ${ticketNumber} est fermé.`)).toBeVisible();
  await expect(jean.getByText(`Le support a besoin d'informations supplémentaires pour le ticket ${ticketNumber}.`)).toBeVisible();

  // --- History shows the complete audited lifecycle to support
  await sophie.getByRole("tab", { name: "Historique" }).click();
  for (const label of ["Ticket créé", "Attribué", "Réouverture demandée", "Réouverture acceptée", "Note interne ajoutée"]) {
    await expect(sophie.getByText(label, { exact: true }).first()).toBeVisible();
  }
});

test("requesters cannot open the support back-office", async ({ browser }) => {
  const marie = await signIn(browser, "Marie K.");
  await marie.goto("/support");
  await expect(marie).toHaveURL(/acces-refuse/);
  await expect(marie.getByRole("heading", { name: "Accès refusé" })).toBeVisible();
});

test("an agent can view but not act on a colleague's ticket", async ({ browser }) => {
  const sophie = await signIn(browser, "Sophie N.");
  await sophie.goto("/support/tickets?q=Paiement non revers");
  await sophie.getByRole("link", { name: "Paiement non reversé" }).first().click();
  await expect(sophie.getByText("Lecture seule")).toBeVisible();
  await expect(sophie.getByRole("button", { name: "Envoyer" })).toHaveCount(0);
});

test("@mobile requester flow is usable on a phone", async ({ browser, isMobile }) => {
  test.skip(!isMobile, "mobile project only");
  const kossi = await signIn(browser, "Kossi D.");
  await expect(kossi.getByRole("heading", { name: "Support livraison" })).toBeVisible();
  await kossi.getByRole("link", { name: "Signaler un incident" }).click();
  await kossi.waitForURL(/\/aide\/nouveau/);
  await kossi.getByLabel(/Livraison concernée/).selectOption("LIV-2026-0912");
  await kossi.getByLabel("Sujet").fill(`Client absent ${Date.now()}`);
  await kossi.getByLabel("Description").fill("Client absent, téléphone sans réponse.");
  await kossi.getByRole("button", { name: "Envoyer le signalement" }).click();
  await expect(kossi.getByRole("heading", { name: "Ticket créé avec succès !" })).toBeVisible();
});

test("language and theme can be switched and are remembered", async ({ page }) => {
  await page.goto("/login");
  await expect(page.locator("html")).toHaveAttribute("lang", "fr");
  await page.getByLabel("Langue").selectOption("en");
  await expect(page.locator("html")).toHaveAttribute("lang", "en");
  await expect(page.getByRole("heading", { name: "Demo sign-in" })).toBeVisible();
  await page.getByLabel("Theme").selectOption("dark");
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  // remembered on the next request (cookies), rendered by the server without a flash
  await page.reload();
  await expect(page.locator("html")).toHaveAttribute("lang", "en");
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  await page.getByLabel("Language").selectOption("fr");
  await expect(page.getByRole("heading", { name: "Connexion de démonstration" })).toBeVisible();
});

test("push notifications card and opening a ticket from a push", async ({ browser }) => {
  const jean = await signIn(browser, "Jean K.");
  await jean.goto("/notifications");
  const card = jean.getByRole("region", { name: "Notifications push" });
  await expect(card).toBeVisible();
  // without Firebase web values the card says so; with them it offers to turn push on (docs/guide/firebase.md)
  await expect(
    card.getByText(/pas encore configurées|Activer les notifications push|activées sur cet appareil|bloquées|ne prend pas en charge/).first(),
  ).toBeVisible();

  // a push click opens "/?ticket=<id>", which leads to the ticket screen of the signed-in role
  await jean.goto("/aide/tickets");
  const href = await jean.locator('a[href^="/aide/tickets/"]').first().getAttribute("href");
  const id = href!.split("/").pop()!;
  await jean.goto(`/?ticket=${id}`);
  await jean.waitForURL(new RegExp(`/aide/tickets/${id}$`));
});

test("iteration 6: canned replies in the composer, CSV export and the configurable vocabulary", async ({ browser }) => {
  // an agent takes a new ticket and inserts a canned reply (the text is only inserted, never sent by itself)
  const paul = await signIn(browser, "Paul D.");
  await paul.goto("/support/tickets?assignee=unassigned&open=true");
  await paul.locator("a.ticket-number").first().click();
  await paul.getByRole("button", { name: "Prendre en charge" }).click();
  const canned = paul.getByLabel("Insérer une réponse type");
  await expect(canned).toBeVisible();
  await canned.selectOption({ index: 1 });
  await expect(paul.getByRole("textbox", { name: "Votre message" })).not.toHaveValue("");

  // a manager exports the queue as CSV
  const amina = await signIn(browser, "Amina K.");
  await amina.goto("/support/tickets");
  const download = amina.waitForEvent("download");
  await amina.getByRole("link", { name: "Exporter (CSV)" }).click();
  expect((await download).suggestedFilename()).toMatch(/^tickets-.*\.csv$/);

  // the administrator sees the statuses, the permission matrix and the notification texts
  const admin = await signIn(browser, "Admin Maplenou");
  await admin.goto("/support/admin/flux");
  await expect(admin.getByRole("heading", { name: "Statuts", exact: true })).toBeVisible();
  await expect(admin.getByLabel("Libellé (FR) WAITING_FOR_CUSTOMER")).toHaveValue("En attente du client");
  await admin.goto("/support/admin/permissions");
  await expect(admin.getByRole("columnheader", { name: "Attribué" })).toBeVisible();
  await admin.goto("/support/admin/modeles");
  await expect(admin.getByRole("heading", { name: "Réponses types" })).toBeVisible();
});
