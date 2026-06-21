package com.java.boilerplate.modelQueryJPA;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

@MappedSuperclass
@NamedQueries({
    @NamedQuery(name = "ChatContacts.checkBlockedContact", query = ChatContactsQueriesJPA.sqlCheckBlockedContact),
    @NamedQuery(name = "ChatContacts.findLastMessage", query = ChatMessagesQueriesJPA.sqlFindConversation),
    @NamedQuery(name = "ChatContacts.deleteContactRelation", query = ChatContactsQueriesJPA.sqlDeleteContactRelation)
})
public class ChatContactsQueriesJPA {
    static final String sqlCheckBlockedContact = """
        SELECT COUNT(c) > 0
        FROM ChatContacts c
        WHERE c.user.idUser = :receiverId
        AND c.contact.idUser = :senderId
        AND c.user.contextKey = :contextKey
        AND c.contact.contextKey = :contextKey
        AND c.contactBlocked = true
        """;

    static final String sqlDeleteContactRelation = """
        DELETE FROM ChatContacts c
        WHERE c.user.idUser = :userId
        AND c.contact.idUser = :contactId
        AND c.user.contextKey = :contextKey
        AND c.contact.contextKey = :contextKey
        """;
}
