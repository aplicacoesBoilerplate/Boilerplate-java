package com.java.boilerplate.modelQueryJPA;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

@MappedSuperclass
@NamedQueries({
        @NamedQuery(name = "ChatMessages.findConversation", query = ChatMessagesQueriesJPA.sqlFindConversation),
        @NamedQuery(name = "ChatMessages.countUnreadMessages", query = ChatMessagesQueriesJPA.sqlCountUnreadMessages)
})
public class ChatMessagesQueriesJPA {
    static final String sqlFindConversation = """
        SELECT m FROM ChatMessages m
        WHERE (
            (m.sender.idUser = :myId AND m.receiver.idUser = :contactId)
            OR
            (m.sender.idUser = :contactId AND m.receiver.idUser = :myId)
        )
        AND (:nextEntryId IS NULL OR m.idMessage < :nextEntryId)
        ORDER BY m.idMessage DESC
        """;

    static final String sqlCountUnreadMessages = """
        SELECT COUNT(m) FROM ChatMessages m
        WHERE m.sender.idUser = :contactId
        AND m.receiver.idUser = :myId
        AND m.read = false
        """;
}
