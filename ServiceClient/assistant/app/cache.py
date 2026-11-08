"""Small bounded caches. Everything the assistant keeps in memory has a maximum size, so a busy instance
cannot grow without limit (see docs/guide/caching.md)."""

from __future__ import annotations

import time
from collections import OrderedDict
from typing import Generic, Hashable, TypeVar

K = TypeVar("K", bound=Hashable)
V = TypeVar("V")


class TtlLru(Generic[K, V]):
    """Least-recently-used cache with a time-to-live. size <= 0 disables it."""

    def __init__(self, size: int, ttl_seconds: float) -> None:
        self.size = size
        self.ttl = ttl_seconds
        self._data: OrderedDict[K, tuple[float, V]] = OrderedDict()
        self.hits = 0
        self.misses = 0

    def get(self, key: K, now: float | None = None) -> V | None:
        now = time.monotonic() if now is None else now
        item = self._data.get(key)
        if item is None or now - item[0] > self.ttl:
            if item is not None:
                del self._data[key]
            self.misses += 1
            return None
        self._data.move_to_end(key)
        self.hits += 1
        return item[1]

    def put(self, key: K, value: V, now: float | None = None) -> None:
        if self.size <= 0:
            return
        self._data[key] = (time.monotonic() if now is None else now, value)
        self._data.move_to_end(key)
        while len(self._data) > self.size:
            self._data.popitem(last=False)

    def clear(self) -> None:
        self._data.clear()

    def __len__(self) -> int:
        return len(self._data)
