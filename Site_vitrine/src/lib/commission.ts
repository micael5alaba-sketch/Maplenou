/**
 * Maplenou fees for sellers (Bakend order/CommissionService + seller/SellerFeeCategory, document "Frais vendeurs et
 * commissions acheteurs", in force since 5 November 2026 — replaces the former 25 % / 22.5 % taken from the seller):
 * - the seller sets a base price; the buyer sees and pays base price + commission (never an extra at payment);
 * - commission rate by seller category, on the base price, delivery excluded: Standard 5 %, High price 3 %;
 *   rounded to the whole FCFA per unit (half up);
 * - the seller receives the base price (the commission is not taken from the payout);
 * - monthly seller fee: 500 FCFA (Standard) / 2 000 FCFA (High price), from the 4th delivered sale, only for a month
 *   with at least one delivered sale, deducted from the payout.
 * Amounts in FCFA (no decimals).
 */
export type FeeCategory = "standard" | "high";

export const MIN_ITEM_PRICE = 500;
/** Standard = items from 500 to 10 000 FCFA; one item above makes the seller "High price". */
export const HIGH_PRICE_THRESHOLD = 10_000;
export const FREE_DELIVERED_SALES = 3;
export const MONTHLY_FEE: Record<FeeCategory, number> = { standard: 500, high: 2000 };

export function commissionRate(category: FeeCategory): number {
  return category === "high" ? 0.03 : 0.05;
}

/** Commission included in the displayed price of ONE unit (whole FCFA). */
export function commissionPerUnit(basePrice: number, category: FeeCategory): number {
  return Math.round(basePrice * commissionRate(category));
}

/** What the buyer sees and pays for one unit, and what the seller receives. */
export function buyerPrice(basePrice: number, category: FeeCategory): {
  rate: number; commission: number; display: number; sellerReceives: number;
} {
  const commission = commissionPerUnit(basePrice, category);
  return { rate: commissionRate(category), commission, display: basePrice + commission, sellerReceives: basePrice };
}

/** Category a seller falls into if this item is in their shop. */
export function categoryForPrice(basePrice: number): FeeCategory {
  return basePrice > HIGH_PRICE_THRESHOLD ? "high" : "standard";
}

/** "12 500 FCFA" / "12,500 FCFA". */
export function fcfa(amount: number, locale: "fr" | "en"): string {
  return `${new Intl.NumberFormat(locale === "fr" ? "fr-FR" : "en-GB", { maximumFractionDigits: 0 }).format(amount)} FCFA`;
}
