package com.marketinghub.nichocnae.evidencelevelgate;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** Cliente HTTP do executor para ler pendências e escrever resultados E0-E5 exclusivamente via backend. */
@Component
public class EvidenceLevelGateBackendClient {
    private final RestTemplate restTemplate;
    private final String backendBaseUrl;

    /** Inicializa o cliente com URL base configurável do backend. */
    public EvidenceLevelGateBackendClient(RestTemplateBuilder builder, @Value("${backend.base-url:http://191.252.181.168}") String backendBaseUrl) {
        this.restTemplate = builder.build();
        this.backendBaseUrl = backendBaseUrl;
    }

    /** Lê do backend as pendências da etapa onze. */
    public List<EvidenceLevelGatePending> listPending() {
        EvidenceLevelGatePending[] response = restTemplate.getForObject(backendBaseUrl + "/api/internal/oprm/nichocnae/evidence-level-gate/stage-executions/pending", EvidenceLevelGatePending[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }

    /** Envia ao backend a decisão E0-E5 calculada pelo executor. */
    public void complete(EvidenceLevelGatePending pending, EvidenceLevelGateDecision decision) {
        restTemplate.postForObject(
                backendBaseUrl + "/api/internal/oprm/nichocnae/evidence-level-gate/stage-executions/" + pending.researchCycleId() + "/complete",
                new EvidenceLevelGateCompletionRequest(pending.routineCardId(), decision.evidenceLevel(), decision.gateStatus(), decision.approvedForMaterialization(), decision.confidenceScore(), decision.rejectionReasons(), decision.nextMovements(), "oprmEvidenceLevelGate"),
                Object.class);
    }

    /** Registra falha técnica no backend para manter rastreabilidade sem decisão de mercado. */
    public void fail(EvidenceLevelGatePending pending, RuntimeException ex) {
        restTemplate.postForObject(
                backendBaseUrl + "/api/internal/oprm/nichocnae/evidence-level-gate/stage-executions/" + pending.researchCycleId() + "/fail",
                java.util.Map.of("errorMessage", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()),
                Object.class);
    }
}
