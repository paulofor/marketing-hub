package com.marketinghub.repository.jpa.metaaudience;

import com.marketinghub.metaaudience.ExperimentMetaAudience;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA dos vínculos entre experimentos e audiências Meta Ads por CNAE. */
public interface ExperimentMetaAudienceRepository extends JpaRepository<ExperimentMetaAudience, Long> {
    /** Busca vínculo existente para manter a operação idempotente por experimento, audiência e parcela. */
    Optional<ExperimentMetaAudience> findByExperimentIdAndMetaAudienceIdAndMetaAudienceSegmentId(
            Long experimentId, Long metaAudienceId, Long metaAudienceSegmentId);

    /** Lista as audiências CNAE vinculadas a um experimento. */
    List<ExperimentMetaAudience> findByExperimentIdOrderByUpdatedAtDesc(Long experimentId);

    /** Lista os vínculos de audiências CNAE de um nicho. */
    List<ExperimentMetaAudience> findByMarketNicheIdOrderByUpdatedAtDesc(Long nicheId);
}
