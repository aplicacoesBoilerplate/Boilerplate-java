package com.java.boilerplate.controller.chatMessages;

import com.java.boilerplate.dto.chatMessages.DTOChatMessages;
import com.java.boilerplate.dto.chatMessages.DTORequestSendMessage;
import com.java.boilerplate.model.ChatMessages;
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
    public ResponseEntity<List<ChatMessages>> findConversation(
            @RequestParam Long currentUserId,
            @RequestParam Long contactId,
            @RequestParam Long nextEntry,
            @RequestParam(required = false, defaultValue = "20") int limit
    ) { return chatMessagesHandler.findConversation(currentUserId, contactId, nextEntry, limit); }
}
