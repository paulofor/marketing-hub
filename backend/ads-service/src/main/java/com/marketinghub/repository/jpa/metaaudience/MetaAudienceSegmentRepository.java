package com.marketinghub.repository.jpa.metaaudience;

import com.marketinghub.metaaudience.MetaAudienceSegment;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA das parcelas funcionais vinculadas às audiências Meta Ads. */
public interface MetaAudienceSegmentRepository extends JpaRepository<MetaAudienceSegment, Long> {}
