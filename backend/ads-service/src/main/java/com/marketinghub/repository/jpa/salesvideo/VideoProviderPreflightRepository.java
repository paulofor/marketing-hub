package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.VideoProviderPreflight;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir preflights oficiais e dry runs de ciclos de vídeo. */
public interface VideoProviderPreflightRepository
    extends JpaRepository<VideoProviderPreflight, Long> {
  /** Localiza o preflight único de um ciclo. */
  Optional<VideoProviderPreflight> findByVideoProductionCycleId(Long cycleId);

  /** Lê e bloqueia a versão mais recente do preflight durante callback idempotente. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select preflight from VideoProviderPreflight preflight where preflight.videoProductionCycleId = :cycleId")
  Optional<VideoProviderPreflight> findByVideoProductionCycleIdForUpdate(
      @Param("cycleId") Long cycleId);

  /** Lista preflights ainda não processados na ordem em que foram solicitados. */
  List<VideoProviderPreflight> findByStatusOrderByCreatedAtAsc(String status);
}
