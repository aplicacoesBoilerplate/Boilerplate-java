package com.java.boilerplate.service;

import com.java.boilerplate.dto.DTOChatContactResponse;
import com.java.boilerplate.model.ChatContacts;
import com.java.boilerplate.repository.IChatContactsRepository;
import com.java.boilerplate.repository.IUsersRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatContactsService {
    private final IChatContactsRepository chatContactsRepository;
    private final IUsersRepository usersRepository;
    private final AuthService authService;

    public ChatContactsService(IChatContactsRepository chatContactsRepository, IUsersRepository usersRepository, AuthService authService) {
        this.chatContactsRepository = chatContactsRepository;
        this.usersRepository = usersRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<DTOChatContactResponse> getChatContacts(Long nextEntry, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        String username = authService.getMe().getUserUsername();
        List<ChatContacts> contacts = chatContactsRepository.findContactsByUsername(username, nextEntry, pageable);
        return contacts.stream().map(DTOChatContactResponse::new).toList();
    }

    @Transactional
    public DTOChatContactResponse updateContactStatus(Long receiverId, Long senderId, Boolean isBlocked) {
        ChatContacts contact = chatContactsRepository.findByUser_IdUserAndContact_IdUser(receiverId, senderId)
            .map(existingContact -> {
                existingContact.setContactBlocked(isBlocked);
                return chatContactsRepository.save(existingContact);
            })
            .orElseGet(() -> {
                ChatContacts newContact = new ChatContacts();
                newContact.setUser(usersRepository.getReferenceById(receiverId));
                newContact.setContact(usersRepository.getReferenceById(senderId));
                newContact.setContactBlocked(isBlocked);
                return chatContactsRepository.save(newContact);
            });

        return new DTOChatContactResponse(contact);
    }

    @Transactional
    public void removeContact(Long userId, Long contactId) {
        chatContactsRepository.deleteByUser_IdUserAndContact_IdUser(userId, contactId);
    }

    @Transactional(readOnly = true)
    public Boolean checkBlockedContact(Long receiverId, Long senderId) {
        return chatContactsRepository.checkBlockedContact(receiverId, senderId);
    }
}
