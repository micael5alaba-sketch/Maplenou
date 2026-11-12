import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";

/**
 * How people sign in (read at run time): `maplenou` = phone number + password of the Maplenou account
 * (backend MAPLENOU_LOGIN_ENABLED=true), `demo` = demo personas (backend demo profile).
 */
export function GET() {
  const mode = process.env.AUTH_MODE === "maplenou" ? "maplenou" : "demo";
  return NextResponse.json({ mode }, { headers: { "Cache-Control": "no-store" } });
}
