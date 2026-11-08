"""Answering: retrieve sections, ask the local model to answer from them with numbered references."""

from __future__ import annotations

import hashlib
import json
import logging
import re
import time
from collections.abc import AsyncIterator
from dataclasses import dataclass, field

from .config import KNOWN_LANGS, Settings
from .cache import TtlLru
from .index import Section, SiteIndex, load_sections, parse_sections
from .ollama import Ollama, OllamaError
from .prompts import NOT_FOUND, RETRIEVAL_ONLY, SITE_NAMES, context_message, system_prompt

log = logging.getLogger(__name__)
_CITATION = re.compile(r"\[(\d{1,2})\]")


@dataclass
class Reference:
    n: int
    title: str
    breadcrumb: str
    url: str
    excerpt: str
    cited: bool = True

    def as_dict(self) -> dict:
        return self.__dict__.copy()


@dataclass
class Answer:
    answer: str
    references: list[Reference]
    mode: str  # "generated" | "retrieval" | "not_found"
    model: str | None = None
    timings: dict[str, float] = field(default_factory=dict)

    def as_dict(self) -> dict:
        return {"answer": self.answer, "references": [r.as_dict() for r in self.references], "mode": self.mode,
                "model": self.model, "timings": self.timings}


def cited_numbers(text: str, max_n: int) -> list[int]:
    seen: list[int] = []
    for m in _CITATION.finditer(text):
        n = int(m.group(1))
        if 1 <= n <= max_n and n not in seen:
            seen.append(n)
    return seen


