import { describe, expect, it } from "vitest";
import { labelsFor } from "./labels";
import { defaultPriority, kindOf, openStatusCodes, priorityCodes, statusCodes } from "./vocabulary";
import type { Vocabulary } from "./types";
import { ERROR_CODES_EN } from "./i18n/messages/errors";

const vocabulary: Vocabulary = {
  statuses: [
    { code: "NEW", kind: "NEW", labelFr: "Nouveau", labelEn: "New", sortOrder: 10, slaPaused: false, system: true, active: true },
    { code: "IN_PROGRESS", kind: "IN_PROGRESS", labelFr: "En cours", labelEn: "In progress", sortOrder: 30, slaPaused: false, system: true, active: true },
    { code: "WAITING_SELLER", kind: "ON_HOLD", labelFr: "En attente du vendeur", labelEn: "Waiting for the seller", sortOrder: 45, slaPaused: true, system: false, active: true },
    { code: "RESOLVED", kind: "RESOLVED", labelFr: "Résolu", labelEn: "Resolved", sortOrder: 50, slaPaused: false, system: true, active: true },
    { code: "OLD_CUSTOM", kind: "IN_PROGRESS", labelFr: "Ancien", labelEn: null, sortOrder: 46, slaPaused: false, system: false, active: false },
  ],
  transitions: [],
  priorities: [
    { code: "LOW", rank: 1, labelFr: "Basse", labelEn: "Low", requesterSelectable: true, defaultPriority: false, system: true, active: true },
    { code: "URGENT_PAYOUT", rank: 3, labelFr: "Versement urgent", labelEn: "Urgent payout", requesterSelectable: false, defaultPriority: false, system: false, active: true },
    { code: "MEDIUM", rank: 2, labelFr: "Moyenne", labelEn: "Medium", requesterSelectable: true, defaultPriority: true, system: true, active: true },
    { code: "CRITICAL", rank: 4, labelFr: "Critique", labelEn: "Critical", requesterSelectable: false, defaultPriority: false, system: true, active: true },
  ],
};

describe("configurable vocabulary (DEC-CS11-02)", () => {
  it("lists active statuses in order and keeps open ones", () => {
    expect(statusCodes(vocabulary)).toEqual(["NEW", "IN_PROGRESS", "WAITING_SELLER", "RESOLVED"]);
    expect(openStatusCodes(vocabulary)).toEqual(["NEW", "IN_PROGRESS", "WAITING_SELLER"]);
    expect(kindOf("WAITING_SELLER", vocabulary)).toBe("ON_HOLD");
    expect(kindOf("WAITING_FOR_CUSTOMER")).toBe("WAITING_REQUESTER");
  });

  it("orders priorities by rank and filters what a requester may choose", () => {
    expect(priorityCodes(vocabulary)).toEqual(["CRITICAL", "URGENT_PAYOUT", "MEDIUM", "LOW"]);
    expect(priorityCodes(vocabulary, true)).toEqual(["MEDIUM", "LOW"]);
    expect(priorityCodes(undefined, true)).not.toContain("CRITICAL");
    expect(defaultPriority(vocabulary)).toBe("MEDIUM");
  });

  it("labels custom codes in each language and falls back to the code", () => {
    expect(labelsFor("fr", vocabulary).status.WAITING_SELLER).toBe("En attente du vendeur");
    expect(labelsFor("en", vocabulary).status.WAITING_SELLER).toBe("Waiting for the seller");
    expect(labelsFor("en", vocabulary).status.OLD_CUSTOM).toBe("Ancien"); // no English label yet
    expect(labelsFor("en", vocabulary).priority.URGENT_PAYOUT).toBe("Urgent payout");
    expect(labelsFor("fr").status.UNKNOWN_CODE).toBe("UNKNOWN_CODE");
    expect(labelsFor("fr").status.WAITING_FOR_CUSTOMER).toBe("En attente du client");
  });

  it("translates the error codes of the new features", () => {
    for (const code of ["AI_UNAVAILABLE", "ATTACHMENT_INFECTED", "MAPLENOU_REFUSED", "NO_ORDER_LINKED", "INVALID_WORKFLOW",
      "TOO_MANY_REQUESTS", "STATUS_IN_USE"]) {
      expect(ERROR_CODES_EN[code], code).toBeTruthy();
    }
  });
});
