package com.java.boilerplate.modelQueryJPA;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

@MappedSuperclass
@NamedQueries({
    @NamedQuery(name = "ChatMessages.findConversation", query = ChatMessagesQueriesJPA.sqlFindConversation),
    @NamedQuery(name = "ChatMessages.findLastMessageExchange", query = ChatMessagesQueriesJPA.sqlFindLastMessageExchange),
    @NamedQuery(name = "ChatMessages.countUnreadMessages", query = ChatMessagesQueriesJPA.sqlCountUnreadMessages),
    @NamedQuery(name = "ChatMessages.findMessagesWithFiles", query = ChatMessagesQueriesJPA.sqlFindMessagesWithFiles),
    @NamedQuery(name = "ChatMessages.deleteConversation", query = ChatMessagesQueriesJPA.sqlDeleteConversation),
})
public class ChatMessagesQueriesJPA {
    static final String sqlFindConversation = """
        SELECT m FROM ChatMessages m
        WHERE (
            (m.sender.idUser = :userId AND m.receiver.idUser = :contactId)
            OR
            (m.sender.idUser = :contactId AND m.receiver.idUser = :userId)
        )
        AND (:nextEntryId IS NULL OR m.idMessage < :nextEntryId)
        ORDER BY m.idMessage DESC
        """;

    static final String sqlFindLastMessageExchange = """
        SELECT m FROM ChatMessages m
        WHERE (m.sender = :user AND m.receiver = :contact)
        OR (m.sender = :contact AND m.receiver = :user)
        ORDER BY m.timestamp DESC
        """;

    static final String sqlCountUnreadMessages = """
        SELECT COUNT(m) FROM ChatMessages m
        WHERE m.sender.idUser = :contactId
        AND m.receiver.idUser = :userId
        AND m.read = false
        """;

    static final String sqlFindMessagesWithFiles = """
        SELECT m FROM ChatMessages m WHERE
        ((m.sender.idUser = :userId AND m.receiver.idUser = :contactId) OR
        (m.sender.idUser = :contactId AND m.receiver.idUser = :userId)) AND
        m.fileUrl IS NOT NULL
        """;

    static final String sqlDeleteConversation = """
        DELETE FROM ChatMessages m WHERE
        (m.sender.idUser = :userId AND m.receiver.idUser = :contactId) OR
        (m.sender.idUser = :contactId AND m.receiver.idUser = :userId)
        """;
}
