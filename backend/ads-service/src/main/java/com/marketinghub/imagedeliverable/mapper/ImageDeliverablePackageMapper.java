package com.marketinghub.imagedeliverable.mapper;

import com.marketinghub.imagedeliverable.ImageDeliverablePackage;
import com.marketinghub.imagedeliverable.dto.ImageDeliverablePackageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps {@link ImageDeliverablePackage} to DTOs.
 */
@Mapper(componentModel = "spring", uses = ImageDeliverableItemMapper.class)
public interface ImageDeliverablePackageMapper {
    @Mapping(target = "leadId", source = "lead.id")
    @Mapping(target = "inputAssetId", source = "inputAsset.id")
    @Mapping(target = "inputAssetUrl", source = "inputAsset.url")
    ImageDeliverablePackageDto toDto(ImageDeliverablePackage entity);
}
