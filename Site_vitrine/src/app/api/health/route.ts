/** Liveness probe for Docker (HEALTHCHECK) — never calls the Maplenou API. */
export const dynamic = "force-dynamic";

export function GET() {
  return Response.json({ status: "UP" });
}
