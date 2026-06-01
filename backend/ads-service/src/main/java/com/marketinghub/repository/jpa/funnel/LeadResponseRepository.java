package com.marketinghub.repository.jpa.funnel;

import com.marketinghub.funnel.LeadResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repository for {@link LeadResponse}. */
public interface LeadResponseRepository extends JpaRepository<LeadResponse, Long> {
    List<LeadResponse> findByFunnelStepId(java.util.UUID funnelStepId);
}
