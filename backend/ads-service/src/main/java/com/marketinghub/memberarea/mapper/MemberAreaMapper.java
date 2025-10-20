package com.marketinghub.memberarea.mapper;

import com.marketinghub.memberarea.MemberArea;
import com.marketinghub.memberarea.dto.MemberAreaDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link MemberArea}.
 */
@Mapper(componentModel = "spring")
public interface MemberAreaMapper {
    @Mapping(target = "productId", source = "product.id")
    MemberAreaDto toDto(MemberArea memberArea);
}
