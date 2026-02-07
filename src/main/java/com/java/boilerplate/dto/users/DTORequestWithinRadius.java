package com.java.boilerplate.dto.users;

import org.locationtech.jts.geom.Point;

public record DTORequestWithinRadius(
        Point point,
        Long radius
) { }
