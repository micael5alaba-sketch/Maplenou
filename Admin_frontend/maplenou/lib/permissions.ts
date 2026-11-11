/**
 * Rôles et permissions du back-office — miroir de `Permission.java` / `RolePermissions.java`
 * (backend, source de vérité). Ici, les permissions servent UNIQUEMENT à masquer les menus et
 * les boutons ; chaque route de l'API les revérifie côté serveur.
 */
import type { Role, UserResponse } from "./types";

export type Permission =
  | "DASHBOARD_VIEW"
  | "SELLER_APPLICATIONS_READ"
  | "SELLER_APPLICATIONS_WRITE"
  | "SHOPS_READ"
  | "SHOPS_WRITE"
  | "CATEGORY_CHANGES_READ"
  | "CATEGORY_CHANGES_WRITE"
  | "ORDERS_READ"
  | "ORDERS_WRITE"
  | "RETURNS_READ"
  | "RETURNS_WRITE"
  | "COURIERS_READ"
  | "COURIERS_WRITE"
  | "COURIER_EARNINGS_READ"
  | "DELIVERIES_READ"
  | "DELIVERIES_WRITE"
  | "DELIVERY_PRICING_READ"
  | "DELIVERY_PRICING_WRITE"
  | "REVENUE_READ"
  | "PAYOUTS_READ"
  | "PAYOUTS_PREPARE"
  | "PAYOUTS_APPROVE"
  | "SUPPORT_READ"
  | "SUPPORT_WRITE"
  | "CONTENT_READ"
  | "CONTENT_WRITE"
  | "RESELLERS_READ"
  | "RESELLERS_WRITE"
  | "RESELLERS_SETTINGS"
  | "USERS_READ"
  | "USERS_WRITE"
  | "AUDIT_READ"
  | "STAFF_MANAGE";

export const STAFF_ROLES: Exclude<Role, null | "DELIVERY_AGENT">[] = [
  "ADMIN",
  "DELIVERY_ADMIN",
  "SUPPORT_AGENT",
  "AUDITOR",
];

export function isStaffRole(role: Role | undefined): boolean {
  return role != null && (STAFF_ROLES as string[]).includes(role);
}

export const ROLE_LABELS: Record<string, string> = {
  ADMIN: "Super Admin",
  DELIVERY_ADMIN: "Administrateur des livraisons",
  SUPPORT_AGENT: "Agent support et retraits",
  AUDITOR: "Auditeur (lecture seule)",
  DELIVERY_AGENT: "Livreur",
};

export function hasPermission(user: UserResponse | null | undefined, permission: Permission): boolean {
  return !!user?.permissions?.includes(permission);
}

/** Compte du personnel dont la première connexion n'est pas terminée (mot de passe ou MFA). */
export function needsOnboarding(user: UserResponse | null | undefined): boolean {
  if (!user || !isStaffRole(user.role)) return false;
  // Le Super Admin existant n'est pas forcé à la MFA (voir STAFF_ENFORCE_MFA_SUPER_ADMIN côté
  // backend) : seuls un mot de passe initial à changer ou un rôle limité sans MFA y mènent.
  return user.mustChangePassword || (!user.totpEnabled && user.role !== "ADMIN");
}

/** Permission requise pour afficher chaque page (menus masqués ET accès direct refusé). */
export const ROUTE_PERMISSIONS: Record<string, Permission> = {
  "/": "DASHBOARD_VIEW",
  "/sellers": "SELLER_APPLICATIONS_READ",
  "/shops": "SHOPS_READ",
  "/seller-fees": "CATEGORY_CHANGES_READ",
  "/sub-orders": "ORDERS_READ",
  "/couriers": "COURIERS_WRITE",
  "/courier-documents": "COURIERS_READ",
  "/courier-earnings": "COURIER_EARNINGS_READ",
  "/deliveries": "DELIVERIES_READ",
  "/delivery-pricing": "DELIVERY_PRICING_READ",
  "/revenue": "REVENUE_READ",
  "/payouts": "PAYOUTS_READ",
  "/resellers": "RESELLERS_READ",
  "/reseller-withdrawals": "PAYOUTS_READ",
  "/reseller-commissions": "RESELLERS_READ",
  "/reseller-settings": "RESELLERS_READ",
  "/courier-withdrawals": "PAYOUTS_READ",
  "/promo-codes": "CONTENT_READ",
  "/categories": "CONTENT_READ",
  "/content-pages": "CONTENT_READ",
  "/returns": "RETURNS_READ",
  "/support-tickets": "SUPPORT_READ",
  "/support-conversations": "SUPPORT_READ",
  "/users": "USERS_READ",
  "/audit-log": "AUDIT_READ",
  "/staff": "STAFF_MANAGE",
  "/newsletter": "STAFF_MANAGE",
  "/maintenance": "STAFF_MANAGE",
};
