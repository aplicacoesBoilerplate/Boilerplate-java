package com.java.boilerplate.model.pagination;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RequestFilters {
    private String field;
    private String condition;
    private String value;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<Object> selectValues;
}
