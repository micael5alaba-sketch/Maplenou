"use client";

import { createContext, useContext, type ReactNode } from "react";
import { useApi } from "./useApi";
import type { StatusKind, Vocabulary } from "./types";

/**
 * Statuses, transitions and priorities configured by the administrators (GET /vocabulary, DEC-CS11-02).
 * Loaded once by the signed-in shells; screens read it through `useVocabulary()` and `useLabels()`.
 * Without it (tests, first paint) the built-in vocabulary of `labels.ts` is used.
 */
const VocabularyContext = createContext<Vocabulary | undefined>(undefined);

export function VocabularyProvider({ children }: { children: ReactNode }) {
  const { data } = useApi<Vocabulary>("/vocabulary");
  return <VocabularyContext.Provider value={data}>{children}</VocabularyContext.Provider>;
}

export const useVocabulary = (): Vocabulary | undefined => useContext(VocabularyContext);

/** Kinds of the built-in statuses, used before the vocabulary is loaded. */
export const SYSTEM_STATUS_KIND: Record<string, StatusKind> = {
  NEW: "NEW",
  ASSIGNED: "ASSIGNED",
  IN_PROGRESS: "IN_PROGRESS",
  WAITING_FOR_CUSTOMER: "WAITING_REQUESTER",
  RESOLVED: "RESOLVED",
  CLOSED: "CLOSED",
};

/** code → kind per vocabulary object, built once: kindOf is called for every row of a list (O(1), not O(statuses)). */
const kindIndex = new WeakMap<Vocabulary, Map<string, StatusKind>>();

export function kindOf(code: string, vocabulary?: Vocabulary): StatusKind | undefined {
  if (!vocabulary) return SYSTEM_STATUS_KIND[code];
  let index = kindIndex.get(vocabulary);
  if (!index) {
    index = new Map(vocabulary.statuses.map((s) => [s.code, s.kind]));
    kindIndex.set(vocabulary, index);
  }
  return index.get(code) ?? SYSTEM_STATUS_KIND[code];
}

const DEFAULT_STATUSES = ["NEW", "ASSIGNED", "IN_PROGRESS", "WAITING_FOR_CUSTOMER", "RESOLVED", "CLOSED"];
const DEFAULT_PRIORITIES = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];

/** Active status codes in display order. */
export function statusCodes(vocabulary?: Vocabulary): string[] {
  if (!vocabulary) return DEFAULT_STATUSES;
  return [...vocabulary.statuses].filter((s) => s.active).sort((a, b) => a.sortOrder - b.sortOrder).map((s) => s.code);
}

/** Statuses of an open ticket (everything but resolved and closed). */
export function openStatusCodes(vocabulary?: Vocabulary): string[] {
  return statusCodes(vocabulary).filter((c) => {
    const k = kindOf(c, vocabulary);
    return k !== "RESOLVED" && k !== "CLOSED";
  });
}

/** Active priority codes, most urgent first (a higher rank is more urgent, as in the backend). */
export function priorityCodes(vocabulary?: Vocabulary, requesterOnly = false): string[] {
  if (!vocabulary) return requesterOnly ? DEFAULT_PRIORITIES.filter((p) => p !== "CRITICAL") : DEFAULT_PRIORITIES;
  return [...vocabulary.priorities]
    .filter((p) => p.active && (!requesterOnly || p.requesterSelectable))
    .sort((a, b) => b.rank - a.rank)
    .map((p) => p.code);
}

export function defaultPriority(vocabulary?: Vocabulary): string {
  return vocabulary?.priorities.find((p) => p.defaultPriority)?.code ?? "MEDIUM";
}
