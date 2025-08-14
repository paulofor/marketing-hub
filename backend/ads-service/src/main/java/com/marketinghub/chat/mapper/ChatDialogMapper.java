package com.marketinghub.chat.mapper;

import com.marketinghub.chat.ChatDialog;
import com.marketinghub.chat.dto.ChatDialogDto;
import org.mapstruct.Mapper;

/** Mapper for ChatDialog. */
@Mapper(componentModel = "spring")
public interface ChatDialogMapper {
    ChatDialogDto toDto(ChatDialog dialog);
}

