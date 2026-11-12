import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { isSupportRole } from "@/lib/labels";

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Entry point: sends each role to its home, or to a ticket when opened from a push notification (?ticket=). */
export default async function Home({ searchParams }: { searchParams: Promise<{ ticket?: string | string[] }> }) {
  const session = await getSession();
  if (!session) redirect("/login");
  const support = isSupportRole(session.role);
  const { ticket } = await searchParams;
  if (typeof ticket === "string" && UUID.test(ticket)) {
    redirect(support ? `/support/tickets/${ticket}` : `/aide/tickets/${ticket}`);
  }
  redirect(support ? "/support" : "/aide");
}
