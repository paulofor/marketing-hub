package com.marketinghub.salesvideo.repository;

import com.marketinghub.salesvideo.LandingVideoSlot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositório das publicações de vídeo nas landings.
 */
public interface LandingVideoSlotRepository extends JpaRepository<LandingVideoSlot, Long> {
    @EntityGraph(attributePaths = {"landingPage", "profile", "asset", "posterAsset", "vttAsset"})
    Optional<LandingVideoSlot> findByLandingPageIdAndSlotName(Long landingPageId, String slotName);

    @EntityGraph(attributePaths = {"landingPage", "profile", "asset", "posterAsset", "vttAsset"})
    List<LandingVideoSlot> findByLandingPageIdAndTenantId(Long landingPageId, String tenantId);

    @Query("select case when count(s) > 0 then true else false end from LandingVideoSlot s " +
            "where (s.asset.id = :assetId or s.posterAsset.id = :assetId or s.vttAsset.id = :assetId)")
    boolean existsByAnyAssetReference(@Param("assetId") Long assetId);
}
