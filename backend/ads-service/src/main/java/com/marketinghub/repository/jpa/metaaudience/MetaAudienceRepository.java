package com.marketinghub.repository.jpa.metaaudience;

import com.marketinghub.metaaudience.MetaAudience;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA das audiências Meta Ads controladas pelo backend. */
public interface MetaAudienceRepository extends JpaRepository<MetaAudience, Long> {
    /** Lista audiências prontas para sincronização pelo Facebook Ads Worker. */
    List<MetaAudience> findByEligibilityStatusOrderByUpdatedAtAsc(String status, Pageable pageable);

    /** Lista audiências planejadas para um nicho. */
    List<MetaAudience> findByMarketNicheIdOrderByUpdatedAtDesc(Long nicheId);
}
