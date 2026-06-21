package com.java.boilerplate.repository;

import com.java.boilerplate.enums.GenderUser;
import com.java.boilerplate.model.Users;
import org.locationtech.jts.geom.Point;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IUsersRepository extends IBaseRepository<Users> {
    Optional<Users> findByIdUserAndContextKey(Long idUser, String contextKey);
    List<Users> findWithinRadius(@Param("point") Point point, @Param("radius") Long radius, @Param("gender") GenderUser gender, @Param("requesterId") Long requesterId, @Param("contextKey") String contextKey, @Param("oppositeGenderOnly") Boolean oppositeGenderOnly);
    Users findByUsernameOrEmailAndContextKey(@Param("usernameOrEmail") String usernameOrEmail, @Param("contextKey") String contextKey);
    Boolean existsByPhoneNumber(String phoneNumber);
    Boolean existsByPhoneNumberAndContextKey(String phoneNumber, String contextKey);
}
