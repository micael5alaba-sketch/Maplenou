"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { useTheme } from "@/lib/theme";

interface NavItem {
  href: string;
  label: string;
}

interface NavGroup {
  title: string;
  items: NavItem[];
}

const NAV_GROUPS: NavGroup[] = [
  {
    title: "Vue d'ensemble",
    items: [{ href: "/", label: "Tableau de bord" }],
  },
  {
    title: "Vendeurs & boutiques",
    items: [
      { href: "/sellers", label: "Candidatures vendeurs" },
      { href: "/shops", label: "Boutiques" },
      { href: "/seller-fees", label: "Changements de catégorie" },
    ],
  },
  {
    title: "Livraison",
    items: [
      { href: "/sub-orders", label: "Commandes" },
      { href: "/couriers", label: "Créer un livreur" },
      { href: "/courier-documents", label: "Documents livreurs" },
      { href: "/courier-earnings", label: "Gains livreurs" },
      { href: "/deliveries", label: "Livraisons en attente" },
      { href: "/delivery-pricing", label: "Tarification livraison" },
    ],
  },
  {
    title: "Argent",
    items: [
      { href: "/payouts", label: "Reversements vendeurs" },
      { href: "/promo-codes", label: "Codes promo" },
    ],
  },
  {
    title: "Catalogue & contenu",
    items: [
      { href: "/categories", label: "Catégories" },
      { href: "/content-pages", label: "Pages (CMS)" },
      { href: "/returns", label: "Retours" },
    ],
  },
  {
    title: "Support & comptes",
    items: [
      { href: "/support-tickets", label: "Tickets support" },
      { href: "/support-conversations", label: "Conversations support" },
      { href: "/users", label: "Utilisateurs" },
      { href: "/audit-log", label: "Journal d'audit" },
      { href: "/newsletter", label: "Newsletter" },
    ],
  },
  {
    title: "Système",
    items: [{ href: "/maintenance", label: "Maintenance" }],
  },
];

export function Sidebar({ userName, onLogout }: { userName: string; onLogout: () => void }) {
  const pathname = usePathname();
  const { theme, setTheme } = useTheme();

  return (
    <aside className="flex h-screen w-64 flex-shrink-0 flex-col border-r border-border bg-surface">
      <div className="flex items-center gap-2 px-5 py-5">
        <Image src="/logo.svg" alt="Maplenou" width={140} height={41} />
      </div>

      <nav className="flex-1 overflow-y-auto px-3 pb-4">
        {NAV_GROUPS.map((group) => (
          <div key={group.title} className="mb-7">
            <p className="mb-2.5 px-3 text-[11px] font-bold uppercase tracking-wide text-muted">
              {group.title}
            </p>
            <div className="flex flex-col gap-1.5">
              {group.items.map((item) => {
                const active = pathname === item.href;
                const danger = item.href === "/maintenance";
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={`rounded-lg px-3 py-2.5 text-sm font-medium transition ${
                      active
                        ? danger
                          ? "bg-error text-white"
                          : "bg-primary text-white"
                        : danger
                          ? "text-error hover:bg-error/10"
                          : "text-foreground hover:bg-input-fill"
                    }`}
                  >
                    {item.label}
                  </Link>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      <div className="border-t border-border px-4 py-4">
        <Link
          href="/profile"
          className={`mb-2 block rounded-lg px-1 py-1 transition hover:bg-input-fill ${
            pathname === "/profile" ? "bg-input-fill" : ""
          }`}
        >
          <p className="truncate text-sm font-semibold text-foreground">{userName}</p>
          <p className="text-xs text-muted">Administrateur — voir mon profil</p>
        </Link>

        <div className="mb-2 flex gap-1 rounded-lg bg-input-fill p-1">
          {[
            { value: "light" as const, label: "Clair" },
            { value: "dark" as const, label: "Sombre" },
            { value: "system" as const, label: "Auto" },
          ].map((opt) => (
            <button
              key={opt.value}
              onClick={() => setTheme(opt.value)}
              className={`flex-1 rounded-md py-1 text-xs font-semibold transition ${
                theme === opt.value ? "bg-surface text-foreground shadow-sm" : "text-muted"
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>

        <button
          onClick={onLogout}
          className="w-full rounded-lg border border-border py-1.5 text-xs font-semibold text-foreground transition hover:bg-input-fill"
        >
          Déconnexion
        </button>
      </div>
    </aside>
  );
}
