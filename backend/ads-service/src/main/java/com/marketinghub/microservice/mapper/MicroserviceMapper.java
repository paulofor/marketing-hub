package com.marketinghub.microservice.mapper;

import com.marketinghub.microservice.Microservice;
import com.marketinghub.microservice.dto.MicroserviceDto;
import com.marketinghub.microservice.exception.dto.MicroserviceExceptionSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link Microservice}.
 */
@Mapper(componentModel = "spring")
public interface MicroserviceMapper {
    @Mapping(target = "lastExceptionAt", source = "summary.lastOccurredAt")
    @Mapping(target = "lastExceptionMessage", source = "summary.lastMessage")
    @Mapping(target = "lastExceptionSeverity", source = "summary.lastSeverity")
    @Mapping(target = "exceptionCount", source = "summary.totalCount")
    MicroserviceDto toDto(Microservice microservice, MicroserviceExceptionSummary summary);

    default MicroserviceDto toDto(Microservice microservice) {
        return toDto(microservice, null);
    }
}
