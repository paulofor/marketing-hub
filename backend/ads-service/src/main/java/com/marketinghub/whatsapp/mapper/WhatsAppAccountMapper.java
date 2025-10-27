package com.marketinghub.whatsapp.mapper;

import com.marketinghub.whatsapp.WhatsAppAccount;
import com.marketinghub.whatsapp.dto.WhatsAppAccountDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for WhatsAppAccount.
 */
@Mapper(componentModel = "spring")
public interface WhatsAppAccountMapper {
    WhatsAppAccountDto toDto(WhatsAppAccount account);
}
