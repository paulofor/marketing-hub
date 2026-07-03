package com.marketinghub.productai.delivery;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Responsabilidade: agrupar contratos internos da entrega paga de Produto IA. */
public final class ProductAiPaidDeliveryDtos {
    private ProductAiPaidDeliveryDtos() {
    }

    /** Contrato entregue ao worker para execução de uma entrega paga pendente. */
    public record PendingResponse(
            String idJob,
            Long purchaseId,
            Long packageId,
            Long experimentId,
            String pipelineCode,
            String stageCode,
            Instant requestedAt,
            Map<String, Object> buyer,
            Map<String, Object> personalizationInput,
            Map<String, Object> experiment,
            Map<String, Object> promptSchemaTemplate) {}

    /** Payload usado pelo payments-service para notificar compra aprovada. */
    public record PurchaseApprovedRequest(Long purchaseId, Long packageId) {}

    /** Resposta de enfileiramento da entrega paga. */
    public record EnqueueResponse(String idJob, String status) {}

    /** Request bruto que o worker enviará ao provedor externo. */
    public record ReceiveRequestRequest(
            String prompt,
            String schemaJson,
            String requestBodyJson,
            String openAiModel,
            String serviceTier) {}

    /** Resultado ou falha reportado pelo worker após a execução. */
    public record ReceiveResponseRequest(
            String responseBodyJson,
            String functionalOutputJson,
            String artifactUrl,
            String openAiModel,
            String serviceTier,
            Integer inputTokens,
            Integer outputTokens,
            BigDecimal estimatedCostUsd,
            String errorMessage) {}
}
