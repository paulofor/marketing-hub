package com.marketinghub.repository.jpa.imagegeneration;

import com.marketinghub.imagegeneration.ImageGenerationQuality;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de ImageGenerationQuality.
 */
public interface ImageGenerationQualityRepository extends JpaRepository<ImageGenerationQuality, Long> {

    Optional<ImageGenerationQuality> findByModelIdAndId(Long modelId, Long id);
}
