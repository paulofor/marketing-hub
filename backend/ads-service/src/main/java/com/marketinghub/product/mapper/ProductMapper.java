package com.marketinghub.product.mapper;

import com.marketinghub.product.Product;
import com.marketinghub.product.dto.ProductDto;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Responsabilidade: converter entidade de produto comercial em DTO de frontend. */
@Mapper(componentModel = "spring")
public interface ProductMapper {
  /** Converte a entidade de produto para o contrato de leitura da API. */
  @Mapping(target = "instagramAccountId", source = "instagramAccount.id")
  @Mapping(target = "marketNicheId", source = "marketNiche.id")
  @Mapping(target = "videoSeedImageAssetId", source = "videoSeedImageAsset.id")
  @Mapping(target = "aliases", expression = "java(toSortedAliases(product))")
  ProductDto toDto(Product product);

  /** Ordena os apelidos internos para manter respostas estáveis para tela e agentes. */
  default List<String> toSortedAliases(Product product) {
    if (product.getAliases() == null) {
      return List.of();
    }
    return product.getAliases().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
  }
}
