package com.maplenou.support.shared;

/** A page request, sanitized: page ≥ 0 and 1 ≤ size ≤ 100. */
public record PageQuery(int page, int size) {

    public PageQuery {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);
    }
}
