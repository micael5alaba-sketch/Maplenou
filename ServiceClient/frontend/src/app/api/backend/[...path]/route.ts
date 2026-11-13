import { NextRequest, NextResponse } from "next/server";
import { BACKEND_URL, getSessionToken } from "@/lib/session";

export const dynamic = "force-dynamic";

// x-forwarded-for: set by the public profile's Caddy; the API trusts it only from the Docker network
const FORWARDED_REQUEST_HEADERS = ["content-type", "accept", "if-match", "if-none-match", "x-forwarded-for"];
// content-length is deliberately NOT forwarded: fetch() decompresses the API's gzip body, so the length announced by
// the API (the compressed size, e.g. 206 bytes) no longer matches the bytes sent to the browser, which then received a
// truncated JSON ("Connexion impossible" on every screen). The body is streamed without a declared length instead.
const FORWARDED_RESPONSE_HEADERS = [
  "content-type",
  "content-disposition",
  "cache-control",
  "x-content-type-options",
  "location",
  "etag",
  "retry-after",
  "x-request-id",
];

const SAFE_REQUEST_ID = /^[A-Za-z0-9._-]{8,64}$/;

/**
 * Same-origin proxy to the Spring Boot API. The access token stays in an httpOnly cookie and is added here,
 * so browser JavaScript never handles it. Only /api/v1/support/** is reachable through this proxy.
 */
async function proxy(req: NextRequest, ctx: { params: Promise<{ path: string[] }> }) {
  const { path } = await ctx.params;
  if (path.some((p) => p === ".." || p.includes("/"))) {
    return NextResponse.json({ status: 400, detail: "Chemin invalide." }, { status: 400 });
  }
  const target = path.join("/");
  if (!target.startsWith("api/v1/support/") && target !== "api/v1/support") {
    return NextResponse.json({ status: 404, detail: "Ressource introuvable." }, { status: 404 });
  }
  // CSRF defence in depth (the cookie is also SameSite=Lax): state-changing calls must come from this origin.
  if (!["GET", "HEAD"].includes(req.method)) {
    const origin = req.headers.get("origin");
    const host = req.headers.get("x-forwarded-host") ?? req.headers.get("host");
    if (origin && host && new URL(origin).host !== host) {
      return NextResponse.json({ status: 403, detail: "Origine non autorisée." }, { status: 403 });
    }
  }
  const token = await getSessionToken();
  if (!token) {
    return NextResponse.json({ status: 401, detail: "Votre session a expiré. Reconnectez-vous." }, { status: 401 });
  }
  const headers = new Headers();
  for (const h of FORWARDED_REQUEST_HEADERS) {
    const v = req.headers.get(h);
    if (v) headers.set(h, v);
  }
  headers.set("authorization", `Bearer ${token}`);
  // one id per call, found in the API logs (X-Request-Id) and returned to the browser for support requests
  const requestId = req.headers.get("x-request-id");
  headers.set("x-request-id", requestId && SAFE_REQUEST_ID.test(requestId) ? requestId : crypto.randomUUID());
  const init: RequestInit & { duplex?: "half" } = { method: req.method, headers, cache: "no-store" };
  if (!["GET", "HEAD"].includes(req.method)) {
    init.body = req.body;
    init.duplex = "half";
  }
  let upstream: Response;
  try {
    upstream = await fetch(`${BACKEND_URL}/${target}${req.nextUrl.search}`, init);
  } catch {
    return NextResponse.json({ status: 503, detail: "Le service support est indisponible." }, { status: 503 });
  }
  const out = new Headers();
  for (const h of FORWARDED_RESPONSE_HEADERS) {
    const v = upstream.headers.get(h);
    if (v) out.set(h, v);
  }
  return new NextResponse(upstream.status === 204 ? null : upstream.body, { status: upstream.status, headers: out });
}

export { proxy as GET, proxy as POST, proxy as PUT, proxy as DELETE, proxy as PATCH };
