package com.marketinghub.imagegeneration.repository;

import com.marketinghub.imagegeneration.ImageGenerationPrice;
import com.marketinghub.imagegeneration.ImageOrientation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageGenerationPriceRepository extends JpaRepository<ImageGenerationPrice, Long> {

    List<ImageGenerationPrice> findByQualityId(Long qualityId);

    Optional<ImageGenerationPrice> findFirstByQualityIdAndOrientationOrderByPreferredDescWidthDesc(
            Long qualityId, ImageOrientation orientation);

    Optional<ImageGenerationPrice> findFirstByQualityIdOrderByPreferredDescWidthDesc(Long qualityId);
}
