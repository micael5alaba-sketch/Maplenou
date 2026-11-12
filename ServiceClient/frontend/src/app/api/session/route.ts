import { NextRequest, NextResponse } from "next/server";
import { BACKEND_URL, SESSION_COOKIE, decodeToken, getSession } from "@/lib/session";

export const dynamic = "force-dynamic";

/** Short-lived cookie holding the core's "MFA pending" token between the password step and the code step. */
const MFA_COOKIE = "maplenou_mfa";

export async function GET() {
  const user = await getSession();
  return user ? NextResponse.json(user) : NextResponse.json({ status: 401 }, { status: 401 });
}

type Body = { userId?: unknown; phoneNumber?: unknown; password?: unknown; code?: unknown; coreAccessToken?: unknown };

/**
 * Sign-in, four forms:
 * - `{ phoneNumber, password }` — Maplenou account (backend checks it with the Maplenou platform);
 * - `{ code }` — 6-digit two-factor code after the previous step answered `mfaRequired`;
 * - `{ coreAccessToken }` — the person arrives from the Maplenou back office with a valid Maplenou token (no password);
 * - `{ userId }` — demo persona (backend demo profile only).
 * The resulting support token is stored in an httpOnly cookie; the browser never sees it.
 */
export async function POST(req: NextRequest) {
  // login CSRF defence (the cookie is also SameSite=Lax): the form must come from this origin
  const origin = req.headers.get("origin");
  const host = req.headers.get("host");
  if (origin && host && new URL(origin).host !== host) {
    return NextResponse.json({ status: 403, detail: "Origine refusée." }, { status: 403 });
  }
  const body = (await req.json().catch(() => ({}))) as Body;
  const forwardedFor = req.headers.get("x-forwarded-for") ?? "";
  let target: string;
  let payload: unknown;
  if (typeof body.phoneNumber === "string" && typeof body.password === "string") {
    target = "/api/v1/auth/maplenou/login";
    payload = { phoneNumber: body.phoneNumber, password: body.password };
  } else if (typeof body.code === "string") {
    const mfaToken = req.cookies.get(MFA_COOKIE)?.value;
    if (!mfaToken) {
      return NextResponse.json({ status: 401, code: "MFA_EXPIRED", detail: "Recommencez la connexion." }, { status: 401 });
    }
    target = "/api/v1/auth/maplenou/mfa";
    payload = { mfaToken, code: body.code };
  } else if (typeof body.coreAccessToken === "string") {
    target = "/api/v1/auth/maplenou/exchange";
    payload = { coreAccessToken: body.coreAccessToken };
  } else if (typeof body.userId === "string") {
    target = "/api/v1/dev/token";
    payload = { userId: body.userId };
  } else {
    return NextResponse.json({ status: 400, detail: "Identifiants manquants." }, { status: 400 });
  }

  const res = await fetch(`${BACKEND_URL}${target}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...(forwardedFor ? { "X-Forwarded-For": forwardedFor } : {}) },
    body: JSON.stringify(payload),
    cache: "no-store",
  }).catch(() => null);
  if (!res) {
    return NextResponse.json({ status: 503, code: "CORE_UNAVAILABLE", detail: "Service indisponible." }, { status: 503 });
  }
  const data = (await res.json().catch(() => ({}))) as {
    accessToken?: string;
    mfaRequired?: boolean;
    mfaToken?: string;
    code?: string;
    detail?: string;
  };
  if (!res.ok) {
    return NextResponse.json({ status: res.status, code: data.code, detail: data.detail }, { status: res.status });
  }
  if (data.mfaRequired && data.mfaToken) {
    const response = NextResponse.json({ mfaRequired: true });
    response.cookies.set(MFA_COOKIE, data.mfaToken, {
      httpOnly: true,
      sameSite: "lax",
      secure: process.env.SESSION_COOKIE_SECURE === "true",
      path: "/api/session",
      maxAge: 5 * 60,
    });
    return response;
  }
  const user = data.accessToken ? decodeToken(data.accessToken) : null;
  if (!data.accessToken || !user) {
    return NextResponse.json({ status: 502, detail: "Jeton invalide." }, { status: 502 });
  }
  const response = NextResponse.json(user);
  response.cookies.set(SESSION_COOKIE, data.accessToken, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.SESSION_COOKIE_SECURE === "true",
    path: "/",
    expires: new Date(user.expiresAt),
  });
  response.cookies.set(MFA_COOKIE, "", { httpOnly: true, path: "/api/session", maxAge: 0 });
  return response;
}

export async function DELETE() {
  const response = NextResponse.json({ ok: true });
  response.cookies.set(SESSION_COOKIE, "", { httpOnly: true, path: "/", maxAge: 0 });
  return response;
}
