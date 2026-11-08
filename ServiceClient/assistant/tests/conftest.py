import json
from pathlib import Path

import httpx
import pytest

from app.config import Settings
from app.ollama import Ollama

SEARCH_FR = {
    "config": {"lang": ["fr"]},
    "items": [
        {"location": "", "level": 1, "title": "Accueil", "text": "<p>Guide du service client Maplenou.</p>",
         "path": ["Accueil"]},
        {"location": "tickets/creer/#ajouter-une-piece-jointe", "level": 2, "title": "Ajouter une pièce jointe",
         "text": "<p>Formats acceptés : JPG, PNG, WEBP ou PDF, 10 Mo maximum par fichier, 5 fichiers par message.</p>",
         "path": ["Créer un ticket"]},
        {"location": "tickets/suivre/#rouvrir-un-ticket", "level": 2, "title": "Rouvrir un ticket",
         "text": "<p>Un ticket résolu peut être rouvert pendant 7 jours depuis Mes tickets.</p>",
         "path": ["Suivre mes tickets"]},
        {"location": "compte/#langue", "level": 2, "title": "Changer la langue",
         "text": "<p>Choisissez Français ou English dans Compte › Préférences.</p>", "path": ["Mon compte"]},
    ],
}
SEARCH_EN = {
    "config": {"lang": ["en"]},
    "items": [
        {"location": "tickets/create/#add-an-attachment", "level": 2, "title": "Add an attachment",
         "text": "<p>Accepted formats: JPG, PNG, WEBP or PDF, 10 MB per file.</p>", "path": ["Create a ticket"]},
    ],
}
SEARCH_STAFF = {"items": [{"location": "sla/#targets", "level": 2, "title": "SLA targets",
                           "text": "<p>Critical: first response within 1 business hour.</p>", "path": ["SLA"]}]}


@pytest.fixture
def index_root(tmp_path: Path) -> Path:
    for site, lang, data in (("user", "fr", SEARCH_FR), ("user", "en", SEARCH_EN), ("staff", "en", SEARCH_STAFF)):
        d = tmp_path / site / lang
        d.mkdir(parents=True)
        (d / "search.json").write_text(json.dumps(data), encoding="utf-8")
    return tmp_path


class FakeOllama:
    """Stands in for the Ollama API: deterministic embeddings and a canned answer citing [1]."""

    def __init__(self, up: bool = True, answer: str = "Joignez un fichier JPG, PNG, WEBP ou PDF de 10 Mo maximum [1].",
                 models: tuple[str, ...] = ("qwen2.5:3b", "nomic-embed-text:latest")) -> None:
        self.up = up
        self.answer = answer
        self.models = models
        self.requests: list[dict] = []

    @staticmethod
    def vector(text: str) -> list[float]:
        words = ["piece", "jointe", "fichier", "rouvrir", "langue", "ticket", "sla", "format"]
        low = text.lower().replace("è", "e")
        counts = [float(low.count(w)) for w in words]
        # text without any keyword points to its own direction (documents and queries differ)
        empty = not any(counts)
        query = text.startswith("search_query:")
        return [*counts, 1.0 if empty and not query else 0.0, 1.0 if empty and query else 0.0]

    def __call__(self, request: httpx.Request) -> httpx.Response:
        if not self.up:
            raise httpx.ConnectError("down", request=request)
        body = json.loads(request.content) if request.content else {}
        self.requests.append({"path": request.url.path, "auth": request.headers.get("authorization"), **body})
        if request.url.path == "/api/tags":
            return httpx.Response(200, json={"models": [{"name": m} for m in self.models]})
        if request.url.path == "/api/embed":
            return httpx.Response(200, json={"embeddings": [self.vector(t) for t in body["input"]]})
        if request.url.path == "/api/chat":
            if body.get("stream"):
                half = len(self.answer) // 2
                lines = [json.dumps({"message": {"content": self.answer[:half]}, "done": False}),
                         json.dumps({"message": {"content": self.answer[half:]}, "done": True})]
                return httpx.Response(200, content="\n".join(lines).encode())
            return httpx.Response(200, json={"message": {"role": "assistant", "content": self.answer}, "done": True})
        return httpx.Response(404)


@pytest.fixture
def fake_ollama() -> FakeOllama:
    return FakeOllama()


def make_settings(index_root: Path, tmp_path: Path, **overrides) -> Settings:
    values = dict(index_root=index_root, sites=("user", "staff"), ollama_url="http://ollama",
                  public_base_url="http://docs.test", allowed_origins=("http://docs.test",),
                  cache_dir=tmp_path / "cache", rate_limit_per_minute=100, pull_models=False)
    values.update(overrides)
    return Settings(**values)


def make_ollama(fake: FakeOllama) -> Ollama:
    return Ollama("http://ollama", "qwen2.5:3b", "nomic-embed-text", transport=httpx.MockTransport(fake))
