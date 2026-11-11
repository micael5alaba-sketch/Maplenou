import { describe, expect, it } from "vitest";
import { apiCacheSize, clearApiCache, dedupe, readCache, writeCache } from "./apiCache";

describe("client API cache (docs/guide/caching.md)", () => {
  it("stores, reads and clears responses", () => {
    clearApiCache();
    writeCache("/a", { n: 1 });
    expect(readCache("/a")).toEqual({ n: 1 });
    clearApiCache();
    expect(readCache("/a")).toBeUndefined();
  });

  it("is bounded (least recently used entries are dropped)", () => {
    clearApiCache();
    for (let i = 0; i < 150; i++) writeCache(`/k${i}`, i);
    expect(apiCacheSize()).toBe(100);
    expect(readCache("/k0")).toBeUndefined();
    expect(readCache("/k149")).toBe(149);
  });

  it("shares one request between concurrent callers", async () => {
    let calls = 0;
    const load = () => new Promise<number>((r) => { calls++; setTimeout(() => r(42), 5); });
    const [a, b] = await Promise.all([dedupe("/x", load), dedupe("/x", load)]);
    expect([a, b, calls]).toEqual([42, 42, 1]);
  });
});
