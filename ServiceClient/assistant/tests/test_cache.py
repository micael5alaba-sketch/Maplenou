from app.cache import TtlLru


def test_lru_is_bounded_and_expires():
    c = TtlLru(2, ttl_seconds=10)
    c.put("a", 1, now=0)
    c.put("b", 2, now=0)
    assert c.get("a", now=1) == 1  # "a" becomes most recent
    c.put("c", 3, now=1)            # evicts "b"
    assert c.get("b", now=1) is None
    assert len(c) == 2
    assert c.get("a", now=20) is None  # expired


def test_disabled_cache_keeps_nothing():
    c = TtlLru(0, ttl_seconds=10)
    c.put("a", 1)
    assert len(c) == 0
