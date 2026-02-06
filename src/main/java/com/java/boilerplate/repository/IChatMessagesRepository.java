package com.java.boilerplate.repository;

import com.java.boilerplate.model.ChatMessages;
import org.springframework.stereotype.Repository;

@Repository
public interface IChatMessagesRepository extends IBaseRepository<ChatMessages> { }
