"""Minimal Ollama HTTP client (https://github.com/ollama/ollama/blob/main/docs/api.md)."""

from __future__ import annotations

import json
from collections.abc import AsyncIterator

import httpx


class OllamaError(RuntimeError):
    pass


class Ollama:
    def __init__(self, base_url: str, chat_model: str, embed_model: str, timeout: float = 120.0,
                 transport: httpx.AsyncBaseTransport | None = None, api_key: str = "", think: str = "") -> None:
        self.chat_model = chat_model
        self.embed_model = embed_model
        self.think = think
        # Ollama cloud needs "Authorization: Bearer <key>"; a local Ollama ignores it.
        headers = {"Authorization": f"Bearer {api_key}"} if api_key else {}
        self.client = httpx.AsyncClient(base_url=base_url, timeout=httpx.Timeout(timeout, connect=5.0),
                                        transport=transport, headers=headers)

    def _chat_body(self, messages: list[dict[str, str]], stream: bool) -> dict:
        body: dict = {"model": self.chat_model, "messages": messages, "stream": stream,
                      "options": {"temperature": 0.1, "num_ctx": 4096}}
        if self.think:
            body["think"] = {"true": True, "false": False}.get(self.think, self.think)
        return body

    async def models(self) -> set[str]:
        """Names of the installed models ("qwen2.5:3b"); empty when Ollama is unreachable."""
        try:
            r = await self.client.get("/api/tags")
            r.raise_for_status()
            return {m.get("name", "") for m in r.json().get("models", [])}
        except (httpx.HTTPError, ValueError):
            return set()

    async def reachable(self) -> bool:
        try:
            return (await self.client.get("/api/tags")).status_code == 200
        except httpx.HTTPError:
            return False

    @staticmethod
    def has(models: set[str], wanted: str) -> bool:
        # "nomic-embed-text" matches "nomic-embed-text:latest"
        return wanted in models or f"{wanted}:latest" in models

    async def pull(self, model: str) -> None:
        """Downloads a model into Ollama (blocking until done; can take minutes the first time)."""
        try:
            async with self.client.stream("POST", "/api/pull", json={"model": model, "stream": True},
                                          timeout=httpx.Timeout(None, connect=5.0)) as r:
                r.raise_for_status()
                async for line in r.aiter_lines():
                    if line.strip() and '"error"' in line:
                        raise OllamaError(line)
        except httpx.HTTPError as e:
            raise OllamaError(f"pull {model} failed: {e}") from e

    async def embed(self, texts: list[str]) -> list[list[float]]:
        try:
            r = await self.client.post("/api/embed", json={"model": self.embed_model, "input": texts})
            r.raise_for_status()
            return r.json()["embeddings"]
        except (httpx.HTTPError, KeyError, ValueError) as e:
            raise OllamaError(f"embedding failed: {e}") from e

    async def chat(self, messages: list[dict[str, str]]) -> str:
        try:
            r = await self.client.post("/api/chat", json=self._chat_body(messages, stream=False))
            r.raise_for_status()
            return r.json()["message"]["content"]
        except (httpx.HTTPError, KeyError, ValueError) as e:
            raise OllamaError(f"chat failed: {e}") from e

    async def chat_stream(self, messages: list[dict[str, str]]) -> AsyncIterator[str]:
        try:
            async with self.client.stream("POST", "/api/chat", json=self._chat_body(messages, stream=True)) as r:
                r.raise_for_status()
                async for line in r.aiter_lines():
                    if not line.strip():
                        continue
                    chunk = json.loads(line)
                    piece = chunk.get("message", {}).get("content", "")
                    if piece:
                        yield piece
                    if chunk.get("done"):
                        break
        except (httpx.HTTPError, ValueError) as e:
            raise OllamaError(f"chat failed: {e}") from e

    async def aclose(self) -> None:
        await self.client.aclose()
