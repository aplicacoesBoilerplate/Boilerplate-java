package com.java.boilerplate.service;

import com.java.boilerplate.dto.chatMessages.DTOChatMessages;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.ChatMessages;
import com.java.boilerplate.repository.IChatMessagesRepository;
import com.java.boilerplate.repository.IUsersRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatMessagesService {
    private final IChatMessagesRepository chatMessagesRepository;
    private final IUsersRepository usersRepository;
    private final UsersService usersService;
    private final ChatContactsService chatContactsService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessagesService(IChatMessagesRepository chatMessagesRepository, IUsersRepository usersRepository, @Lazy UsersService usersService, ChatContactsService chatContactsService, SimpMessagingTemplate messagingTemplate) {
        this.chatMessagesRepository = chatMessagesRepository;
        this.usersRepository = usersRepository;
        this.usersService = usersService;
        this.chatContactsService = chatContactsService;
        this.messagingTemplate = messagingTemplate;
    }
    
    @Transactional
    public DTOChatMessages sendMessage(Long senderId, Long receiverId, String content) {
        boolean isBlocked = chatContactsService.checkBlockedContact(receiverId, senderId);

        if (isBlocked) {
            throw new ExceptionsSystem(
                    "You cannot send messages to this user",
                    HttpStatus.UNAUTHORIZED
            );
        }

        ChatMessages message = new ChatMessages();
        message.setSender(usersRepository.getReferenceById(senderId));
        message.setReceiver(usersRepository.getReferenceById(receiverId));
        message.setContent(content);
        message.setRead(false);
        message.setTimestamp(LocalDateTime.now());

        ChatMessages savedMessage = chatMessagesRepository.save(message);

        DTOChatMessages dto = new DTOChatMessages(
                savedMessage.getIdMessage(),
                senderId,
                receiverId,
                savedMessage.getContent(),
                savedMessage.getTimestamp(),
                savedMessage.getRead()
        );

        String usernameReceiver = usersService.findById(receiverId).getUsername();

        messagingTemplate.convertAndSendToUser(
                String.valueOf(usernameReceiver),
                "/queue/messages",
                dto
        );

        return dto;
    }

    @Transactional(readOnly = true)
    public List<ChatMessages> findConversation(Long currentUserId, Long contactId, Long nextEntry, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessages> messages = chatMessagesRepository.findConversation(
                currentUserId,
                contactId,
                nextEntry,
                pageable
        );

        return messages;
    }
}
