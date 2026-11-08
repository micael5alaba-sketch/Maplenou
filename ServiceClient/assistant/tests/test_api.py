import json

import pytest
from starlette.testclient import TestClient

from app.main import RateLimiter, create_app
from conftest import FakeOllama, make_ollama, make_settings


@pytest.fixture
def client_for(index_root, tmp_path):
    def build(fake: FakeOllama, **overrides):
        app = create_app(make_settings(index_root, tmp_path, **overrides), make_ollama(fake))
        return TestClient(app)
    return build


def ask(client, question, site="user", lang="fr", stream=False):
    url = "/api/assistant/ask" + ("?stream=1" if stream else "")
    return client.post(url, json={"question": question, "site": site, "lang": lang})


def test_answers_with_cited_references(client_for, fake_ollama):
    with client_for(fake_ollama) as client:
        r = ask(client, "Quels formats de pièce jointe ?")
        assert r.status_code == 200
        body = r.json()
        assert body["mode"] == "generated"
        assert "[1]" in body["answer"]
        assert [ref["n"] for ref in body["references"]] == [1]
        ref = body["references"][0]
        assert ref["title"] == "Ajouter une pièce jointe"
        assert ref["url"] == "http://docs.test/user/fr/tickets/creer/#ajouter-une-piece-jointe"
        chat = [q for q in fake_ollama.requests if q["path"] == "/api/chat"][-1]
        assert "UNIQUEMENT" in chat["messages"][0]["content"]  # French system prompt
        assert "[1] Créer un ticket › Ajouter une pièce jointe" in chat["messages"][1]["content"]


def test_english_site(client_for):
    fake = FakeOllama(answer="Attach JPG, PNG, WEBP or PDF files up to 10 MB [1].")
    with client_for(fake) as client:
        body = ask(client, "Which attachment formats are accepted?", lang="en").json()
        assert body["references"][0]["url"].endswith("/user/en/tickets/create/#add-an-attachment")
        chat = [q for q in fake.requests if q["path"] == "/api/chat"][-1]
        assert "Answer in English" in chat["messages"][0]["content"]


def test_uncited_answer_keeps_best_sections_as_see_also(client_for):
    with client_for(FakeOllama(answer="Voir la documentation.")) as client:
        refs = ask(client, "pièce jointe format").json()["references"]
        assert refs and all(not r["cited"] for r in refs)


def test_falls_back_to_retrieval_when_ollama_is_down(client_for):
    with client_for(FakeOllama(up=False)) as client:
        body = ask(client, "Comment rouvrir un ticket ?").json()
        assert body["mode"] == "retrieval"
        assert body["references"][0]["title"] == "Rouvrir un ticket"
        assert "n'est pas disponible" in body["answer"]


def test_not_found(client_for, fake_ollama):
    with client_for(fake_ollama) as client:
        body = ask(client, "xylophone quantique").json()
        assert body["mode"] == "not_found"
        assert body["references"] == []


def test_streaming_events(client_for, fake_ollama):
    with client_for(fake_ollama) as client:
        r = ask(client, "formats de pièce jointe", stream=True)
        events = [json.loads(line) for line in r.text.splitlines() if line]
        assert events[0]["type"] == "references"
        assert any(e["type"] == "token" for e in events)
        done = events[-1]
        assert done["type"] == "done" and done["mode"] == "generated"
        assert done["answer"] == fake_ollama.answer


def test_site_scope_hides_internal_sites(client_for, fake_ollama):
    # a public instance only serves the user guide
    with client_for(fake_ollama, sites=("user",)) as client:
        r = ask(client, "SLA targets", site="staff", lang="en")
        assert r.status_code == 404
        assert r.json()["code"] == "UNKNOWN_SITE"


def test_validation(client_for, fake_ollama):
    with client_for(fake_ollama) as client:
        assert ask(client, "  ").json()["code"] == "QUESTION_REQUIRED"
        assert ask(client, "x" * 501).json()["code"] == "QUESTION_TOO_LONG"
        assert ask(client, "ok", lang="de").json()["code"] == "UNKNOWN_LANGUAGE"
        r = client.post("/api/assistant/ask", content=b"not json", headers={"content-type": "application/json"})
        assert r.status_code == 400


def test_health_reports_indexes_and_embeddings(client_for, fake_ollama):
    with client_for(fake_ollama) as client:
        app = client.app
        import asyncio
        asyncio.run(app.state.assistant.embed_all()) if not app.state.assistant.embeddings_ready else None
        body = client.get("/api/assistant/health").json()
        assert body["ollama"] is True
        assert body["chatModel"] == "qwen2.5:3b"
        assert body["sites"]["user/fr"] == 4


def test_embeddings_are_cached(index_root, tmp_path, fake_ollama):
    import asyncio
    from app.service import Assistant
    settings = make_settings(index_root, tmp_path)
    first = Assistant(settings, make_ollama(fake_ollama))
    first.load()
    assert asyncio.run(first.embed_all())
    calls = len([q for q in fake_ollama.requests if q["path"] == "/api/embed"])
    second = Assistant(settings, make_ollama(fake_ollama))
    second.load()
    asyncio.run(second.embed_all())
    assert len([q for q in fake_ollama.requests if q["path"] == "/api/embed"]) == calls
    assert second.indexes[("user", "fr")].vectors is not None


def test_rate_limiter():
    limiter = RateLimiter(2)
    assert limiter.allow("a", 0) and limiter.allow("a", 1)
    assert not limiter.allow("a", 2)
    assert limiter.allow("b", 2)
    assert limiter.allow("a", 62)


def test_repeated_question_is_served_from_the_answer_cache(client_for, fake_ollama):
    with client_for(fake_ollama) as client:
        first = ask(client, "Quels formats de pièce jointe ?").json()
        chats = len([q for q in fake_ollama.requests if q["path"] == "/api/chat"])
        second = ask(client, "  quels FORMATS de pièce jointe ? ").json()
        assert second["answer"] == first["answer"]
        assert len([q for q in fake_ollama.requests if q["path"] == "/api/chat"]) == chats  # no new model call
        assert client.get("/api/assistant/health").json()["answerCache"]["hits"] >= 1


def test_fallback_answers_are_not_cached(client_for):
    fake = FakeOllama(up=False)
    with client_for(fake) as client:
        ask(client, "Comment rouvrir un ticket ?")
        assert client.get("/api/assistant/health").json()["answerCache"]["entries"] == 0


def test_indexes_can_be_read_from_the_documentation_server(index_root, tmp_path, fake_ollama):
    import asyncio
    import json as _json
    import httpx
    from app.service import Assistant

    def docs_server(request: httpx.Request) -> httpx.Response:
        f = index_root / request.url.path.strip("/")
        return httpx.Response(200, json=_json.loads(f.read_text())) if f.is_file() else httpx.Response(404)

    settings = make_settings(index_root, tmp_path, index_url="http://docs")
    assistant = Assistant(settings, make_ollama(fake_ollama))

    async def run():
        async with httpx.AsyncClient(transport=httpx.MockTransport(docs_server)) as client:
            return await assistant.load_from_url(client)

    complete = asyncio.run(run())
    assert complete is False  # staff/fr does not exist in the fixture
    assert assistant.ready
    assert len(assistant.indexes[("user", "fr")].sections) == 4
