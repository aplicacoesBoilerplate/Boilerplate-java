package com.java.boilerplate.controller.chatMessages;

import com.java.boilerplate.dto.chatMessages.DTOChatMessages;
import com.java.boilerplate.service.ChatMessagesService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class ChatMessagesHandler {
    private final ChatMessagesService chatMessagesService;

    public ChatMessagesHandler(ChatMessagesService chatMessagesService) {
        this.chatMessagesService = chatMessagesService;
    }

    public ResponseEntity<DTOChatMessages> sendMessage(Long receiverId, String content, MultipartFile file) {
        return ResponseEntity.ok(chatMessagesService.sendMessage(receiverId, content, file));
    }

    public ResponseEntity<List<DTOChatMessages>> findConversation(Long contactId, Long nextEntry, int limit) {
        return ResponseEntity.ok(chatMessagesService.findConversation(contactId, nextEntry, limit));
    }

    public ResponseEntity<Void> readMessage(List<Long> idMessages) {
        chatMessagesService.readMessage(idMessages);
        return ResponseEntity.ok().build();
    }
}
