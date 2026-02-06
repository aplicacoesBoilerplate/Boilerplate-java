package com.java.boilerplate.dto.users;


import org.springframework.data.geo.Point;

public record DTOInsertLocationUser(
        Long idUser,
        Point location
) { }
