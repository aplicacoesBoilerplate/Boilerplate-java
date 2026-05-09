CREATE TABLE push_subscription (
   id_subscription BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Unique push subscription identifier',
   id_user BIGINT NOT NULL COMMENT 'Reference to the user associated with this push subscription',
   endpoint TEXT NOT NULL UNIQUE COMMENT 'Unique browser push service endpoint used to deliver notifications',
   p256dh TEXT NOT NULL COMMENT 'Client public encryption key used for Web Push payload encryption',
   auth TEXT NOT NULL COMMENT 'Authentication secret used for secure Web Push message encryption',
   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp when the subscription was created',

   CONSTRAINT pk_push_sub PRIMARY KEY (id_subscription),
   CONSTRAINT fk_push_subscription_user FOREIGN KEY (id_user) REFERENCES users_app(id_user) ON DELETE CASCADE

) ENGINE=InnoDB COMMENT = 'Stores Web Push subscriptions associated with application users';

CREATE INDEX idx_push_subscription_user ON push_subscription(id_user);