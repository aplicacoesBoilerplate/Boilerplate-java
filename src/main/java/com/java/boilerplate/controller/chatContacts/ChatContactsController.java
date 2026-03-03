package com.java.boilerplate.controller.chatContacts;

import com.java.boilerplate.dto.DTOChatContactResponse;
import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.model.pagination.RequestPagination;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contacts")
public class ChatContactsController {
    private final ChatContactsHandler chatContactsHandler;

    public ChatContactsController(ChatContactsHandler chatContactsHandler) {
        this.chatContactsHandler = chatContactsHandler;
    }

    @PostMapping
    public ResponseEntity<DTOPagination<DTOChatContactResponse>> findChatContacts(
            @RequestBody RequestPagination request
    ) { return chatContactsHandler.findChatContacts(request); }

    @PutMapping
    public ResponseEntity<DTOChatContactResponse> updateContactStatus(
            @RequestParam Long receiverId,
            @RequestParam(required = false, defaultValue = "false") Boolean isBlocked
    ) { return chatContactsHandler.updateContactStatus(receiverId, isBlocked); }

    @DeleteMapping
    public ResponseEntity<String> removeContact(
            @RequestParam Long contactId
    ) { return chatContactsHandler.removeContact(contactId); }
}
