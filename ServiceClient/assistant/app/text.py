"""Text helpers: HTML to plain text, accent-insensitive tokens, stop words (FR + EN)."""

from __future__ import annotations

import html
import re
import unicodedata

_TAG = re.compile(r"<[^>]+>")
_SPACE = re.compile(r"\s+")
_TOKEN = re.compile(r"[a-z0-9]+")

STOP_WORDS = frozenset(
    """
    a au aux avec ce ces cet cette dans de des du elle en est et il ils je la le les leur lui ma mais me mes mon ne
    nos notre nous on ou par pas pour qu que qui sa se ses son sur ta te tes toi ton tu un une vos votre vous y
    comment quoi quel quelle quels quelles est-ce puis peut dois faire fait etre avoir
    the an and are as at be by can do does for from how i if in is it its me my of on or our so that the their
    them then there these they this to was we what when where which who why will with you your
    """.split()
)


def html_to_text(value: str) -> str:
    return _SPACE.sub(" ", html.unescape(_TAG.sub(" ", value or ""))).strip()


def fold(value: str) -> str:
    """Lower case without accents: "Pièce jointe" -> "piece jointe"."""
    decomposed = unicodedata.normalize("NFKD", value.lower())
    return "".join(c for c in decomposed if not unicodedata.combining(c))


def tokens(value: str) -> list[str]:
    return [t for t in _TOKEN.findall(fold(value)) if t not in STOP_WORDS and len(t) > 1]
