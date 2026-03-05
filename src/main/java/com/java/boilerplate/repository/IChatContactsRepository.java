package com.java.boilerplate.repository;

import com.java.boilerplate.model.ChatContacts;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IChatContactsRepository extends IBaseRepository<ChatContacts> {
    Boolean checkBlockedContact(Long receiverId, Long senderId);
    Optional<ChatContacts> findByUser_IdUserAndContact_IdUser(Long userId, Long contactId);
    Boolean existsByUser_IdUserAndContact_IdUser(Long receiverId, Long senderId);
    void deleteContactRelation(@Param("userId") Long userId, @Param("contactId") Long contactId);
}
