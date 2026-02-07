package com.java.boilerplate.model.pagination;

import lombok.Data;

import java.util.List;

@Data
public class RequestPagination {
    private Integer limit;
    private Integer nextEntry;
    private List<RequestFilters> filters;
}
