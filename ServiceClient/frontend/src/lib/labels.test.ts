import { describe, expect, it } from "vitest";
import {
  formatBusinessMinutes,
  formatBytes,
  formatMoney,
  historyValueLabel,
  isSupervisor,
  isSupportRole,
  labelsFor,
  timeAgo,
} from "./labels";
import { statusLabels } from "./i18n/messages/common";

const { status: STATUS_LABELS, priority: PRIORITY_LABELS } = labelsFor("fr");

describe("vocabulary (AC-DS-003, AC-DS-004)", () => {
  it("uses the approved French status and priority labels", () => {
    expect(Object.keys(STATUS_LABELS)).toEqual(["NEW", "ASSIGNED", "IN_PROGRESS", "WAITING_FOR_CUSTOMER", "RESOLVED", "CLOSED"]);
    expect(STATUS_LABELS.WAITING_FOR_CUSTOMER).toBe("En attente du client");
    expect(PRIORITY_LABELS.CRITICAL).toBe("Critique");
  });

  it("labels history values by action", () => {
    expect(historyValueLabel("STATUS_CHANGED", "IN_PROGRESS")).toBe("En cours");
    expect(historyValueLabel("PRIORITY_CHANGED", "HIGH")).toBe("Élevée");
    expect(historyValueLabel("ASSIGNED", "Sophie N.")).toBe("Sophie N.");
    expect(historyValueLabel("ASSIGNED", null)).toBeNull();
  });
});

describe("roles", () => {
  it("distinguishes support staff and supervisors", () => {
    expect(isSupportRole("CUSTOMER")).toBe(false);
    expect(isSupportRole("SUPPORT_AGENT")).toBe(true);
    expect(isSupervisor("SUPPORT_AGENT")).toBe(false);
    expect(isSupervisor("SUPPORT_MANAGER")).toBe(true);
    expect(isSupervisor("ADMINISTRATOR")).toBe(true);
  });
});

describe("formatting", () => {
  it("formats FCFA amounts", () => {
    expect(formatMoney(48500).replace(/\s/g, " ")).toBe("48 500 FCFA");
    expect(formatMoney(null)).toBe("—");
  });

  it("formats sizes", () => {
    expect(formatBytes(500)).toBe("500 o");
    expect(formatBytes(2048)).toBe("2 Ko");
    expect(formatBytes(1.2 * 1024 * 1024)).toBe("1,2 Mo");
  });

  it("formats business durations (1 day = 10 h)", () => {
    expect(formatBusinessMinutes(45)).toBe("45 min");
    expect(formatBusinessMinutes(85)).toBe("1 h 25");
    // business-hours mode: a day is 10 h
    expect(formatBusinessMinutes(600, "fr", 600)).toBe("1 j");
    expect(formatBusinessMinutes(780, "fr", 600)).toBe("1 j 3 h");
    // 24/7 mode (default): a day is 24 h
    expect(formatBusinessMinutes(600)).toBe("10 h");
    expect(formatBusinessMinutes(1440 + 180)).toBe("1 j 3 h");
    expect(formatBusinessMinutes(null)).toBe("—");
  });

  it("formats relative time", () => {
    const now = new Date("2026-10-08T12:00:00Z");
    expect(timeAgo("2026-10-08T11:59:40Z", now)).toBe("à l'instant");
    expect(timeAgo("2026-10-08T11:54:00Z", now)).toBe("il y a 6 min");
    expect(timeAgo("2026-10-08T09:00:00Z", now)).toBe("il y a 3 h");
    expect(timeAgo("2026-10-06T12:00:00Z", now)).toBe("il y a 2 j");
  });
});

describe("English (language switch)", () => {
  it("has a translation for every French label", () => {
    expect(Object.keys(statusLabels.en)).toEqual(Object.keys(statusLabels.fr));
    const en = labelsFor("en");
    expect(en.status.WAITING_FOR_CUSTOMER).toBe("Waiting for customer");
    expect(en.priority.CRITICAL).toBe("Critical");
    expect(en.historyValue("STATUS_CHANGED", "RESOLVED")).toBe("Resolved");
  });

  it("formats in English", () => {
    const now = new Date("2026-10-08T12:00:00Z");
    expect(timeAgo("2026-10-08T11:54:00Z", now, "en")).toBe("6 min ago");
    expect(formatBytes(1.2 * 1024 * 1024, "en")).toBe("1.2 MB");
    expect(formatBusinessMinutes(780, "en", 600)).toBe("1 d 3 h");
    expect(formatBusinessMinutes(1620, "en")).toBe("1 d 3 h");
    expect(formatMoney(48500, "XOF", "en")).toBe("48,500 FCFA");
  });
});
