package com.marketinghub.chat.service;

import com.marketinghub.chat.ChatMessage;
import com.marketinghub.chat.ChatSession;
import com.marketinghub.chat.dto.CreateChatMessageRequest;
import com.marketinghub.chat.dto.CreateChatSessionRequest;
import com.marketinghub.repository.jpa.chat.ChatMessageRepository;
import com.marketinghub.repository.jpa.chat.ChatSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for chat sessions and messages.
 */
@Service
public class ChatService {
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    public ChatService(ChatSessionRepository sessionRepository, ChatMessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * Creates and stores a chat session.
     */
    @Transactional
    public ChatSession createSession(CreateChatSessionRequest request) {
        ChatSession session = ChatSession.builder()
                .userId(request.getUserId())
                .channel(request.getChannel())
                .state(request.getState())
                .build();
        return sessionRepository.save(session);
    }

    public ChatSession getSession(Long id) {
        return sessionRepository.findById(id).orElseThrow();
    }

    /**
     * Adds a message to a session.
     */
    @Transactional
    public ChatMessage addMessage(CreateChatMessageRequest request) {
        ChatSession session = sessionRepository.findById(request.getSessionId()).orElseThrow();
        ChatMessage message = ChatMessage.builder()
                .session(session)
                .origin(request.getOrigin())
                .content(request.getContent())
                .build();
        return messageRepository.save(message);
    }

    /**
     * Retrieves messages for a session ordered by creation time.
     */
    public List<ChatMessage> getMessages(Long sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
}
