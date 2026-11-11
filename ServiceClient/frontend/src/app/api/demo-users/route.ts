import { NextResponse } from "next/server";
import { BACKEND_URL } from "@/lib/session";

export const dynamic = "force-dynamic";

export async function GET() {
  const res = await fetch(`${BACKEND_URL}/api/v1/dev/users`, { cache: "no-store" }).catch(() => null);
  if (!res) return NextResponse.json({ status: 503, detail: "Service indisponible." }, { status: 503 });
  if (!res.ok) return NextResponse.json([], { status: res.status });
  return NextResponse.json(await res.json());
}
