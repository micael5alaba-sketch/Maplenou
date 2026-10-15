"use client";

import { useEffect, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import { useAuth } from "@/lib/auth";
import { Sidebar } from "@/components/Sidebar";

export default function DashboardLayout({ children }: { children: ReactNode }) {
  const { user, loading, logout } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) router.replace("/login");
  }, [loading, user, router]);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <Image src="/logo-icon.svg" alt="" width={48} height={48} className="animate-pulse" />
      </div>
    );
  }

  if (!user) return null;

  return (
    <div className="flex min-h-screen bg-background">
      <Sidebar userName={user.fullName} onLogout={logout} />
      <main className="flex-1 overflow-y-auto">
        <div className="mx-auto max-w-7xl px-6 py-8">{children}</div>
      </main>
    </div>
  );
}
