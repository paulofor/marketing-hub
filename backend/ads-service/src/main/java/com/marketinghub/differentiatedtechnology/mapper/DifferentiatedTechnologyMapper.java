package com.marketinghub.differentiatedtechnology.mapper;

import com.marketinghub.differentiatedtechnology.DifferentiatedTechnology;
import com.marketinghub.differentiatedtechnology.dto.DifferentiatedTechnologyDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DifferentiatedTechnologyMapper {
    DifferentiatedTechnologyDto toDto(DifferentiatedTechnology technology);
}
