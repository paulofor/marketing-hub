package com.marketinghub.repository.jpa.chat;

import com.marketinghub.chat.ChatDialog;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for ChatDialog entities. */
public interface ChatDialogRepository extends JpaRepository<ChatDialog, Long> {
}

