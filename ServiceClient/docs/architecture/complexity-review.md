# Complexity review (CS-12)

Review of the algorithms on the hot paths, requested by the project owner on 2026-10-31 ("check every time the
algorithm complexities of programs to optimize it", DEC-CS12-06). *n* = items handled by one request, *S* = number of
statuses, *R* = permission rules, *C* = categories, *P* = page size, *T* = query terms, *D* = days in a period.

## Changed in CS-12

| Where | Before | After | Why it matters |
| --- | --- | --- | --- |
| `ticket.domain.PermissionMatrix.allows` | scan of every rule: O(R) (≈100) per action, ≈25 actions per ticket, for every ticket of a page | `EnumMap` role → action → scopes built once: O(1) | `allowedActions` is computed for every ticket of every list, detail and export |
| `ticket.application.CategoryService.find` | linear scan of the categories: O(C) per ticket | map by code, rebuilt with the cached snapshot: O(1) | called for each line of a list (P) and of a CSV export (up to the export limit) |
| `ticket.application.LegacyImportService` | one `existsByLegacyId` query per legacy ticket: *n* queries | one `findAllById` query per page of 50: *n*/50 queries | the import now also runs every `LEGACY_SYNC_INTERVAL` (DEC-CS12-02) |
| `TicketSpecifications` (search `q`) | a complete number only matched `ticket_number` (legacy numbers had the same prefix and were not found) | exact match on `ticket_number` **or** `legacy_number` (both indexed) | correctness fix found during the review |
| Assistant `app/index.py` `Bm25` | every section × every query term: O(N·T), then a full sort O(N log N) | inverted index (postings with the term-frequency part precomputed): O(matching postings), then `heapq.nlargest(4k)`: O(m log k) | the public help search and every assistant question; `tests/test_bm25.py` proves identical scores |
| Frontend `kindOf` (`lib/vocabulary.tsx`) | `Array.find` over the statuses: O(S) per badge; `openStatusCodes` O(S²) | `WeakMap` vocabulary → `Map` code → kind: O(1) | every status badge of the queue and dashboard |
| Frontend « Statuts et priorités » transitions matrix | the list of active statuses filtered again for every row: O(S²) extra | filtered once (`useMemo`); labels follow the interface language | the matrix itself stays S×S (inherent) |

## Checked, already fine

| Where | Complexity | Note |
| --- | --- | --- |
| Ticket list (`TicketQueryService.summaries`) | 1 query for the page + 1 batched query for the people (requesters and agents) + `@BatchSize(100)` for context references | no N+1 |
| Dashboard counters (`TicketStatistics.dashboard`) | ~10 `count(*)` queries with indexes, independent of the number of tickets returned | constant number of queries |
| Rate limiter (`RateLimitFilter`) | O(1) per request; Caffeine cache bounded to 50,000 callers, entries expire after 2 minutes | memory bounded |
| Browser API cache (`apiCache.ts`) | O(1) LRU on a `Map`, 100 entries | memory bounded |
| Answer cache (`TtlLru`) | O(1), `ASSISTANT_ANSWER_CACHE_SIZE` entries | memory bounded |
| Outbox relay | one indexed query per poll (`idx_outbox_due`), batch of events | — |
| CSV export | O(n) with n ≤ the export limit; rows written as they are formatted | bounded by the limit |

## Accepted, with their bound

| Where | Complexity | Why it stays |
| --- | --- | --- |
| `BusinessCalendar.businessMinutesBetween` / `plusBusinessMinutes` in `business-hours` mode | O(D) per ticket, at most 3,660 days scanned | the default `SLA_MODE=always` is O(1); holidays are a predicate, so an O(1) formula would need a different calendar model; a day step is a few comparisons |
| Period report (`TicketStatistics.report`) | O(n) memory and time for the tickets created in the period (≤ 366 days) | business-time averages need each ticket; a SQL aggregate would only fit the 24/7 mode. Revisit if a period exceeds ~50,000 tickets |
| Embedding search (optional, `ASSISTANT_EMBED_MODEL`) | O(N·dim) cosine per question | off with Ollama cloud (default); a few hundred sections |

## How to keep it that way

- A loop over tickets never calls a store: collect the ids, then one batched call (`UserDirectory.summaries`,
  `LegacySyncStore.find`).
- Lookups by code use a map built with the cached snapshot, never a `stream().filter()` per item.
- Every cache has a size bound and an expiry (`docs/guide/caching.md`).
- Tests that prove an optimisation keeps the results: `tests/test_bm25.py`, `TicketAccessPolicyTest` (every permission through the indexed matrix),
  `MaplenouIntegrationIT` (legacy sync and exact number search).
