ALTER TABLE chat_messages CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE chat_messages MODIFY message_content TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;