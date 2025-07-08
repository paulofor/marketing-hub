package com.marketinghub.successproduct.mapper;

import com.marketinghub.successproduct.SuccessProduct;
import com.marketinghub.successproduct.dto.SuccessProductDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link SuccessProduct}.
 */
@Mapper(componentModel = "spring")
public interface SuccessProductMapper {
    @Mapping(target = "instagramAccountId", source = "instagramAccount.id")
    SuccessProductDto toDto(SuccessProduct product);
}
