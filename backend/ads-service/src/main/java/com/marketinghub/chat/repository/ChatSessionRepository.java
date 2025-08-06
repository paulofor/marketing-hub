package com.marketinghub.chat.repository;

import com.marketinghub.chat.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for ChatSession.
 */
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {}
