package com.java.boilerplate.modelQueryJPA;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

@MappedSuperclass
@NamedQueries({
    @NamedQuery(name = "ChatContacts.checkBlockedContact", query = ChatContactsQueriesJPA.sqlCheckBlockedContact),
    @NamedQuery(name = "ChatContacts.findContactsByUsername", query = ChatContactsQueriesJPA.sqlFindContactsByUsername)
})
public class ChatContactsQueriesJPA {
    static final String sqlCheckBlockedContact = """
        SELECT COUNT(c) > 0
        FROM ChatContacts c
        WHERE c.user.idUser = :receiverId
        AND c.contact.idUser = :senderId
        AND c.contactBlocked = true
        """;

    static final String sqlFindContactsByUsername = """
        SELECT c FROM ChatContacts c
        WHERE c.user.userUsername = :username
        AND (:nextEntryId IS NULL OR c.idChatContact < :nextEntryId)
        ORDER BY c.idChatContact DESC
        """;
}
