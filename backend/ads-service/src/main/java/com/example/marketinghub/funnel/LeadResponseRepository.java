package com.example.marketinghub.funnel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repository for {@link LeadResponse}. */
public interface LeadResponseRepository extends JpaRepository<LeadResponse, Long> {
    List<LeadResponse> findByFunnelStepId(java.util.UUID funnelStepId);
}
