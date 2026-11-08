"""Settings from environment variables (see docker-compose.yml and .env.example)."""

from __future__ import annotations

import os
from dataclasses import dataclass, field
from pathlib import Path

KNOWN_SITES = ("user", "staff", "developer")
KNOWN_LANGS = ("fr", "en")


def _list(name: str, default: str) -> tuple[str, ...]:
    return tuple(x.strip() for x in os.environ.get(name, default).split(",") if x.strip())


@dataclass(frozen=True)
class Settings:
    # Folder that contains <site>/<lang>/search.json (the built documentation sites)
    index_root: Path = field(default_factory=lambda: Path(os.environ.get("ASSISTANT_INDEX_ROOT", "/srv/site")))
    # When set (e.g. http://docs), the indexes are read from the documentation server instead of index_root:
    # <url>/<site>/<lang>/search.json. The docs image then is the only one that builds the sites.
    index_url: str = field(default_factory=lambda: os.environ.get("ASSISTANT_INDEX_URL", "").rstrip("/"))
    # Download the chat and embedding models into Ollama at start-up when they are missing.
    pull_models: bool = field(default_factory=lambda: os.environ.get("ASSISTANT_PULL_MODELS", "true").lower() == "true")
    # Answer cache: same question + site + language within the TTL is answered from memory (bounded LRU).
    answer_cache_size: int = field(default_factory=lambda: int(os.environ.get("ASSISTANT_ANSWER_CACHE_SIZE", "256")))
    answer_cache_ttl_seconds: int = field(default_factory=lambda: int(os.environ.get("ASSISTANT_ANSWER_CACHE_TTL", "3600")))
    # Sites this instance may answer about. A public instance must expose `user` only.
    sites: tuple[str, ...] = field(default_factory=lambda: _list("ASSISTANT_SITES", "user,staff,developer"))
    ollama_url: str = field(default_factory=lambda: os.environ.get("OLLAMA_URL", "http://localhost:11434").rstrip("/"))
    # Ollama cloud (https://ollama.com): API key sent as "Authorization: Bearer"; empty for a local Ollama.
    # Server-side only: the key never reaches the browser.
    ollama_api_key: str = field(default_factory=lambda: os.environ.get("OLLAMA_API_KEY", "").strip())
    chat_model: str = field(default_factory=lambda: os.environ.get("ASSISTANT_CHAT_MODEL", "qwen2.5:3b"))
    # Empty = no embeddings (lexical BM25 retrieval only), e.g. with Ollama cloud, which serves chat models.
    embed_model: str = field(default_factory=lambda: os.environ.get("ASSISTANT_EMBED_MODEL", "nomic-embed-text").strip())
    # Reasoning effort for "thinking" models such as gpt-oss ("low", "medium", "high"); empty = not sent.
    think: str = field(default_factory=lambda: os.environ.get("ASSISTANT_THINK", "").strip())
    # Base URL used to build absolute links in references, e.g. http://localhost:8006
    public_base_url: str = field(
        default_factory=lambda: os.environ.get("ASSISTANT_PUBLIC_BASE_URL", "").rstrip("/")
    )
    allowed_origins: tuple[str, ...] = field(
        default_factory=lambda: _list("ASSISTANT_ALLOWED_ORIGINS", "http://localhost:8006")
    )
    # Shared secret of the support backend (X-Assistant-Token) for /api/assistant/internal/*. Empty = those
    # endpoints do not exist (404): always the case on a public instance.
    internal_token: str = field(default_factory=lambda: os.environ.get("ASSISTANT_INTERNAL_TOKEN", "").strip())
    cache_dir: Path = field(default_factory=lambda: Path(os.environ.get("ASSISTANT_CACHE_DIR", "/tmp/assistant-cache")))
    top_k: int = field(default_factory=lambda: int(os.environ.get("ASSISTANT_TOP_K", "5")))
    max_question_chars: int = 500
    # requests per minute and per client address
    rate_limit_per_minute: int = field(default_factory=lambda: int(os.environ.get("ASSISTANT_RATE_LIMIT", "20")))
    chat_timeout_seconds: float = field(default_factory=lambda: float(os.environ.get("ASSISTANT_CHAT_TIMEOUT", "120")))

    def __post_init__(self) -> None:
        if self.think and self.think not in ("low", "medium", "high", "true", "false"):
            raise ValueError("ASSISTANT_THINK must be low, medium, high, true or false")
        unknown = [s for s in self.sites if s not in KNOWN_SITES]
        if unknown:
            raise ValueError(f"Unknown ASSISTANT_SITES {unknown}; allowed: {KNOWN_SITES}")
        if self.internal_token and len(self.internal_token) < 24:
            raise ValueError("ASSISTANT_INTERNAL_TOKEN must be at least 24 characters (or empty)")
