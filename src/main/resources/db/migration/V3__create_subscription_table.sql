CREATE TABLE user_subscriptions (
    id BIGINT AUTO_INCREMENT COMMENT 'Unique subscription identifier',
    id_user BIGINT NOT NULL COMMENT 'Relationship with the subscriber user',
    expire_at DATETIME NOT NULL COMMENT 'Subscription expiration date',
    last_payment_id VARCHAR(255) COMMENT 'Identification of the last payment',
    status VARCHAR(50) NOT NULL COMMENT 'Subscription status',

    CONSTRAINT pk_subscription PRIMARY KEY (id),
    CONSTRAINT fk_subscription_user FOREIGN KEY (id_user) REFERENCES users_app(id_user),
    CONSTRAINT uc_subscription_user UNIQUE (id_user)
) ENGINE=InnoDB, COMMENT = 'Table for storing subscription payment information';
