package com.java.boilerplate.dto.users;

import org.springframework.data.geo.Point;

public record DTORequestWithinRadius(
        Point point,
        Long radius
) { }
