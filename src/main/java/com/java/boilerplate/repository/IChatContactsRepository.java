package com.java.boilerplate.repository;

import com.java.boilerplate.model.ChatContacts;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IChatContactsRepository extends IBaseRepository<ChatContacts> {
    Boolean checkBlockedContact(Long receiverId, Long senderId);
    Optional<ChatContacts> findByUser_IdUserAndContact_IdUser(Long userId, Long contactId);
    void deleteByUser_IdUserAndContact_IdUser(Long userId, Long contactId);
    List<Object[]> findContactsWithLastMessage(@Param("username") String username, @Param("nextEntryId") Long nextEntryId, Pageable pageable);
}
