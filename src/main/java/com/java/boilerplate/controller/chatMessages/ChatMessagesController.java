package com.java.boilerplate.controller.chatMessages;

import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.dto.chatMessages.DTOChatMessages;
import com.java.boilerplate.model.pagination.RequestPagination;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class ChatMessagesController {
    private final ChatMessagesHandler chatMessagesHandler;

    public ChatMessagesController(ChatMessagesHandler chatMessagesHandler) {
        this.chatMessagesHandler = chatMessagesHandler;
    }

    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DTOChatMessages> sendMessage(
            @RequestParam Long receiverId,
            @RequestParam(required = false) String content,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) { return chatMessagesHandler.sendMessage(receiverId, content, file); }


    @PostMapping
    public ResponseEntity<DTOPagination<DTOChatMessages>> findConversation(
            @RequestParam Long contactId,
            @RequestBody RequestPagination request
    ) { return chatMessagesHandler.findConversation(contactId, request); }

    @PatchMapping
    public ResponseEntity<Void> readMessage(
            @RequestBody List<Long> idMessages
    ) { return chatMessagesHandler.readMessage(idMessages); }
}
