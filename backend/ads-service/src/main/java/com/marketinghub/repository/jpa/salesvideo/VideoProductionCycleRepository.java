package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.VideoProductionCycle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: acessar ciclos governados de produção de vídeo. */
public interface VideoProductionCycleRepository extends JpaRepository<VideoProductionCycle, Long> {
  /** Lista ciclos de um projeto do mais recente para o mais antigo. */
  List<VideoProductionCycle> findByVideoProjectIdOrderByCreatedAtDesc(Long videoProjectId);

  /** Lista ciclos aguardando decisão financeira. */
  List<VideoProductionCycle> findByStatusOrderByCreatedAtAsc(String status);
}
