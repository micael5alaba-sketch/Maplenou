import { describe, expect, it, vi } from "vitest";

vi.mock("server-only", () => ({}));
const { citiesOf } = await import("./catalog");

const shop = (city: string | null) => ({ id: city ?? "x", name: "B", slug: "b", description: null, logoUrl: null, city, district: null });

describe("citiesOf", () => {
  it("lists each city once, trimmed and sorted for the language", () => {
    expect(citiesOf([shop("Lomé"), shop(" Kara "), shop("Lomé"), shop(null), shop("  "), shop("Aného")], "fr"))
      .toEqual(["Aného", "Kara", "Lomé"]);
  });
});
