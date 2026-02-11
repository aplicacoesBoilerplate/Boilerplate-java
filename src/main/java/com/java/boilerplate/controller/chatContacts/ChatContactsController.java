package com.java.boilerplate.controller.chatContacts;

import com.java.boilerplate.dto.DTOChatContactResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contacts")
public class ChatContactsController {
    private final ChatContactsHandler chatContactsHandler;

    public ChatContactsController(ChatContactsHandler chatContactsHandler) {
        this.chatContactsHandler = chatContactsHandler;
    }

    @GetMapping
    public ResponseEntity<List<DTOChatContactResponse>> getChatContacts(
            @RequestParam(required = false, defaultValue = "20") Long nextEntry,
            @RequestParam(required = false, defaultValue = "20") int limit
    ) { return chatContactsHandler.getChatContacts(nextEntry, limit); }

    @PostMapping
    public ResponseEntity<DTOChatContactResponse> updateContactStatus(
            @RequestParam Long senderId,
            @RequestParam(required = false, defaultValue = "false") Boolean isBlocked
    ) { return chatContactsHandler.updateContactStatus(senderId, isBlocked); }

    @DeleteMapping
    public ResponseEntity<String> removeContact(
            @RequestParam Long userId,
            @RequestParam Long contactId
    ) { return chatContactsHandler.removeContact(userId, contactId); }
}
