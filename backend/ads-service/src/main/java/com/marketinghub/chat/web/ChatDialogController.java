package com.marketinghub.chat.web;

import com.marketinghub.chat.dto.ChatDialogDto;
import com.marketinghub.chat.dto.CreateChatDialogRequest;
import com.marketinghub.chat.mapper.ChatDialogMapper;
import com.marketinghub.chat.service.ChatDialogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST controller for ChatDialog. */
@RestController
@RequestMapping("/api/chat-dialogs")
public class ChatDialogController {
    private final ChatDialogService service;
    private final ChatDialogMapper mapper;

    public ChatDialogController(ChatDialogService service, ChatDialogMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ChatDialogDto> list() {
        return service.findAll().stream().map(mapper::toDto).toList();
    }

    @PostMapping
    public ChatDialogDto create(@RequestBody CreateChatDialogRequest request) {
        return mapper.toDto(service.create(request));
    }
}

