package com.java.boilerplate.modelQueryJPA;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

@MappedSuperclass
@NamedQueries({
    @NamedQuery(name = "Users.findWithinRadius", query = UsersQueriesJPA.sqlFindWithinRadius),
    @NamedQuery(name = "Users.findByUsernameOrEmailAndContextKey", query = UsersQueriesJPA.sqlFindByUsernameOrEmailAndContextKey)
})
public class UsersQueriesJPA {
    static final String sqlFindWithinRadius = """
        SELECT u FROM Users u
        WHERE function('ST_Distance_Sphere', u.location, :point) <= :radius
        AND u.contextKey = :contextKey
        AND (:oppositeGenderOnly = false OR u.userGender != :gender)
        AND u.isActive = true
        AND u.idUser != :requesterId
        AND NOT EXISTS (
            SELECT c
            FROM ChatContacts c
            WHERE c.user = u
            AND c.contact.idUser = :requesterId
            AND c.contactBlocked = true
        )
        AND NOT EXISTS (
            SELECT c2 FROM ChatContacts c2\s
            WHERE c2.user.idUser = :requesterId\s
            AND c2.contact = u\s
            AND c2.contactBlocked = true
        )
        """;

    static final String sqlFindByUsernameOrEmailAndContextKey = """
        SELECT u FROM Users u
        WHERE u.contextKey = :contextKey
        AND (u.userUsername = :usernameOrEmail OR u.email = :usernameOrEmail)
        """;
}
