package com.campushub.common.model;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

public final class PageRequests {
    private PageRequests() {}

    public static void validateSort(String field, String direction, java.util.Set<String> allowed) {
        if (!allowed.contains(field) || !("asc".equalsIgnoreCase(direction) || "desc".equalsIgnoreCase(direction))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field or direction");
        }
    }

    public static PageRequest of(int page, int size, Sort sort) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0 and size must be > 0");
        }
        return PageRequest.of(page, Math.min(size, 100), sort.and(Sort.by("id")));
    }
}
