package com.marketinghub.repository.jpa.socialdistribution;

import com.marketinghub.socialdistribution.SocialPlatform;
import com.marketinghub.socialdistribution.SocialVideoPublication;
import com.marketinghub.socialdistribution.SocialVideoPublicationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir publicações orgânicas de vídeo por produto e rede. */
public interface SocialVideoPublicationRepository
    extends JpaRepository<SocialVideoPublication, Long> {
  /** Lista publicações recentes de todos os produtos. */
  List<SocialVideoPublication> findTop100ByOrderByCreatedAtDesc();

  /** Lista publicações recentes de um produto. */
  List<SocialVideoPublication> findTop100ByProductIdOrderByCreatedAtDesc(Long productId);

  /** Lista publicações recentes de uma rede. */
  List<SocialVideoPublication> findTop100ByPlatformOrderByCreatedAtDesc(SocialPlatform platform);

  /** Lista publicações recentes de um produto em uma rede. */
  List<SocialVideoPublication> findTop100ByProductIdAndPlatformOrderByCreatedAtDesc(
      Long productId, SocialPlatform platform);

  /** Lista publicações pendentes para consumo futuro por worker externo. */
  List<SocialVideoPublication> findTop50ByStatusOrderByQueuedAtAsc(
      SocialVideoPublicationStatus status);
}
