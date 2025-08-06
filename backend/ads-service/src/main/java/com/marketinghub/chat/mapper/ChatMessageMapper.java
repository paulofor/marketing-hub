package com.marketinghub.chat.mapper;

import com.marketinghub.chat.ChatMessage;
import com.marketinghub.chat.dto.ChatMessageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for ChatMessage.
 */
@Mapper(componentModel = "spring")
public interface ChatMessageMapper {
    @Mapping(source = "session.id", target = "sessionId")
    ChatMessageDto toDto(ChatMessage message);
}
