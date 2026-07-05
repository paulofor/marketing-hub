package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageEntity;
import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageStatusHistoryEntity;
import com.marketinghub.leadportal.repository.FlowSubmissionImagePackageStatusHistoryRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsabilidade: registrar o histórico funcional de status dos pacotes de imagem do Lead Portal. */
@Service
public class FlowSubmissionImagePackageStatusHistoryService {

    private static final ZoneId SAO_PAULO_ZONE = ZoneId.of("America/Sao_Paulo");

    private final FlowSubmissionImagePackageStatusHistoryRepository repository;

    /** Inicializa o serviço com o repositório de histórico de status. */
    public FlowSubmissionImagePackageStatusHistoryService(
            FlowSubmissionImagePackageStatusHistoryRepository repository) {
        this.repository = repository;
    }

    /** Registra uma mudança de status usando o enum canônico do pacote de imagem. */
    public void recordStatusChange(Long packageId, FlowSubmissionImagePackageEntity.Status status, String reason) {
        recordStatusChange(packageId, status == null ? null : status.name(), reason);
    }

    /** Registra uma mudança de status textual recebida de integrações de engajamento. */
    public void recordStatusChange(Long packageId, String status, String reason) {
        if (packageId == null || status == null) {
            return;
        }
        String normalizedReason = StringUtils.hasText(reason) ? reason.trim() : null;

        FlowSubmissionImagePackageStatusHistoryEntity history =
                new FlowSubmissionImagePackageStatusHistoryEntity();
        history.setPackageId(packageId);
        history.setStatus(status);
        history.setFailureReason(normalizedReason);
        history.setCreatedAt(LocalDateTime.now(SAO_PAULO_ZONE));

        repository.save(history);
    }
}
