package com.marketinghub.repository.jpa.product;

import com.marketinghub.product.ProductVideoImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: acessar as imagens vinculadas à galeria de vídeos de produto. */
public interface ProductVideoImageRepository extends JpaRepository<ProductVideoImage, Long> {
  /** Lista as imagens de vídeo de um produto começando pelas mais recentes. */
  List<ProductVideoImage> findByProductIdOrderByCreatedAtDesc(Long productId);

  /** Busca uma imagem da galeria pelo produto e pelo asset vinculado. */
  Optional<ProductVideoImage> findFirstByProductIdAndAssetId(Long productId, Long assetId);
}
