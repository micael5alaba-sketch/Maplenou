"use client";

import { useEffect, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import { useAuth } from "@/lib/auth";
import { Sidebar } from "@/components/Sidebar";
import { usePathname } from "next/navigation";
import { ROUTE_PERMISSIONS, hasPermission, needsOnboarding } from "@/lib/permissions";

export default function DashboardLayout({ children }: { children: ReactNode }) {
  const { user, loading, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (loading) return;
    if (!user) router.replace("/login");
    // Première connexion du personnel : mot de passe + MFA avant tout accès au tableau de bord.
    else if (needsOnboarding(user)) router.replace("/onboarding");
  }, [loading, user, router]);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <Image src="/logo-icon.svg" alt="" width={48} height={48} className="animate-pulse" />
      </div>
    );
  }

  if (!user || needsOnboarding(user)) return null;

  // Accès direct à une page non autorisée pour ce rôle : refus explicite (l'API refuse aussi).
  const required = ROUTE_PERMISSIONS[pathname];
  const forbidden = required !== undefined && !hasPermission(user, required);

  return (
    <div className="flex min-h-screen bg-background">
      <Sidebar userName={user.fullName} onLogout={logout} />
      <main className="flex-1 overflow-y-auto">
        <div className="mx-auto max-w-7xl px-6 py-8">
          {forbidden ? (
            <div className="rounded-2xl bg-surface p-8 text-center ring-1 ring-border">
              <h1 className="mb-2 text-xl font-bold text-foreground">Accès refusé</h1>
              <p className="text-sm text-muted">Votre rôle n&apos;a pas accès à cette page.</p>
            </div>
          ) : (
            children
          )}
        </div>
      </main>
    </div>
  );
}
