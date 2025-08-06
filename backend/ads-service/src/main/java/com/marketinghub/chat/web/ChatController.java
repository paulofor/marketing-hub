package com.marketinghub.chat.web;

import com.marketinghub.chat.dto.*;
import com.marketinghub.chat.mapper.ChatMessageMapper;
import com.marketinghub.chat.mapper.ChatSessionMapper;
import com.marketinghub.chat.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for chat sessions and messages.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService service;
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    public ChatController(ChatService service, ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper) {
        this.service = service;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }

    @PostMapping("/sessions")
    public ChatSessionDto createSession(@RequestBody CreateChatSessionRequest request) {
        return sessionMapper.toDto(service.createSession(request));
    }

    @PostMapping("/messages")
    public ChatMessageDto addMessage(@RequestBody CreateChatMessageRequest request) {
        return messageMapper.toDto(service.addMessage(request));
    }

    @GetMapping("/sessions/{id}/messages")
    public List<ChatMessageDto> getMessages(@PathVariable Long id) {
        return service.getMessages(id).stream().map(messageMapper::toDto).toList();
    }
}
