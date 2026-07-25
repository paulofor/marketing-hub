package com.marketinghub.repository.jpa.product;

import com.marketinghub.product.ProductVideoProviderAvatar;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir personagens de vídeo de produto por provider. */
public interface ProductVideoProviderAvatarRepository
    extends JpaRepository<ProductVideoProviderAvatar, Long> {

  /** Lista personagens de vídeo de um produto para seleção em renders futuros. */
  List<ProductVideoProviderAvatar> findByProductIdOrderByCreatedAtDesc(Long productId);

  /** Localiza o cadastro canônico de um provider para uma imagem fonte de produto. */
  Optional<ProductVideoProviderAvatar> findFirstByProductIdAndProviderIgnoreCaseAndSourceAssetId(
      Long productId, String provider, Long sourceAssetId);
}
