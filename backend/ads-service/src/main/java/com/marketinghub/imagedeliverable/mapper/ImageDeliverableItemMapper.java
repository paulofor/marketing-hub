package com.marketinghub.imagedeliverable.mapper;

import com.marketinghub.imagedeliverable.ImageDeliverableItem;
import com.marketinghub.imagedeliverable.dto.ImageDeliverableItemDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper converting generated image items to DTOs.
 */
@Mapper(componentModel = "spring")
public interface ImageDeliverableItemMapper {
    @Mapping(target = "assetId", source = "asset.id")
    @Mapping(target = "assetUrl", source = "asset.url")
    ImageDeliverableItemDto toDto(ImageDeliverableItem item);
}
