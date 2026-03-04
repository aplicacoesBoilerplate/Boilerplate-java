package com.java.boilerplate.repository;

import com.java.boilerplate.model.ChatMessages;
import com.java.boilerplate.model.Users;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IChatMessagesRepository extends IBaseRepository<ChatMessages> {
    List<ChatMessages> findConversation(@Param("myId") Long myId, @Param("contactId") Long contactId, @Param("nextEntryId") Integer nextEntryId, Pageable pageable );
    List<ChatMessages> findLastMessageExchange(@Param("user") Users user, @Param("contact") Users contact, Pageable pageable);
    Long countUnreadMessages(@Param("contactId") Long contactId, @Param("myId") Long myId);
}
