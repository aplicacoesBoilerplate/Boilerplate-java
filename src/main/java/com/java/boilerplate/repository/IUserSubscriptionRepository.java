package com.java.boilerplate.repository;

import com.java.boilerplate.model.UserSubscription;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IUserSubscriptionRepository extends IBaseRepository<UserSubscription>{
    Optional<UserSubscription> findByUser_IdUser(@Param("idUser") Long idUser);
    Optional<LocalDateTime> findLatestExpirationByEmailHash(@Param("emailHash") String emailHash);
}
