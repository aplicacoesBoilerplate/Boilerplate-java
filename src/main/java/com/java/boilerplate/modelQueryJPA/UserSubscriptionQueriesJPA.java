package com.java.boilerplate.modelQueryJPA;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

@MappedSuperclass
@NamedQueries({
    @NamedQuery(name = "UserSubscription.findLatestExpirationByEmailHash", query = UserSubscriptionQueriesJPA.sqlFindLatestExpirationByEmailHash)
})
public class UserSubscriptionQueriesJPA {
    static final String sqlFindLatestExpirationByEmailHash = """
        SELECT MAX(us.expireAt) FROM UserSubscription us
        WHERE us.user.emailHash = :emailHash
        AND us.contextKey = :contextKey
        """;
}
