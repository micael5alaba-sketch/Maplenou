import { describe, expect, it } from "vitest";
import { validateFiles } from "./attachments";

const MB = 1024 * 1024;

describe("client-side attachment checks mirror DEC-003", () => {
  it("accepts allowed formats", () => {
    expect(validateFiles([{ name: "a.JPG", size: 10 }, { name: "b.pdf", size: 10 }, { name: "c.webp", size: 1 }])).toBeNull();
  });
  it("rejects other formats", () => {
    expect(validateFiles([{ name: "virus.exe", size: 10 }])).toMatch(/Format non autorisé/);
    expect(validateFiles([{ name: "noextension", size: 10 }])).toMatch(/Format non autorisé/);
  });
  it("rejects files over 10 MB", () => {
    expect(validateFiles([{ name: "a.png", size: 10 * MB + 1 }])).toMatch(/dépasse 10 Mo/);
    expect(validateFiles([{ name: "a.png", size: 10 * MB }])).toBeNull();
  });
  it("rejects more than 5 files", () => {
    expect(validateFiles(Array.from({ length: 6 }, (_, i) => ({ name: `${i}.png`, size: 1 })))).toMatch(/Maximum 5/);
  });
  it("rejects when the ticket would exceed 50 MB", () => {
    expect(validateFiles([{ name: "a.pdf", size: 2 * MB }], 49 * MB)).toMatch(/50 Mo/);
  });
  it("rejects empty files", () => {
    expect(validateFiles([{ name: "a.pdf", size: 0 }])).toMatch(/vide/);
  });
});

describe("attachment messages follow the language", () => {
  it("words the same rule in English", () => {
    expect(validateFiles([{ name: "a.png", size: 10 * MB + 1 }], 0, "en")).toBe("The file “a.png” exceeds 10 MB.");
    expect(validateFiles([{ name: "virus.exe", size: 1 }], 0, "en")).toMatch(/not allowed/);
  });
});
