package com.marketinghub.whatsapp.mapper;

import com.marketinghub.whatsapp.WhatsAppMessage;
import com.marketinghub.whatsapp.dto.WhatsAppMessageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for WhatsAppMessage.
 */
@Mapper(componentModel = "spring")
public interface WhatsAppMessageMapper {
    @Mapping(source = "account.id", target = "accountId")
    WhatsAppMessageDto toDto(WhatsAppMessage message);
}
