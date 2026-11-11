"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError, apiFetch } from "./api";
import { dedupe, readCache, writeCache } from "./apiCache";

export interface ApiState<T> {
  data: T | undefined;
  error: ApiError | undefined;
  loading: boolean;
  reload: () => Promise<void>;
  setData: (d: T) => void;
}

/**
 * Loads `path` (null = don't load). Shows the cached response at once when there is one (stale-while-revalidate,
 * see lib/apiCache.ts), always refreshes it, and keeps the previous data while reloading.
 */
export function useApi<T>(path: string | null): ApiState<T> {
  const [data, setData] = useState<T | undefined>(() => (path === null ? undefined : readCache<T>(path)));
  const [error, setError] = useState<ApiError>();
  const [loading, setLoading] = useState<boolean>(path !== null);
  const latest = useRef(path);
  useEffect(() => {
    latest.current = path;
  }, [path]);

  const load = useCallback(async () => {
    if (path === null) return;
    setLoading(true);
    try {
      const result = await dedupe(path, () => apiFetch<T>(path));
      writeCache(path, result);
      if (latest.current === path) {
        setData(result);
        setError(undefined);
      }
    } catch (e) {
      if (latest.current === path) setError(e instanceof ApiError ? e : new ApiError({ status: 0 }));
    } finally {
      if (latest.current === path) setLoading(false);
    }
  }, [path]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetching on dependency change is the purpose of this hook
    void load();
  }, [load]);

  return { data, error, loading, reload: load, setData };
}
