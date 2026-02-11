package com.java.boilerplate.controller.chatMessages;

import com.java.boilerplate.dto.chatMessages.DTOChatMessages;
import com.java.boilerplate.dto.chatMessages.DTORequestSendMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class ChatMessagesController {
    private final ChatMessagesHandler chatMessagesHandler;

    public ChatMessagesController(ChatMessagesHandler chatMessagesHandler) {
        this.chatMessagesHandler = chatMessagesHandler;
    }

    @PostMapping("/send")
    public ResponseEntity<DTOChatMessages> sendMessage(
            @RequestBody DTORequestSendMessage request
    ) { return chatMessagesHandler.sendMessage(request); }

    @GetMapping
    public ResponseEntity<List<DTOChatMessages>> findConversation(
            @RequestParam Long contactId,
            @RequestParam(required = false, defaultValue = "0") Long nextEntry,
            @RequestParam(required = false, defaultValue = "20") int limit
    ) { return chatMessagesHandler.findConversation(contactId, nextEntry, limit); }

    @PatchMapping
    public ResponseEntity<Void> readMessage(
            @RequestBody List<Long> idMessages
    ) { return chatMessagesHandler.readMessage(idMessages); }
}
