package com.java.boilerplate.service;

import com.java.boilerplate.dto.DTOChatContactResponse;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.ChatContacts;
import com.java.boilerplate.model.ChatMessages;
import com.java.boilerplate.repository.IChatContactsRepository;
import com.java.boilerplate.repository.IChatMessagesRepository;
import com.java.boilerplate.repository.IUsersRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatContactsService {
    private final IChatContactsRepository chatContactsRepository;
    private final IChatMessagesRepository chatMessagesRepository;
    private final IUsersRepository usersRepository;
    private final AuthService authService;

    public ChatContactsService(IChatContactsRepository chatContactsRepository, IChatMessagesRepository chatMessagesRepository, IUsersRepository usersRepository, AuthService authService) {
        this.chatContactsRepository = chatContactsRepository;
        this.chatMessagesRepository = chatMessagesRepository;
        this.usersRepository = usersRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<DTOChatContactResponse> getChatContacts(Long nextEntry, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        String username = authService.getMe().getUserUsername();

        List<Object[]> results = chatContactsRepository.findContactsWithLastMessage(username, nextEntry, pageable);

        return results.stream().map(result -> {
            ChatContacts contact = (ChatContacts) result[0];
            String lastMessage = (String) result[1];
            LocalDateTime lastTime = (LocalDateTime) result[2];
            Long unreadCount = (Long) result[3];

            Boolean contactIsOnline = usersRepository.findById(contact.getUser().getIdUser())
                    .orElseThrow(() -> new ExceptionsSystem(
                            "User not found",
                            HttpStatus.NOT_FOUND
                    )).getOnline();

            return new DTOChatContactResponse(contact, lastMessage, lastTime, unreadCount, contactIsOnline);
        }).toList();
    }

    @Transactional
    public DTOChatContactResponse updateContactStatus(Long receiverId, Boolean isBlocked) {
        Long senderId = authService.getMe().getIdUser();
        ChatContacts contact = chatContactsRepository.findByUser_IdUserAndContact_IdUser(senderId, receiverId)
                .map(existingContact -> {
                    existingContact.setContactBlocked(isBlocked);
                    return chatContactsRepository.save(existingContact);
                })
                .orElseGet(() -> {
                    ChatContacts contactReceiver = new ChatContacts();
                    contactReceiver.setUser(usersRepository.getReferenceById(senderId));
                    contactReceiver.setContact(usersRepository.getReferenceById(receiverId));
                    contactReceiver.setContactBlocked(false);
                    chatContactsRepository.save(contactReceiver);
                    
                    ChatContacts newContact = new ChatContacts();
                    newContact.setUser(usersRepository.getReferenceById(receiverId));
                    newContact.setContact(usersRepository.getReferenceById(senderId));
                    newContact.setContactBlocked(isBlocked);
                    return chatContactsRepository.save(newContact);
                });

        Boolean contactIsOnline = contact.getContact().getOnline();
        Long unreadCount = chatMessagesRepository.countUnreadMessages(senderId, receiverId);

        List<ChatMessages> lastMessages = chatMessagesRepository.findConversation(
                receiverId,
                senderId,
                null,
                PageRequest.of(0, 1)
        );

        String lastContent = null;
        LocalDateTime lastTime = null;

        if (!lastMessages.isEmpty()) {
            ChatMessages msg = lastMessages.get(0);
            lastContent = msg.getContent();
            lastTime = msg.getTimestamp();
        }

        return new DTOChatContactResponse(contact, lastContent, lastTime, unreadCount, contactIsOnline);
    }

    @Transactional
    public void removeContact(Long contactId) {
        Long userId = authService.getMe().getIdUser();
        chatContactsRepository.deleteByUser_IdUserAndContact_IdUser(userId, contactId);
    }

    @Transactional(readOnly = true)
    public Boolean checkBlockedContact(Long receiverId, Long senderId) {
        return chatContactsRepository.checkBlockedContact(receiverId, senderId);
    }
}
