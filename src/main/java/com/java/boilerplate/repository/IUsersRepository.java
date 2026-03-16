package com.java.boilerplate.repository;

import com.java.boilerplate.enums.GenderUser;
import com.java.boilerplate.model.Users;
import org.locationtech.jts.geom.Point;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IUsersRepository extends IBaseRepository<Users> {
    List<Users> findWithinRadius(@Param("point") Point point, @Param("radius") Long radius, @Param("userGender")GenderUser gender, @Param("requesterId") Long requesterId);
    Users findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);
    Boolean existsByPhoneNumber(String phoneNumber);
}
