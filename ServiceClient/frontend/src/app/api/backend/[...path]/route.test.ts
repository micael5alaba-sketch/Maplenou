import { afterEach, describe, expect, it, vi } from "vitest";
import { NextRequest } from "next/server";

vi.mock("server-only", () => ({}));
vi.mock("next/headers", () => ({
  cookies: vi.fn().mockResolvedValue({ get: () => ({ value: "support-token" }) }),
}));

const { GET } = await import("./route");

afterEach(() => vi.unstubAllGlobals());

function call(path: string) {
  const req = new NextRequest(`http://localhost:3001/api/backend/${path}`, { headers: { accept: "*/*" } });
  return GET(req, { params: Promise.resolve({ path: path.split("/") }) });
}

describe("proxy to the support API", () => {
  it("does not forward the upstream content-length (the body is decompressed by fetch, so the length would be wrong)", async () => {
    const json = JSON.stringify({ openCount: 0, openByCategory: [], recent: [], note: "x".repeat(400) });
    // the API announces the COMPRESSED size (206 bytes) while the stream carries the full JSON
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response(json, { status: 200, headers: { "content-type": "application/json", "content-length": "206" } })),
    );

    const res = await call("api/v1/support/dashboard");

    expect(res.status).toBe(200);
    expect(res.headers.get("content-length")).toBeNull();
    expect(res.headers.get("content-type")).toBe("application/json");
    expect(await res.text()).toBe(json); // the whole body, never cut at 206 bytes
  });

  it("only reaches /api/v1/support/**", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    const res = await call("actuator/health");
    expect(res.status).toBe(404);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("answers 401 without a session", async () => {
    const { cookies } = await import("next/headers");
    vi.mocked(cookies).mockResolvedValueOnce({ get: () => undefined } as never);
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    const res = await call("api/v1/support/dashboard");
    expect(res.status).toBe(401);
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
