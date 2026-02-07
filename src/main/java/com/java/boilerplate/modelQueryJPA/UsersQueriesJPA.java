package com.java.boilerplate.modelQueryJPA;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.NamedQuery;

@MappedSuperclass
@NamedQuery(name = "Users.findWithinRadius", query = UsersQueriesJPA.sqlFindWithinRadius)
@NamedQuery(name = "Users.findByUsernameOrEmail", query = UsersQueriesJPA.sqlFindByUsernameOrEmail)
public class UsersQueriesJPA {
    static final String sqlFindWithinRadius = """
        SELECT u FROM Users u
        WHERE function(
            'ST_Distance_Sphere',
            u.location,
            :point
        ) <= :radius
        """;

    static final String sqlFindByUsernameOrEmail = """
        SELECT u FROM Users u
        WHERE u.userUsername = :usernameOrEmail
        OR u.email = :usernameOrEmail
        """;
}