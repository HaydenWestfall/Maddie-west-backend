package com.maddiewest.events.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaginationMeta {

    private int current;
    private int pages;
    private int limit;
    private long total;

    public static PaginationMeta of(int page, int limit, long total) {
        int pages = limit > 0 ? (int) Math.ceil((double) total / limit) : 0;
        return new PaginationMeta(page, pages, limit, total);
    }
}
