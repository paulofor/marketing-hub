package com.marketinghub.microservice.mapper;

import com.marketinghub.microservice.Microservice;
import com.marketinghub.microservice.dto.MicroserviceDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for {@link Microservice}.
 */
@Mapper(componentModel = "spring")
public interface MicroserviceMapper {
    MicroserviceDto toDto(Microservice microservice);
}
