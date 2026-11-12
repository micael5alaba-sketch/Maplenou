from app.index import SiteIndex, load_sections, terms
from app.text import fold, html_to_text


def test_text_helpers():
    assert fold("Pièce JOINTE") == "piece jointe"
    assert html_to_text("<p>A &amp; <b>B</b></p>") == "A & B"
    assert terms("Comment ajouter des pièces jointes ?") == ["ajouter", "piece", "jointe"]


def test_loads_sections_with_anchors(index_root):
    sections = load_sections(index_root, "user", "fr")
    assert len(sections) == 4
    attachment = sections[1]
    assert attachment.location == "tickets/creer/#ajouter-une-piece-jointe"
    assert attachment.breadcrumb == "Créer un ticket › Ajouter une pièce jointe"
    assert attachment.url("http://docs") == "http://docs/user/fr/tickets/creer/#ajouter-une-piece-jointe"


def test_missing_index_is_empty(index_root):
    assert load_sections(index_root, "developer", "fr") == []


def test_lexical_search_ranks_the_right_section(index_root):
    index = SiteIndex(load_sections(index_root, "user", "fr"))
    best = index.search("Quels formats de pièce jointe sont acceptés ?", k=3)
    assert best[0][0].title == "Ajouter une pièce jointe"
    assert index.search("rouvrir mon ticket", k=1)[0][0].title == "Rouvrir un ticket"
    assert index.search("xylophone quantique", k=3) == []
