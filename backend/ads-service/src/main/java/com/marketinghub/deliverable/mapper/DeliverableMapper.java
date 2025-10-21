package com.marketinghub.deliverable.mapper;

import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.dto.DeliverableDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper to convert {@link Deliverable} entities to DTOs.
 */
@Mapper(componentModel = "spring")
public interface DeliverableMapper {
    @Mapping(target = "nicheId", source = "niche.id")
    @Mapping(target = "nicheName", source = "niche.name")
    DeliverableDto toDto(Deliverable deliverable);
}
