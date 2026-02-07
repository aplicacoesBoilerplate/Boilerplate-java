package com.java.boilerplate.repository;

import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.model.Users;
import org.locationtech.jts.geom.Point;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IUsersRepository extends IBaseRepository<Users> {
    DTOPagination<Users> findWithinRadius(@Param("point") Point point, @Param("radius") Long radius);
    Users findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);
}
