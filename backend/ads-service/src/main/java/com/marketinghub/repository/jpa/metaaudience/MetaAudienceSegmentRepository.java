package com.marketinghub.repository.jpa.metaaudience;

import com.marketinghub.metaaudience.MetaAudienceSegment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA das parcelas funcionais vinculadas às audiências Meta Ads. */
public interface MetaAudienceSegmentRepository extends JpaRepository<MetaAudienceSegment, Long> {
    /** Lista as parcelas funcionais de uma audiência. */
    List<MetaAudienceSegment> findByMetaAudienceIdOrderByUpdatedAtDesc(Long metaAudienceId);
}
