package com.marketinghub.repository.jpa.experiment.report;

import com.marketinghub.experiment.report.ExperimentReportRequest;
import com.marketinghub.experiment.report.ExperimentReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Repositório das solicitações de relatório de experimento.
 */
public interface ExperimentReportRequestRepository extends JpaRepository<ExperimentReportRequest, Long> {
    List<ExperimentReportRequest> findTop5ByExperimentIdOrderByRequestedAtDesc(Long experimentId);

    List<ExperimentReportRequest> findByExperimentIdOrderByRequestedAtDesc(Long experimentId);

    boolean existsByExperimentIdAndStatusIn(Long experimentId, Collection<ExperimentReportStatus> statuses);

    List<ExperimentReportRequest> findByStatusInOrderByRequestedAtAsc(Collection<ExperimentReportStatus> statuses);

    List<ExperimentReportRequest> findAllByOrderByRequestedAtDesc();
}
