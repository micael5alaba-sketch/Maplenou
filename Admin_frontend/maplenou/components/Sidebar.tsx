"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { useTheme } from "@/lib/theme";
import { useAuth } from "@/lib/auth";
import { useWorkBadges } from "@/lib/workBadges";
import { useEffect, useState } from "react";
import { ROLE_LABELS, ROUTE_PERMISSIONS, hasPermission } from "@/lib/permissions";

interface NavItem {
  href: string;
  label: string;
}

interface NavGroup {
  title: string;
  items: NavItem[];
}

// Chaque page exige une permission (voir ROUTE_PERMISSIONS) : un menu non autorisé est masqué,
// et l'API refuse de toute façon l'action côté serveur.
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
      { href: "/deliveries", label: "Livraisons" },
      { href: "/delivery-pricing", label: "Tarification livraison" },
    ],
  },
  {
    title: "Argent",
    items: [
      { href: "/revenue", label: "Revenus plateforme" },
      { href: "/payouts", label: "Retraits vendeurs" },
      { href: "/courier-withdrawals", label: "Retraits livreurs" },
      { href: "/promo-codes", label: "Codes promo" },
    ],
  },
  {
    title: "Partenaires revendeurs",
    items: [
      { href: "/resellers", label: "Partenaires" },
      { href: "/reseller-commissions", label: "Commissions partenaires" },
      { href: "/reseller-withdrawals", label: "Retraits partenaires" },
      { href: "/reseller-settings", label: "Paramètres du programme" },
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
      { href: "/staff", label: "Personnel" },
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
  const { user, realUser, viewAsRole, setViewAsRole } = useAuth();
  const { badges, total } = useWorkBadges();
  const [alertPermission, setAlertPermission] = useState<NotificationPermission | "unsupported">("unsupported");
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (typeof Notification !== "undefined") setAlertPermission(Notification.permission);
  }, []);
  const isSuperAdmin = !!realUser?.permissions.includes("STAFF_MANAGE");

  const visibleGroups = NAV_GROUPS.map((group) => ({
    ...group,
    items: group.items.filter((item) => {
      const required = ROUTE_PERMISSIONS[item.href];
      return !required || hasPermission(user, required);
    }),
  })).filter((group) => group.items.length > 0);

  return (
    <aside className="flex h-screen w-64 flex-shrink-0 flex-col border-r border-border bg-surface">
      <div className="flex items-center gap-2 px-5 py-5">
        <Image src="/logo.svg" alt="Maplenou" width={140} height={41} />
      </div>

      <nav className="flex-1 overflow-y-auto px-3 pb-4">
        {visibleGroups.map((group) => (
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
                    <span className="flex items-center justify-between gap-2">
                      <span>{item.label}</span>
                      {(badges[item.href] ?? 0) > 0 && (
                        <span
                          title="Travail en attente"
                          className="min-w-[20px] rounded-full bg-error px-1.5 py-0.5 text-center text-[11px] font-bold leading-none text-white"
                        >
                          {badges[item.href] > 99 ? "99+" : badges[item.href]}
                        </span>
                      )}
                    </span>
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
          <p className="text-xs text-muted">{ROLE_LABELS[user?.role ?? ""] ?? "Personnel"} — voir mon profil</p>
        </Link>

        {total > 0 && (
          <p className="mb-2 rounded-lg bg-error/10 px-2 py-1.5 text-xs font-semibold text-error">
            {total} chose{total > 1 ? "s" : ""} à traiter
          </p>
        )}
        {alertPermission === "default" && (
          <button
            onClick={async () => setAlertPermission(await Notification.requestPermission())}
            className="mb-2 w-full rounded-lg border border-border py-1.5 text-xs font-semibold text-foreground transition hover:bg-input-fill"
          >
            Activer les alertes du navigateur
          </button>
        )}
        {isSuperAdmin && (
          <div className="mb-2">
            <label className="mb-1 block text-[11px] font-semibold uppercase tracking-wide text-muted">Voir en tant que</label>
            <select
              value={viewAsRole ?? ""}
              onChange={(e) => void setViewAsRole((e.target.value || null) as typeof viewAsRole)}
              className="w-full rounded-lg border border-border bg-input-fill px-2 py-1.5 text-xs text-foreground"
            >
              <option value="">Mon tableau de bord (Super Admin)</option>
              <option value="DELIVERY_ADMIN">{ROLE_LABELS.DELIVERY_ADMIN}</option>
              <option value="SUPPORT_AGENT">{ROLE_LABELS.SUPPORT_AGENT}</option>
              <option value="AUDITOR">{ROLE_LABELS.AUDITOR}</option>
            </select>
          </div>
        )}

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
