package com.java.boilerplate.service;

import com.java.boilerplate.dto.DTOChatContactResponse;
import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.model.ChatContacts;
import com.java.boilerplate.model.ChatMessages;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.model.pagination.RequestFilters;
import com.java.boilerplate.model.pagination.RequestPagination;
import com.java.boilerplate.repository.IChatContactsRepository;
import com.java.boilerplate.repository.IChatMessagesRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChatContactsService {
    private final IChatContactsRepository chatContactsRepository;
    private final IChatMessagesRepository chatMessagesRepository;
    private final AuthService authService;
    private final UsersService usersService;

    public ChatContactsService(IChatContactsRepository chatContactsRepository, IChatMessagesRepository chatMessagesRepository, AuthService authService, UsersService usersService) {
        this.chatContactsRepository = chatContactsRepository;
        this.chatMessagesRepository = chatMessagesRepository;
        this.authService = authService;
        this.usersService = usersService;
    }

    @Transactional(readOnly = true)
    public DTOPagination<DTOChatContactResponse> findChatContacts(RequestPagination request) {
        Users me = authService.getMe();

        RequestFilters myContactsFilter = new RequestFilters();
        myContactsFilter.setField("user.idUser");
        myContactsFilter.setCondition("equals");
        myContactsFilter.setValue(me.getIdUser().toString());

        RequestFilters userContextFilter = new RequestFilters();
        userContextFilter.setField("user.contextKey");
        userContextFilter.setCondition("equals");
        userContextFilter.setValue(me.getContextKey());

        RequestFilters contactContextFilter = new RequestFilters();
        contactContextFilter.setField("contact.contextKey");
        contactContextFilter.setCondition("equals");
        contactContextFilter.setValue(me.getContextKey());

        if (request.getFilters() == null) {
            request.setFilters(new ArrayList<>());
        }
        request.getFilters().add(myContactsFilter);
        request.getFilters().add(userContextFilter);
        request.getFilters().add(contactContextFilter);

        DTOPagination<ChatContacts> pagination = chatContactsRepository.findPaginationItens(request, "idChatContact");

        return pagination.map(contact -> {
            List<ChatMessages> lastMessageExchange = chatMessagesRepository.findLastMessageExchange(
                    contact.getUser(),
                    contact.getContact(),
                    PageRequest.of(0, 1)
            );

            ChatMessages lastMsg = lastMessageExchange.isEmpty() ? null : lastMessageExchange.get(0);
            Long unread = chatMessagesRepository.countUnreadMessages(
                    contact.getContact().getIdUser(),
                    me.getIdUser(),
                    me.getContextKey()
            );

            return new DTOChatContactResponse(
                    contact,
                    lastMsg != null ? lastMsg.getContent() : null,
                    lastMsg != null ? lastMsg.getTimestamp() : null,
                    unread,
                    contact.getContact().getIsOnline()
            );
        });
    }

    @Transactional
    public DTOChatContactResponse updateContactStatus(Long receiverId, Boolean isBlocked) {
        Users sender = authService.getMe();
        Users receiver = usersService.findById(receiverId);
        Long senderId = sender.getIdUser();
        String contextKey = sender.getContextKey();

        ChatContacts contact = chatContactsRepository.findByUser_IdUserAndContact_IdUserAndUser_ContextKeyAndContact_ContextKey(senderId, receiverId, contextKey, contextKey)
                .map(existingContact -> {
                    existingContact.setContactBlocked(isBlocked);
                    return chatContactsRepository.save(existingContact);
                })
                .orElseGet(() -> {
                    ChatContacts contactSender = new ChatContacts();
                    contactSender.setUser(sender);
                    contactSender.setContact(receiver);
                    contactSender.setContactBlocked(false);

                    Boolean receiveExist = chatContactsRepository.existsByUser_IdUserAndContact_IdUserAndUser_ContextKeyAndContact_ContextKey(receiverId, senderId, contextKey, contextKey);
                    if (!receiveExist) {
                        ChatContacts contactReceiver = new ChatContacts();
                        contactReceiver.setUser(receiver);
                        contactReceiver.setContact(sender);
                        contactReceiver.setContactBlocked(false);
                        chatContactsRepository.save(contactReceiver);
                    }

                    return chatContactsRepository.save(contactSender);
                });

        Boolean contactIsOnline = contact.getContact().getIsOnline();
        Long unreadCount = chatMessagesRepository.countUnreadMessages(senderId, receiverId, contextKey);

        List<ChatMessages> lastMessages = chatMessagesRepository.findConversation(
                receiverId,
                senderId,
                null,
                contextKey,
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
        Users me = authService.getMe();
        usersService.findById(contactId);
        chatContactsRepository.deleteContactRelation(me.getIdUser(), contactId, me.getContextKey());
    }

    @Transactional(readOnly = true)
    public Boolean checkBlockedContact(Long receiverId, Long senderId) {
        return chatContactsRepository.checkBlockedContact(receiverId, senderId, authService.getMe().getContextKey());
    }
}
