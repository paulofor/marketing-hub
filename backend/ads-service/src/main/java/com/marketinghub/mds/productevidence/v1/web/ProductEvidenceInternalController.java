package com.marketinghub.mds.productevidence.v1.web;

import com.marketinghub.mds.productevidence.v1.service.ProductEvidenceStageCallbackRequest;
import com.marketinghub.mds.productevidence.v1.service.ProductEvidenceStagePendingResponse;
import com.marketinghub.mds.productevidence.v1.service.ProductEvidenceWorkflowService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor contratos internos do pipeline MDS product-evidence v1 para o worker científico. */
@RestController
@RequestMapping("/api/internal/scientific-research/product-evidence/v1")
public class ProductEvidenceInternalController {
    private final ProductEvidenceWorkflowService workflowService;

    /** Recebe o serviço de orquestração das etapas científicas. */
    public ProductEvidenceInternalController(ProductEvidenceWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /** Lista pendências canônicas de uma etapa para consumo pelo scientific-research-worker. */
    @GetMapping("/{stageCode}/stage-executions/pending")
    public List<ProductEvidenceStagePendingResponse> pending(
            @PathVariable String stageCode,
            @RequestParam(defaultValue = "5") int limit) {
        return workflowService.listPending(stageCode, limit);
    }

    /** Recebe o callback de resultado ou falha de uma execução científica. */
    @PostMapping("/{stageCode}/stage-executions/{executionId}/callback")
    public void callback(
            @PathVariable String stageCode,
            @PathVariable Long executionId,
            @RequestBody ProductEvidenceStageCallbackRequest request) {
        workflowService.receiveCallback(stageCode, executionId, request);
    }
}
