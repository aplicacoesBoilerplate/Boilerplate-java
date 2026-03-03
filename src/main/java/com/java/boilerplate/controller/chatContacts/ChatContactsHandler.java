package com.java.boilerplate.controller.chatContacts;

import com.java.boilerplate.dto.DTOChatContactResponse;
import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.model.pagination.RequestPagination;
import com.java.boilerplate.service.ChatContactsService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ChatContactsHandler {
    private final ChatContactsService  chatContactsService;

    public ChatContactsHandler(ChatContactsService chatContactsService) {
        this.chatContactsService = chatContactsService;
    }

    public ResponseEntity<DTOPagination<DTOChatContactResponse>> findChatContacts(RequestPagination request) {
        return ResponseEntity.ok(chatContactsService.findChatContacts(request));
    }

    public ResponseEntity<DTOChatContactResponse> updateContactStatus(Long receiverId, Boolean isBlocked) {
        return ResponseEntity.ok(chatContactsService.updateContactStatus(receiverId, isBlocked));
    }

    public ResponseEntity<String> removeContact(Long contactId) {
        chatContactsService.removeContact(contactId);
        return ResponseEntity.ok().body("Contact successfully removed!");
    }
}
