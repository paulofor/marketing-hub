package com.marketinghub.imagegeneration.repository;

import com.marketinghub.imagegeneration.ImageGenerationQuality;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageGenerationQualityRepository extends JpaRepository<ImageGenerationQuality, Long> {

    Optional<ImageGenerationQuality> findByModelIdAndId(Long modelId, Long id);
}
