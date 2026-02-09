package com.java.boilerplate.dto;

import java.util.List;
import java.util.function.Function;

public record DTOPagination<T>(
        Integer limit,
        Integer nextEntry,
        Integer total,
        Boolean hasMore,
        List<T> itens
) {
    public static <T> DTOPagination<T> fromEntity(
            Integer limit,
            Integer nextEntry,
            Integer total,
            Boolean hasMore,
            List<T> entity
    ) {
        return new DTOPagination<>(
                limit,
                nextEntry,
                total,
                hasMore,
                entity
        );
    }

    public <R> DTOPagination<R> map(Function<T, R> mapper) {
        List<R> mappedItens = this.itens.stream()
                .map(mapper)
                .toList();

        return new DTOPagination<>(
                this.limit,
                this.nextEntry,
                this.total,
                this.hasMore,
                mappedItens
        );
    }
}