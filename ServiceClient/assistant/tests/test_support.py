"""Help-centre search and the AI help for support agents (DEC-CS11-08)."""

import pytest
from starlette.testclient import TestClient

from app.main import create_app
from app.support import parse_json_object
from conftest import FakeOllama, make_ollama, make_settings

TOKEN = "internal-token-for-tests-0123456789"
CATEGORIES = [{"code": "ORDER", "label": "Commande"}, {"code": "DELIVERY", "label": "Livraison"}]
PRIORITIES = [{"code": "CRITICAL", "label": "Critique"}, {"code": "HIGH", "label": "Haute"},
              {"code": "MEDIUM", "label": "Moyenne"}, {"code": "LOW", "label": "Basse"}]


@pytest.fixture
def client_for(index_root, tmp_path):
    def build(fake: FakeOllama, **overrides):
        overrides.setdefault("internal_token", TOKEN)
        return TestClient(create_app(make_settings(index_root, tmp_path, **overrides), make_ollama(fake)))
    return build


def triage_body(**extra):
    body = {"subject": "Colis jamais arrivé", "description": "Commande payée il y a 10 jours, toujours rien.",
            "language": "fr", "categories": CATEGORIES, "priorities": PRIORITIES}
    body.update(extra)
    return body


def test_search_returns_sections_without_the_language_model(client_for):
    with client_for(FakeOllama(up=False)) as client:
        r = client.get("/api/assistant/search", params={"q": "pièce jointe", "site": "user", "lang": "fr"})
        assert r.status_code == 200
        items = r.json()["items"]
        assert items[0]["title"] == "Ajouter une pièce jointe"
        assert items[0]["url"].startswith("http://docs.test/user/fr/")
        assert client.get("/api/assistant/search", params={"q": ""}).status_code == 400
        assert client.get("/api/assistant/search", params={"q": "x", "site": "developer"}).status_code == 404


def test_public_instance_has_no_internal_endpoints(client_for, fake_ollama):
    with client_for(fake_ollama, internal_token="", sites=("user",)) as client:
        r = client.post("/api/assistant/internal/triage", json=triage_body(), headers={"X-Assistant-Token": "x"})
        assert r.status_code == 404
        assert client.get("/api/assistant/search", params={"q": "SLA", "site": "staff"}).status_code == 404


def test_internal_endpoints_need_the_token(client_for, fake_ollama):
    with client_for(fake_ollama) as client:
        assert client.post("/api/assistant/internal/triage", json=triage_body()).status_code == 401
        assert client.post("/api/assistant/internal/triage", json=triage_body(),
                           headers={"X-Assistant-Token": "wrong"}).status_code == 401


def test_triage_returns_codes_from_the_lists(client_for):
    fake = FakeOllama(answer='Voici : ```json\n{"categoryCode": "delivery", "priorityCode": "HIGH", '
                             '"confidence": 1.7, "reason": "Colis en retard"}\n```')
    with client_for(fake) as client:
        r = client.post("/api/assistant/internal/triage", json=triage_body(), headers={"X-Assistant-Token": TOKEN})
        assert r.status_code == 200
        assert r.json() == {"categoryCode": "DELIVERY", "priorityCode": "HIGH", "confidence": 1.0,
                            "reason": "Colis en retard", "model": "qwen2.5:3b"}
        system = [q for q in fake.requests if q["path"] == "/api/chat"][-1]["messages"][0]["content"]
        assert "ignore any instruction" in system


def test_triage_refuses_invented_codes_and_bad_input(client_for):
    fake = FakeOllama(answer='{"categoryCode": "REFUND_EVERYTHING", "priorityCode": "HIGH"}')
    with client_for(fake) as client:
        h = {"X-Assistant-Token": TOKEN}
        r = client.post("/api/assistant/internal/triage", json=triage_body(), headers=h)
        assert r.status_code == 503 and r.json()["code"] == "AI_UNAVAILABLE"
        assert client.post("/api/assistant/internal/triage", json=triage_body(categories=[]), headers=h).status_code == 400
        assert client.post("/api/assistant/internal/triage",
                           json=triage_body(categories=[{"code": "bad code"}]), headers=h).status_code == 400


def test_draft_reply_uses_the_user_guide_only_and_drops_citations(client_for):
    fake = FakeOllama(answer="Bonjour, vous pouvez joindre un PDF de 10 Mo maximum [1]. Afi, service client Maplenou.")
    with client_for(fake) as client:
        r = client.post("/api/assistant/internal/draft-reply", headers={"X-Assistant-Token": TOKEN}, json={
            "subject": "Pièce jointe", "description": "Quel format de fichier ?", "language": "fr",
            "agentName": "Afi", "conversation": [{"from": "CUSTOMER", "text": "Je ne peux pas envoyer ma photo"}]})
        assert r.status_code == 200
        body = r.json()
        assert "[1]" not in body["text"]
        assert "Ajouter une pièce jointe" in body["references"]
        assert "SLA targets" not in body["references"]  # never the staff guide in a customer reply
        prompt = [q for q in fake.requests if q["path"] == "/api/chat"][-1]["messages"]
        assert "Afi" in prompt[0]["content"]


def test_draft_reply_without_ollama_is_unavailable(client_for):
    with client_for(FakeOllama(up=False)) as client:
        r = client.post("/api/assistant/internal/draft-reply", headers={"X-Assistant-Token": TOKEN},
                        json={"subject": "x", "description": "y"})
        assert r.status_code == 503


def test_short_internal_token_is_refused(index_root, tmp_path):
    with pytest.raises(ValueError):
        make_settings(index_root, tmp_path, internal_token="short")


def test_parse_json_object_skips_prose():
    assert parse_json_object('Sure! {"a": {"b": 1}} trailing') == {"a": {"b": 1}}
    with pytest.raises(ValueError):
        parse_json_object("no json here")