class Assistant:
    def __init__(self, settings: Settings, ollama: Ollama) -> None:
        self.settings = settings
        self.ollama = ollama
        self.indexes: dict[tuple[str, str], SiteIndex] = {}
        self.embeddings_ready = False
        self.answers: TtlLru[tuple[str, str, str], Answer] = TtlLru(settings.answer_cache_size,
                                                                     settings.answer_cache_ttl_seconds)

    @property
    def ready(self) -> bool:
        return any(i.sections for i in self.indexes.values())

    # ------------------------------------------------------------------ indexing

    def load(self) -> None:
        for site in self.settings.sites:
            for lang in KNOWN_LANGS:
                self.indexes[(site, lang)] = SiteIndex(load_sections(self.settings.index_root, site, lang))
        self.answers.clear()
        log.info("Loaded %s", {f"{s}/{l}": len(i.sections) for (s, l), i in self.indexes.items()})

    async def load_from_url(self, client) -> bool:
        """Reads <index_url>/<site>/<lang>/search.json from the documentation server. True when all were read."""
        complete = True
        indexes: dict[tuple[str, str], SiteIndex] = {}
        for site in self.settings.sites:
            for lang in KNOWN_LANGS:
                url = f"{self.settings.index_url}/{site}/{lang}/search.json"
                try:
                    r = await client.get(url)
                    r.raise_for_status()
                    indexes[(site, lang)] = SiteIndex(parse_sections(r.json(), site, lang))
                except Exception as e:  # noqa: BLE001 — the docs server may still be starting
                    log.info("index %s not available yet: %s", url, e)
                    complete = False
        if indexes:
            self.indexes = indexes
            self.answers.clear()
            log.info("Loaded %s from %s", {f"{s}/{l}": len(i.sections) for (s, l), i in indexes.items()},
                     self.settings.index_url)
        return complete

    async def embed_all(self) -> bool:
        """Computes (or reads from cache) the section vectors. Without them, retrieval is lexical only."""
        if not self.settings.embed_model:
            return False  # embeddings disabled (ASSISTANT_EMBED_MODEL empty): lexical retrieval only
        models = await self.ollama.models()
        if not self.ollama.has(models, self.settings.embed_model):
            log.warning("Embedding model %s not available: lexical retrieval only", self.settings.embed_model)
            return False
        self.settings.cache_dir.mkdir(parents=True, exist_ok=True)
        for (site, lang), index in self.indexes.items():
            if not index.sections:
                continue
            docs = [f"search_document: {s.passage(1500)}" for s in index.sections]
            key = hashlib.sha256(json.dumps([self.settings.embed_model, docs]).encode()).hexdigest()[:16]
            cache = self.settings.cache_dir / f"{site}-{lang}-{key}.json"
            if cache.is_file():
                index.vectors = json.loads(cache.read_text())
                continue
            vectors: list[list[float]] = []
            for start in range(0, len(docs), 32):
                vectors.extend(await self.ollama.embed(docs[start:start + 32]))
            index.vectors = vectors
            cache.write_text(json.dumps(vectors))
        self.embeddings_ready = True
        return True

    # ------------------------------------------------------------------ answering

    async def retrieve(self, question: str, site: str, lang: str) -> list[Section]:
        return await self.search(question, site, lang, self.settings.top_k)

    async def search(self, question: str, site: str, lang: str, limit: int) -> list[Section]:
        index = self.indexes.get((site, lang))
        if index is None:
            return []
        query_vector = None
        if self.embeddings_ready and index.vectors:
            try:
                query_vector = (await self.ollama.embed([f"search_query: {question}"]))[0]
            except OllamaError as e:
                log.warning("query embedding failed: %s", e)
        return [s for s, _ in index.search(question, limit, query_vector)]

    def references(self, sections: list[Section]) -> list[Reference]:
        return self._references(sections)

    def _references(self, sections: list[Section]) -> list[Reference]:
        return [
            Reference(n=i, title=s.title, breadcrumb=s.breadcrumb, url=s.url(self.settings.public_base_url),
                      excerpt=(s.text[:220].rsplit(" ", 1)[0] + " …") if len(s.text) > 220 else s.text)
            for i, s in enumerate(sections, start=1)
        ]

    def _messages(self, question: str, site: str, lang: str, sections: list[Section]) -> list[dict[str, str]]:
        return [
            {"role": "system", "content": system_prompt(lang, site)},
            {"role": "user", "content": context_message(lang, question, [s.passage() for s in sections])},
        ]

    def _fallback(self, site: str, lang: str, sections: list[Section]) -> Answer:
        name = SITE_NAMES[lang][site]
        if not sections:
            return Answer(NOT_FOUND[lang].format(site=name), [], "not_found")
        refs = self._references(sections)
        lines = [RETRIEVAL_ONLY[lang].format(site=name), *[f"[{r.n}] {r.breadcrumb}" for r in refs]]
        return Answer("\n".join(lines), refs, "retrieval")

    @staticmethod
    def _keep_cited(answer: str, refs: list[Reference]) -> list[Reference]:
        cited = cited_numbers(answer, len(refs))
        if not cited:
            # the model forgot to cite: show the best sections as "see also"
            for r in refs[:3]:
                r.cited = False
            return refs[:3]
        return [r for r in refs if r.n in cited]

    @staticmethod
    def _cache_key(question: str, site: str, lang: str) -> tuple[str, str, str]:
        return (site, lang, " ".join(question.lower().split()))

    async def ask(self, question: str, site: str, lang: str) -> Answer:
        cached = self.answers.get(self._cache_key(question, site, lang))
        if cached is not None:
            return cached
        t0 = time.perf_counter()
        sections = await self.retrieve(question, site, lang)
        t1 = time.perf_counter()
        if not sections:
            return Answer(NOT_FOUND[lang].format(site=SITE_NAMES[lang][site]), [], "not_found",
                          timings={"retrieval": round(t1 - t0, 3)})
        refs = self._references(sections)
        try:
            text = await self.ollama.chat(self._messages(question, site, lang, sections))
        except OllamaError as e:
            log.warning("%s — answering with retrieval only", e)
            answer = self._fallback(site, lang, sections)
            answer.timings = {"retrieval": round(t1 - t0, 3)}
            return answer
        answer = Answer(text.strip(), self._keep_cited(text, refs), "generated", self.ollama.chat_model,
                        {"retrieval": round(t1 - t0, 3), "generation": round(time.perf_counter() - t1, 3)})
        self.answers.put(self._cache_key(question, site, lang), answer)  # only real answers are cached
        return answer

    async def ask_stream(self, question: str, site: str, lang: str) -> AsyncIterator[dict]:
        """Events: {"type": "references"} first, then "token"s, then "done" with the references actually cited."""
        cached = self.answers.get(self._cache_key(question, site, lang))
        if cached is not None:
            yield {"type": "done", **cached.as_dict(), "cached": True}
            return
        sections = await self.retrieve(question, site, lang)
        if not sections:
            fallback = self._fallback(site, lang, sections)
            yield {"type": "done", **fallback.as_dict()}
            return
        refs = self._references(sections)
        yield {"type": "references", "references": [r.as_dict() for r in refs]}
        parts: list[str] = []
        try:
            async for piece in self.ollama.chat_stream(self._messages(question, site, lang, sections)):
                parts.append(piece)
                yield {"type": "token", "text": piece}
        except OllamaError as e:
            log.warning("%s — answering with retrieval only", e)
            if not parts:
                yield {"type": "done", **self._fallback(site, lang, sections).as_dict()}
                return
        text = "".join(parts).strip()
        answer = Answer(text, self._keep_cited(text, refs), "generated", self.ollama.chat_model)
        self.answers.put(self._cache_key(question, site, lang), answer)
        yield {"type": "done", **answer.as_dict()}
