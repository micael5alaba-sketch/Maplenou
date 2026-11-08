import { describe, expect, it } from "vitest";
import { negotiateLocale } from "./config";
import { format, plural } from "./define";
import { localizeNotification } from "./messages/notifications";
import { common } from "./messages/common";

describe("i18n helpers", () => {
  it("negotiates the language from Accept-Language", () => {
    expect(negotiateLocale(null)).toBe("fr");
    expect(negotiateLocale("en-GB,en;q=0.9,fr;q=0.8")).toBe("en");
    expect(negotiateLocale("de-DE,fr;q=0.5,en;q=0.7")).toBe("en");
    expect(negotiateLocale("de-DE")).toBe("fr");
  });

  it("formats placeholders and plurals", () => {
    expect(format("Page {page} / {pages}", { page: 1, pages: 3 })).toBe("Page 1 / 3");
    expect(format("{missing}")).toBe("{missing}");
    expect(plural("fr", 0, "{count} ticket", "{count} tickets")).toBe("0 ticket");
    expect(plural("en", 0, "{count} ticket", "{count} tickets")).toBe("0 tickets");
    expect(plural("en", 1, "{count} ticket", "{count} tickets")).toBe("1 ticket");
  });

  it("has the same keys in both languages", () => {
    expect(Object.keys(common.en).sort()).toEqual(Object.keys(common.fr).sort());
  });

  it("renders notifications in English from language-neutral facts", () => {
    const n = {
      id: "1",
      ticketId: "t",
      type: "STATUS_CHANGED",
      title: "Statut mis à jour",
      body: "Le statut de votre ticket TKT-2026-000001 a changé : En cours.",
      ticketNumber: "TKT-2026-000001",
      detail: "IN_PROGRESS",
      createdAt: "2026-10-08T12:00:00Z",
      readAt: null,
    };
    expect(localizeNotification(n, "fr").body).toBe(n.body);
    expect(localizeNotification(n, "en")).toEqual({
      title: "Status updated",
      body: "The status of your ticket TKT-2026-000001 changed: In progress.",
    });
    // older notifications without the facts keep the original text
    expect(localizeNotification({ ...n, ticketNumber: null }, "en").title).toBe("Statut mis à jour");
  });
});
