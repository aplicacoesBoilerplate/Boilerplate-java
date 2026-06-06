package com.java.boilerplate.repository;

import com.java.boilerplate.model.ChatContacts;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IChatContactsRepository extends IBaseRepository<ChatContacts> {
    Boolean checkBlockedContact(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId, @Param("contextKey") String contextKey);
    Optional<ChatContacts> findByUser_IdUserAndContact_IdUserAndUser_ContextKeyAndContact_ContextKey(Long userId, Long contactId, String userContextKey, String contactContextKey);
    Boolean existsByUser_IdUserAndContact_IdUserAndUser_ContextKeyAndContact_ContextKey(Long receiverId, Long senderId, String userContextKey, String contactContextKey);
    @Modifying
    void deleteContactRelation(@Param("userId") Long userId, @Param("contactId") Long contactId, @Param("contextKey") String contextKey);
}
