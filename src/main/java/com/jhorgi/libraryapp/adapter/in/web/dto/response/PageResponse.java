package com.jhorgi.libraryapp.adapter.in.web.dto.response;

import com.jhorgi.libraryapp.domain.model.PagedResult;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <D, T> PageResponse<T> from(PagedResult<D> result, Function<D, T> mapper) {
        return new PageResponse<>(
                result.items().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalItems(),
                result.totalPages()
        );
    }
}
