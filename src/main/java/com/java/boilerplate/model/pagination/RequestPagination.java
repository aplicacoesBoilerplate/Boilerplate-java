package com.java.boilerplate.model.pagination;

import lombok.Data;

import java.util.List;

@Data
public class RequestPagination {
    private int limit;
    private int nextEntry;
    private List<RequestFilters> filters;
}
