package com.java.boilerplate.controller.ChatContacts;

import com.java.boilerplate.model.ChatContacts;
import com.java.boilerplate.service.ChatContactsService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatContactsHandler {
    private final ChatContactsService  chatContactsService;

    public ChatContactsHandler(ChatContactsService chatContactsService) {
        this.chatContactsService = chatContactsService;
    }

    public ResponseEntity<List<ChatContacts>> getChatContacts(String username, Long nextEntry, int limit) {
        List<ChatContacts> chatContactsList = chatContactsService.getChatContacts(username, nextEntry, limit);
        return ResponseEntity.ok(chatContactsList);
    }

    public ResponseEntity<ChatContacts> updateContactStatus(Long receiverId, Long senderId, Boolean isBlocked) {
        ChatContacts contact = chatContactsService.updateContactStatus(receiverId, senderId, isBlocked);
        return ResponseEntity.ok(contact);
    }

    public ResponseEntity<String> removeContact(Long userId, Long contactId) {
        chatContactsService.removeContact(userId, contactId);
        return ResponseEntity.ok().body("Contact successfully removed!");
    }
}
