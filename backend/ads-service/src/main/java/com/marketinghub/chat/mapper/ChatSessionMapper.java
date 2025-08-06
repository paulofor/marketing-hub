package com.marketinghub.chat.mapper;

import com.marketinghub.chat.ChatSession;
import com.marketinghub.chat.dto.ChatSessionDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for ChatSession.
 */
@Mapper(componentModel = "spring")
public interface ChatSessionMapper {
    ChatSessionDto toDto(ChatSession session);
}
