package com.marketinghub.repository.jpa.imagegeneration;

import com.marketinghub.imagegeneration.ImageGenerationPrice;
import com.marketinghub.imagegeneration.ImageOrientation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de ImageGenerationPrice.
 */
public interface ImageGenerationPriceRepository extends JpaRepository<ImageGenerationPrice, Long> {

    List<ImageGenerationPrice> findByQualityId(Long qualityId);

    Optional<ImageGenerationPrice> findFirstByQualityIdAndOrientationOrderByPreferredDescWidthDesc(
            Long qualityId, ImageOrientation orientation);

    Optional<ImageGenerationPrice> findFirstByQualityIdOrderByPreferredDescWidthDesc(Long qualityId);
}
