CREATE TABLE users_app (
    id_user BIGINT AUTO_INCREMENT COMMENT 'Unique user identifier',
    user_full_name VARCHAR(255) NOT NULL COMMENT "User's full name",
    user_username VARCHAR(150) NOT NULL UNIQUE COMMENT 'Name by which users are found',
    user_bio TEXT COMMENT 'User biography, optional',
    user_gender VARCHAR(10) NOT NULL COMMENT "User's gender",
    user_avatar_url VARCHAR(255) COMMENT 'User profile photo url, optional',
    user_show_wpp_number BOOLEAN DEFAULT FALSE COMMENT 'Make your phone number available on your profile',
    user_phone_number VARCHAR(20) COMMENT "User's phone number",
    user_email VARCHAR(150) NOT NULL UNIQUE COMMENT "User's email",
    user_password VARCHAR(255) NOT NULL COMMENT "User's password",
    user_role VARCHAR(10) NOT NULL COMMENT "user's permission",
    user_location POINT SRID 4326 NOT NULL COMMENT "Stores the user's most recent location",
    user_online BOOLEAN DEFAULT FALSE COMMENT 'Online user',
    user_is_active BOOLEAN DEFAULT TRUE NOT NULL COMMENT 'Active user',

    CONSTRAINT pk_users PRIMARY KEY (id_user),
    CONSTRAINT check_user_gender CHECK (user_gender IN ('MALE', 'FEMALE')),
    CONSTRAINT check_user_role CHECK (user_role IN ('ADMIN', 'USER')),
    CONSTRAINT check_full_name_not_empty CHECK (CHAR_LENGTH(TRIM(user_full_name)) > 0),

    SPATIAL INDEX(user_location)
) ENGINE=InnoDB, COMMENT = 'Table to store various user information';

CREATE TABLE gallery_photos (
    id_gallery BIGINT AUTO_INCREMENT COMMENT 'Unique photo identifier',
    gallery_photo_url VARCHAR(255) NOT NULL COMMENT 'Photo url in gallery',
    id_user BIGINT NOT NULL COMMENT 'Foreign key for user, represents who posted this photo in the gallery',

    CONSTRAINT pk_photo PRIMARY KEY (id_gallery),
    CONSTRAINT fk_gallery_user FOREIGN KEY (id_user) REFERENCES users_app(id_user) ON DELETE CASCADE
) ENGINE=InnoDB, COMMENT = 'Table to store the url of photos that users post to the gallery';

CREATE TABLE chat_messages (
    id_message BIGINT AUTO_INCREMENT COMMENT 'Unique message identifier',
    id_user_send BIGINT NOT NULL COMMENT 'Relationship with the user responsible for sending the message',
    id_user_receiver BIGINT NOT NULL COMMENT 'Relationship with the user receiving the message',
    message_content TEXT NOT NULL COMMENT 'Message content',
    message_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Date/time stamp of message sending',
    message_is_read BOOLEAN DEFAULT FALSE COMMENT 'Marking for read message',

    CONSTRAINT pk_message PRIMARY KEY (id_message),
    CONSTRAINT fk_sender_user FOREIGN KEY (id_user_send) REFERENCES users_app(id_user),
    CONSTRAINT fk_receiver_user FOREIGN KEY (id_user_receiver) REFERENCES users_app(id_user)
) ENGINE=InnoDB, COMMENT = 'Table to store sent messages and their states between users';

CREATE TABLE chat_contacts (
    id_chat_contact BIGINT AUTO_INCREMENT COMMENT 'Unique contact identifier',
    id_user BIGINT NOT NULL COMMENT 'Relationship with the user who owns the list',
    contact_id BIGINT NOT NULL COMMENT 'Relationship with the user who is the contact',
    contact_blocked BOOLEAN DEFAULT FALSE NOT NULL COMMENT 'Blocked contact control',

    CONSTRAINT pk_chat_contact PRIMARY KEY (id_chat_contact),
    CONSTRAINT fk_chat_user FOREIGN KEY (id_user) REFERENCES users_app(id_user) ON DELETE CASCADE,
    CONSTRAINT fk_chat_contact FOREIGN KEY (contact_id) REFERENCES users_app(id_user) ON DELETE CASCADE,
    UNIQUE KEY uk_user_contact (id_user, contact_id)
) ENGINE=InnoDB, COMMENT = "Table to store a user's contact list as well as blocked contacts";
