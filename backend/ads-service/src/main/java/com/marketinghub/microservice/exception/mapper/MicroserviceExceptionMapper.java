package com.marketinghub.microservice.exception.mapper;

import com.marketinghub.microservice.exception.MicroserviceExceptionLog;
import com.marketinghub.microservice.exception.dto.MicroserviceExceptionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MicroserviceExceptionMapper {
    @Mapping(target = "microserviceId", source = "microservice.id")
    @Mapping(target = "microserviceName", source = "microservice.name")
    MicroserviceExceptionDto toDto(MicroserviceExceptionLog entity);
}
