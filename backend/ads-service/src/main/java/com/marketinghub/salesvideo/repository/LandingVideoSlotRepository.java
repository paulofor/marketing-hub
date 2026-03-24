package com.marketinghub.salesvideo.repository;

import com.marketinghub.salesvideo.LandingVideoSlot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório das publicações de vídeo nas landings.
 */
public interface LandingVideoSlotRepository extends JpaRepository<LandingVideoSlot, Long> {
    @EntityGraph(attributePaths = {"landingPage", "profile", "asset", "posterAsset", "vttAsset"})
    Optional<LandingVideoSlot> findByLandingPageIdAndSlotName(Long landingPageId, String slotName);

    @EntityGraph(attributePaths = {"landingPage", "profile", "asset", "posterAsset", "vttAsset"})
    List<LandingVideoSlot> findByLandingPageId(Long landingPageId);
}
