package com.marketinghub.chat.service;

import com.marketinghub.chat.ChatDialog;
import com.marketinghub.chat.dto.CreateChatDialogRequest;
import com.marketinghub.chat.repository.ChatDialogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Service layer for ChatDialog. */
@Service
public class ChatDialogService {
    private final ChatDialogRepository repository;

    public ChatDialogService(ChatDialogRepository repository) {
        this.repository = repository;
    }

    public List<ChatDialog> findAll() {
        return repository.findAll();
    }

    @Transactional
    public ChatDialog create(CreateChatDialogRequest request) {
        ChatDialog dialog = ChatDialog.builder()
                .url(request.getUrl())
                .description(request.getDescription())
                .theme(request.getTheme())
                .build();
        return repository.save(dialog);
    }
}

