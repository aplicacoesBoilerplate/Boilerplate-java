package com.java.boilerplate.repository;

import com.java.boilerplate.model.UserOtp;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IUserOtpRepository extends IBaseRepository<UserOtp> {
    Optional<UserOtp> findByUser_IdUser(@Param("idUser") Long idUser);
}
