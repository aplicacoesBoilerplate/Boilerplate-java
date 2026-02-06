package com.java.boilerplate.repository;

import com.java.boilerplate.model.ChatContacts;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IChatContactsRepository extends IBaseRepository<ChatContacts> {
    Boolean checkBlockedContact(Long receiverId, Long senderId);
}
