package com.marketinghub.repository.jpa.funnel;

import com.marketinghub.funnel.LeadResponse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link LeadResponse}. */
public interface LeadResponseRepository extends JpaRepository<LeadResponse, Long> {
  List<LeadResponse> findByFunnelStepId(java.util.UUID funnelStepId);
}
