package com.jhorgi.libraryapp.domain.model;

import java.util.List;

public record PagedResult<T>(List<T> items, int page, int size, long totalItems) {

    public int totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (int) ((totalItems + size - 1) / size);
    }
}
