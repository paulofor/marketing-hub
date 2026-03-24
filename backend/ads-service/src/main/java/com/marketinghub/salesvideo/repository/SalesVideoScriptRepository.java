package com.marketinghub.salesvideo.repository;

import com.marketinghub.salesvideo.SalesVideoScript;
import com.marketinghub.salesvideo.SalesVideoScriptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistência das versões de script do Avatar Sales Video.
 */
public interface SalesVideoScriptRepository extends JpaRepository<SalesVideoScript, Long> {
    Optional<SalesVideoScript> findFirstByProfileIdOrderByVersionDesc(Long profileId);

    Optional<SalesVideoScript> findFirstByProfileIdAndStatusOrderByVersionDesc(Long profileId, SalesVideoScriptStatus status);
}
