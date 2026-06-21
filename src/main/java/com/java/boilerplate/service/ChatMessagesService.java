package com.java.boilerplate.service;

import com.java.boilerplate.config.TokensProperties;
import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.dto.DTOPushNotification;
import com.java.boilerplate.dto.chatMessages.DTOChatMessages;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.ChatMessages;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.model.pagination.RequestPagination;
import com.java.boilerplate.repository.IChatMessagesRepository;
import com.java.boilerplate.repository.IFileStorageService;
import com.java.boilerplate.service.context.AppContextService;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ChatMessagesService {
    private final IChatMessagesRepository chatMessagesRepository;
    private final UsersService usersService;
    private final ChatContactsService chatContactsService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuthService authService;
    private final SocketService socketService;
    private final IFileStorageService fileStorageService;
    private final PushNotificationService notificationService;
    private final TokensProperties properties;
    private final AppContextService appContextService;

    public ChatMessagesService(IChatMessagesRepository chatMessagesRepository, @Lazy UsersService usersService, ChatContactsService chatContactsService, SimpMessagingTemplate messagingTemplate, AuthService authService, SocketService socketService, IFileStorageService fileStorageService, PushNotificationService notificationService, TokensProperties properties, AppContextService appContextService) {
        this.chatMessagesRepository = chatMessagesRepository;
        this.usersService = usersService;
        this.chatContactsService = chatContactsService;
        this.messagingTemplate = messagingTemplate;
        this.authService = authService;
        this.socketService = socketService;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.properties = properties;
        this.appContextService = appContextService;
    }

    private ChatMessages findById(Long idMessage) {
        Users me = authService.getMe();
        ChatMessages message = chatMessagesRepository.findById(idMessage)
            .orElseThrow(() -> new ExceptionsSystem(
                "Message not found",
                HttpStatus.NOT_FOUND
            ));

        boolean sameContext = me.getContextKey().equals(message.getSender().getContextKey())
                && me.getContextKey().equals(message.getReceiver().getContextKey());
        boolean participant = me.getIdUser().equals(message.getSender().getIdUser())
                || me.getIdUser().equals(message.getReceiver().getIdUser());

        if (!sameContext || !participant) {
            throw new ExceptionsSystem("Message not found", HttpStatus.NOT_FOUND);
        }

        return message;
    }

    @Transactional
    public DTOChatMessages sendMessage(Long receiverId, String content, MultipartFile file) {
        Users sender = authService.getMe();
        Long senderId = authService.getMe().getIdUser();
        Users receiver = usersService.findById(receiverId);

        boolean hasContent = content != null && !content.trim().isEmpty();
        boolean hasFile = file != null && !file.isEmpty();

        if (!hasContent && !hasFile) {
            throw new ExceptionsSystem("Você não pode enviar uma mensagem vazia", HttpStatus.BAD_REQUEST);
        }

        boolean isBlocked = chatContactsService.checkBlockedContact(receiverId, senderId);
        if (isBlocked) {
            throw new ExceptionsSystem("O envio de mensagens foi bloqueada para esse usuário", HttpStatus.UNAUTHORIZED);
        }

        this.chatContactsService.updateContactStatus(receiverId, false);

        ChatMessages message = new ChatMessages();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);

        String prefix = "chat_from_" + senderId + "_to_" + receiverId;
        String fileUrl = fileStorageService.storeFile(file, prefix);
        message.setFileUrl(fileUrl);

        message.setRead(false);
        message.setTimestamp(LocalDateTime.now());

        ChatMessages savedMessage = chatMessagesRepository.save(message);
        DTOChatMessages dtoMessage = DTOChatMessages.fromEntity(savedMessage);
        String usernameReceiver = receiver.getUsername();
        socketService.notifyNewMessage(usernameReceiver, dtoMessage);

        try {
            DTOPushNotification pushNotification = new DTOPushNotification();
            pushNotification.setTitle(sender.getFullName());

            String body = hasFile ? "Enviou um arquivo 📎" : content;
            pushNotification.setBody(body);

            // URL para o frontend redirecionar ao chat específico
            String fullUrl = "/#/chat/" + sender.getUserUsername();
            pushNotification.setUrl(fullUrl);
            pushNotification.setContextKey(appContextService.getCurrentKey());

            notificationService.notifyUser(receiverId, pushNotification);
        } catch (Exception e) {
            System.err.println("Falha ao disparar Web Push: " + e.getMessage());
        }

        return dtoMessage;
    }

    @Transactional(readOnly = true)
    public DTOPagination<DTOChatMessages> findConversation(Long contactId, RequestPagination request) {
        Long currentUserId = authService.getMe().getIdUser();
        usersService.findById(contactId);

        int limit = (request.getLimit() != null && request.getLimit() > 0) ? request.getLimit() : 20;
        Integer nextEntry = (request.getNextEntry() != null && request.getNextEntry() > 0)
                ? request.getNextEntry()
                : null;

        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessages> messages = chatMessagesRepository.findConversation(
                currentUserId,
                contactId,
                nextEntry,
                appContextService.getCurrentKey(),
                pageable
        );

        Integer newNextEntry = messages.isEmpty() ? null : messages.get(0).getIdMessage().intValue();

        if (!messages.isEmpty()) {
            Collections.reverse(messages);
        }

        List<DTOChatMessages> dtoMessages = messages.stream().map(DTOChatMessages::fromEntity).toList();
        Boolean hasMore = messages.size() == limit;

        return new DTOPagination<>(
                limit,
                newNextEntry,
                0,
                hasMore,
                dtoMessages
        );
    }

    @Transactional
    public void readMessage(List<Long> idMessages) {
        List<ChatMessages> messages = idMessages.stream().map(this::findById).toList();
        messages.forEach(msg -> msg.setRead(true));
        chatMessagesRepository.saveAll(messages);
    }

    @Transactional
    public void clearChatHistory(Long contactId) {
        Long userId = authService.getMe().getIdUser();
        usersService.findById(contactId);
        String contextKey = appContextService.getCurrentKey();
        List<ChatMessages> messagesWithFiles = chatMessagesRepository.findMessagesWithFiles(userId, contactId, contextKey);
        messagesWithFiles.forEach(msg -> {
            if (msg.getFileUrl() != null) {
                fileStorageService.deleteFile(msg.getFileUrl());
            }
        });

        chatMessagesRepository.deleteConversation(userId, contactId, contextKey);
    }
}
