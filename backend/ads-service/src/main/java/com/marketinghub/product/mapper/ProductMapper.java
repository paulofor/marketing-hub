package com.marketinghub.product.mapper;

import com.marketinghub.product.Product;
import com.marketinghub.product.dto.ProductDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for {@link Product}.
 */
@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductDto toDto(Product product);
}
