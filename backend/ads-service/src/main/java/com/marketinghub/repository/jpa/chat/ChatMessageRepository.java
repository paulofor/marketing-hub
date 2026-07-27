package com.marketinghub.repository.jpa.chat;

import com.marketinghub.chat.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** JPA repository for ChatMessage. */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
  List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
