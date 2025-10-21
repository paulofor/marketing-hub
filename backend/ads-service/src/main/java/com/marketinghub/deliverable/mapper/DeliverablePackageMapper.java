package com.marketinghub.deliverable.mapper;

import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.deliverable.dto.DeliverablePackageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper converting {@link DeliverablePackage} to DTOs.
 */
@Mapper(componentModel = "spring", uses = DeliverableMapper.class)
public interface DeliverablePackageMapper {
    @Mapping(target = "experimentId", source = "experiment.id")
    @Mapping(target = "experimentName", source = "experiment.name")
    DeliverablePackageDto toDto(DeliverablePackage entity);
}
