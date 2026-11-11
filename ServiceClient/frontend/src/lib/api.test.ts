import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, api, toQuery } from "./api";

afterEach(() => vi.unstubAllGlobals());

describe("toQuery", () => {
  it("skips empty values and repeats arrays", () => {
    expect(toQuery({ q: "", status: ["NEW", "ASSIGNED"], page: 0, x: undefined, open: true })).toBe(
      "?status=NEW&status=ASSIGNED&page=0&open=true",
    );
    expect(toQuery({})).toBe("");
  });
});

describe("api client", () => {
  it("calls the same-origin proxy and returns JSON", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ ok: 1 }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    await expect(api.get("/tickets")).resolves.toEqual({ ok: 1 });
    expect(fetchMock.mock.calls[0][0]).toBe("/api/backend/api/v1/support/tickets");
  });

  it("turns RFC 9457 problems into ApiError with the server message", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ status: 422, code: "CRITICAL_PRIORITY_RESERVED", detail: "Réservé au support", rule: "BR-036" }),
      { status: 422 },
    )));
    const err = (await api.post("/tickets", {}).catch((e: unknown) => e)) as ApiError;
    expect(err).toBeInstanceOf(ApiError);
    expect(err.status).toBe(422);
    expect(err.code).toBe("CRITICAL_PRIORITY_RESERVED");
    expect(err.message).toBe("Réservé au support");
  });

  it("gives a readable message on network failure", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("failed")));
    const err = (await api.get("/tickets").catch((e: unknown) => e)) as ApiError;
    expect(err.status).toBe(0);
    expect(err.message).toMatch(/Connexion impossible/);
  });

  it("uses a default message for non-JSON errors", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("oops", { status: 503 })));
    const err = (await api.get("/x").catch((e: unknown) => e)) as ApiError;
    expect(err.message).toMatch(/indisponible/);
  });
});
