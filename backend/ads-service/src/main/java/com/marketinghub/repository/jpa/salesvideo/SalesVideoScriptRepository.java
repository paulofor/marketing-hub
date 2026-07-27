package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.SalesVideoScript;
import com.marketinghub.salesvideo.SalesVideoScriptStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistência das versões de script do Avatar Sales Video. */
public interface SalesVideoScriptRepository extends JpaRepository<SalesVideoScript, Long> {
  Optional<SalesVideoScript> findFirstByProfileIdOrderByVersionDesc(Long profileId);

  Optional<SalesVideoScript> findFirstByProfileIdAndStatusOrderByVersionDesc(
      Long profileId, SalesVideoScriptStatus status);

  List<SalesVideoScript> findByProfileIdOrderByVersionDesc(Long profileId);
}
