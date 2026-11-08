import "server-only";
import { siteConfig } from "./config";

/**
 * Live data from the public catalogue of the Maplenou API (no login needed): root categories, a few products, the
 * delivery price tiers and the approved shops. Cached by Next.js for 5 minutes (bounded: four small JSON documents)
 * so a visit never waits for the API; if the API is down, the page shows its static content instead.
 */
const REVALIDATE_SECONDS = 300;
const TIMEOUT_MS = 2500;

export interface Category { id: string; name: string; slug: string; imageUrl: string | null; active: boolean }
export interface Product {
  id: string; name: string; slug: string; basePrice: number; oldPrice: number | null;
  /** price the buyer pays (base + commission included); absent on a Bakend older than 5 Nov 2026 */
  displayPrice?: number | null; displayOldPrice?: number | null; shopName: string;
  thumbnailUrl: string | null; averageRating?: number; reviewCount?: number;
}
export interface Shop {
  id: string; name: string; slug: string; description: string | null; logoUrl: string | null;
  city: string | null; district: string | null;
}
export interface DeliveryTier { maxDistanceKm: number; price: number }

async function getJson<T>(path: string): Promise<T | null> {
  try {
    const res = await fetch(`${siteConfig.apiUrl}${path}`, {
      next: { revalidate: REVALIDATE_SECONDS },
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    return res.ok ? ((await res.json()) as T) : null;
  } catch {
    return null;
  }
}

export async function getCategories(): Promise<Category[]> {
  const list = await getJson<Category[]>("/api/categories");
  return (list ?? []).filter((c) => c.active).slice(0, 8);
}

export async function getProducts(): Promise<Product[]> {
  const page = await getJson<{ content: Product[] }>("/api/products?size=8");
  return (page?.content ?? []).slice(0, 8);
}

export async function getDeliveryTiers(): Promise<DeliveryTier[]> {
  const tiers = await getJson<DeliveryTier[]>("/api/delivery-pricing-tiers");
  return (tiers ?? [])
    .filter((t) => t.maxDistanceKm != null && t.price != null)
    .sort((a, b) => Number(a.maxDistanceKm) - Number(b.maxDistanceKm))
    .slice(0, 6);
}

/** Approved shops (Bakend lists only APPROVED ones), at most 100: null when the API does not answer. */
export async function getShops(): Promise<Shop[] | null> {
  const page = await getJson<{ content: Shop[] }>("/api/shops?size=100&sort=name");
  return page ? page.content ?? [] : null;
}

/** Distinct cities of the shops, sorted for the visitor's language. */
export function citiesOf(shops: Shop[], locale: string): string[] {
  const set = new Set<string>();
  for (const s of shops) if (s.city?.trim()) set.add(s.city.trim());
  return [...set].sort((a, b) => a.localeCompare(b, locale));
}
