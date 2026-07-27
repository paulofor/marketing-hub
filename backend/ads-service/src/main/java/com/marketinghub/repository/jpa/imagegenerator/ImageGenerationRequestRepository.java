package com.marketinghub.repository.jpa.imagegenerator;

import com.marketinghub.imagegenerator.ImageGenerationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir registros de auditoria do gerador manual de imagens. */
public interface ImageGenerationRequestRepository
    extends JpaRepository<ImageGenerationRequest, Long> {}
