package com.marketinghub.repository.jpa.funnel;

import com.marketinghub.funnel.StepMetricSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repository for {@link StepMetricSnapshot}. */
public interface StepMetricSnapshotRepository extends JpaRepository<StepMetricSnapshot, Long> {
    List<StepMetricSnapshot> findByFunnelStepIdOrderByCapturedAtDesc(java.util.UUID funnelStepId);
}
