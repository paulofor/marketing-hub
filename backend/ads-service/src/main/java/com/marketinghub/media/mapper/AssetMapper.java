package com.marketinghub.media.mapper;

import com.marketinghub.media.Asset;
import com.marketinghub.media.dto.AssetDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for {@link Asset}.
 */
@Mapper(componentModel = "spring")
public interface AssetMapper {
    AssetDto toDto(Asset asset);
}
