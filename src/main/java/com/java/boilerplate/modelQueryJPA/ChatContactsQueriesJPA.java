package com.java.boilerplate.modelQueryJPA;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

@MappedSuperclass
@NamedQueries({
    @NamedQuery(name = "ChatContacts.checkBlockedContact", query = ChatContactsQueriesJPA.sqlCheckBlockedContact),
    @NamedQuery(name = "ChatContacts.findLastMessage", query = ChatMessagesQueriesJPA.sqlFindConversation),
    @NamedQuery(name = "ChatContacts.findContactsWithLastMessage", query = ChatContactsQueriesJPA.sqlFindContactsWithLastMessage)
})
public class ChatContactsQueriesJPA {
    static final String sqlCheckBlockedContact = """
        SELECT COUNT(c) > 0
        FROM ChatContacts c
        WHERE c.user.idUser = :receiverId
        AND c.contact.idUser = :senderId
        AND c.contactBlocked = true
        """;

    static final String sqlFindContactsWithLastMessage = """
        SELECT c, m.content, m.timestamp, (
            SELECT COUNT(m3) FROM ChatMessages m3 WHERE m3.sender = c.contact AND m3.receiver = c.user AND m3.read = false)
            FROM ChatContacts c
            LEFT JOIN ChatMessages m ON (
                (m.sender = c.user AND m.receiver = c.contact) OR
                (m.sender = c.contact AND m.receiver = c.user)
            )
            WHERE c.user.userUsername = :username
            AND c.contactBlocked = false
            AND (:nextEntryId IS NULL OR c.idChatContact < :nextEntryId)
            AND (m IS NULL OR m.idMessage = (
                SELECT MAX(m2.idMessage)
                FROM ChatMessages m2
                WHERE (m2.sender = c.user AND m2.receiver = c.contact)
                OR (m2.sender = c.contact AND m2.receiver = c.user)
            )
        )
        ORDER BY c.idChatContact DESC
        """;
}
