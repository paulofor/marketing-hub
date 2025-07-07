package com.marketinghub.product.mapper;

import com.marketinghub.product.Product;
import com.marketinghub.product.dto.ProductDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link Product}.
 */
@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "instagramAccountId", source = "instagramAccount.id")
    ProductDto toDto(Product product);
}
