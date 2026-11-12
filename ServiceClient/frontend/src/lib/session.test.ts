import { describe, expect, it, vi } from "vitest";

vi.mock("server-only", () => ({}));
vi.mock("next/headers", () => ({ cookies: vi.fn() }));

const { backendUrl } = await import("./session");

describe("backendUrl", () => {
  it("prefers BACKEND_URL and drops a trailing slash", () => {
    expect(backendUrl({ BACKEND_URL: "https://maplenou-support-api.onrender.com/", BACKEND_HOSTPORT: "x:1" }))
      .toBe("https://maplenou-support-api.onrender.com");
  });
  it("uses the private host:port given by Render", () => {
    expect(backendUrl({ BACKEND_HOSTPORT: "maplenou-support-api:8080" })).toBe("http://maplenou-support-api:8080");
  });
  it("falls back to the local API", () => {
    expect(backendUrl({ BACKEND_URL: " " })).toBe("http://localhost:8081");
  });
});
