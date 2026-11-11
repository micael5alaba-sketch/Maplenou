"""Ollama cloud (https://ollama.com): API key, no model pull, no embeddings, thinking level."""

import asyncio

import httpx
from starlette.testclient import TestClient

from app.main import create_app
from app.ollama import Ollama
from app.service import Assistant
from conftest import FakeOllama, make_settings

CLOUD = dict(chat_model="gpt-oss:20b", embed_model="", ollama_api_key="test-key", think="low", pull_models=True)


def cloud_ollama(fake: FakeOllama) -> Ollama:
    return Ollama("http://ollama", "gpt-oss:20b", "", transport=httpx.MockTransport(fake), api_key="test-key",
                  think="low")


def test_cloud_requests_carry_the_api_key_and_the_thinking_level(index_root, tmp_path):
    fake = FakeOllama(models=("gpt-oss:20b",))
    assistant = Assistant(make_settings(index_root, tmp_path, **CLOUD), cloud_ollama(fake))
    assistant.load()
    answer = asyncio.run(assistant.ask("Comment joindre une pièce jointe ?", site="user", lang="fr"))
    assert answer.references
    chat = [r for r in fake.requests if r["path"] == "/api/chat"][-1]
    assert chat["auth"] == "Bearer test-key"
    assert chat["think"] == "low"
    assert chat["model"] == "gpt-oss:20b"


def test_without_embedding_model_retrieval_is_lexical_and_nothing_is_embedded(index_root, tmp_path):
    fake = FakeOllama(models=("gpt-oss:20b",))
    assistant = Assistant(make_settings(index_root, tmp_path, **CLOUD), cloud_ollama(fake))
    assistant.load()
    assert asyncio.run(assistant.embed_all()) is False
    assert not any(r["path"] == "/api/embed" for r in fake.requests)


def test_cloud_mode_never_pulls_models_and_health_says_cloud(index_root, tmp_path):
    fake = FakeOllama(models=("gpt-oss:20b",))
    app = create_app(make_settings(index_root, tmp_path, **CLOUD), cloud_ollama(fake))
    with TestClient(app) as client:
        health = client.get("/api/assistant/health").json()
    assert health["ollamaCloud"] is True
    assert health["chatModel"] == "gpt-oss:20b"
    assert not any(r["path"] == "/api/pull" for r in fake.requests)


def test_local_ollama_sends_no_authorization_header(index_root, tmp_path, fake_ollama):
    from conftest import make_ollama
    assistant = Assistant(make_settings(index_root, tmp_path), make_ollama(fake_ollama))
    assistant.load()
    asyncio.run(assistant.ask("Comment joindre une pièce jointe ?", site="user", lang="fr"))
    chat = [r for r in fake_ollama.requests if r["path"] == "/api/chat"][-1]
    assert chat["auth"] is None
    assert "think" not in chat
