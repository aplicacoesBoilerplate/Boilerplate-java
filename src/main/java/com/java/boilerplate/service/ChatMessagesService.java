package com.java.boilerplate.service;

import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.dto.chatMessages.DTOChatMessages;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.ChatMessages;
import com.java.boilerplate.model.pagination.RequestPagination;
import com.java.boilerplate.repository.IChatMessagesRepository;
import com.java.boilerplate.repository.IFileStorageService;
import com.java.boilerplate.repository.IUsersRepository;
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
    private final IUsersRepository usersRepository;
    private final UsersService usersService;
    private final ChatContactsService chatContactsService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuthService authService;
    private final SocketService socketService;
    private final IFileStorageService fileStorageService;

    public ChatMessagesService(IChatMessagesRepository chatMessagesRepository, IUsersRepository usersRepository, @Lazy UsersService usersService, ChatContactsService chatContactsService, SimpMessagingTemplate messagingTemplate, AuthService authService, SocketService socketService, IFileStorageService fileStorageService) {
        this.chatMessagesRepository = chatMessagesRepository;
        this.usersRepository = usersRepository;
        this.usersService = usersService;
        this.chatContactsService = chatContactsService;
        this.messagingTemplate = messagingTemplate;
        this.authService = authService;
        this.socketService = socketService;
        this.fileStorageService = fileStorageService;
    }

    private ChatMessages findById(Long idMessage) {
        return chatMessagesRepository.findById(idMessage)
                .orElseThrow(() -> new ExceptionsSystem(
                        "Message not found",
                        HttpStatus.NOT_FOUND
                ));
    }

    @Transactional
    public DTOChatMessages sendMessage(Long receiverId, String content, MultipartFile file) {
        Long senderId = authService.getMe().getIdUser();

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
        message.setSender(usersRepository.getReferenceById(senderId));
        message.setReceiver(usersRepository.getReferenceById(receiverId));
        message.setContent(content);

        String prefix = "chat_from_" + senderId + "_to_" + receiverId;
        String fileUrl = fileStorageService.storeFile(file, prefix);
        message.setFileUrl(fileUrl);

        message.setRead(false);
        message.setTimestamp(LocalDateTime.now());

        ChatMessages savedMessage = chatMessagesRepository.save(message);
        DTOChatMessages dtoMessage = DTOChatMessages.fromEntity(savedMessage);
        String usernameReceiver = usersService.findById(receiverId).getUsername();
        socketService.notifyNewMessage(usernameReceiver, dtoMessage);
        return dtoMessage;
    }

    @Transactional(readOnly = true)
    public DTOPagination<DTOChatMessages> findConversation(Long contactId, RequestPagination request) {
        Long currentUserId = authService.getMe().getIdUser();

        int limit = (request.getLimit() != null && request.getLimit() > 0) ? request.getLimit() : 20;
        Integer nextEntry = (request.getNextEntry() != null && request.getNextEntry() > 0)
                ? request.getNextEntry()
                : null;

        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessages> messages = chatMessagesRepository.findConversation(
                currentUserId,
                contactId,
                nextEntry,
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
        List<ChatMessages> messagesWithFiles = chatMessagesRepository.findMessagesWithFiles(userId, contactId);
        messagesWithFiles.forEach(msg -> {
            if (msg.getFileUrl() != null) {
                fileStorageService.deleteFile(msg.getFileUrl());
            }
        });

        chatMessagesRepository.deleteConversation(userId, contactId);
    }
}
