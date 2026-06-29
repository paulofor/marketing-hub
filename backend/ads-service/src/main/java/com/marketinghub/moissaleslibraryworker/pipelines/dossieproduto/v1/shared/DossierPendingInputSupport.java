package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.shared;

import com.marketinghub.repository.jpa.mois.dossieproduto.PipelineDossieProdutoRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Monta entradas pendentes ricas para as etapas do dossiê de produto MOIS v1. */
public final class DossierPendingInputSupport {
    private static final String INTAKE_STAGE_CODE = "intake";
    private static final String PIPELINE_VERSION = "v1";
    private static final String STATUS_STARTED = "INICIADO";

    /** Impede instanciação de classe utilitária. */
    private DossierPendingInputSupport() {}

    /** Monta o input operacional com histórico funcional já persistido no backend. */
    public static Map<String, Object> inputFor(
            Long pageId,
            String jobId,
            String stageCode,
            String nextStageCode,
            PipelineDossieProdutoRepository repository) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("jobId", jobId);
        input.put("productKey", String.valueOf(pageId));
        input.put("pageId", pageId);
        input.put("stageCode", stageCode);
        input.put("status", "INICIADO");
        input.put("nextStageCode", nextStageCode == null ? "" : nextStageCode);
        List<PipelineDossieProduto> currentAudits = currentFlowAudits(pageId, repository);
        input.put("previousStages", previousStages(currentAudits, stageCode));
        input.put("previousStageResponses", previousStageResponses(currentAudits, stageCode));
        return input;
    }

    /** Recupera somente auditorias da execução atual iniciada pelo último intake do produto. */
    private static List<PipelineDossieProduto> currentFlowAudits(
            Long pageId,
            PipelineDossieProdutoRepository repository) {
        String productKey = String.valueOf(pageId);
        Instant currentFlowStartedAt = repository
                .findTopByIdExternoAndCodigoEtapaAndStatusOrderByDataHoraDescIdDesc(
                        productKey, INTAKE_STAGE_CODE, STATUS_STARTED)
                .map(PipelineDossieProduto::getDataHora)
                .orElse(null);
        if (currentFlowStartedAt == null) {
            return repository.findByIdExternoAndVersaoPipelineOrderByDataHoraAscIdAsc(productKey, PIPELINE_VERSION);
        }
        return repository.findByIdExternoAndVersaoPipelineAndDataHoraGreaterThanEqualOrderByDataHoraAscIdAsc(
                productKey, PIPELINE_VERSION, currentFlowStartedAt);
    }

    /** Recupera as últimas respostas por etapa anterior para orientar a próxima etapa. */
    private static Map<String, Object> previousStages(List<PipelineDossieProduto> currentAudits, String currentStageCode) {
        Map<String, Object> previousStages = new LinkedHashMap<>();
        currentAudits.forEach(audit -> {
            if (!currentStageCode.equals(audit.getCodigoEtapa()) && hasText(audit.getResponse())) {
                previousStages.put(audit.getCodigoEtapa(), Map.of(
                        "stageCode", audit.getCodigoEtapa(),
                        "status", audit.getStatus(),
                        "response", audit.getResponse(),
                        "occurredAt", String.valueOf(audit.getDataHora())));
            }
        });
        return previousStages;
    }

    /** Recupera respostas funcionais anteriores em ordem de auditoria para consumo simples pelo worker. */
    private static List<String> previousStageResponses(List<PipelineDossieProduto> currentAudits, String currentStageCode) {
        return currentAudits.stream()
                .filter(audit -> !currentStageCode.equals(audit.getCodigoEtapa()))
                .map(PipelineDossieProduto::getResponse)
                .filter(DossierPendingInputSupport::hasText)
                .toList();
    }

    /** Verifica se um texto contém conteúdo útil para compor a entrada da etapa. */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
