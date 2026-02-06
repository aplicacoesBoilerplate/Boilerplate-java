package com.java.boilerplate.modelQueryJPA;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.NamedQuery;

@MappedSuperclass
@NamedQuery(name = "ChatContacts.checkBlockedContact", query = ChatContactsQueriesJPA.sqlCheckBlockedContact)
public class ChatContactsQueriesJPA {
    static final String sqlCheckBlockedContact = """
            SELECT c.contactBlocked
            FROM ChatContacts c
            WHERE c.user.idUser = :receiverId
            AND c.contact.idUser = :senderId
            """;
}
