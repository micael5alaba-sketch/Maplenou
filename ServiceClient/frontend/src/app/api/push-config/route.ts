import { NextResponse } from "next/server";
import { readWebPushConfig } from "@/lib/pushConfig";

export const dynamic = "force-dynamic";

/** Public Firebase web configuration (or `{ configured: false }`), read from the server environment at run time. */
export function GET() {
  return NextResponse.json(readWebPushConfig(process.env), { headers: { "Cache-Control": "no-store" } });
}
