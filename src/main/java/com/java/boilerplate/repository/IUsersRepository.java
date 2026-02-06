package com.java.boilerplate.repository;

import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.modelQueryJPA.UsersQueriesJPA;
import jakarta.transaction.Transactional;
import org.springframework.data.geo.Point;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface IUsersRepository extends IBaseRepository<Users> {
    DTOPagination<Users> findWithinRadius(@Param("point") Point point, @Param("radius") Long radius);
    UserDetails findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    @Modifying
    @Transactional
    @Query(value = UsersQueriesJPA.sqlInsertNewUserLocation, nativeQuery = true)
    void insertNewUserLocation(@Param("idUser") Long idUser, @Param("point") String point);
}
