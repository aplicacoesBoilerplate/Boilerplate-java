package com.java.boilerplate.modelQueryJPA;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.NamedQuery;

@MappedSuperclass
@NamedQuery(name = "Users.findWithinRadius", query = UsersQueriesJPA.sqlFindWithinRadius)
@NamedQuery(name = "Users.findByUsernameOrEmail", query = UsersQueriesJPA.sqlFindByUsernameOrEmail)
public class UsersQueriesJPA {
    static final String sqlFindWithinRadius = """
            SELECT u FROM Users u
            WHERE function('dwithin', u.location, :point, :radius) = true
            """;

    static final String sqlFindByUsernameOrEmail = """
            SELECT u FROM Users u
            WHERE u.username = :usernameOrEmail
            OR u.email = :usernameOrEmail
            """;

    public static final String sqlInsertNewUserLocation = """
        INSERT INTO users_app (id_user, user_location)
        VALUES (:idUser, ST_GeomFromText(:point, 4326))
        ON DUPLICATE KEY UPDATE user_location = ST_GeomFromText(:point, 4326)
        """;
}