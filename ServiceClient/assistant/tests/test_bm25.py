"""The inverted-index BM25 gives exactly the scores of the textbook formula (complexity review, DEC-CS12-06)."""

import math
import random
from collections import Counter

from app.index import Bm25


def naive(documents, query, k1=1.4, b=0.75):
    docs = [Counter(d) for d in documents]
    lengths = [len(d) for d in documents]
    avg = sum(lengths) / len(lengths)
    df = Counter()
    for d in docs:
        df.update(d.keys())
    n = len(docs)
    idf = {t: math.log(1 + (n - f + 0.5) / (f + 0.5)) for t, f in df.items()}
    out = []
    for doc, length in zip(docs, lengths):
        s = 0.0
        for t in query:
            f = doc.get(t)
            if f:
                norm = 1 - b + b * length / avg
                s += idf[t] * f * (k1 + 1) / (f + k1 * norm)
        out.append(s)
    return out


def test_same_scores_as_the_formula():
    rnd = random.Random(7)
    vocabulary = [f"w{i}" for i in range(40)]
    documents = [[rnd.choice(vocabulary) for _ in range(rnd.randint(1, 60))] for _ in range(120)]
    bm25 = Bm25(documents)
    for _ in range(30):
        query = [rnd.choice(vocabulary + ["absent"]) for _ in range(rnd.randint(1, 5))]
        expected = naive(documents, query)
        assert all(math.isclose(a, e, rel_tol=1e-12, abs_tol=1e-12) for a, e in zip(bm25.scores(query), expected))
        assert set(bm25.matches(query)) == {i for i, s in enumerate(expected) if s > 0}


def test_empty_index_and_unknown_terms():
    assert Bm25([]).scores(["x"]) == []
    assert Bm25([["a", "b"]]).matches(["zzz"]) == {}
