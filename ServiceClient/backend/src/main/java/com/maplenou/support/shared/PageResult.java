package com.maplenou.support.shared;

import java.util.List;
import java.util.function.Function;

/** Stable pagination envelope for list use cases and endpoints. */
public record PageResult<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, totalItems, totalPages);
    }

    public static <T> PageResult<T> of(List<T> items, int page, int size, long totalItems) {
        int pages = size <= 0 ? 0 : (int) ((totalItems + size - 1) / size);
        return new PageResult<>(items, page, size, totalItems, pages);
    }
}
