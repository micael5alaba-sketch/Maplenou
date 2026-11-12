import { afterEach, describe, expect, it, vi } from "vitest";
import { NextRequest } from "next/server";

vi.mock("server-only", () => ({}));
vi.mock("next/headers", () => ({ cookies: vi.fn() }));

const { POST } = await import("./route");

function supportToken(roles: string[]) {
  const payload = Buffer.from(
    JSON.stringify({ sub: "u-1", name: "Super Admin", roles, exp: Math.floor(Date.now() / 1000) + 3600 }),
  ).toString("base64url");
  return `h.${payload}.s`;
}

function request(body: unknown, origin = "http://localhost:3001") {
  return new NextRequest("http://localhost:3001/api/session", {
    method: "POST",
    headers: { "Content-Type": "application/json", origin, host: "localhost:3001" },
    body: JSON.stringify(body),
  });
}

afterEach(() => vi.unstubAllGlobals());

describe("POST /api/session with a Maplenou token (arrival from the Maplenou back office)", () => {
  it("exchanges the token and opens the session without any password", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ accessToken: supportToken(["ADMINISTRATOR"]) }), { status: 200 }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const res = await POST(request({ coreAccessToken: "core-token" }));

    expect(res.status).toBe(200);
    expect((await res.json()).role).toBe("ADMINISTRATOR");
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain("/api/v1/auth/maplenou/exchange");
    expect(JSON.parse(init.body)).toEqual({ coreAccessToken: "core-token" });
    expect(res.cookies.get("maplenou_session")?.value).toContain(".");
  });

  it("refuses an invalid Maplenou token and sets no session", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: "INVALID_CORE_TOKEN", detail: "Session invalide." }), { status: 401 })),
    );
    const res = await POST(request({ coreAccessToken: "bad" }));
    expect(res.status).toBe(401);
    expect(res.cookies.get("maplenou_session")).toBeUndefined();
  });

  it("refuses a request that does not come from this site (login CSRF defence)", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    const res = await POST(request({ coreAccessToken: "core-token" }, "https://evil.example"));
    expect(res.status).toBe(403);
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
