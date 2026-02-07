package com.java.boilerplate.service.helpers;

import com.java.boilerplate.dto.users.DTOLocation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    public Point createPoint(DTOLocation location) {
        return GEOMETRY_FACTORY.createPoint(
                new Coordinate(location.longitude(), location.latitude())
        );
    }
}
