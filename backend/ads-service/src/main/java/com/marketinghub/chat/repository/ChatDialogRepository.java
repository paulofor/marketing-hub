package com.marketinghub.chat.repository;

import com.marketinghub.chat.ChatDialog;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for ChatDialog entities. */
public interface ChatDialogRepository extends JpaRepository<ChatDialog, Long> {
}

