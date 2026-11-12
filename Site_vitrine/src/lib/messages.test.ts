import { MESSAGES } from "./messages";

function keys(o: object, prefix = ""): string[] {
  return Object.entries(o).flatMap(([k, v]) =>
    typeof v === "string" ? [`${prefix}${k}`] : keys(v as object, `${prefix}${k}.`));
}

describe("messages", () => {
  it("have the same keys in French and English", () => {
    expect(keys(MESSAGES.en).sort()).toEqual(keys(MESSAGES.fr).sort());
  });

  it("have no empty text", () => {
    for (const locale of ["fr", "en"] as const) {
      for (const k of keys(MESSAGES[locale])) {
        const value = k.split(".").reduce<unknown>((o, p) => (o as Record<string, unknown>)[p], MESSAGES[locale]);
        expect(String(value).trim(), `${locale}.${k}`).not.toBe("");
      }
    }
  });
});
