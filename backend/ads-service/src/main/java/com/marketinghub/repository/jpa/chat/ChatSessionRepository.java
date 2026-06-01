package com.marketinghub.repository.jpa.chat;

import com.marketinghub.chat.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for ChatSession.
 */
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {}
