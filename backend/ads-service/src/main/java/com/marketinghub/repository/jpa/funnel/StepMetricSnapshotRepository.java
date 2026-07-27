package com.marketinghub.repository.jpa.funnel;

import com.marketinghub.funnel.StepMetricSnapshot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link StepMetricSnapshot}. */
public interface StepMetricSnapshotRepository extends JpaRepository<StepMetricSnapshot, Long> {
  List<StepMetricSnapshot> findByFunnelStepIdOrderByCapturedAtDesc(java.util.UUID funnelStepId);
}
