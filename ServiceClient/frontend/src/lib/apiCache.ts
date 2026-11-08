// Client-side cache of API GET responses (docs/guide/caching.md).
//
// - stale-while-revalidate: a screen shows the last known data at once, then refreshes it in the background;
// - identical requests made at the same time share one network call (deduplication);
// - bounded: at most MAX_ENTRIES responses, least recently used first out; memory only (never written to disk);
// - cleared on sign-out and after every change (POST/PUT/DELETE), so a user never sees another user's data and
//   never keeps outdated data after an action.

const MAX_ENTRIES = 100;

const entries = new Map<string, unknown>();
const inflight = new Map<string, Promise<unknown>>();

const inBrowser = () => typeof window !== "undefined";

export function readCache<T>(key: string): T | undefined {
  if (!inBrowser() || !entries.has(key)) return undefined;
  const value = entries.get(key) as T;
  entries.delete(key); // refresh its LRU position
  entries.set(key, value);
  return value;
}

export function writeCache(key: string, value: unknown): void {
  if (!inBrowser()) return; // never share data between users during server rendering
  entries.delete(key);
  entries.set(key, value);
  while (entries.size > MAX_ENTRIES) {
    const oldest = entries.keys().next().value as string;
    entries.delete(oldest);
  }
}

/** Runs `load` once for concurrent callers of the same key. */
export function dedupe<T>(key: string, load: () => Promise<T>): Promise<T> {
  const running = inflight.get(key) as Promise<T> | undefined;
  if (running) return running;
  const p = load().finally(() => inflight.delete(key));
  inflight.set(key, p);
  return p;
}

export function clearApiCache(): void {
  entries.clear();
  inflight.clear();
}

export const apiCacheSize = () => entries.size;
