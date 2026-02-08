package com.java.boilerplate.controller.chatMessages;

import com.java.boilerplate.dto.chatMessages.DTOChatMessages;
import com.java.boilerplate.dto.chatMessages.DTORequestSendMessage;
import com.java.boilerplate.model.ChatMessages;
import com.java.boilerplate.service.ChatMessagesService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatMessagesHandler {
    private final ChatMessagesService chatMessagesService;

    public ChatMessagesHandler(ChatMessagesService chatMessagesService) {
        this.chatMessagesService = chatMessagesService;
    }

    public ResponseEntity<DTOChatMessages> sendMessage(DTORequestSendMessage request) {
        DTOChatMessages message = chatMessagesService.sendMessage(request.senderId(), request.receiverId(), request.content());
        return ResponseEntity.ok(message);
    }

    public ResponseEntity<List<ChatMessages>> findConversation(Long currentUserId, Long contactId, Long nextEntry, int limit) {
        List<ChatMessages> messages = chatMessagesService.findConversation(currentUserId, contactId, nextEntry, limit);
        return ResponseEntity.ok(messages);
    }
}
