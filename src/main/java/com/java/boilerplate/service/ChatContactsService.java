package com.java.boilerplate.service;

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

    public ChatContactsService(IChatContactsRepository chatContactsRepository, IUsersRepository usersRepository) {
        this.chatContactsRepository = chatContactsRepository;
        this.usersRepository = usersRepository;
    }

    @Transactional(readOnly = true)
    public List<ChatContacts> getChatContacts(String username, Long nextEntry, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return chatContactsRepository.findContactsByUsername(username, nextEntry, pageable);
    }

    @Transactional
    public ChatContacts updateContactStatus(Long receiverId, Long senderId, Boolean isBlocked) {
        return chatContactsRepository.findByUser_IdUserAndContact_IdUser(receiverId, senderId)
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
