import { redirect } from "next/navigation";
import { RequesterShell } from "@/components/RequesterShell";
import { SessionProvider } from "@/components/session";
import { getSession } from "@/lib/session";
import { isSupportRole } from "@/lib/labels";

export const dynamic = "force-dynamic";

export default async function RequesterLayout({ children }: { children: React.ReactNode }) {
  const session = await getSession();
  if (!session) redirect("/login");
  if (isSupportRole(session.role) && session.role !== "ADMINISTRATOR") redirect("/support");
  return (
    <SessionProvider session={{ id: session.id, role: session.role, name: session.name }}>
      <RequesterShell>{children}</RequesterShell>
    </SessionProvider>
  );
}
