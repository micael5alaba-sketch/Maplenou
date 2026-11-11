"""Documentation index: sections from the Zensical search indexes, ranked by BM25 and (optionally) embeddings.

Each built site has a `search.json` whose items are page sections with their anchor (`location`), title and
HTML text. Using it means the assistant's references link to exactly the sections the reader sees.
"""

from __future__ import annotations

import heapq
import json
import logging
import math
from collections import Counter
from dataclasses import dataclass
from pathlib import Path

from .text import html_to_text, tokens

log = logging.getLogger(__name__)


def _stem(token: str) -> str:
    # Very light stemming shared by FR and EN: plural "s"/"x" ("tickets" -> "ticket", "travaux" -> "travau").
    return token[:-1] if len(token) > 3 and token[-1] in "sx" else token


def terms(value: str) -> list[str]:
    return [_stem(t) for t in tokens(value)]


@dataclass(frozen=True)
class Section:
    site: str
    lang: str
    location: str  # relative to the site root, e.g. "tickets/create/#attachments"
    title: str
    path: tuple[str, ...]  # breadcrumb (page titles)
    text: str

    @property
    def breadcrumb(self) -> str:
        parts = [p for p in self.path if p and p != self.title]
        return " › ".join([*parts, self.title])

    def url(self, public_base_url: str) -> str:
        return f"{public_base_url}/{self.site}/{self.lang}/{self.location}"

    def passage(self, limit: int = 1200) -> str:
        body = self.text if len(self.text) <= limit else self.text[:limit].rsplit(" ", 1)[0] + " …"
        return f"{self.breadcrumb}\n{body}"


def load_sections(index_root: Path, site: str, lang: str) -> list[Section]:
    file = index_root / site / lang / "search.json"
    if not file.is_file():
        log.warning("No search index at %s", file)
        return []
    return parse_sections(json.loads(file.read_text(encoding="utf-8")), site, lang)


def parse_sections(data: object, site: str, lang: str) -> list[Section]:
    """Sections of a Zensical `search.json` document (already decoded)."""
    items = data.get("items", data.get("docs", [])) if isinstance(data, dict) else data
    sections = []
    for item in items:
        text = html_to_text(item.get("text", ""))
        title = html_to_text(item.get("title", ""))
        if not text and item.get("level", 1) > 1:
            continue
        sections.append(
            Section(
                site=site,
                lang=lang,
                location=item.get("location", ""),
                title=title,
                path=tuple(html_to_text(p) for p in item.get("path", [])),
                text=text,
            )
        )
    return sections


class Bm25:
    """Okapi BM25 over an inverted index: a query only visits the sections that contain one of its terms
    (O(matching postings)), instead of every section for every term."""

    def __init__(self, documents: list[list[str]], k1: float = 1.4, b: float = 0.75) -> None:
        self.k1, self.b = k1, b
        self.size = len(documents)
        lengths = [len(d) for d in documents]
        avg = (sum(lengths) / len(lengths)) if lengths else 0.0
        # length normalisation of each section, computed once
        norms = [1 - b + b * (length / avg if avg else 1) for length in lengths]
        self.postings: dict[str, list[tuple[int, float]]] = {}
        for i, doc in enumerate(documents):
            for t, f in Counter(doc).items():
                # term-frequency part of the score, so a query only multiplies by the idf
                self.postings.setdefault(t, []).append((i, f * (k1 + 1) / (f + k1 * norms[i])))
        n = self.size
        self.idf = {t: math.log(1 + (n - len(p) + 0.5) / (len(p) + 0.5)) for t, p in self.postings.items()}

    def matches(self, query: list[str]) -> dict[int, float]:
        """Score of every section that contains at least one query term (others score 0)."""
        out: dict[int, float] = {}
        for t in query:  # a repeated term counts again, as in the classic formula
            idf = self.idf.get(t)
            if idf is None:
                continue
            for i, tf in self.postings[t]:
                out[i] = out.get(i, 0.0) + idf * tf
        return out

    def scores(self, query: list[str]) -> list[float]:
        out = [0.0] * self.size
        for i, score in self.matches(query).items():
            out[i] = score
        return out


def cosine(a: list[float], b: list[float]) -> float:
    dot = sum(x * y for x, y in zip(a, b))
    na = math.sqrt(sum(x * x for x in a))
    nb = math.sqrt(sum(y * y for y in b))
    return dot / (na * nb) if na and nb else 0.0


class SiteIndex:
    """Sections of one site in one language, with BM25 and optional embedding vectors."""

    def __init__(self, sections: list[Section]) -> None:
        self.sections = sections
        # titles and breadcrumbs count twice: they are the best summary of a section
        self.bm25 = Bm25([terms(f"{s.breadcrumb} {s.breadcrumb} {s.text}") for s in sections])
        self.vectors: list[list[float]] | None = None

    def search(self, question: str, k: int, query_vector: list[float] | None = None) -> list[tuple[Section, float]]:
        if not self.sections:
            return []
        lexical = self.bm25.matches(terms(question))
        # only the k*4 best are used below: a partial selection instead of sorting every section
        keep = k * 4
        ranked_lexical = [i for i, score in heapq.nlargest(keep, lexical.items(), key=lambda kv: (kv[1], -kv[0]))
                          if score > 0]
        rankings = [ranked_lexical]
        if query_vector is not None and self.vectors:
            semantic = [cosine(query_vector, v) for v in self.vectors]
            # keep only reasonably close sections so unrelated questions do not get random references
            rankings.append([i for i in sorted(range(len(semantic)), key=lambda i: -semantic[i]) if semantic[i] > 0.55])
        # reciprocal rank fusion of the lexical and semantic rankings
        fused: dict[int, float] = {}
        for ranking in rankings:
            for rank, i in enumerate(ranking[: k * 4]):
                fused[i] = fused.get(i, 0.0) + 1.0 / (60 + rank)
        best = sorted(fused.items(), key=lambda kv: -kv[1])[:k]
        return [(self.sections[i], score) for i, score in best]
