package com.java.boilerplate.controller.chatContacts;

import com.java.boilerplate.dto.DTOChatContactResponse;
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

    public ResponseEntity<List<DTOChatContactResponse>> getChatContacts(Long nextEntry, int limit) {
        return ResponseEntity.ok(chatContactsService.getChatContacts(nextEntry, limit));
    }

    public ResponseEntity<DTOChatContactResponse> updateContactStatus(Long senderId, Boolean isBlocked) {
        return ResponseEntity.ok(chatContactsService.updateContactStatus(senderId, isBlocked));
    }

    public ResponseEntity<String> removeContact(Long userId, Long contactId) {
        chatContactsService.removeContact(userId, contactId);
        return ResponseEntity.ok().body("Contact successfully removed!");
    }
}
