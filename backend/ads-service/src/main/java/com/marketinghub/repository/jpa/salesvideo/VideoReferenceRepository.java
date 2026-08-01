package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.VideoReference;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório canônico dos vídeos externos enviados para análise comercial. */
public interface VideoReferenceRepository extends JpaRepository<VideoReference, Long> {
  /** Lista vídeos de referência do tenant em ordem operacional de atualização. */
  List<VideoReference> findByTenantIdOrderByUpdatedAtDesc(String tenantId);
}
