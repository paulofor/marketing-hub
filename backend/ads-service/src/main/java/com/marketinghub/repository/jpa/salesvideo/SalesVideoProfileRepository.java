package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.SalesVideoProfile;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso aos perfis de vídeo de venda. */
public interface SalesVideoProfileRepository extends JpaRepository<SalesVideoProfile, Long> {
  @EntityGraph(attributePaths = {"product", "landingPage"})
  List<SalesVideoProfile> findByProductIdAndTenantIdOrderByCreatedAtDesc(
      Long productId, String tenantId);
}
