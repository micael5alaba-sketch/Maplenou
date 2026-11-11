import { redirect } from "next/navigation";
import { SessionProvider } from "@/components/session";
import { SupportShell } from "@/components/SupportShell";
import { getSession } from "@/lib/session";
import { isSupportRole } from "@/lib/labels";

export const dynamic = "force-dynamic";

export default async function SupportLayout({ children }: { children: React.ReactNode }) {
  const session = await getSession();
  if (!session) redirect("/login");
  if (!isSupportRole(session.role)) redirect("/acces-refuse");
  return (
    <SessionProvider session={{ id: session.id, role: session.role, name: session.name }}>
      <SupportShell>{children}</SupportShell>
    </SessionProvider>
  );
}
