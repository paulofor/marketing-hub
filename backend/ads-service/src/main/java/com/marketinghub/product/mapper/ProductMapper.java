package com.marketinghub.product.mapper;

import com.marketinghub.product.Product;
import com.marketinghub.product.dto.ProductDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Responsabilidade: converter entidade de produto comercial em DTO de frontend.
 */
@Mapper(componentModel = "spring")
public interface ProductMapper {
    /** Converte a entidade de produto para o contrato de leitura da API. */
    @Mapping(target = "instagramAccountId", source = "instagramAccount.id")
    @Mapping(target = "marketNicheId", source = "marketNiche.id")
    ProductDto toDto(Product product);
}
